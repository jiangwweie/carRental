package com.carrental.common.config;

import com.carrental.common.security.JwtInterceptor;
import com.carrental.common.security.RoleInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final JwtInterceptor jwtInterceptor;
    private final RoleInterceptor roleInterceptor;

    @Value("${upload.local.base-path:uploads}")
    private String uploadBasePath;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // JWT 拦截器：验证登录态
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/api/v1/auth/**",           // 登录接口
                        "/api/v1/vehicles/**",       // 车辆列表/详情(公开)
                        "/api/v1/agreement",          // 用户协议(公开)
                        "/api/v1/pricing/**",         // 价格预估(公开)
                        "/uploads/**",                // 静态资源(公开)
                        "/error",                     // 错误页面
                        "/actuator/**"                // 健康检查
                );

        // 角色拦截器：仅管理员端路径
        registry.addInterceptor(roleInterceptor)
                .addPathPatterns("/api/v1/admin/**")
                .excludePathPatterns(
                        "/api/v1/auth/**",           // 登录接口
                        "/error",                     // 错误页面
                        "/actuator/**"                // 健康检查
                );
    }

    /**
     * 静态资源映射
     * /uploads/** -> file:uploads/
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadBasePath + "/");
    }
}
