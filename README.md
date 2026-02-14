# YouAI

## 项目简介
`YouAI` 是一个面向 AI Agent 学习与实践的代码仓库，重点是把常见 Agent 能力从概念落到可运行的工程实现。

当前核心子项目是 `springboot-agent`，使用 Java + Spring Boot 实现了从基础调用到进阶 Agent 的完整链路，适合做：
- AI 应用开发入门
- Python Agent 思路迁移到 Java
- 工具调用、记忆、ReAct 等能力拆解学习

## 核心能力
- Prompt 工程基础示例（系统提示词、结构化输出、Few-shot）
- LLM 基础调用与参数控制
- 流式输出（SSE）
- Tool Calling（工具注册、工具执行、结果回填）
- ReAct 模式（Reasoning + Acting）
- 多轮对话记忆（窗口记忆、Token 记忆等）
- 项目化 Agent 示例（Chat Agent、Tool Agent）

## 技术栈
- Java 21
- Spring Boot 3.3
- Maven
- OkHttp
- Jackson
- Lombok

## 仓库结构
- `springboot-agent/`：Spring Boot 版 AI Agent 学习项目（主项目）

## 适用人群
- 想系统学习 AI Agent 工程化开发的开发者
- 想把 Python 版本 Agent 迁移到 Java 生态的同学
- 需要一个可运行、可扩展的 Agent 示例项目作为模板
