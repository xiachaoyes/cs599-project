# XiaAgent — 双模式 AI 智能体系统

## 项目简介
从零构建的双模式 AI Agent 系统，包含房产分析助手（PropertyApp）和超级智能体（XiaManus），运用 ReAct 自主推理、MCP 协议、RAG 知识库等技术，实现"一句话完成复杂任务"——用户只需自然语言描述目标，Agent 自主规划步骤、调用工具、交付结果。

## 方向
方向一：Agentic AI 原生开发

## 技术栈
- AI IDE: Trae CN
- LLM: 阿里云 DashScope (qwen3-coder-plus) / Ollama 本地模型
- 框架: Spring AI 1.0 + Spring AI Alibaba 1.0
- Agent 模式: ReAct (Reasoning + Acting) 自主推理循环
- 协议: MCP (Model Context Protocol)、Function Calling、SSE
- 向量数据库: PgVector (PostgreSQL)
- 前端: Vue 3 + Vite
- 容器: Docker
- 构建工具: Maven

## 目录结构
```
cs599-project/                              ← GitHub 仓库根目录
├── docs/                                   ← 项目文档
│   └── CS599_大作业报告.md                  ← 课程大作业报告
├── src/
│   └── xia-ai-agent-new/                   ← 主项目代码
│       ├── src/main/java/.../agent/        ← Agent 核心（BaseAgent / ReActAgent / ToolCallAgent / XiaManus）
│       ├── src/main/java/.../app/          ← PropertyApp 房产分析助手
│       ├── src/main/java/.../tools/        ← 7 个内置工具（搜索/抓取/下载/文件/终端/PDF/终止）
│       ├── src/main/java/.../rag/          ← RAG 检索增强（PgVector / QueryRewrite）
│       ├── src/main/java/.../controller/   ← REST API 控制器
│       ├── src/main/resources/             ← 配置文件（API Key 全部环境变量注入）
│       ├── src/test/                       ← 测试代码
│       ├── xia-ai-agent-frontend/          ← Vue 3 前端
│       ├── xia-image-search-mcp-server/    ← 自建 MCP Server（图片搜索）
│       ├── Dockerfile                      ← 容器化部署
│       └── pom.xml                         ← Maven 项目描述
├── README.md                               ← 本文件
├── LICENSE                                 ← MIT 开源协议
└── .gitignore                              ← Git 忽略规则
```

## 环境搭建
1. 依赖安装
   - JDK 21+
   - Maven 3.8+
   - Node.js 18+（前端可选）
   - Docker（可选）
2. 环境变量配置（⚠️ 不硬编码 API Key）
   ```bash
   cd src/xia-ai-agent-new
   cp src/main/resources/application-local.yml.example src/main/resources/application-local.yml
   cp src/main/resources/mcp-servers.json.example src/main/resources/mcp-servers.json
   # 编辑 application-local.yml 和 mcp-servers.json，填入真实的 API Key
   ```
3. 启动步骤
   ```bash
   # 1. 构建 MCP Server
   cd src/xia-ai-agent-new/xia-image-search-mcp-server
   mvn clean package -DskipTests
   cd ../..

   # 2. 启动后端
   cd src/xia-ai-agent-new
   mvn spring-boot:run
   # 或 IDE 中直接运行 XiaAiAgentApplication.main()
   # 后端运行在 http://localhost:8123/api

   # 3. 启动前端（可选）
   cd xia-ai-agent-frontend
   npm install && npm run dev
   ```

## 项目状态
- [x] Proposal
- [x] MVP
- [ ] Final

## 开源引用与致谢
| 项目 / 技术 | 用途 | 许可 |
|-------------|------|------|
| [yu-ai-agent](https://github.com/liyupi/yu-ai-agent) | 项目架构与实现参考 | MIT |
| [Spring AI](https://github.com/spring-projects/spring-ai) | AI 集成框架 | Apache 2.0 |
| [Spring AI Alibaba](https://github.com/alibaba/spring-ai-alibaba) | 阿里云 DashScope 集成 | Apache 2.0 |
| [OpenManus](https://github.com/mannaandpoem/OpenManus) | ReAct Agent 架构参考 | MIT |
| [高德地图 MCP Server](https://github.com/amap/amap-maps-mcp-server) | 地图服务 | — |
| [Hutool](https://github.com/dromara/hutool) | Java 工具库 | MulanPSL-2.0 |
| [iText 7](https://github.com/itext/itext-java) | PDF 生成 | AGPL |
| [Jsoup](https://github.com/jhy/jsoup) | HTML 解析 | MIT |
| [Kryo](https://github.com/EsotericSoftware/kryo) | 序列化框架 | BSD-3 |
