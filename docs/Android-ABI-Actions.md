# GitHub Actions 分架构打包说明

## 使用

工作流：`.github/workflows/build.yml`（**Android ABI APKs**）。

1. 这个仓库是上游仓库的 Fork。如果 Actions 页面要求启用工作流，请在仓库 **Actions** 页点击启用，并在 **Settings → Actions → General** 确认允许第三方 Actions。
2. 功能分支合并进默认分支 `BiLi_PC_Gamer` 后，进入 **Actions → Android ABI APKs → Run workflow**，选择默认分支并运行。也支持向默认分支 push 自动触发；功能分支的 PR 可触发验证。
3. 打包成功后，在该次 workflow run 页面底部的 **Artifacts** 区域分别下载 `EhViewer-arm64-v8a`、`EhViewer-armeabi-v7a`、`EhViewer-x86_64`、`EhViewer-x86` 和 `EhViewer-universal`。
4. 每个 Artifacts ZIP 内都只有对应架构的 APK。工作流会检查五种输出全部存在，缺失即失败，保留 30 天。

## 签名及安装注意事项

目前 CI 构建 **appReleaseDebug** 调试变体（项目自带 Android debug keystore），所以 APK 可以直接用于测试，但包名使用现有调试后缀 `.debug1`，**不能覆盖安装既有正式版**。CI 无需上传私人签名文件或添加 GitHub Secrets。

尚未配置正式版签名。发布可以覆盖安装原正式应用的正式版 APK，需要单独配置原有正式签名证书及安全的 GitHub Actions Secrets；不要上传密钥到 Git 仓库，也不要把调试包标为正式版本。

构建不会改变普通本地的多架构打包行为，只有在使用 `-PsplitAbiApks=true` 时生成五种独立 APK。

## 诊断

- 工作流没出现 / 没有运行记录：优先检查 Fork 的 Actions 是否已启用，工作流是否允许运行。GitHub 不会在禁用时自动运行。
- 构建失败：打开失败步骤的 Gradle 日志；当前项目采用 JDK 21、Android SDK 35 和 CMake 3.22.1。
- 缺少某架构 APK：`Verify and rename APK outputs using Gradle metadata` 会报告缺失的 ABI，避免上传错误产物。
- `Run workflow` 不显示：GitHub 只允许对默认分支上的工作流手动触发，因此需要先合并包含工作流的 PR。
