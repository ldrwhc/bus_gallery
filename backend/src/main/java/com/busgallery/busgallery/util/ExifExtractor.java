package com.busgallery.busgallery.util;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;

import java.io.ByteArrayInputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * ExifExtractor类用于封装ExifExtractor相关的领域职责（所在包：com.busgallery.busgallery.util）。
 */
public final class ExifExtractor {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withLocale(Locale.CHINA);

    /**
     * ExifExtractor构造器用于初始化对象状态。
     * @return 构造器无返回值。
     */
    private ExifExtractor() {
    }

    /**
     * extract方法用于处理extract相关的业务逻辑。
     * @param data data参数，详见调用方上下文。
     * @return 返回String>类型结果。
     */
    public static Map<String, String> extract(byte[] data) {
        if (data == null || data.length == 0) {
            return Map.of();
        }
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(data)) {
            Metadata metadata = ImageMetadataReader.readMetadata(inputStream);
            Map<String, String> exif = new LinkedHashMap<>();
            ExifIFD0Directory primary = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            ExifSubIFDDirectory sub = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (primary != null) {
                addIfPresent(exif, "相机品牌", primary, ExifIFD0Directory.TAG_MAKE);
                addIfPresent(exif, "相机型号", primary, ExifIFD0Directory.TAG_MODEL);
            }
            if (sub != null) {
                addIfPresent(exif, "镜头型号", sub, ExifSubIFDDirectory.TAG_LENS_MODEL);
                addIfPresent(exif, "光圈", sub, ExifSubIFDDirectory.TAG_FNUMBER);
                addIfPresent(exif, "快门", sub, ExifSubIFDDirectory.TAG_EXPOSURE_TIME);
                addIfPresent(exif, "感光度", sub, ExifSubIFDDirectory.TAG_ISO_EQUIVALENT);
                addIfPresent(exif, "焦距", sub, ExifSubIFDDirectory.TAG_FOCAL_LENGTH);
                Date originalDate = sub.getDateOriginal();
                if (originalDate != null) {
                    exif.put("拍摄时间", DATE_FORMATTER.format(originalDate.toInstant().atZone(ZoneId.systemDefault())));
                }
            }
            return exif;
        } catch (Exception e) {
            return Map.of();
        }
    }

    /**
     * addIfPresent方法用于处理addIfPresent相关的业务逻辑。
     * @param exif exif参数，详见调用方上下文。
     * @param label label参数，详见调用方上下文。
     * @param dir dir参数，详见调用方上下文。
     * @param tag tag参数，详见调用方上下文。
     * @return 无返回值。
     */
    private static void addIfPresent(Map<String, String> exif, String label, Directory dir, int tag) {
        if (dir == null || !dir.containsTag(tag)) {
            return;
        }
        exif.put(label, dir.getDescription(tag));
    }
}
