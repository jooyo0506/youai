# SpringBoot AI Agent 学习指南

## 学习路线

本项目是一个渐进式的 AI Agent 开发学习项目，从基础概念到实战应用。

---

## 第一周：基础知识

### 1. 提示词工程

**文件位置**：`src/main/java/com/agent/basics/prompt/BasicPromptsService.java`

**学习目标**：理解不同类型的提示词及其效果

| 方法 | 功能 | 示例 |
|------|------|--------|
| `basicCompletion()` | 基础文本生成 | 用一句话解释什么是人工智能 |
| `systemPromptExample()` | 系统提示词 | 定义 AI 的角色和行为 |
| `structuredOutputExample()` | 结构化输出 | 返回 JSON 格式数据 |
| `rolePlayingExample()` | 角色扮演 | 让 AI 扮演特定角色（如苏格拉底）|
| `fewShotExample()` | Few-shot 学习 | 提供示例引导输出 |
| `chainOfThoughtExample()` | 思维链 | 引导 AI 逐步思考 |

**API 调用**：
```bash
# 基础文本生成
curl -X POST http://localhost:8080/api/prompts/basic \
  -H "Content-Type: application/json" \
  -d '{"prompt": "用一句话解释什么是人工智能"}'
```

---

### 2. OpenAI API 基础调用

**文件位置**：`src/main/java/com/agent/basics/api/OpenAiBasicService.java`

**学习目标**：掌握 API 基本调用方式和参数配置

| 方法 | 功能 |
|------|------|
| `basicChat()` | 基础对话 |
| `multiTurnChat()` | 多轮对话，保持上下文 |
| `temperatureComparison()` | Temperature 参数对比（控制随机性）|
| `maxTokensExample()` | max_tokens 参数对比（控制输出长度）|
| `jsonMode()` | JSON 模式，确保返回有效 JSON |
| `errorHandling()` | 错误处理示例 |

**参数说明**：
- `temperature`: 0.0-2.0，越高越随机
- `maxTokens`: 控制输出长度
- `stream`: 流式响应（见 StreamingService）

---

### 3. 流式响应

**文件位置**：`src/main/java/com/agent/basics/api/StreamingService.java`

**学习目标**：实现流式响应处理

| 方法 | 功能 |
|------|------|
| `streamChat()` | 使用回调的流式对话 |
| `streamChatWithEmitter()` | 返回 SSE Emitter |
| `streamChatAsync()` | 返回 CompletableFuture |

---

## 第二周：核心概念

### 1. ReAct 模式

**文件位置**：`src/main/java/com/agent/core/react/ReactAgentService.java`

**核心思想**：ReAct = Reasoning(推理) + Acting(行动)

**工作流程**：
```
Thought: 我需要思考如何解决这个问题
Action: 工具名称
Action Input: 工具输入
Observation: 工具返回结果
... (重复上述过程直到得出答案)
Thought: 我现在知道最终答案了
Final Answer: 最终答案
```

**内置工具**：
| 工具名 | 功能 | 实现 |
|--------|------|------|
| `search` | 搜索信息 | 模拟搜索 |
| `calculate` | 计算数学表达式 | JavaScript ScriptEngine |
| `get_weather` | 查询天气 | 模拟天气数据 |

**API 调用**：
```bash
curl -X POST http://localhost:8080/api/react/run \
  -H "Content-Type: application/json" \
  -d '{"question": "Python是什么？", "maxIterations": 5}'
```

---

### 2. 记忆系统

**文件位置**：`src/main/java/com/agent/core/memory/ConversationMemoryService.java`

**学习目标**：管理对话上下文，实现多轮对话

| 记忆类型 | 说明 | 优点 | 缺点 |
|----------|------|------|------|
| `ConversationBufferMemory` | 保存完整历史 | 保留所有上下文 | token 消耗随对话增长 |
| `ConversationWindowMemory` | 只保留最近 N 轮 | 控制 token 消耗 | 可能丢失早期信息 |
| `ConversationTokenBufferMemory` | 根据 token 数量限制 | 精确控制成本 | 需要 token 计数 |
| `TimestampedMemory` | 带时间戳的消息 | 支持时间过滤 | 无 |

**使用示例**：
```java
// 创建窗口记忆（保留3轮）
ConversationWindowMemory memory = new ConversationWindowMemory(3);

// 添加用户消息
memory.addUserMessage("你好");
memory.addAssistantMessage("你好！");

// 获取消息
List<Message> messages = memory.getMessages();
```

---

### 3. 工具调用

**文件位置**：`src/main/java/com/agent/core/tools/`

**核心类**：

