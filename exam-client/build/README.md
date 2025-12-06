# 图标文件说明

## 需要的图标文件

### Windows
- `icon.ico` - Windows图标文件（256x256或512x512，包含多个尺寸）

### macOS
- `icon.icns` - macOS图标文件（包含多个尺寸：16x16, 32x32, 128x128, 256x256, 512x512, 1024x1024）

## 如何生成图标文件

### 从PNG生成ICO（Windows）
可以使用在线工具或ImageMagick：
```bash
# 使用ImageMagick（如果已安装）
convert icon.png -define icon:auto-resize=256,128,64,48,32,16 icon.ico
```

### 从PNG生成ICNS（macOS）
可以使用在线工具或iconutil（macOS自带）：
```bash
# 1. 创建iconset目录
mkdir icon.iconset

# 2. 生成不同尺寸的图片（需要1024x1024的源图）
sips -z 16 16     icon.png --out icon.iconset/icon_16x16.png
sips -z 32 32     icon.png --out icon.iconset/icon_16x16@2x.png
sips -z 32 32     icon.png --out icon.iconset/icon_32x32.png
sips -z 64 64     icon.png --out icon.iconset/icon_32x32@2x.png
sips -z 128 128   icon.png --out icon.iconset/icon_128x128.png
sips -z 256 256   icon.png --out icon.iconset/icon_128x128@2x.png
sips -z 256 256   icon.png --out icon.iconset/icon_256x256.png
sips -z 512 512   icon.png --out icon.iconset/icon_256x256@2x.png
sips -z 512 512   icon.png --out icon.iconset/icon_512x512.png
sips -z 1024 1024 icon.png --out icon.iconset/icon_512x512@2x.png

# 3. 生成icns文件
iconutil -c icns icon.iconset -o icon.icns
```

## 在线工具推荐

- **ICO转换**: https://convertio.co/zh/png-ico/
- **ICNS转换**: https://cloudconvert.com/png-to-icns
- **全平台图标生成**: https://www.icoconverter.com/

## 注意事项

1. 源图片建议使用1024x1024或更大的正方形PNG
2. 图标应该有透明背景（PNG格式）
3. 图标内容应该清晰，在小尺寸下也能识别



