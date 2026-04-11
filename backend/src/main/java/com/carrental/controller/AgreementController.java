package com.carrental.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carrental.common.result.ApiResponse;
import com.carrental.infrastructure.persistence.dataobject.UserAgreementDO;
import com.carrental.infrastructure.persistence.mapper.UserAgreementMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AgreementController {

    private final UserAgreementMapper userAgreementMapper;

    /**
     * 获取当前协议
     */
    @GetMapping("/agreement")
    public ApiResponse<AgreementDTO> getCurrent() {
        LambdaQueryWrapper<UserAgreementDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAgreementDO::getIsActive, true);
        UserAgreementDO agreement = userAgreementMapper.selectOne(wrapper);
        if (agreement == null) {
            return ApiResponse.error(4004, "暂无协议");
        }
        return ApiResponse.success(toDTO(agreement));
    }

    /**
     * 更新协议 (管理端)
     */
    @PutMapping("/admin/agreement")
    public ApiResponse<AgreementDTO> update(@RequestBody UpdateAgreementRequest request) {
        // 先取消所有旧协议
        LambdaQueryWrapper<UserAgreementDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAgreementDO::getIsActive, true);
        UserAgreementDO oldAgreement = userAgreementMapper.selectOne(wrapper);
        if (oldAgreement != null) {
            oldAgreement.setIsActive(false);
            userAgreementMapper.updateById(oldAgreement);
        }

        // 计算新版本号
        String newVersion = calculateNextVersion();

        // 创建新协议
        UserAgreementDO newAgreement = new UserAgreementDO();
        newAgreement.setContent(request.getContent());
        newAgreement.setVersion(newVersion);
        newAgreement.setIsActive(true);
        userAgreementMapper.insert(newAgreement);

        return ApiResponse.success(toDTO(newAgreement));
    }

    /**
     * 计算下一个版本号，格式 major.minor
     */
    private String calculateNextVersion() {
        List<UserAgreementDO> allAgreements = userAgreementMapper.selectList(null);
        if (allAgreements.isEmpty()) {
            return "1.0";
        }
        String maxVersion = allAgreements.stream()
                .map(UserAgreementDO::getVersion)
                .max(this::compareVersions)
                .orElse("1.0");

        String[] parts = maxVersion.split("\\.");
        int major = Integer.parseInt(parts[0]);
        int minor = Integer.parseInt(parts[1]);
        return major + "." + (minor + 1);
    }

    private int compareVersions(String v1, String v2) {
        String[] p1 = v1.split("\\.");
        String[] p2 = v2.split("\\.");
        int majorCmp = Integer.compare(Integer.parseInt(p1[0]), Integer.parseInt(p2[0]));
        if (majorCmp != 0) return majorCmp;
        return Integer.compare(Integer.parseInt(p1[1]), Integer.parseInt(p2[1]));
    }

    private AgreementDTO toDTO(UserAgreementDO agreementDO) {
        AgreementDTO dto = new AgreementDTO();
        dto.setId(agreementDO.getId());
        dto.setContent(agreementDO.getContent());
        dto.setVersion(agreementDO.getVersion());
        dto.setUpdatedAt(agreementDO.getUpdatedAt());
        return dto;
    }

    @Data
    public static class AgreementDTO {
        private Long id;
        private String content;
        private String version;
        private java.time.LocalDateTime updatedAt;
    }

    @Data
    public static class UpdateAgreementRequest {
        private String content;
    }
}
