package com.agent.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 应用级配置类。
 * 负责注册公共 Bean 和启动后初始化逻辑。
 */
@Configuration
@RequiredArgsConstructor
public class AppConfig {

    /**
     * 启动后初始化入口。
     * 当前预留给工具注册等扩展逻辑。
     */
    @PostConstruct
    public void registerTools() {
    }

    /**
     * 提供全局 `ObjectMapper` Bean。
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
