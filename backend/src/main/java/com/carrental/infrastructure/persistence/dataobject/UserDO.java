package com.carrental.infrastructure.persistence.dataobject;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("users")
public class UserDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String phone;

    private String wechatOpenid;

    private String nickname;

    private String role;

    private String status;

    private String passwordHash;

    @TableField("must_change_pwd")
    private Boolean mustChangePwd;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
