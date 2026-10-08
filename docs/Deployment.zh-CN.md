# 部署说明

[English](Deployment.md) · [简体中文](Deployment.zh-CN.md)

## 构建目标

交付物为由仓库内 Flutter/Gradle 工程构建的 Android APK。发布构建启用 R8 代码缩减和资源缩减。构建三个 ARMCP APK：

```bash
bash script/build_android_apks.sh
```

Flutter 和 Gradle 中间文件保存在 `build/`。三个发布 APK 直接保存在 `release/`。用于 Play 商店发布时，条件允许则优先使用 Android App Bundle。

## 运行模式

- **关闭：**不运行 MCP 监听器。
- **回环：**供设备本地调试的认证监听器；为默认绑定模式。
- **LAN：**显式打开 MCP 总开关并取得所需 Android 网络权限后，在局域网接口运行认证监听器。

当前实现监听 `8787` 端口，MCP 启用时绑定所有本地接口，并公布当前非回环 IPv4 地址。设备没有非回环地址时，公布地址会回退为仅本机可访问的回环地址。

Flutter 界面显示当前调试地址 `/debug/` 和 MCP 端点 `/mcp`。只有 MCP 总开关已启用且监听器报告运行时才能访问。调试页面使用 `GET`；MCP 端点要求经过认证的 `POST`。有可用地址时，显示设备当前 LAN 地址。

## 网络策略

LAN 部署优先使用 TLS。初期调试构建若使用明文，必须明确标识、限定在受控网络，并说明其为临时开发模式。Android 本地网络权限须遵循目标 SDK；面向 Android 17 的构建需显式处理 `ACCESS_LOCAL_NETWORK` 或采用获批的系统媒介方案。

## 发布检查清单

- 在目标 API 级别验证 Root 和 UI 后端能力声明。
- 确认服务默认关闭。
- 确认未经认证不会暴露密钥、调试操作或不受限 Shell。
- 根据已提交配置确认发布签名和版本值。
- 完成构建、安装、启动和针对性集成测试，再运行完整配置检查。

## CI/CD

`main` 分支在 `.github/workflows/ci.yml` 和 `.gitea/workflows/ci.yml` 中都配置了自动 CI。推送到 `main` 或创建目标为 `main` 的 Pull Request 时，CI 会执行依赖解析、Dart 分析、Flutter 测试和 Debug APK 构建。

发布 CD 只允许手动运行。在 `main` 分支手动 dispatch `Release` 工作流，填写一个已经存在且属于 `main` 历史的 `v` 开头版本标签和标题，并将 `confirm_release` 精确设置为 `true`。工作流会验证该标签属于所选 `main` 历史，然后构建三个 APK，并通过已配置的主机 API 创建 Release 和上传 APK。CI 也会在推送 `v*` 标签时运行。

在每个代码托管平台配置以下仓库变量：

- `RELEASE_API_URL`：创建 Release 的 API 基地址，例如 `https://api.github.com` 或 `https://gitea.example.com/api/v1`。
- `RELEASE_UPLOAD_URL`：上传资产的 API 基地址，例如 `https://uploads.github.com` 或 Gitea API 基地址。
- `RELEASE_REPOSITORY`：`owner/repository`。

将具有创建 Release 和上传资产权限的凭据配置为 Actions secret `RELEASE_TOKEN`。GitHub 还可以为环境配置 required reviewers，但由于 Gitea 会忽略 `jobs.<job_id>.environment`，显式的手动 dispatch 确认输入仍是跨平台的审批门槛。

## ARMCP APK 包

| 文件 | Android ABI | Flutter 输出 |
| --- | --- | --- |
| `release/armcp-v8.apk` | `arm64-v8a` | `app-arm64-v8a-release.apk` |
| `release/armcp-v7.apk` | `armeabi-v7a` | `app-armeabi-v7a-release.apk` |
| `release/armcp-universe.apk` | `arm64-v8a`、`armeabi-v7a`、`x86_64` | `app-release.apk` |

统一运行 `bash script/build_android_apks.sh` 构建全部三个包。`--split-per-abi` 构建架构专用 APK，最后一次构建生成通用 APK。脚本将这三个发布包直接复制到 `release/`。`x86_64` 单架构包保留在中间构建目录，不属于发布包集合。
