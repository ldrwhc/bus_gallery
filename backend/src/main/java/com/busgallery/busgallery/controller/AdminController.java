package com.busgallery.busgallery.controller;

import com.busgallery.busgallery.auth.RequireLogin;
import com.busgallery.busgallery.auth.RoleGuard;
import com.busgallery.busgallery.auth.UserRole;
import com.busgallery.busgallery.auth.UserSessionService;
import com.busgallery.busgallery.dto.request.UserRoleUpdateRequest;
import com.busgallery.busgallery.dto.response.AdminOverviewResponse;
import com.busgallery.busgallery.dto.response.AdminUserResponse;
import com.busgallery.busgallery.entity.*;
import com.busgallery.busgallery.exception.BizException;
import com.busgallery.busgallery.exception.ErrorCode;
import com.busgallery.busgallery.repository.VehicleSubmissionRepository;
import com.busgallery.busgallery.service.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
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
            // Province-level划分：仅允许父级为空或level=1的区域作为审核目标区域
            if (region.getParentId() != null && (region.getLevel() == null || region.getLevel() > 1)) {
                throw new BizException(ErrorCode.INVALID_PARAM, "Reviewer region must be province-level");
            }
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
        company.setLogoUrl(request.getLogoUrl());
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
        existing.setLogoUrl(request.getLogoUrl());
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
        brand.setCountry(request.getCountry());
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
        existing.setCountry(request.getCountry());
        existing.setDescription(request.getDescription());
        return toBrandRow(brandService.update(existing));
    }

    @DeleteMapping("/tables/brands/{id}")
    @RequireLogin
    public void deleteBrand(@PathVariable Long id) {
        RoleGuard.requireStation();
        brandService.delete(id);
    }

    @GetMapping("/tables/models")
    @RequireLogin
    public List<AdminModelRow> listModels() {
        RoleGuard.requireStation();
        return modelService.findAll().stream().map(this::toModelRow).collect(Collectors.toList());
    }

    @PostMapping("/tables/models")
    @RequireLogin
    public AdminModelRow createModel(@RequestBody ModelUpsertRequest request) {
        RoleGuard.requireStation();
        validateModelRequest(request);
        Model model = new Model();
        model.setName(request.getName().trim());
        model.setModelCode(request.getModelCode());
        model.setDescription(request.getDescription());
        model.setReleaseYear(request.getReleaseYear());
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
        existing.setModelCode(request.getModelCode());
        existing.setDescription(request.getDescription());
        existing.setReleaseYear(request.getReleaseYear());
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
        row.setLevel(region.getLevel());
        return row;
    }

    private AdminCompanyRow toCompanyRow(Company company) {
        AdminCompanyRow row = new AdminCompanyRow();
        row.setId(company.getId());
        row.setName(company.getName());
        row.setDescription(company.getDescription());
        row.setLogoUrl(company.getLogoUrl());
        row.setRegionId(company.getRegion() != null ? company.getRegion().getId() : null);
        row.setRegionName(company.getRegion() != null ? company.getRegion().getName() : null);
        return row;
    }

    private AdminBrandRow toBrandRow(Brand brand) {
        AdminBrandRow row = new AdminBrandRow();
        row.setId(brand.getId());
        row.setName(brand.getName());
        row.setChnName(brand.getChnName());
        row.setCountry(brand.getCountry());
        row.setDescription(brand.getDescription());
        return row;
    }

    private AdminModelRow toModelRow(Model model) {
        AdminModelRow row = new AdminModelRow();
        row.setId(model.getId());
        row.setName(model.getName());
        row.setModelCode(model.getModelCode());
        row.setDescription(model.getDescription());
        row.setReleaseYear(model.getReleaseYear());
        row.setBrandId(model.getBrand() != null ? model.getBrand().getId() : null);
        row.setBrandName(model.getBrand() != null ? model.getBrand().getName() : null);
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
        private String logoUrl;
        private Long regionId;
    }

    @Data
    public static class BrandUpsertRequest {
        private String name;
        private String chnName;
        private String country;
        private String description;
    }

    @Data
    public static class ModelUpsertRequest {
        private String name;
        private String modelCode;
        private String description;
        private Integer releaseYear;
        private Long brandId;
    }

    @Data
    public static class AdminRegionRow {
        private Long id;
        private String name;
        private Long parentId;
        private String parentName;
        private Integer level;
    }

    @Data
    public static class AdminCompanyRow {
        private Long id;
        private String name;
        private String description;
        private String logoUrl;
        private Long regionId;
        private String regionName;
    }

    @Data
    public static class AdminBrandRow {
        private Long id;
        private String name;
        private String chnName;
        private String country;
        private String description;
    }

    @Data
    public static class AdminModelRow {
        private Long id;
        private String name;
        private String modelCode;
        private String description;
        private Integer releaseYear;
        private Long brandId;
        private String brandName;
    }
}
