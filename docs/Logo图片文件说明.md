# Logo图片文件说明

## 📋 需要放置的图片文件

**⚠️ 重要提示：** 当前已创建占位符文件，系统可以正常构建和运行。但请尽快将您提供的Logo图片文件替换占位符。

**所有Logo现在统一使用 `/assets/images/logo.png` 路径，不再使用代码生成的SVG文件。**

请将您提供的Logo图片文件（包含书本、铅笔、叶子和"择学"文字的图片）放置到以下位置：

### 1. 后台管理系统 Logo

**文件路径：** `ruoyi-ui/public/assets/images/logo.png`

**用途：**
- 侧边栏Logo（展开和折叠状态）
- 浏览器标签页图标（Favicon）
- 建议尺寸：280x200px 或更高分辨率（保持比例）

### 2. 客户端 Logo

**文件路径：** `exam-client/public/assets/images/logo.png`

**用途：**
- 客户端登录页Logo
- 客户端试卷选择页Logo
- 建议尺寸：280x200px 或更高分辨率（保持比例）

## 📝 文件格式要求

- **格式：** PNG（推荐）或 JPG
- **背景：** 透明背景（PNG）或白色背景
- **分辨率：** 建议使用高分辨率图片，确保在不同尺寸下显示清晰

## ✅ 已更新的文件

以下文件已更新为使用 `/assets/images/logo.png` 路径：

1. `ruoyi-ui/src/layout/components/Sidebar/Logo.vue` - 侧边栏Logo，使用 `/assets/images/logo.png`
2. `ruoyi-ui/public/index.html` - Favicon引用，使用 `/assets/images/logo.png`
3. `exam-client/src/renderer/views/Login.vue` - 客户端登录页Logo，使用 `/assets/images/logo.png`
4. `exam-client/src/renderer/views/PaperSelect.vue` - 客户端试卷选择页Logo，使用 `/assets/images/logo.png`

## 🗑️ 已删除的文件

以下代码生成的SVG文件已被删除：

1. `ruoyi-ui/src/assets/images/logo.svg` - 已删除
2. `ruoyi-ui/public/favicon.svg` - 已删除

## 🚀 使用步骤

1. 将您的Logo图片文件重命名为 `logo.png`
2. **直接替换** `ruoyi-ui/public/assets/images/logo.png`（当前为占位符文件）
3. **直接替换** `exam-client/public/assets/images/logo.png`（当前为占位符文件）
4. 刷新页面查看效果

**注意：** 两个位置都需要替换，后台和客户端使用相同的Logo文件。

## 📌 注意事项

- 确保图片文件名与代码中引用的文件名一致
- 如果使用JPG格式，需要将代码中的 `.png` 改为 `.jpg`
- 图片路径使用相对路径，确保路径正确

