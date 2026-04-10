package com.carrental.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.carrental.common.result.ApiResponse;
import com.carrental.infrastructure.persistence.dataobject.UserAgreementDO;
import com.carrental.infrastructure.persistence.mapper.UserAgreementMapper;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
    public ApiResponse<Void> update(@RequestBody UpdateAgreementRequest request) {
        // 先取消所有旧协议
        LambdaQueryWrapper<UserAgreementDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserAgreementDO::getIsActive, true);
        UserAgreementDO oldAgreement = userAgreementMapper.selectOne(wrapper);
        if (oldAgreement != null) {
            oldAgreement.setIsActive(false);
            userAgreementMapper.updateById(oldAgreement);
        }

        // 创建新协议
        UserAgreementDO newAgreement = new UserAgreementDO();
        newAgreement.setContent(request.getContent());
        newAgreement.setVersion("1.0");
        newAgreement.setIsActive(true);
        userAgreementMapper.insert(newAgreement);

        return ApiResponse.success(null);
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
