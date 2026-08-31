# Copy Path Tool
One plugin for all JetBrains IDEs: **IntelliJ IDEA、PyCharm、GoLand、PhpStorm、WebStorm** 以及其它基于 IntelliJ Platform 的 IDE（2024.1 及以上）。

> **已验证环境**
> - 平台行为测试 10/10 通过（真实 2024.2.4 平台：扁平菜单结构、分区动态显隐、None/@/# 前缀、剪贴板内容、行号范围、边界场景）
> - **PhpStorm 2026.2.1（PS-262.9437.196）** 真实环境端到端验证通过（v1.4.0）
> - **WebStorm 2026.1.1（WS-261.24374.125）** 真实环境端到端验证通过（v1.4.0）

## 功能说明

编辑器内右键 → **Copy Path Tool**（菜单位于右键菜单最顶部），为扁平二级菜单，**分区随选中状态动态显隐**：

- **未选中代码**：只显示两个 File 分区（复制整个文件路径，不带行号）
- **选中代码后**：只显示两个 Code Block 分区（复制“路径:行号”，自带所选行号范围）

分区标题仅作展示（置灰、不可点击），子项带 4 空格缩进和 `-` 前缀，**点击子项一步完成复制**：

```
Copy Path Tool
├── Copy File Disk Path               ← 未选中代码时显示
│   ├── - None        → D:/work/demo/src/App.java
│   ├── - @ Prefix    → @D:/work/demo/src/App.java
│   └── - # Prefix    → #D:/work/demo/src/App.java
└── Copy File Project Path
    ├── - None        → src/main/java/App.java
    ├── - @ Prefix    → @src/main/java/App.java
    └── - # Prefix    → #src/main/java/App.java

（选中代码后菜单变为：）
Copy Path Tool
├── Copy Code Block Disk Path
│   ├── - None        → D:/work/demo/src/App.java:12-34
│   ├── - @ Prefix    → @D:/work/demo/src/App.java:12-34
│   └── - # Prefix    → #D:/work/demo/src/App.java:12-34
└── Copy Code Block Project Path
    └── - None / - @ Prefix / - # Prefix   → src/App.java:12-34 及其前缀变体
```

细节约定：

- 行号为编辑器中显示的行号（从 1 开始）；只选中单行时形如 `App.java:12`。
- 选区恰好结束在某行行首时，该空行不计入行号范围。
- **路径统一使用正斜杠 `/`**（含 Windows 磁盘路径）：粘贴到 agent 应用的 Markdown 对话框中不会被转义吞字符，Windows 下所有工具链同样原样识别。
- 项目路径优先相对**项目根目录**，多模块项目下若文件不在项目根目录之下，则相对其上层内容根目录。
- 非 IDE 内置文件（如 jar 包内的反编译类）菜单项置灰。

### 前缀怎么选

| 子项 | 适用场景 |
| --- | --- |
| None | **默认推荐**。所有 agent 应用都识别纯路径文本（模型通过文件读取工具解析），最稳妥 |
| @ Prefix | Qoder / ZCode 等使用 `@` 提及约定的应用。**建议搭配 Copy File Project Path 使用**（`@` 后接项目相对路径，这类应用输入 `@` 时会实时过滤文件选择器）；直接粘贴 `@绝对路径` 在多数应用中会退化为普通文本，但模型同样可理解 |
| # Prefix | Trae 系应用使用 `#` 引用约定时选用 |

## 在 AI Agent 应用中使用（推荐格式）

复制的路径专为本类 agent 应用的对话上下文设计：**ZCode、Kimi Work、Qoder Work、WorkBuddy、Trae Work** 等。

- **整文件上下文**：粘贴 `Copy File Disk Path → None` 的结果即可，例如：

  ```
  D:/work/demo/src/main/java/com/demo/App.java
  ```

  agent 会按该绝对路径直接读取文件（绝对路径不依赖 agent 当前工作目录，最稳妥）。

- **代码块上下文（带行号范围）**：选中代码后使用 `Copy Code Block Disk Path → None`，例如：

  ```
  D:/work/demo/src/main/java/com/demo/App.java:12-34
  ```

  `文件:起始行-结束行` 是通用的“定位到行”约定，agent 会读取该文件并关注指定行范围；单行时为 `文件:12`。

- **相对路径 / @ 提及**：agent 已工作在该项目目录内时可用 `Copy File Project Path → None`；对 Qoder / ZCode 这类支持 `@` 提及的应用，先在输入框敲 `@` 再粘贴相对路径（会实时过滤文件选择器），或直接使用 `Copy File Project Path → @ Prefix` 的结果。

> 提示：各应用对上下文引用的入口不同（`@` 提及、`#` 引用、直接粘贴），但纯路径文本在上述应用中均被识别；绝对路径 + 行号范围的组合兼容性最好。

## 安装方法

### 方式一：从磁盘安装（推荐，无需编译）

