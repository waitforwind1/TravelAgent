
package com.usst.superai.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

// 全局CORS配置
@Configuration
public class CorsConfig {

    @Value("${app.cors.allowed-origins[0]}")
    private String allowedOrigin;
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true); // 允许携带凭证
        config.addAllowedOrigin(allowedOrigin); // 允许的前端域名
        config.addAllowedHeader("*"); // 允许所有请求头
        config.addAllowedMethod("*"); // 允许所有HTTP方法
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}