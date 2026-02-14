# SpringBoot AI Agent

AI Agent 开发学习项目 - Java 实现版本 (JDK 21 + Spring Boot 3.3)

## 项目简介

这是原 Python AI Agent 学习项目的 Java 移植版本，使用 Spring Boot 3.3 + JDK 21 构建。项目从基础知识到实战项目，帮助开发者掌握 Agent 开发技能。

## 技术栈

- **Java**: JDK 21
- **框架**: Spring Boot 3.3.0
- **HTTP 客户端**: OkHttp 4.12.0
- **JSON 处理**: Jackson
- **构建工具**: Maven

## 项目结构

```
springboot-agent/
├── src/
│   ├── main/
│   │   ├── java/com/agent/
│   │   │   ├── AgentApplication.java          # 主应用入口
│   │   │   ├── config/                       # 配置类
│   │   │   │   ├── LlmProperties.java        # LLM 配置属性
│   │   │   │   └── AppConfig.java           # 应用配置
│   │   │   ├── controller/                   # REST API 控制器
│   │   │   │   ├── PromptController.java    # 提示词相关 API
│   │   │   │   ├── ReactAgentController.java # ReAct Agent API
│   │   │   │   ├── ChatAgentController.java # 对话 Agent API
│   │   │   │   └── ToolAgentController.java # 工具 Agent API
│   │   │   ├── model/                       # 数据模型
│   │   │   │   ├── Message.java             # 消息模型
│   │   │   │   ├── ToolCall.java           # 工具调用模型
│   │   │   │   ├── ApiTool.java            # API 工具定义
│   │   │   │   ├── ChatCompletionRequest.java
│   │   │   │   └── ChatCompletionResponse.java
│   │   │   ├── service/                     # 服务层
│   │   │   │   └── LlmClientService.java    # LLM 客户端服务
│   │   │   ├── basics/                      # 基础模块
│   │   │   │   ├── prompt/
│   │   │   │   │   └── BasicPromptsService.java
│   │   │   │   └── api/
│   │   │   │       ├── OpenAiBasicService.java
│   │   │   │       └── StreamingService.java
│   │   │   ├── core/                        # 核心概念
│   │   │   │   ├── react/
│   │   │   │   │   └── ReactAgentService.java
│   │   │   │   ├── memory/
│   │   │   │   │   └── ConversationMemoryService.java
│   │   │   │   └── tools/
│   │   │   │       ├── BaseTool.java
│   │   │   │       ├── WeatherTool.java
│   │   │   │       ├── CalculatorTool.java
│   │   │   │       ├── SearchTool.java
│   │   │   │       ├── DateTimeTool.java
│   │   │   │       └── ToolManager.java
│   │   │   └── projects/                    # 实战项目
│   │   │       ├── chat/
│   │   │       │   └── ChatAgentService.java
│   │   │       └── toolagent/
│   │   │           └── ToolAgentService.java
│   │   └── resources/
│   │       └── application.yml              # 应用配置
│   └── test/
└── pom.xml                                # Maven 配置
```

## 模块说明

### 1. 基础模块 (basics/)

#### 提示词工程 (prompt/)
- **BasicPromptsService.java**: 基础提示词、系统提示词、结构化输出、角色扮演、Few-shot、思维链

#### API 练习 (api/)
- **OpenAiBasicService.java**: OpenAI API 基础调用、多轮对话、参数控制
- **StreamingService.java**: 流式响应处理

### 2. 核心概念 (core/)

#### ReAct 模式 (react/)
- **ReactAgentService.java**: 推理+行动模式实现

#### 记忆系统 (memory/)
- **ConversationMemoryService.java**:
  - ConversationBufferMemory: 完整对话历史
  - ConversationWindowMemory: 窗口记忆
  - ConversationTokenBufferMemory: Token 限制记忆
  - TimestampedMemory: 带时间戳的记忆

