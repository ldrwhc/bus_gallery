package com.busgallery.busgallery.controller;

import com.busgallery.busgallery.auth.RequireLogin;
import com.busgallery.busgallery.auth.RoleGuard;
import com.busgallery.busgallery.auth.UserRole;
import com.busgallery.busgallery.auth.UserSessionService;
import com.busgallery.busgallery.dto.request.UserRoleUpdateRequest;
import com.busgallery.busgallery.dto.response.AdminOverviewResponse;
import com.busgallery.busgallery.dto.response.AdminUserResponse;
import com.busgallery.busgallery.dto.response.PageResponse;
import com.busgallery.busgallery.entity.*;
import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import com.busgallery.busgallery.mapper.VehicleCommentMapper;
import com.busgallery.busgallery.repository.VehicleSubmissionRepository;
import com.busgallery.busgallery.service.*;
import com.busgallery.busgallery.mapper.VehicleImageMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserService userService;
    private final RegionService regionService;
    private final CompanyService companyService;
    private final BrandService brandService;
    private final ModelService modelService;
    private final VehicleSubmissionRepository submissionRepository;
    private final UserSessionService userSessionService;
    private final ImageService imageService;
    private final ImageAccessService imageAccessService;
    private final VehicleImageMapper vehicleImageMapper;
    private final VehicleCommentMapper vehicleCommentMapper;
    private final VehicleCommentService vehicleCommentService;

    @GetMapping("/overview")
    @RequireLogin
    public AdminOverviewResponse overview() {
        RoleGuard.requireStation();
        return AdminOverviewResponse.builder()
                .totalUsers(userService.countUsers())
                .stationUsers(userService.countByRole(UserRole.STATION))
                .reviewerUsers(userService.countByRole(UserRole.REVIEWER))
                .normalUsers(userService.countByRole(UserRole.USER))
                .pendingSubmissions(submissionRepository.countByStatus(SubmissionStatus.PENDING))
                .approvedSubmissions(submissionRepository.countByStatus(SubmissionStatus.APPROVED))
                .rejectedSubmissions(submissionRepository.countByStatus(SubmissionStatus.REJECTED))
                .build();
    }

    @GetMapping("/users")
    @RequireLogin
    public List<AdminUserResponse> users() {
        RoleGuard.requireStation();
        return userService.listAllUsers().stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }

    @PutMapping("/users/{id}/role")
    @RequireLogin
    public AdminUserResponse updateRole(@PathVariable Long id,
                                        @RequestBody UserRoleUpdateRequest request) {
        RoleGuard.requireStation();
        if (request == null || request.getRole() == null) {
            throw new BizException(ErrorCode.INVALID_PARAM, "Role is required");
        }
        User current = userService.findById(id);
        if (current == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "User not found");
        }
        if (current.getRole() == UserRole.STATION
                && request.getRole() != UserRole.STATION
                && userService.countByRole(UserRole.STATION) <= 1) {
            throw new BizException(ErrorCode.INVALID_PARAM, "At least one station user must remain");
        }

        Long reviewRegionId = request.getReviewRegionId();
        if (request.getRole() == UserRole.REVIEWER) {
            if (reviewRegionId == null) {
                throw new BizException(ErrorCode.INVALID_PARAM, "Reviewer must be assigned to a province region");
            }
            Region region = regionService.findById(reviewRegionId);
            if (region == null) {
                throw new BizException(ErrorCode.NOT_FOUND, "Review region not found");
            }
            Long normalizedProvinceId = regionService.resolveProvinceId(region.getId());
            if (normalizedProvinceId == null) {
                throw new BizException(ErrorCode.INVALID_PARAM, "Unable to resolve reviewer province region");
            }
            reviewRegionId = normalizedProvinceId;
        } else {
            reviewRegionId = null;
        }

        User saved = userService.updateRole(id, request.getRole(), reviewRegionId);
        userSessionService.refreshRoleScopeByUserId(saved.getId(), saved.getRole(), saved.getReviewRegionId());
        return toUserResponse(saved);
    }

    @GetMapping("/submissions")
    @RequireLogin
    public List<Long> submissionIds(@RequestParam(defaultValue = "PENDING") SubmissionStatus status) {
        RoleGuard.requireStation();
        return submissionRepository.findByStatusOrderByCreatedAtAsc(status).stream()
                .map(VehicleSubmission::getId)
                .collect(Collectors.toList());
    }

    @GetMapping("/tables/regions")
    @RequireLogin
    public List<AdminRegionRow> listRegions() {
        RoleGuard.requireStation();
        return regionService.findAll().stream().map(this::toRegionRow).collect(Collectors.toList());
    }

    @PostMapping("/tables/regions")
    @RequireLogin
    public AdminRegionRow createRegion(@RequestBody RegionUpsertRequest request) {
        RoleGuard.requireStation();
        validateRegionRequest(request);
        Region region = new Region();
        region.setName(request.getName().trim());
        region.setParentId(request.getParentId());
        region.setLevel(request.getLevel());
        return toRegionRow(regionService.create(region));
    }

    @PutMapping("/tables/regions/{id}")
    @RequireLogin
    public AdminRegionRow updateRegion(@PathVariable Long id, @RequestBody RegionUpsertRequest request) {
        RoleGuard.requireStation();
        validateRegionRequest(request);
        Region existing = regionService.findById(id);
        if (existing == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Region not found");
        }
        existing.setName(request.getName().trim());
        existing.setParentId(request.getParentId());
        existing.setLevel(request.getLevel());
        return toRegionRow(regionService.update(existing));
    }

    @DeleteMapping("/tables/regions/{id}")
    @RequireLogin
    public void deleteRegion(@PathVariable Long id) {
        RoleGuard.requireStation();
        regionService.delete(id);
    }

    @PostMapping("/tables/regions/batch-delete")
    @RequireLogin
    public BatchDeleteResult batchDeleteRegions(@RequestBody IdBatchDeleteRequest request) {
        RoleGuard.requireStation();
        return executeBatchDelete(request, regionService::delete);
    }

    @GetMapping("/tables/companies")
    @RequireLogin
    public List<AdminCompanyRow> listCompanies() {
        RoleGuard.requireStation();
        return companyService.findAll().stream().map(this::toCompanyRow).collect(Collectors.toList());
    }

    @PostMapping("/tables/companies")
    @RequireLogin
    public AdminCompanyRow createCompany(@RequestBody CompanyUpsertRequest request) {
        RoleGuard.requireStation();
        validateCompanyRequest(request);
        Company company = new Company();
        company.setName(request.getName().trim());
        company.setDescription(request.getDescription());
        if (request.getRegionId() != null) {
            Region region = new Region();
            region.setId(request.getRegionId());
            company.setRegion(region);
        }
        return toCompanyRow(companyService.create(company));
    }

    @PutMapping("/tables/companies/{id}")
    @RequireLogin
    public AdminCompanyRow updateCompany(@PathVariable Long id, @RequestBody CompanyUpsertRequest request) {
        RoleGuard.requireStation();
        validateCompanyRequest(request);
        Company existing = companyService.findById(id);
        if (existing == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Company not found");
        }
        existing.setName(request.getName().trim());
        existing.setDescription(request.getDescription());
        if (request.getRegionId() != null) {
            Region region = new Region();
            region.setId(request.getRegionId());
            existing.setRegion(region);
        } else {
            existing.setRegion(null);
        }
        return toCompanyRow(companyService.update(existing));
    }

    @DeleteMapping("/tables/companies/{id}")
    @RequireLogin
    public void deleteCompany(@PathVariable Long id) {
        RoleGuard.requireStation();
        companyService.delete(id);
    }

    @PostMapping("/tables/companies/batch-delete")
    @RequireLogin
    public BatchDeleteResult batchDeleteCompanies(@RequestBody IdBatchDeleteRequest request) {
        RoleGuard.requireStation();
        return executeBatchDelete(request, companyService::delete);
    }

    @GetMapping("/tables/brands")
    @RequireLogin
    public List<AdminBrandRow> listBrands() {
        RoleGuard.requireStation();
        return brandService.findAll().stream().map(this::toBrandRow).collect(Collectors.toList());
    }

    @PostMapping("/tables/brands")
    @RequireLogin
    public AdminBrandRow createBrand(@RequestBody BrandUpsertRequest request) {
        RoleGuard.requireStation();
        validateBrandRequest(request);
        Brand brand = new Brand();
        brand.setName(request.getName().trim());
        brand.setChnName(request.getChnName());
        brand.setDescription(request.getDescription());
        return toBrandRow(brandService.create(brand));
    }

    @PutMapping("/tables/brands/{id}")
    @RequireLogin
    public AdminBrandRow updateBrand(@PathVariable Long id, @RequestBody BrandUpsertRequest request) {
        RoleGuard.requireStation();
        validateBrandRequest(request);
        Brand existing = brandService.findById(id);
        if (existing == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Brand not found");
        }
        existing.setName(request.getName().trim());
        existing.setChnName(request.getChnName());
        existing.setDescription(request.getDescription());
        return toBrandRow(brandService.update(existing));
    }

    @DeleteMapping("/tables/brands/{id}")
    @RequireLogin
    public void deleteBrand(@PathVariable Long id) {
        RoleGuard.requireStation();
        brandService.delete(id);
    }

    @PostMapping("/tables/brands/batch-delete")
    @RequireLogin
    public BatchDeleteResult batchDeleteBrands(@RequestBody IdBatchDeleteRequest request) {
        RoleGuard.requireStation();
        return executeBatchDelete(request, brandService::delete);
    }

    @GetMapping("/tables/models")
    @RequireLogin
    public List<AdminModelRow> listModels() {
        RoleGuard.requireStation();
        return modelService.findAll().stream().map(this::toModelRow).collect(Collectors.toList());
    }

    @GetMapping("/comments")
    @RequireLogin
    public PageResponse<AdminCommentRow> listComments(@RequestParam(defaultValue = "1") int page,
                                                      @RequestParam(defaultValue = "20") int size) {
        RoleGuard.requireStation();
        int pageNo = Math.max(page, 1);
        int pageSize = Math.max(size, 1);
        int offset = (pageNo - 1) * pageSize;
        List<AdminCommentRow> records = vehicleCommentMapper.selectPage(offset, pageSize).stream()
                .map(this::toCommentRow)
                .collect(Collectors.toList());
        long total = vehicleCommentMapper.countAll();
        return PageResponse.of(records, total, pageNo, pageSize);
    }

    @DeleteMapping("/comments/{commentId}")
    @RequireLogin
    public void deleteComment(@PathVariable Long commentId) {
        RoleGuard.requireStation();
        deleteCommentById(commentId);
    }

    @PostMapping("/comments/batch-delete")
    @RequireLogin
    public BatchDeleteResult batchDeleteComments(@RequestBody IdBatchDeleteRequest request) {
        RoleGuard.requireStation();
        return executeBatchDelete(request, this::deleteCommentById);
    }

    @PostMapping("/tables/models")
    @RequireLogin
    public AdminModelRow createModel(@RequestBody ModelUpsertRequest request) {
        RoleGuard.requireStation();
        validateModelRequest(request);
        Model model = new Model();
        model.setName(request.getName().trim());
        model.setDescription(request.getDescription());
        Brand brand = new Brand();
        brand.setId(request.getBrandId());
        model.setBrand(brand);
        return toModelRow(modelService.create(model));
    }

    @PutMapping("/tables/models/{id}")
    @RequireLogin
    public AdminModelRow updateModel(@PathVariable Long id, @RequestBody ModelUpsertRequest request) {
        RoleGuard.requireStation();
        validateModelRequest(request);
        Model existing = modelService.findById(id);
        if (existing == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Model not found");
        }
        existing.setName(request.getName().trim());
        existing.setDescription(request.getDescription());
        Brand brand = new Brand();
        brand.setId(request.getBrandId());
        existing.setBrand(brand);
        return toModelRow(modelService.update(existing));
    }

    @DeleteMapping("/tables/models/{id}")
    @RequireLogin
    public void deleteModel(@PathVariable Long id) {
        RoleGuard.requireStation();
        modelService.delete(id);
    }

    @PostMapping("/tables/models/batch-delete")
    @RequireLogin
    public BatchDeleteResult batchDeleteModels(@RequestBody IdBatchDeleteRequest request) {
        RoleGuard.requireStation();
        return executeBatchDelete(request, modelService::delete);
    }

    @GetMapping("/images/suspects")
    @RequireLogin
    public List<AdminImageIssueRow> listSuspectImages() {
        RoleGuard.requireStation();
        return collectSuspectImages(null);
    }

    @PostMapping("/images/suspects/cleanup")
    @RequireLogin
    public AdminImageCleanupResult cleanupSuspectImages(@RequestBody(required = false) AdminImageCleanupRequest request) {
        RoleGuard.requireStation();
        List<Long> requestedIds = request == null ? null : request.getImageIds();
        List<AdminImageIssueRow> suspects = collectSuspectImages(requestedIds);
        List<Long> deletedIds = new ArrayList<>();
        List<Long> failedIds = new ArrayList<>();
        for (AdminImageIssueRow row : suspects) {
            Long imageId = row.getId();
            if (imageId == null) {
                continue;
            }
            try {
                imageService.delete(imageId);
                deletedIds.add(imageId);
            } catch (Exception ex) {
                failedIds.add(imageId);
            }
        }
        return new AdminImageCleanupResult(
                requestedIds == null ? suspects.size() : requestedIds.size(),
                deletedIds,
                failedIds
        );
    }

    private List<AdminImageIssueRow> collectSuspectImages(List<Long> requestedIds) {
        List<Image> images = imageService.listAllRaw();
        List<Long> normalizedIds = requestedIds == null ? null : requestedIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        return images.stream()
                .filter(Objects::nonNull)
                .filter(image -> normalizedIds == null || normalizedIds.contains(image.getId()))
                .map(this::toSuspectImageRow)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private AdminImageIssueRow toSuspectImageRow(Image image) {
        if (image == null || image.getId() == null) {
            return null;
        }
        long relationCount = vehicleImageMapper.countByImageId(image.getId());
        Long primaryVehicleId = vehicleImageMapper.selectPrimaryVehicleIdByImageId(image.getId());
        String primaryObject = imageAccessService.resolveObjectNameRef(image.getObjectName());
        String thumbnailObject = imageAccessService.resolveThumbnailObjectName(image);
        boolean primaryObjectExists = StringUtils.hasText(primaryObject) && imageAccessService.objectExistsRef(primaryObject);
        boolean thumbnailObjectExists = !StringUtils.hasText(thumbnailObject) || imageAccessService.objectExistsRef(thumbnailObject);
        List<String> issueTypes = new ArrayList<>();
        if (relationCount <= 0) {
            issueTypes.add("UNLINKED");
        }
        if (!primaryObjectExists) {
            issueTypes.add("PRIMARY_MISSING");
        }
        if (!thumbnailObjectExists) {
            issueTypes.add("THUMBNAIL_MISSING");
        }
        if (issueTypes.isEmpty()) {
            return null;
        }
        AdminImageIssueRow row = new AdminImageIssueRow();
        row.setId(image.getId());
        row.setObjectName(image.getObjectName());
        row.setThumbnailObjectName(thumbnailObject);
        row.setUploadUser(image.getUploadUser());
        row.setUploaderId(image.getUploaderId());
        row.setUploaderUsername(image.getUploaderUsername());
        row.setUploaderDisplayName(image.getUploaderDisplayName());
        row.setCreateTime(image.getCreateTime());
        row.setRelationCount(relationCount);
        row.setPrimaryVehicleId(primaryVehicleId);
        row.setPrimaryObjectExists(primaryObjectExists);
        row.setThumbnailObjectExists(thumbnailObjectExists);
        row.setIssueTypes(issueTypes);
        row.setIssueSummary(buildIssueSummary(issueTypes));
        return row;
    }

    private String buildIssueSummary(List<String> issueTypes) {
        if (issueTypes == null || issueTypes.isEmpty()) {
            return "";
        }
        List<String> labels = new ArrayList<>();
        if (issueTypes.contains("UNLINKED")) {
            labels.add("未关联任何车辆");
        }
        if (issueTypes.contains("PRIMARY_MISSING")) {
            labels.add("原图对象缺失");
        }
        if (issueTypes.contains("THUMBNAIL_MISSING")) {
            labels.add("缩略图对象缺失");
        }
        return String.join(" / ", labels);
    }

    private void deleteCommentById(Long commentId) {
        VehicleComment comment = vehicleCommentMapper.selectById(commentId);
        if (comment == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "Comment not found");
        }
        vehicleCommentService.deleteComment(comment.getVehicleId(), commentId, null, true);
    }

    private BatchDeleteResult executeBatchDelete(IdBatchDeleteRequest request, Consumer<Long> deleteAction) {
        List<Long> ids = normalizeBatchIds(request);
        List<Long> deletedIds = new ArrayList<>();
        List<Long> failedIds = new ArrayList<>();
        for (Long id : ids) {
            try {
                deleteAction.accept(id);
                deletedIds.add(id);
            } catch (Exception ex) {
                failedIds.add(id);
            }
        }
        return new BatchDeleteResult(ids.size(), deletedIds, failedIds);
    }

    private List<Long> normalizeBatchIds(IdBatchDeleteRequest request) {
        if (request == null || request.getIds() == null) {
            throw new BizException(ErrorCode.INVALID_PARAM, "ids are required");
        }
        Set<Long> dedupe = new LinkedHashSet<>();
        for (Long id : request.getIds()) {
            if (id != null) {
                dedupe.add(id);
            }
        }
        if (dedupe.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_PARAM, "ids are required");
        }
        return new ArrayList<>(dedupe);
    }

    private void validateRegionRequest(RegionUpsertRequest request) {
        if (request == null || !StringUtils.hasText(request.getName())) {
            throw new BizException(ErrorCode.INVALID_PARAM, "Region name is required");
        }
    }

    private void validateCompanyRequest(CompanyUpsertRequest request) {
        if (request == null || !StringUtils.hasText(request.getName())) {
            throw new BizException(ErrorCode.INVALID_PARAM, "Company name is required");
        }
    }

    private void validateBrandRequest(BrandUpsertRequest request) {
        if (request == null || !StringUtils.hasText(request.getName())) {
            throw new BizException(ErrorCode.INVALID_PARAM, "Brand name is required");
        }
    }

    private void validateModelRequest(ModelUpsertRequest request) {
        if (request == null || !StringUtils.hasText(request.getName()) || request.getBrandId() == null) {
            throw new BizException(ErrorCode.INVALID_PARAM, "Model name and brand are required");
        }
    }

    private AdminUserResponse toUserResponse(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .displayName(user.getDisplayName())
                .role(user.getRole() == null ? UserRole.USER : user.getRole())
                .reviewRegionId(user.getReviewRegionId())
                .build();
    }

    private AdminRegionRow toRegionRow(Region region) {
        Region parent = region != null && region.getParentId() != null ? regionService.findById(region.getParentId()) : null;
        AdminRegionRow row = new AdminRegionRow();
        row.setId(region.getId());
        row.setName(region.getName());
        row.setParentId(region.getParentId());
        row.setParentName(parent != null ? parent.getName() : null);
        row.setProvinceId(region.getProvinceId());
        row.setRegionType(region.getRegionType());
        row.setLevel(region.getLevel());
        return row;
    }

    private AdminCompanyRow toCompanyRow(Company company) {
        AdminCompanyRow row = new AdminCompanyRow();
        row.setId(company.getId());
        row.setName(company.getName());
        row.setDescription(company.getDescription());
        row.setRegionId(company.getRegion() != null ? company.getRegion().getId() : null);
        row.setRegionName(company.getRegion() != null ? company.getRegion().getName() : null);
        return row;
    }

    private AdminBrandRow toBrandRow(Brand brand) {
        AdminBrandRow row = new AdminBrandRow();
        row.setId(brand.getId());
        row.setName(brand.getName());
        row.setChnName(brand.getChnName());
        row.setDescription(brand.getDescription());
        return row;
    }

    private AdminModelRow toModelRow(Model model) {
        AdminModelRow row = new AdminModelRow();
        row.setId(model.getId());
        row.setName(model.getName());
        row.setDescription(model.getDescription());
        row.setBrandId(model.getBrand() != null ? model.getBrand().getId() : null);
        row.setBrandName(model.getBrand() != null ? model.getBrand().getName() : null);
        return row;
    }

    private AdminCommentRow toCommentRow(VehicleComment comment) {
        AdminCommentRow row = new AdminCommentRow();
        row.setId(comment.getId());
        row.setVehicleId(comment.getVehicleId());
        row.setUserId(comment.getUserId());
        row.setDisplayName(comment.getDisplayName());
        row.setUsername(comment.getUsername());
        row.setContent(comment.getContent());
        row.setCreatedAt(comment.getCreatedAt());
        return row;
    }

    @Data
    public static class RegionUpsertRequest {
        private String name;
        private Long parentId;
        private Integer level;
    }

    @Data
    public static class CompanyUpsertRequest {
        private String name;
        private String description;
        private Long regionId;
    }

    @Data
    public static class BrandUpsertRequest {
        private String name;
        private String chnName;
        private String description;
    }

    @Data
    public static class ModelUpsertRequest {
        private String name;
        private String description;
        private Long brandId;
    }

    @Data
    public static class IdBatchDeleteRequest {
        private List<Long> ids;
    }

    @Data
    public static class AdminRegionRow {
        private Long id;
        private String name;
        private Long parentId;
        private String parentName;
        private Long provinceId;
        private String regionType;
        private Integer level;
    }

    @Data
    public static class AdminCompanyRow {
        private Long id;
        private String name;
        private String description;
        private Long regionId;
        private String regionName;
    }

    @Data
    public static class AdminBrandRow {
        private Long id;
        private String name;
        private String chnName;
        private String description;
    }

    @Data
    public static class AdminModelRow {
        private Long id;
        private String name;
        private String description;
        private Long brandId;
        private String brandName;
    }

    @Data
    public static class AdminCommentRow {
        private Long id;
        private Long vehicleId;
        private Long userId;
        private String displayName;
        private String username;
        private String content;
        private LocalDateTime createdAt;
    }

    @Data
    public static class AdminImageIssueRow {
        private Long id;
        private String objectName;
        private String thumbnailObjectName;
        private String uploadUser;
        private Long uploaderId;
        private String uploaderUsername;
        private String uploaderDisplayName;
        private LocalDateTime createTime;
        private long relationCount;
        private Long primaryVehicleId;
        private boolean primaryObjectExists;
        private boolean thumbnailObjectExists;
        private List<String> issueTypes;
        private String issueSummary;
    }

    @Data
    public static class AdminImageCleanupRequest {
        private List<Long> imageIds;
    }

    @Data
    public static class AdminImageCleanupResult {
        private final int requestedCount;
        private final List<Long> deletedIds;
        private final List<Long> failedIds;
    }

    @Data
    public static class BatchDeleteResult {
        private final int requestedCount;
        private final List<Long> deletedIds;
        private final List<Long> failedIds;
    }
}
