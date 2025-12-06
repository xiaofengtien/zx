# 考试客户端 (Exam Client)

基于 Electron + Vue 的桌面考试客户端应用。

## 功能特性

- ✅ 在线登录（需要验证码）
- ✅ 离线登录（使用本地凭证）
- ✅ SQLite 本地数据库存储
- ✅ 试卷类型选择
- ✅ 答题页面（待实现）

## 开发环境要求

- Node.js >= 14.0.0
- npm >= 6.0.0

## 安装依赖

```bash
npm install
```

## 开发运行

```bash
npm run dev
```

## 构建应用

### 构建所有平台
```bash
npm run build
```

### 构建 Windows
```bash
npm run build:win
```

### 构建 macOS
```bash
npm run build:mac
```

### 构建 Linux
```bash
npm run build:linux
```

## 打包应用

### 打包 Windows
```bash
npm run pack:win
```

### 打包 macOS
```bash
npm run pack:mac
```

打包后的文件在 `dist` 目录下。

## 配置

### API 地址配置

在 `.env` 文件中配置后端 API 地址：

```
API_BASE_URL=http://localhost:8080/dev-api
```

## 数据库

SQLite 数据库文件自动存储在应用数据目录：

- **Windows**: `C:\Users\{用户名}\AppData\Roaming\exam-client\exam.db`
- **macOS**: `~/Library/Application Support/exam-client/exam.db`
- **Linux**: `~/.config/exam-client/exam.db`

## 项目结构

```
exam-client/
├── .electron-vue/          # Electron 构建配置
├── src/
│   ├── main/               # Electron 主进程（待实现）
│   ├── renderer/           # Vue 渲染进程
│   │   ├── views/         # 页面组件
│   │   ├── router/        # 路由配置
│   │   └── main.js        # 入口文件
│   └── database/          # 数据库相关
│       ├── db.js          # 数据库初始化
│       └── loginService.js # 登录服务
├── build/                  # 构建资源（图标等）
├── dist/                   # 构建输出
└── package.json
```

## 开发说明

### 登录流程

1. **在线登录**：
   - 输入学员账号、密码、验证码
   - 调用后端 `/student/onlineLogin` 接口
   - 登录成功后保存凭证到本地数据库

2. **离线登录**：
   - 输入学员账号、密码
   - 从本地数据库验证凭证
   - 验证通过后进入试卷选择页面

### 数据库表结构

- `student_credentials`: 存储学员凭证信息
- `student_papers`: 存储学员适用考卷类型

## 许可证

MIT

