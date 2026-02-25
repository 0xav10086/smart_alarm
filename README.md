# Smart Sleep Alarm (Modern Edition)

![LibXposed](https://img.shields.io/badge/LibXposed-Module-blueviolet.svg) ![Root](https://img.shields.io/badge/Root-Required-red.svg) ![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)

## 📖 简介 (Introduction)

这是一个利用 **Xposed 注入** 与 **Root 权限** 技术，深度整合 **Gadgetbridge** 数据的智能唤醒闹钟。

与传统定时闹钟不同，**Smart Sleep Alarm** 会根据你的实际入睡时间动态调整闹钟，并利用手环检测到的生理数据（如浅睡状态）在最合适的时机将你唤醒。

> Magisk 模块相关由 [Bemly](https://github.com/Bemly/smart_alarm) 提供，本人没有能力去测试 Magisk 相关的代码:(
``
## ✨ 核心特性 (Features)

*   **💉 Xposed 深度注入**：基于 `LibXposed` 框架，直接注入 Gadgetbridge 进程 (`nodomain.freeyourgadget.gadgetbridge`)，实现更高效的数据监听与逻辑触发。
*   **🔓 Root 数据库访问**：集成 `libsu` 库，通过 Root 权限直接读取 Gadgetbridge 的本地 SQLite 数据库，获取精确到分钟的运动强度 (`RAW_INTENSITY`) 与睡眠样本。
*   **😴 弹性睡眠保障**：根据实际入睡时间计算睡眠时长，确保在保障基础睡眠的前提下进行唤醒。
*   **🌅 智能柔和唤醒**：分析 `MI_BAND_ACTIVITY_SAMPLE` 等数据表，在设定的唤醒窗口内检测到浅睡/活动时立即响铃，告别起床气。
*   **📱 原生控制界面**：采用 Material Design 构建的 App 界面，支持实时测试 Root 权限及数据库连接状态。

## 🛠️ 技术栈 (Tech Stack)

*   **语言**: Kotlin
*   **Hook 框架**: [LibXposed](https://github.com/libxposed/api) (API 1.0.0+)
*   **Root 框架**: [libsu](https://github.com/topjohnwu/libsu) (6.0.0+)
*   **依赖管理**: Gradle Version Catalog (libs.versions.toml)
*   **数据源**: Gadgetbridge SQLite Database

## 📋 前置要求 (Prerequisites)

1.  **环境**: 已安装 LSPosed (或支持 LibXposed 的管理器) 的 Root 设备。
2.  **软件**: 已安装 **Gadgetbridge** 且已有同步的手环数据。
3.  **设备**: 支持睡眠监测的 Wearable 设备。

## 📂 项目结构 (Project Structure)

*   `app/src/main/java/.../ModuleMain.kt`: Xposed 模块入口，处理进程注入逻辑。
*   `app/src/main/java/.../MainActivity.kt`: 主界面，负责 UI 交互、权限请求及数据库测试。
*   `gradle/libs.versions.toml`: 统一版本控制中心。

## 🚀 开发与调试 (Development)

### 编译环境
- Android Studio Iguana (2023.2.1) 或更高版本。
- JDK 21。
- Gradle 8.7+。

### 调试步骤
1.  编译生成 APK 并安装到设备。
2.  在 LSPosed 管理器中激活本模块，并勾选 **Gadgetbridge** 作为作用域。
3.  打开本应用，授予 **Root 权限**。
4.  点击 “读取数据库” 按钮验证数据链路是否通畅。
5.  查看 Logcat 过滤 `Smart Alarm` 标签查看注入日志。

## ⚙️ 关键配置 (Configuration)

目前项目正处于从 Shell 迁移至 Kotlin 的阶段。数据库路径硬编码为：
`/data/data/nodomain.freeyourgadget.gadgetbridge/databases/gadgetbridge`

## ⚠️ 免责声明

*   本模块通过直接读取其他 App 的私有数据库工作，由于 Gadgetbridge 数据库结构可能随更新改变，请关注项目更新。
*   **请务必设置一个系统自带的保底闹钟**，以免因进程被杀或 Hook 失效导致迟到。

---
Author: [0xav10086](https://github.com/0xav10086)