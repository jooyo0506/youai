# SpringBoot Agent 学习流程（项目版）

## 1. 先明确这项目在做什么
这个项目是 **AI 应用编排**，不是训练大模型。  
核心是：`HTTP 请求 -> 组装上下文 -> 调用 LLM -> (可选)工具调用 -> 返回结果`。

## 2. 项目结构怎么读

### 启动与配置
- `src/main/java/com/agent/AgentApplication.java`
- `src/main/java/com/agent/config/LlmProperties.java`
- `src/main/java/com/agent/config/AppConfig.java`

### 数据模型（和 LLM API 对齐）
- `src/main/java/com/agent/model/Message.java`
- `src/main/java/com/agent/model/ChatCompletionRequest.java`
- `src/main/java/com/agent/model/ChatCompletionResponse.java`
- `src/main/java/com/agent/model/ApiTool.java`
- `src/main/java/com/agent/model/ToolCall.java`
- `src/main/java/com/agent/model/FunctionCall.java`

### LLM 调用核心
- `src/main/java/com/agent/service/LlmClientService.java`

### 基础能力
- `src/main/java/com/agent/basics/prompt/BasicPromptsService.java`
- `src/main/java/com/agent/basics/api/OpenAiBasicService.java`
- `src/main/java/com/agent/basics/api/StreamingService.java`

### 核心概念
- `src/main/java/com/agent/core/tools/*`
- `src/main/java/com/agent/core/react/ReactAgentService.java`
- `src/main/java/com/agent/core/memory/ConversationMemoryService.java`

### 实战 Agent
- `src/main/java/com/agent/projects/chat/ChatAgentService.java`
- `src/main/java/com/agent/projects/toolagent/ToolAgentService.java`

### 对外 API 入口
- `src/main/java/com/agent/controller/*`

---

## 3. 按“调用链”理解系统（最重要）

### 普通对话链路
`Controller -> Chat/Prompt Service -> LlmClientService -> LLM -> Response`

### 工具调用链路
`Controller -> ToolAgentService -> LLM 返回 tool_calls -> ToolManager 执行工具 -> 工具结果回填消息 -> LLM 二次总结 -> Response`

### ReAct 链路
`ReactAgentService` 内部循环：
1. Thought（思考）
2. Action（选择工具）
3. Observation（工具结果）
4. Final Answer（最终答案）

### 记忆链路
`ChatAgentService` 使用 `ConversationMemoryService` 管理上下文（buffer/window/token/time）。

### 流式链路
`StreamingService` 通过 SSE/WebClient 把模型增量内容持续推给前端。

---

## 4. 推荐阅读顺序（流程，不是时间计划）
1. `AgentApplication`、`LlmProperties`、`AppConfig`
2. `model/*`（先把请求/响应结构看懂）
3. `LlmClientService`（统一调用入口）
4. `BasicPromptsService`、`OpenAiBasicService`
5. `StreamingService`
6. `core/tools/*` + `ToolManager`
7. `ReactAgentService`
8. `ConversationMemoryService`
9. `ChatAgentService`、`ToolAgentService`
10. `controller/*`（把整个 API 串起来）

---

## 5. 最小实操路径（边看边跑）
1. 启动项目：`mvn spring-boot:run`
2. 跑 Prompt：`/api/prompts/basic`
3. 跑工具直调：`/api/tools/execute`
4. 跑 ReAct：`/api/react/run`
5. 跑对话 Agent：`/api/chat/send`
6. 跑工具 Agent：`/api/tools/agent/run`

---

## 6. 学完自检（你应该能回答）
- `LlmClientService.chatCompletion()` 和 `chatWithTools()` 区别是什么？
- 工具调用结果为什么要以 `Message.tool(...)` 回填？
- `window` 记忆和 `token` 记忆分别解决什么问题？
- ReAct 为什么需要多轮迭代而不是一次回答？
- `controller` 层和 `service` 层在这个项目里的边界是什么？
