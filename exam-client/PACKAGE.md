# 打包命令说明

## Windows 打包

```bash
# 1. 先构建应用
npm run build:win

# 2. 打包为安装程序
npm run pack:win
```

打包后的文件：
- `dist/考试客户端 Setup x.x.x.exe` - Windows 安装程序

## macOS 打包

```bash
# 1. 先构建应用
npm run build:mac

# 2. 打包为 DMG
npm run pack:mac
```

打包后的文件：
- `dist/考试客户端-x.x.x.dmg` - macOS 磁盘镜像

## 注意事项

### Windows
- 打包需要 Windows 环境
- 生成的 `.exe` 文件可以直接分发
- 如果遇到代码签名问题，可以暂时跳过（仅开发测试）

### macOS
- 打包需要 macOS 环境
- 生成的 `.dmg` 文件可以直接分发
- **重要**：未签名的应用在 macOS 上可能无法直接运行
  - 解决方法1：右键点击应用 → 打开
  - 解决方法2：在终端执行 `xattr -cr /path/to/考试客户端.app`
  - 解决方法3：在"系统偏好设置" → "安全性与隐私" → 允许运行

### 跨平台打包

如果需要在一个平台上打包多个平台的应用，可以使用：

```bash
# 安装 electron-builder 的依赖
npm install --save-dev electron-builder

# 打包所有平台（需要对应平台的工具链）
npm run pack
```

## 打包配置

打包配置在 `package.json` 的 `build` 字段中：

```json
{
  "build": {
    "appId": "com.exam.client",
    "productName": "考试客户端",
    "win": {
      "target": ["nsis"],
      "icon": "build/icon.ico"
    },
    "mac": {
      "target": ["dmg"],
      "icon": "build/icon.icns"
    }
  }
}
```

## 图标文件

需要准备以下图标文件（放在 `build/` 目录）：

- `icon.ico` - Windows 图标（256x256）
- `icon.icns` - macOS 图标（512x512）

如果没有图标文件，打包时会使用默认图标。

