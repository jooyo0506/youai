package com.agent.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 大模型提供商配置属性。
 * 通过 `application.yml` 中的 `llm.*` 节点自动绑定。
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "llm")
public class LlmProperties {

    /**
     * `provider`：当前启用的提供商标识。
     * 可选值：`deepseek`、`openai`、`anthropic`。
     */
    private String provider = "deepseek";

    /** `deepseek`：DeepSeek 提供商配置。 */
    private DeepSeek deepseek = new DeepSeek();

    /** `openai`：OpenAI 提供商配置。 */
    private OpenAI openai = new OpenAI();

    /** `anthropic`：Anthropic 提供商配置。 */
    private Anthropic anthropic = new Anthropic();

    @Data
    public static class DeepSeek {
        /** `apiKey`：调用 DeepSeek 接口所需密钥。 */
        private String apiKey;

        /** `baseUrl`：DeepSeek 接口基础地址。 */
        private String baseUrl = "https://api.deepseek.com";

        /** `model`：默认模型名称。 */
        private String model = "deepseek-chat";
    }

    @Data
    public static class OpenAI {
        /** `apiKey`：调用 OpenAI 接口所需密钥。 */
        private String apiKey;

        /** `baseUrl`：OpenAI 接口基础地址。 */
        private String baseUrl = "https://api.openai.com/v1";

        /** `model`：默认模型名称。 */
        private String model = "gpt-3.5-turbo";
    }

    @Data
    public static class Anthropic {
        /** `apiKey`：调用 Anthropic 接口所需密钥。 */
        private String apiKey;

        /** `baseUrl`：Anthropic 接口基础地址。 */
        private String baseUrl = "https://api.anthropic.com";

        /** `model`：默认模型名称。 */
        private String model = "claude-3-haiku-20240307";
    }
}