1. 打开目标 IDE（IDEA / PyCharm / GoLand / PhpStorm / WebStorm 均可）；
2. 进入 `Settings`（macOS 为 `Preferences`）→ `Plugins`；
3. 点击右上角 **⚙ 齿轮图标** → **Install Plugin from Disk...**；
4. 选择安装包 `build/distributions/jetbrains-copy-path-tool-1.4.0.zip`（**无需解压**）；
5. 点击 OK 后按提示 **Restart IDE**；
6. 每个需要使用的 IDE 分别执行一次。

### 方式二：手动安装（解压到配置目录）

将 zip 解压出的 `jetbrains-copy-path-tool` 文件夹放到 IDE 配置目录的 `plugins` 下（Windows 示例）：

```
%APPDATA%\JetBrains\PhpStorm2026.2\plugins\jetbrains-copy-path-tool
%APPDATA%\JetBrains\WebStorm2026.1\plugins\jetbrains-copy-path-tool
%APPDATA%\JetBrains\GoLand2026.x\plugins\jetbrains-copy-path-tool
%APPDATA%\JetBrains\IntelliJIdea2026.x\plugins\jetbrains-copy-path-tool
```

重启 IDE 生效。

### 更新与卸载

- **更新**：用同样方式安装新版本 zip 覆盖（或先在 Plugins 中卸载旧版再装）。
- **卸载**：`Settings` → `Plugins` → 选中 `Copy Path Tool` → `Uninstall` → 重启 IDE。

> **本机当前状态**：v1.4.0 已安装并启用于 PhpStorm 2026.2 与 WebStorm 2026.1 的用户配置目录，打开即用。

## 从源码构建

构建环境要求：JDK 17～23（用于运行 Gradle；注意 PhpStorm 2026.2 自带的 JBR 是 Java 25，不能直接用）、可访问 Maven 仓库的网络。

```bash
# Linux / macOS
./gradlew test buildPlugin

# Windows
gradlew.bat test buildPlugin
```

- `test`：运行 10 个平台行为测试（基于 IntelliJ Platform Test Framework）
- `buildPlugin`：产出安装包 `build/distributions/jetbrains-copy-path-tool-1.4.0.zip`

### 本机（Windows）已验证的构建方式

本机没有独立安装 JDK，构建时使用了下载的 OpenJDK 21，并将 Gradle 用户目录重定向到 D 盘（C 盘空间紧张）：

```bash
export JAVA_HOME="D:/myProject/.gradle-home/jdk-21.0.2"
export GRADLE_USER_HOME="D:/myProject/.gradle-home/home"
/d/myProject/.gradle-home/gradle-8.13/bin/gradle --no-daemon test buildPlugin
```

> 首次构建会下载 IntelliJ IDEA Community 2024.2.4 作为编译目标（约 1 GB，已缓存到 `D:/myProject/.gradle-home/home`），之后构建走缓存，速度很快。

## 内置自检（可选）

插件内置一个默认休眠的自检启动项，用于在任意真实 IDE 中做端到端验证。启动 IDE 时附加：

```
-Dcopypath.selftest          # 激活自检（任意值即可）
-Dcopypath.selftest.file=README.md   # 自检时要打开的项目内文件名（可选）
```

项目打开后，自检会自动打开目标文件，分别执行“未选中代码 / 选中代码”两种场景下的两个复制动作，
并把**菜单文案与剪贴板实际内容**写入报告：`<IDE 系统目录>/log/copypath-selftest-report.txt`（`result=OK` 即全部通过）。
不带该系统属性时自检完全不运行，对正常使用零影响。

## 项目结构

```
├── build.gradle.kts                 # 构建配置（IntelliJ Platform Gradle Plugin 2.x）
├── settings.gradle.kts
├── gradle.properties
└── src/main/
    ├── kotlin/com/copypathtool/
    │   ├── CopyPathAction.kt        # 前缀叶子动作（None/@/#，File/Code Block 两种模式）+ 路径计算
    │   ├── CopyPathGroup.kt         # “Copy Path Tool” 顶级子菜单：分区标题 + 12 个动作（无编辑器时自动隐藏）
    │   └── SelfTestActivity.kt      # 内置自检启动项（默认休眠）
    └── resources/META-INF/
        ├── plugin.xml               # 插件声明：注册到 EditorPopupMenu，兼容所有平台系 IDE
        ├── pluginIcon.svg
        └── pluginIcon_dark.svg
```

## 二次修改指南

- **菜单文案**：分区标题在 `CopyPathGroup.kt` 的 `init` 中（四个 `Separator.create(...)`），前缀子项文案在 `CopyPathAction.kt` 的 `PrefixCopyAction` 的 `init` 中。
- **行号格式**：改 `CopyPathAction.kt` 的 `lineRangeSuffix()`，例如想改成 GitHub 风格 `#L12-L34`。
- **路径分隔符**：改 `CopyPathAction.kt` 的 `diskPath()`（当前统一正斜杠，agent 对话友好）。
- **菜单位置**：改 `plugin.xml` 中 `<add-to-group>` 的 `anchor`（当前为 `first`，即右键菜单最顶部）。
- **兼容版本下限**：改 `build.gradle.kts` 中 `sinceBuild`（当前 `241` = 2024.1）。

修改后重新执行 `test buildPlugin` 生成新 zip。
