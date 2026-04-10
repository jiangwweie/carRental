package com.carrental.application.auth;

import lombok.Data;

@Data
public class WxLoginCommand {
    private String loginCode;
    private String phoneCode;
}
