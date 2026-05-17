# 🚀 OnlineCode

> 纯前端离线多语言编译器 | 支持Python/C/C++/Rust/Go | 3D绘图 | 移动端适配 | 无需后端

[![GitHub Pages](https://img.shields.io/badge/GitHub%20Pages-Live-brightgreen)](https://badhope.github.io/OnlineCode)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)

## ✨ 特性

- 🌐 **纯前端** - 无需后端服务器，完全在浏览器中运行
- 🔄 **离线支持** - PWA技术，可离线使用
- 🐍 **多语言** - Python, C/C++, Rust, Go, JavaScript
- 📦 **包管理** - 在线安装Python包（通过Pyodide）
- 🎨 **3D绘图** - 内置Three.js支持3D可视化
- 📱 **移动端** - 响应式设计，手机平板完美适配
- 🎨 **深色主题** - 舒适的代码编辑体验
- 💾 **自动保存** - 代码自动保存到本地

## 🚀 在线体验

👉 **[https://badhope.github.io/OnlineCode](https://badhope.github.io/OnlineCode)**

## 🛠️ 技术栈

- **前端框架**: 原生 JavaScript (ES6+)
- **Python运行时**: [Pyodide](https://pyodide.org/) (WebAssembly)
- **3D绘图**: [Three.js](https://threejs.org/)
- **样式**: CSS3 + Flexbox/Grid
- **PWA**: Service Worker + Manifest

## 📦 项目结构

```
OnlineCode/
├── index.html          # 主页面
├── manifest.json       # PWA配置
├── sw.js              # Service Worker
├── styles/
│   └── main.css       # 主样式
├── js/
│   ├── app.js         # 主应用逻辑
│   ├── editor.js      # 代码编辑器
│   ├── compiler.js    # 编译器模块
│   └── packages.js    # 包管理器
└── assets/            # 图标资源
```

## 🏃 本地运行

```bash
# 克隆仓库
git clone https://github.com/badhope/OnlineCode.git
cd OnlineCode

# 启动本地服务器
python3 -m http.server 8000

# 访问 http://localhost:8000
```

## 🗺️ 开发路线图

- [x] 基础框架搭建
- [x] 代码编辑器
- [x] Python执行 (Pyodide)
- [x] JavaScript执行
- [x] 包管理器UI
- [x] PWA支持
- [ ] C/C++编译 (WebAssembly)
- [ ] Rust编译 (WebAssembly)
- [ ] Go编译 (WebAssembly)
- [ ] 3D绘图功能
- [ ] 代码分享功能
- [ ] 主题切换
- [ ] 文件系统支持

## 🤝 贡献

欢迎提交Issue和PR！

## 📄 许可证

MIT License
