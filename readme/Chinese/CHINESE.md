	 ![Logo](core/assets-raw/sprites/ui/logo.png)

 [![Build Status](https://travis-ci.org/Anuken/Mindustry.svg?branch=master)](https://travis-ci.org/Anuken/Mindustry) 
 [![Discord](https://img.shields.io/discord/391020510269669376.svg)](https://discord.gg/mindustry)  

 一个用Java写的塔防游戏。

 _[Trello Board](https://trello.com/b/aE2tcUwF/mindustry-40-plans)_  
 _[Wiki](https://mindustrygame.github.io/wiki)_  
 _[Javadoc](https://mindustrygame.github.io/docs/)_ 

 ### 贡献

 见 [CONTRIBUTING](CONTRIBUTING.md).

 ### 构造

 每次提交时都会自动编译。您可以在这看到 [这里](https://github.com/Anuken/MindustryBuilds/releases)。
 如果您喜欢自己编译，请按照下面的说明。
 首先，确保您安装了 [JDK 14](https://adoptopenjdk.net/)。在终端进入root目录，`cd` 到Mindustry的目录并运行下面的命令:

 #### Windows

 _运行:_ `gradlew.bat desktop:run`  
 _构造:_ `gradlew.bat desktop:dist`  
 _包装:_ `gradlew.bat tools:pack`

 #### Linux/Mac 系统

 _运行:_ `./gradlew desktop:run`  
 _构造:_ `./gradlew desktop:dist`  
 _包装:_ `./gradlew tools:pack`

 #### 服务器

 服务器构建与每个发布的构建捆绑在一起(在版本中)。如果您喜欢自己编译, 将 'desktop' 替换为 'server'，`gradlew server:dist`。

 #### 安卓

 1. 下载 Android SDK [这里.](https://developer.android.com/studio#downloads) 确保您正在下载 "Command line tools only"，因为Android Studio是不必要的。
 2. 设置 `ANDROID_HOME` 环境变量为您解压缩的AndroidSDK目录。
 3. 运行 `gradlew android:assembleDebug` (在linux/mac上运行 `./gradlew`)。这会在 `android/build/outputs/apk` 建立一个未签名的apk文件.

 如果要在在连接的手机上调试应用程序，您应该运行 `gradlew android:installDebug android:run`.

 ##### 故障排除

 如果在 Mac/Linux 上的终端显示 `Permission denied` 或者 `Command not found`，先运行 `chmod +x ./gradlew`，再运行 `./gradlew`。*这是一次性完成的*

 ---

 Gradle 需要几分钟来下载文件。请耐心等待. <br>
 在构建前，输出。JAR文件会在 `/desktop/build/libs/Mindustry.jar` 进行桌面构建，另外，在 `/server/build/libs/server-release.jar` 进行服务器构建。

 ### 功能请求

 发布功能请求和反馈 [这里](https://github.com/Anuken/Mindustry-Suggestions/issues/new/choose).

 ### 下载

 [<img src="https://static.itch.io/images/badge.svg"
      alt="在Itch.io下载"
      height="60">](https://anuke.itch.io/mindustry)

 [<img src="https://play.google.com/intl/en_us/badges/images/generic/en-play-badge.png"
      alt="在Google Play下载"
      height="80">](https://play.google.com/store/apps/details?id=io.anuke.mindustry)

 [<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
      alt="在F-Droid下载"
      height="80">](https://f-droid.org/packages/io.anuke.mindustry/)
