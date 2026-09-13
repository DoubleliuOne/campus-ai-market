import { defineConfig } from 'vitepress'
import { withMermaid } from 'vitepress-plugin-mermaid'

export default withMermaid(
  defineConfig({
    lang: 'zh-CN',
    title: 'CampusAI Market',
    description: '基于真实源码的 CampusAI Market 课程、源码解析与面试手册',
    base: '/campus-ai-market/',
    cleanUrls: true,
    lastUpdated: true,
    head: [
      ['meta', { name: 'theme-color', content: '#147a5c' }],
      ['meta', { name: 'author', content: 'CampusAI Market' }],
    ],
    markdown: {
      lineNumbers: true,
      theme: {
        light: 'github-light',
        dark: 'github-dark',
      },
    },
    mermaid: {
      theme: 'neutral',
    },
    themeConfig: {
      siteTitle: 'CampusAI Market',
      nav: [
        { text: '开始学习', link: '/guide/' },
        { text: '源码地图', link: '/guide/source-map' },
        { text: '业务链路', link: '/guide/request-chains' },
        { text: '面试题库', link: '/guide/interview-questions' },
      ],
      sidebar: [
        {
          text: '课程入口',
          items: [
            { text: '学习指南', link: '/guide/' },
            { text: '项目总览与代码基线', link: '/guide/project-overview' },
            { text: '技术栈地图', link: '/guide/tech-stack' },
            { text: '系统整体架构', link: '/guide/architecture' },
            { text: '目录与知识地图', link: '/guide/directory-map' },
          ],
        },
        {
          text: '后端基础',
          items: [
            { text: 'Spring Boot 从零到项目', link: '/guide/spring-boot' },
            { text: 'Controller / Service / Mapper 分层', link: '/guide/backend-layers' },
            { text: '登录、JWT 与 Security', link: '/guide/auth-jwt-security' },
            { text: 'MySQL 与 MyBatis-Plus', link: '/guide/database-mybatis' },
          ],
        },
        {
          text: '业务模块',
          items: [
            { text: '商品完整业务', link: '/guide/item-business' },
            { text: 'Redis 缓存与失效', link: '/guide/redis-cache' },
            { text: '订单状态机与并发', link: '/guide/order-system' },
            { text: '图片上传与持久化', link: '/guide/file-upload' },
          ],
        },
        {
          text: 'AI Agent 与 RAG',
          items: [
            { text: 'Agent 与 Tool Calling', link: '/guide/agent-tool-calling' },
            { text: '多轮会话与用户隔离', link: '/guide/conversation' },
            { text: 'RAG、Embedding 与向量检索', link: '/guide/rag' },
            { text: 'Agent 与 RAG 对比', link: '/guide/agent-vs-rag' },
          ],
        },
        {
          text: '前端与部署',
          items: [
            { text: 'Vue 前端源码学习', link: '/guide/frontend' },
            { text: 'Docker Compose 部署', link: '/guide/docker-deploy' },
          ],
        },
        {
          text: '贯穿复盘',
          items: [
            { text: '六大完整业务链路', link: '/guide/request-chains' },
            { text: '核心源码地图', link: '/guide/source-map' },
          ],
        },
        {
          text: '面试与复习',
          items: [
            { text: '项目面试题库', link: '/guide/interview-questions' },
            { text: '高频追问树与质疑', link: '/guide/interview-challenges' },
            { text: '十二阶段学习路线', link: '/guide/learning-roadmap' },
            { text: '完成检查与当前状态', link: '/guide/completion' },
          ],
        },
      ],
      search: {
        provider: 'local',
        options: {
          translations: {
            button: {
              buttonText: '搜索文档',
              buttonAriaLabel: '搜索文档',
            },
            modal: {
              noResultsText: '没有找到相关内容',
              resetButtonTitle: '清除查询条件',
              footer: {
                selectText: '选择',
                navigateText: '切换',
                closeText: '关闭',
              },
            },
          },
        },
      },
      outline: {
        level: [2, 3],
        label: '本页目录',
      },
      docFooter: {
        prev: '上一页',
        next: '下一页',
      },
      lastUpdated: {
        text: '最后更新',
        formatOptions: {
          dateStyle: 'short',
          timeStyle: 'medium',
        },
      },
      darkModeSwitchLabel: '主题',
      lightModeSwitchTitle: '切换到浅色模式',
      darkModeSwitchTitle: '切换到深色模式',
      sidebarMenuLabel: '目录',
      returnToTopLabel: '返回顶部',
      externalLinkIcon: true,
      socialLinks: [
        {
          icon: 'github',
          link: 'https://github.com/DoubleliuOne/campus-ai-market',
        },
      ],
      footer: {
        message: '基于真实源码生成，不替代亲自阅读代码。',
        copyright: 'CampusAI Market 学习文档',
      },
    },
  }),
)
