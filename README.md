# 2411盘

2411 Pan全部版本源码整合地址
https://www.ilanzou.com/s/0oFKPtSl

构建版演示APK
2411092.xyz



## Android Studio 构建 APK
1. 用 Android Studio 打开项目目录。
2. 等待 Gradle 同步。
3. 点击 `Build > Build APK(s)`。
4. APK 在 `app/build/outputs/apk/debug/app-debug.apk`。

## GitHub 自动打包 APK
项目已包含 `.github/workflows/build-apk.yml`。上传到 GitHub 后，在 Actions 里运行 `Build APK`，即可下载构建产物。


by落璃