#### 工具调用 (tools/)
- **BaseTool.java**: 工具基类接口
- **ToolManager.java**: 工具管理器
- **WeatherTool.java**: 天气查询工具
- **CalculatorTool.java**: 计算器工具
- **SearchTool.java**: 搜索工具
- **DateTimeTool.java**: 日期时间工具

### 3. 实战项目 (projects/)

#### 对话 Agent (chat/)
- **ChatAgentService.java**: 多轮对话、上下文记忆、人设定制

#### 工具调用 Agent (toolagent/)
- **ToolAgentService.java**: 工具调用 Agent，支持多个工具协作

## API 端点

### 提示词相关 API
```
POST   /api/prompts/basic                    - 基础文本生成
GET    /api/prompts/system-prompt           - 系统提示词示例
GET    /api/prompts/structured-output        - 结构化输出示例
GET    /api/prompts/role-playing            - 角色扮演示例
GET    /api/prompts/few-shot                - Few-shot 示例
GET    /api/prompts/chain-of-thought        - 思维链示例
GET    /api/prompts/run-all                 - 运行所有示例
```

### ReAct Agent API
```
POST   /api/react/run                        - 运行 ReAct Agent
GET    /api/react/tools                     - 列出可用工具
```

### 对话 Agent API
```
POST   /api/chat/create                     - 创建会话
POST   /api/chat/send                       - 发送消息
GET    /api/chat/personas                   - 获取可用人设列表
```

### 工具 Agent API
```
POST   /api/tools/agent/run                 - 运行工具 Agent
GET    /api/tools/list                      - 列出所有工具
POST   /api/tools/execute                   - 执行指定工具
```

## 配置说明

在 `application.yml` 中配置 LLM 提供商：

```yaml
llm:
  provider: deepseek  # 可选: deepseek, openai, anthropic
  deepseek:
    api-key: ${DEEPSEEK_API_KEY:}
    base-url: https://api.deepseek.com
    model: deepseek-chat
  openai:
    api-key: ${OPENAI_API_KEY:}
    base-url: https://api.openai.com/v1
    model: gpt-3.5-turbo
```

### 环境变量

- `DEEPSEEK_API_KEY`: DeepSeek API Key (推荐，国内使用)
- `OPENAI_API_KEY`: OpenAI API Key
- `ANTHROPIC_API_KEY`: Anthropic Claude API Key

## 运行项目

### 前置要求
- JDK 21+
- Maven 3.8+

### 启动步骤

1. 设置环境变量：
```bash
export DEEPSEEK_API_KEY=your_api_key_here
```

2. 编译项目：
```bash
mvn clean install
```

3. 运行项目：
```bash
mvn spring-boot:run
```

服务将在 `http://localhost:8080` 启动。

## 代码对比说明

### Python -> Java 主要转换点

| Python | Java |
|--------|------|
| dataclass | record |
| @dataclass | @Data (Lombok) |
| typing.Optional | Optional<T> |
| dict | Map<K, V> |
| list | List<T> |
| str | String |
| f-strings | String.format() |
| lambda | 方法引用/函数式接口 |
| asyncio | CompletableFuture |
| type hints | 泛型 |

### 特殊注意事项

1. **JSON 处理**: 使用 Jackson 替代 Python 的 json 模块
2. **HTTP 客户端**: 使用 OkHttp 替代 Python 的 requests
3. **流式响应**: 使用 okhttp-eventsource 实现 SSE
4. **日期时间**: 使用 java.time API 替代 Python datetime
5. **数学计算**: 使用 JavaScript ScriptEngine 实现安全表达式计算

## 学习路线

### 第一周：基础知识
- 提示词工程基础
- OpenAI API 调用
- 流式响应处理

### 第二周：核心概念
- ReAct 模式
- 工具调用
- 记忆系统

### 第三周：实战项目
- 对话 Agent
- 工具调用 Agent

## 许可证

本项目基于原 Python AI Agent 学习项目进行 Java 移植。
