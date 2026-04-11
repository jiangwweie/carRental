package com.carrental.application.auth;

import cn.binarywang.wx.miniapp.api.WxMaService;
import cn.binarywang.wx.miniapp.bean.WxMaJscode2SessionResult;
import cn.binarywang.wx.miniapp.bean.WxMaPhoneNumberInfo;
import com.carrental.common.exception.BusinessException;
import com.carrental.common.result.ErrorCode;
import com.carrental.common.security.JwtUtil;
import com.carrental.domain.user.User;
import com.carrental.domain.user.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;

import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final WxMaService wxMaService;
    private final JwtUtil jwtUtil;

    @Value("${admin.password}")
    private String defaultAdminPassword;

    /**
     * 简单的 SHA-256 密码编码（管理端固定密码登录，足够用）
     */
    private String encodePassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("密码编码失败", e);
        }
    }

    private boolean matchesPassword(String rawPassword, String encodedPassword) {
        return encodePassword(rawPassword).equals(encodedPassword);
    }

    /**
     * 微信小程序登录
     */
    public LoginResult wxLogin(WxLoginCommand command) {
        // 1. 通过 loginCode 获取 openid
        WxMaJscode2SessionResult session;
        try {
            session = wxMaService.getUserService().getSessionInfo(command.getLoginCode());
        } catch (Exception e) {
            log.error("微信 code2Session 失败", e);
            throw new BusinessException(ErrorCode.WX_LOGIN_FAILED);
        }

        String openid = session.getOpenid();

        // 2. 通过 phoneCode 获取手机号
        String phone;
        try {
            WxMaPhoneNumberInfo phoneInfo = wxMaService.getUserService().getNewPhoneNoInfo(command.getPhoneCode());
            phone = phoneInfo.getPhoneNumber();
        } catch (Exception e) {
            log.error("微信获取手机号失败", e);
            throw new BusinessException(ErrorCode.PHONE_GET_FAILED);
        }

        // 3. 查找或创建用户
        User user = userRepository.findByOpenid(openid).orElse(null);
        boolean isNewUser = false;
        if (user == null) {
            String nickname = "用户" + phone.substring(phone.length() - 4);
            user = new User(phone, openid, nickname);
            user = userRepository.save(user);
            isNewUser = true;
        }

        // 4. 生成 JWT
        String token = jwtUtil.generateToken(user.getId(), user.getRole());

        return new LoginResult(token, toUserDTO(user), isNewUser);
    }

    /**
     * Sprint 1 模拟登录：直接返回 demo 用户的 JWT token
     * 不依赖微信 SDK，便于前端开发和联调
     */
    public LoginResult mockLogin(String role) {
        String openid = "admin".equals(role) ? "wx_demo_admin_001" : "wx_demo_user_001";

        User user = userRepository.findByOpenid(openid)
                .orElseGet(() -> {
                    User newUser = new User(
                            "admin".equals(role) ? "13800000001" : "13800000002",
                            openid,
                            "admin".equals(role) ? "管理员" : "张三"
                    );
                    if ("admin".equals(role)) {
                        newUser.setRole("admin");
                    }
                    return userRepository.save(newUser);
                });

        String token = jwtUtil.generateToken(user.getId(), user.getRole());
        return new LoginResult(token, toUserDTO(user), false);
    }

    /**
     * PC 管理端登录
     */
    public LoginResult adminLogin(String password) {
        // 查找管理员账号
        User admin = userRepository.findByOpenid("admin").orElse(null);
        if (admin == null) {
            // 首次创建管理员账号
            admin = new User("admin", "admin", "管理员");
            admin.setRole("admin");
            admin.setPasswordHash(encodePassword(defaultAdminPassword));
            admin.setMustChangePwd(true);
            admin = userRepository.save(admin);
        }

        // 验证密码
        if (!matchesPassword(password, admin.getPasswordHash())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED.getCode(), "密码错误");
        }

        String token = jwtUtil.generateToken(admin.getId(), admin.getRole());
        return new LoginResult(token, toUserDTO(admin), false);
    }

    private UserDTO toUserDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setPhone(maskPhone(user.getPhone()));
        dto.setNickname(user.getNickname());
        dto.setRole(user.getRole());
        dto.setMustChangePwd(user.isMustChangePwd());
        return dto;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    @Data
    public static class LoginResult {
        private final String token;
        private final UserDTO user;
        private final boolean isNewUser;

        public LoginResult(String token, UserDTO user, boolean isNewUser) {
            this.token = token;
            this.user = user;
            this.isNewUser = isNewUser;
        }
    }

    @Data
    public static class UserDTO {
        private Long id;
        private String phone;
        private String nickname;
        private String role;
        private boolean mustChangePwd;
    }
}
