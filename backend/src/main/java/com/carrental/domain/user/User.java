package com.carrental.domain.user;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {

    private Long id;
    private String phone;
    private String wechatOpenid;
    private String nickname;
    private String role;
    private String status;
    private String passwordHash;
    private boolean mustChangePwd;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public User(String phone, String wechatOpenid, String nickname) {
        this.phone = phone;
        this.wechatOpenid = wechatOpenid;
        this.nickname = nickname;
        this.role = "user";
        this.status = "active";
    }
}
