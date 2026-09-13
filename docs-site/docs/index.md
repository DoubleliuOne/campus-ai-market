---
layout: home

hero:
  name: CampusAI Market
  text: 从能运行，到真正讲明白
  tagline: 以仓库 b3c73f1 为唯一代码基线，拆解 Spring Boot 交易业务、JWT、Redis、Vue、Docker、AI Agent 与 RAG。
  actions:
    - theme: brand
      text: 开始系统学习
      link: /guide/
    - theme: alt
      text: 查看源码地图
      link: /guide/source-map

features:
  - title: 先读代码，再写教材
    details: 所有关键结论都对应真实类、方法、SQL、配置或前端页面，明确区分事实、推断和学习建议。
  - title: 六条完整业务链路
    details: 登录、商品查询、创建订单、AI 商品搜索、RAG 和多轮会话，从 Vue 一路追到 MySQL、Redis 与 DeepSeek。
  - title: 面向面试的表达
    details: 每项技术回答“是什么、为什么、项目哪里用、具体怎么实现、面试官追问怎么办”。
  - title: 可继续扩展
    details: 同时指出当前实现的边界、风险和优化方向，帮助你理解代码，而不是只背结论。
---

## 这套文档解决什么问题

你不缺一个 README，你需要的是能把项目拆开、重新组装，并在面试中用自己的话解释清楚。本课程围绕三个目标组织：

1. **看懂**：知道一次请求经过哪些类，为什么需要这些层。
2. **讲清**：能说明每个设计选择解决的问题，以及它没有解决的问题。
3. **修改**：改动时可以定位到正确的 Controller、Service、Mapper、DTO、VO 或 Vue 页面。

::: warning 阅读方式
不要按页面顺序背诵。先通读“项目总览 → 系统架构 → 六大链路”，再进入 Spring、数据库、Redis、AI 等专题，最后用面试题库做闭卷复述。
:::

## 推荐入口

| 目标 | 建议页面 |
| --- | --- |
| 先建立全局认识 | [项目总览与代码基线](/guide/project-overview) |
| 理解技术和代码位置 | [技术栈地图](/guide/tech-stack) |
| 看懂整体架构 | [系统整体架构](/guide/architecture) |
| 学最重要的 AI 模块 | [Agent 与 Tool Calling](/guide/agent-tool-calling) |
| 学 RAG | [RAG、Embedding 与向量检索](/guide/rag) |
| 按请求追踪源码 | [六大完整业务链路](/guide/request-chains) |
| 准备面试 | [项目面试题库](/guide/interview-questions) |

## 代码基线

- GitHub：<https://github.com/DoubleliuOne/campus-ai-market>
- 分支：`main`
- 基线提交：`b3c73f186b8a81e1fb763eed45a501b06df604ac`
- 代码与远程 `origin/main` 一致

后续如果代码变化，应以最新提交重新核对本文中的方法、表结构与配置；特别是 RAG、缓存和订单状态机最容易随版本变化。
