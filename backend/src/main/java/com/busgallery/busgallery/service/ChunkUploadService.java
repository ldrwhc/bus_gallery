package com.busgallery.busgallery.service;

import com.busgallery.busgallery.config.UploadSecurityProperties;
import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChunkUploadService {

    private static final String KEY_PREFIX = "busgallery:upload:chunk:";
    private static final String META_SUFFIX = ":meta";
    private static final String PARTS_SUFFIX = ":parts";
    private static final long MIN_CHUNK_SIZE = 256L * 1024L;

    private final StringRedisTemplate redisTemplate;
    private final UploadSecurityProperties uploadSecurityProperties;
    private final ConcurrentHashMap<String, Object> uploadLocks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalSessionState> localSessionStore = new ConcurrentHashMap<>();
    private final AtomicLong redisWriteMutedUntilEpochMs = new AtomicLong(0L);

    public ChunkProgress init(Long userId,
                              String fileName,
                              String contentType,
                              long totalSize,
                              Long requestedChunkSize,
                              Integer requestedTotalChunks) {
        if (userId == null) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "Please login first");
        }
        if (totalSize <= 0) {
            throw new BizException(ErrorCode.INVALID_PARAM, "totalSize must be greater than 0");
        }
        if (totalSize > uploadSecurityProperties.getMaxFileBytes()) {
            throw new BizException(ErrorCode.INVALID_PARAM, "Upload file is too large");
        }
        String normalizedContentType = normalizeMimeType(contentType);
        if (!uploadSecurityProperties.getAllowedMimeTypes().contains(normalizedContentType)) {
            throw new BizException(ErrorCode.INVALID_PARAM, "Only JPG/PNG images are supported");
        }

        long chunkSize = normalizeChunkSize(requestedChunkSize);
        int calculatedChunks = (int) ((totalSize + chunkSize - 1) / chunkSize);
        if (calculatedChunks <= 0 || calculatedChunks > uploadSecurityProperties.getChunkMaxParts()) {
            throw new BizException(ErrorCode.INVALID_PARAM, "Too many upload chunks");
        }
        if (requestedTotalChunks != null && requestedTotalChunks > 0 && requestedTotalChunks != calculatedChunks) {
            throw new BizException(ErrorCode.INVALID_PARAM, "totalChunks does not match file size and chunk size");
        }

        String uploadId = UUID.randomUUID().toString().replace("-", "");
        ChunkSession session = new ChunkSession(
                uploadId,
                userId,
                sanitizeFileName(fileName),
                normalizedContentType,
                totalSize,
                chunkSize,
                calculatedChunks
        );
        saveLocalSession(session);

        Map<String, String> meta = new HashMap<>();
        meta.put("uploadId", session.uploadId());
        meta.put("userId", String.valueOf(session.userId()));
        meta.put("fileName", session.fileName());
        meta.put("contentType", session.contentType());
        meta.put("totalSize", String.valueOf(session.totalSize()));
        meta.put("chunkSize", String.valueOf(session.chunkSize()));
        meta.put("totalChunks", String.valueOf(session.totalChunks()));
        meta.put("createdAt", String.valueOf(Instant.now().getEpochSecond()));
        meta.put("status", "UPLOADING");
        safeRedisWrite(() -> {
            redisTemplate.opsForHash().putAll(metaKey(uploadId), meta);
            redisTemplate.expire(metaKey(uploadId), ttl());
            redisTemplate.expire(partsKey(uploadId), ttl());
        }, uploadId, "init-session");

        ensureUploadDir(uploadId);

        return new ChunkProgress(uploadId, chunkSize, calculatedChunks, 0, 0L, 0, false);
    }

    public ChunkProgress uploadPart(String uploadId, Long userId, int index, MultipartFile part) {
        if (part == null || part.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_PARAM, "chunk file is required");
        }
        ChunkSession session = requireSession(uploadId, userId);
        if (index < 0 || index >= session.totalChunks()) {
            throw new BizException(ErrorCode.INVALID_PARAM, "Invalid chunk index");
        }

        long chunkSize = part.getSize();
        long expectedMax = expectedChunkMaxSize(session, index);
        if (chunkSize <= 0 || chunkSize > expectedMax) {
            throw new BizException(ErrorCode.INVALID_PARAM, "Invalid chunk size");
        }

        Object lock = lockFor(uploadId);
        synchronized (lock) {
            Path partPath = partFile(uploadId, index);
            if (!Files.exists(partPath)) {
                writePart(part, partPath);
            }
            touch(uploadId, session);
            return progressInternal(uploadId, userId);
        }
    }

    public ChunkProgress progress(String uploadId, Long userId) {
        return progressInternal(uploadId, userId);
    }

    public CompletedChunkFile complete(String uploadId, Long userId) {
        ChunkSession session = requireSession(uploadId, userId);
        Object lock = lockFor(uploadId);
        synchronized (lock) {
            PartStats stats = scanPartStats(uploadId, session);
            if (stats.uploadedChunks() < session.totalChunks()) {
                throw new BizException(ErrorCode.INVALID_PARAM, "Upload is not complete yet");
            }
            Path mergedPath = mergedFile(uploadId);
            ensureUploadDir(uploadId);
            try (var output = Files.newOutputStream(mergedPath)) {
                for (int index = 0; index < session.totalChunks(); index++) {
                    Path partPath = partFile(uploadId, index);
                    if (!Files.exists(partPath)) {
                        throw new BizException(ErrorCode.INVALID_PARAM, "Missing upload chunk: " + index);
                    }
                    Files.copy(partPath, output);
                }
            } catch (IOException ex) {
                throw new BizException(ErrorCode.STORAGE_ERROR, "Merge upload chunks failed");
            }

            try {
                long mergedSize = Files.size(mergedPath);
                if (mergedSize != session.totalSize()) {
                    throw new BizException(ErrorCode.INVALID_PARAM, "Merged file size mismatch");
                }
            } catch (IOException ex) {
                throw new BizException(ErrorCode.STORAGE_ERROR, "Read merged file failed");
            }

            safeRedisWrite(() -> redisTemplate.opsForHash().put(metaKey(uploadId), "status", "MERGED"),
                    uploadId, "complete-session");
            touch(uploadId, session);
            return new CompletedChunkFile(new TempFileMultipartFile(
                    mergedPath,
                    session.fileName(),
                    session.contentType()
            ));
        }
    }

    public void cancel(String uploadId, Long userId) {
        ChunkSession session = tryReadSession(uploadId);
        if (session != null && userId != null && !userId.equals(session.userId())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "You are not allowed to cancel this upload");
        }
        cleanup(uploadId);
    }

    public void cleanup(String uploadId) {
        if (!StringUtils.hasText(uploadId)) {
            return;
        }
        localSessionStore.remove(uploadId);
        safeRedisWrite(() -> {
            redisTemplate.delete(metaKey(uploadId));
            redisTemplate.delete(partsKey(uploadId));
        }, uploadId, "cleanup-session");
        deleteUploadDir(uploadId);
        uploadLocks.remove(uploadId);
    }

    private ChunkProgress progressInternal(String uploadId, Long userId) {
        ChunkSession session = requireSession(uploadId, userId);
        PartStats stats = scanPartStats(uploadId, session);
        long uploadedChunks = stats.uploadedChunks();
        long uploadedBytes = stats.uploadedBytes();
        int percent = session.totalSize() <= 0
                ? 0
                : (int) Math.min(100, Math.round(uploadedBytes * 100.0d / session.totalSize()));
        boolean completed = uploadedChunks >= session.totalChunks() && uploadedBytes >= session.totalSize();
        touch(uploadId, session);
        return new ChunkProgress(
                uploadId,
                session.chunkSize(),
                session.totalChunks(),
                (int) uploadedChunks,
                uploadedBytes,
                percent,
                completed
        );
    }

    private ChunkSession requireSession(String uploadId, Long userId) {
        ChunkSession session = tryReadSession(uploadId);
        if (session == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Upload session not found or expired");
        }
        if (userId == null || !userId.equals(session.userId())) {
            throw new BizException(ErrorCode.UNAUTHORIZED, "You are not allowed to access this upload session");
        }
        return session;
    }

    private ChunkSession tryReadSession(String uploadId) {
        if (!StringUtils.hasText(uploadId)) {
            return null;
        }
        ChunkSession localSession = readLocalSession(uploadId);
        if (localSession != null) {
            return localSession;
        }
        Map<Object, Object> map;
        try {
            map = redisTemplate.opsForHash().entries(metaKey(uploadId));
        } catch (Exception ex) {
            if (isRedisMisconf(ex)) {
                muteRedisWritesTemporarily(uploadId, "read-session");
                log.warn("Redis read skipped due to MISCONF, uploadId={}, reason={}", uploadId, rootMessage(ex));
            } else {
                log.warn("Read chunk session from redis failed, uploadId={}, reason={}", uploadId, rootMessage(ex));
            }
            return null;
        }
        if (map == null || map.isEmpty()) {
            return null;
        }
        Long userId = readLong(map.get("userId"));
        String fileName = readString(map.get("fileName"), "upload.jpg");
        String contentType = readString(map.get("contentType"), "application/octet-stream");
        Long totalSizeValue = readLong(map.get("totalSize"));
        Long chunkSizeValue = readLong(map.get("chunkSize"));
        Long totalChunksValue = readLong(map.get("totalChunks"));
        if (userId == null
                || totalSizeValue == null
                || chunkSizeValue == null
                || totalChunksValue == null
                || totalSizeValue <= 0
                || chunkSizeValue <= 0
                || totalChunksValue <= 0) {
            return null;
        }
        long totalSize = totalSizeValue;
        long chunkSize = chunkSizeValue;
        int totalChunks = totalChunksValue.intValue();
        ChunkSession session = new ChunkSession(uploadId, userId, fileName, contentType, totalSize, chunkSize, totalChunks);
        saveLocalSession(session);
        return session;
    }

    private long expectedChunkMaxSize(ChunkSession session, int index) {
        if (index < session.totalChunks() - 1) {
            return session.chunkSize();
        }
        long consumed = session.chunkSize() * (session.totalChunks() - 1L);
        long remain = session.totalSize() - consumed;
        return Math.max(remain, 1L);
    }

    private long normalizeChunkSize(Long requestedChunkSize) {
        long configured = Math.max(MIN_CHUNK_SIZE, uploadSecurityProperties.getChunkSizeBytes());
        long chunk = requestedChunkSize != null && requestedChunkSize > 0 ? requestedChunkSize : configured;
        chunk = Math.max(MIN_CHUNK_SIZE, chunk);
        chunk = Math.min(chunk, uploadSecurityProperties.getMaxFileBytes());
        return chunk;
    }

    private String normalizeMimeType(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return "";
        }
        String normalized = contentType.trim().toLowerCase();
        if ("image/jpg".equals(normalized)) {
            return "image/jpeg";
        }
        return normalized;
    }

    private String sanitizeFileName(String fileName) {
        String fallback = "upload.jpg";
        if (!StringUtils.hasText(fileName)) {
            return fallback;
        }
        String normalized = fileName
                .replace("\\", "/")
                .replaceAll(".*/", "")
                .replace("..", "");
        return StringUtils.hasText(normalized) ? normalized : fallback;
    }

    private String metaKey(String uploadId) {
        return KEY_PREFIX + uploadId + META_SUFFIX;
    }

    private String partsKey(String uploadId) {
        return KEY_PREFIX + uploadId + PARTS_SUFFIX;
    }

    private Duration ttl() {
        int seconds = Math.max(uploadSecurityProperties.getChunkSessionTtlSeconds(), 300);
        return Duration.ofSeconds(seconds);
    }

    private void touch(String uploadId, ChunkSession session) {
        saveLocalSession(session);
        Duration ttl = ttl();
        safeRedisWrite(() -> {
            redisTemplate.expire(metaKey(uploadId), ttl);
            redisTemplate.expire(partsKey(uploadId), ttl);
        }, uploadId, "touch-session");
    }

    private Object lockFor(String uploadId) {
        return uploadLocks.computeIfAbsent(uploadId, key -> new Object());
    }

    private Path ensureUploadDir(String uploadId) {
        Path dir = uploadDir(uploadId);
        try {
            Files.createDirectories(dir);
        } catch (IOException ex) {
            throw new BizException(ErrorCode.STORAGE_ERROR, "Prepare upload temp directory failed");
        }
        return dir;
    }

    private Path uploadDir(String uploadId) {
        String configured = uploadSecurityProperties.getChunkTempDir();
        Path root = StringUtils.hasText(configured)
                ? Paths.get(configured)
                : Paths.get(System.getProperty("java.io.tmpdir"), "bus-gallery", "chunk-upload");
        return root.resolve(uploadId);
    }

    private Path partFile(String uploadId, int index) {
        return ensureUploadDir(uploadId).resolve(String.format("part-%05d.bin", index));
    }

    private Path mergedFile(String uploadId) {
        return ensureUploadDir(uploadId).resolve("merged.bin");
    }

    private void writePart(MultipartFile part, Path targetPath) {
        Path tempPath = targetPath.resolveSibling(targetPath.getFileName() + ".tmp");
        try {
            part.transferTo(tempPath.toFile());
            try {
                Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicMoveError) {
                Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException ex) {
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException ignore) {
                // ignore cleanup exception
            }
            throw new BizException(ErrorCode.STORAGE_ERROR, "Write upload chunk failed");
        }
    }

    private void deleteUploadDir(String uploadId) {
        Path dir = uploadDir(uploadId);
        if (!Files.exists(dir)) {
            return;
        }
        try (var stream = Files.walk(dir)) {
            stream.sorted((left, right) -> right.toString().length() - left.toString().length())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException ex) {
                            log.warn("Delete temp path failed: {}", path, ex);
                        }
                    });
        } catch (IOException ex) {
            log.warn("Walk temp upload dir failed: {}", dir, ex);
        }
    }

    private PartStats scanPartStats(String uploadId, ChunkSession session) {
        long uploadedChunks = 0L;
        long uploadedBytes = 0L;
        for (int index = 0; index < session.totalChunks(); index++) {
            Path path = partFile(uploadId, index);
            if (!Files.exists(path)) {
                continue;
            }
            uploadedChunks++;
            try {
                uploadedBytes += Files.size(path);
            } catch (IOException ignore) {
                // ignore single-file size failure and continue.
            }
        }
        uploadedBytes = Math.min(uploadedBytes, session.totalSize());
        return new PartStats(uploadedChunks, uploadedBytes);
    }

    private void saveLocalSession(ChunkSession session) {
        if (session == null || !StringUtils.hasText(session.uploadId())) {
            return;
        }
        long expiresAt = Instant.now().getEpochSecond() + Math.max(300, uploadSecurityProperties.getChunkSessionTtlSeconds());
        localSessionStore.put(session.uploadId(), new LocalSessionState(session, expiresAt));
    }

    private ChunkSession readLocalSession(String uploadId) {
        LocalSessionState state = localSessionStore.get(uploadId);
        if (state == null) {
            return null;
        }
        long now = Instant.now().getEpochSecond();
        if (state.expiresAtEpochSecond() <= now) {
            localSessionStore.remove(uploadId);
            return null;
        }
        return state.session();
    }

    private void safeRedisWrite(Runnable runnable, String uploadId, String operation) {
        if (isRedisWriteMuted()) {
            return;
        }
        try {
            runnable.run();
        } catch (Throwable ex) {
            if (isRedisMisconf(ex)) {
                muteRedisWritesTemporarily(uploadId, operation);
                log.warn("Redis write skipped due to MISCONF, op={}, uploadId={}, reason={}",
                        operation, uploadId, rootMessage(ex));
            } else {
                log.warn("Redis write failed in chunk upload, op={}, uploadId={}, reason={}",
                        operation, uploadId, rootMessage(ex));
            }
        }
    }

    private boolean isRedisWriteMuted() {
        return System.currentTimeMillis() < redisWriteMutedUntilEpochMs.get();
    }

    private void muteRedisWritesTemporarily(String uploadId, String operation) {
        long now = System.currentTimeMillis();
        long until = now + 60_000L;
        redisWriteMutedUntilEpochMs.updateAndGet(prev -> Math.max(prev, until));
        log.warn("Temporarily disabling redis writes for chunk upload (60s), op={}, uploadId={}", operation, uploadId);
    }

    private boolean isRedisMisconf(Throwable e) {
        Throwable cursor = e;
        while (cursor != null) {
            String msg = cursor.getMessage();
            if (msg != null && msg.toUpperCase().contains("MISCONF")) {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    private String rootMessage(Throwable e) {
        Throwable cursor = e;
        Throwable last = e;
        while (cursor != null) {
            last = cursor;
            cursor = cursor.getCause();
        }
        return last == null ? "" : String.valueOf(last.getMessage());
    }

    private Long readLong(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (Exception ex) {
            return null;
        }
    }

    private String readString(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        String text = String.valueOf(value);
        return StringUtils.hasText(text) ? text : fallback;
    }

    private record ChunkSession(
            String uploadId,
            Long userId,
            String fileName,
            String contentType,
            long totalSize,
            long chunkSize,
            int totalChunks
    ) {
    }

    private record PartStats(long uploadedChunks, long uploadedBytes) {
    }

    private record LocalSessionState(ChunkSession session, long expiresAtEpochSecond) {
    }

    public record ChunkProgress(
            String uploadId,
            long chunkSize,
            int totalChunks,
            int uploadedChunks,
            long uploadedBytes,
            int percent,
            boolean completed
    ) {
    }

    @Getter
    @RequiredArgsConstructor
    public static class CompletedChunkFile {
        private final MultipartFile multipartFile;
    }

    @RequiredArgsConstructor
    private static class TempFileMultipartFile implements MultipartFile {
        private final Path path;
        private final String originalFilename;
        private final String contentType;

        @Override
        public String getName() {
            return "file";
        }

        @Override
        public String getOriginalFilename() {
            return originalFilename;
        }

        @Override
        public String getContentType() {
            return contentType;
        }

        @Override
        public boolean isEmpty() {
            return getSize() <= 0;
        }

        @Override
        public long getSize() {
            try {
                return Files.size(path);
            } catch (IOException ex) {
                return 0L;
            }
        }

        @Override
        public byte[] getBytes() throws IOException {
            return Files.readAllBytes(path);
        }

        @Override
        public InputStream getInputStream() throws IOException {
            return Files.newInputStream(path);
        }

        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            Files.copy(path, dest.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        @Override
        public void transferTo(Path dest) throws IOException, IllegalStateException {
            Files.copy(path, dest, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
