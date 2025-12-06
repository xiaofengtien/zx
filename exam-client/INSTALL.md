# 安装和运行指南

## 1. 安装依赖

```bash
cd exam-client
npm install
```

**注意**：如果 `better-sqlite3` 安装失败，可能需要安装编译工具：

### macOS
```bash
# 确保已安装 Xcode Command Line Tools
xcode-select --install
```

### Windows
需要安装 Visual Studio Build Tools 或完整版 Visual Studio。

## 2. 配置环境变量

复制 `.env.example` 为 `.env` 并修改 API 地址：

```bash
cp .env.example .env
```

编辑 `.env` 文件，设置后端 API 地址：
```
API_BASE_URL=http://localhost:8080/dev-api
```

## 3. 开发运行

```bash
npm run dev
```

这将启动：
- Webpack 开发服务器（端口 9080）
- Electron 应用窗口

## 4. 构建应用

### 构建所有文件
```bash
npm run build
```

### 构建特定平台
```bash
# Windows
npm run build:win

# macOS
npm run build:mac

# Linux
npm run build:linux
```

## 5. 打包应用

### Windows 打包
```bash
npm run pack:win
```

打包后的文件位置：`dist/考试客户端 Setup x.x.x.exe`

### macOS 打包
```bash
npm run pack:mac
```

打包后的文件位置：`dist/考试客户端-x.x.x.dmg`

**注意**：macOS 打包需要代码签名，否则可能无法运行。开发测试时可以：

1. 右键点击应用 → 打开
2. 或者在终端执行：
```bash
xattr -cr /path/to/考试客户端.app
```

## 6. 常见问题

### better-sqlite3 编译失败

**macOS**:
```bash
npm install --build-from-source better-sqlite3
```

**Windows**:
确保已安装 Visual Studio Build Tools，然后：
```bash
npm install --build-from-source better-sqlite3
```

### Electron 版本问题

如果遇到 Electron 版本兼容问题，可以尝试：
```bash
npm install electron@latest --save-dev
```

### 数据库文件位置

数据库文件自动存储在应用数据目录，无需手动管理：
- **Windows**: `C:\Users\{用户名}\AppData\Roaming\exam-client\exam.db`
- **macOS**: `~/Library/Application Support/exam-client/exam.db`

## 7. 测试登录

### 在线登录测试
1. 确保后端服务已启动（http://localhost:8080）
2. 在登录页面选择"在线登录"
3. 输入学员账号、密码和验证码
4. 登录成功后会自动保存凭证到本地数据库

### 离线登录测试
1. 先进行一次在线登录（保存凭证）
2. 关闭应用或断开网络
3. 重新打开应用，选择"离线登录"
4. 输入之前登录的学员账号和密码
5. 应该能够成功离线登录