| 类 | 功能 |
|-----|------|
| `BaseTool` | 工具基类接口 |
| `ToolManager` | 工具管理器 |
| `WeatherTool` | 天气查询工具 |
| `CalculatorTool` | 计算器工具 |
| `SearchTool` | 搜索工具 |
| `DateTimeTool` | 日期时间工具 |

**自定义工具步骤**：
1. 继承 `BaseTool` 或实现接口
2. 实现 `getName()`、`getDescription()`、`getParameters()`、`run()`
3. 注册到 `ToolManager`

---

## 第三周：实战项目

### 1. 对话 Agent

**文件位置**：`src/main/java/com/agent/projects/chat/ChatAgentService.java`

**功能**：多轮对话、上下文记忆、人设定制

**预设人设**：
| 人设 | 系统提示词 |
|------|----------|
| `ASSISTANT` | 友好、有帮助的 AI 助手 |
| `TEACHER` | 编程老师，简单易懂地解释 |
| `CREATIVE` | 创意写手，语言生动有趣 |
| `ANALYST` | 数据分析师，结构化分析 |
| `CUSTOM` | 自定义角色 |

**支持的命令**：
| 命令 | 功能 |
|------|------|
| `/help` | 显示帮助信息 |
| `/clear` | 清空对话记录 |
| `/persona [名称]` | 切换或查看人设 |
| `/custom [提示词]` | 设置自定义人设 |
| `/model [模型名]` | 切换或查看模型 |
| `/temp [值]` | 设置 temperature (0-1) |
| `/stream` | 切换流式输出 |

---

### 2. 工具调用 Agent

**文件位置**：`src/main/java/com/agent/projects/toolagent/ToolAgentService.java`

**功能**：天气查询、网页搜索、计算器、日期时间

**工作流程**：
1. 解析用户问题
2. 选择合适的工具
3. 执行工具获取结果
4. 将结果传回 LLM
5. 生成最终回答

**API 调用**：
```bash
# 运行工具 Agent
curl -X POST http://localhost:8080/api/tools/agent/run \
  -H "Content-Type: application/json" \
  -d '{"query": "北京今天天气怎么样？", "verbose": true}'

# 列出所有工具
curl http://localhost:8080/api/tools/list

# 执行特定工具
curl -X POST http://localhost:8080/api/tools/execute \
  -H "Content-Type: application/json" \
  -d '{"toolName": "calculator", "arguments": {"expression": "2+3*4"}}'
```

---

## 配置说明

**配置文件**：`src/main/resources/application.yml`

**环境变量**：`.env`

```bash
# LLM 提供商选择
LLM_PROVIDER=deepseek  # 可选: deepseek, openai, anthropic

# DeepSeek 配置（国内推荐）
DEEPSEEK_API_KEY=your_api_key_here
DEEPSEEK_BASE_URL=https://api.deepseek.com
DEEPSEEK_MODEL=deepseek-chat

# OpenAI 配置
OPENAI_API_KEY=your_openai_api_key
OPENAI_BASE_URL=https://api.openai.com/v1
OPENAI_MODEL=gpt-3.5-turbo

# Anthropic Claude 配置
ANTHROPIC_API_KEY=your_anthropic_api_key
```

---

## 运行项目

```bash
# 1. 配置环境变量
cp .env.example .env
# 编辑 .env 填入你的 API Key

# 2. 编译项目
mvn clean install

# 3. 运行项目
mvn spring-boot:run

# 4. 访问应用
http://localhost:8080
```

---

## API 端点汇总

| 端点 | 功能 |
|--------|------|
| `/api/prompts/basic` | 基础文本生成 |
| `/api/prompts/system-prompt` | 系统提示词示例 |
| `/api/prompts/structured-output` | 结构化输出示例 |
| `/api/prompts/role-playing` | 角色扮演示例 |
| `/api/prompts/few-shot` | Few-shot 示例 |
| `/api/prompts/chain-of-thought` | 思维链示例 |
| `/api/prompts/run-all` | 运行所有示例 |
| `/api/react/run` | 运行 ReAct Agent |
| `/api/react/tools` | 列出 ReAct 工具 |
| `/api/chat/send` | 对话 Agent |
| `/api/chat/personas` | 获取可用 人设列表 |
| `/api/tools/agent/run` | 运行工具 Agent |
| `/api/tools/list` | 列出所有工具 |
| `/api/tools/execute` | 执行指定工具 |

---

## 学习提示

1. **循序渐进**：按顺序学习每个模块，确保理解后再进入下一个
2. **动手实践**：运行每个示例，观察输出结果
3. **调试技巧**：使用 `@Slf4j` 的 `log.debug()` 查看执行过程
4. **扩展学习**：尝试创建自定义工具，扩展 Agent 能力
