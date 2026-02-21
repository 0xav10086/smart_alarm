#!/bin/bash

# 该脚本仅用于创建xposed项目层级结构

#smart_alarm/
#├── app/                        # 你的模块代码核心目录
#│   ├── libs/# 存放本地 jar/aar (暂时为空)
#│   └── src/
#│       └── main/
#│           ├── java/
#│           │   └── io/github/av10086/smartalarm/  # 你的包名路径
#│           │       ├── MainActivity.kt           # 模块的设置界面
#│           │       └── ModuleMain.kt             # 真正的 Hook 逻辑
#│           ├── res/            # 图标、文字资源
#│           └── AndroidManifest.xml               # 模块清单
#├── gradle/                     # Gradle 配置文件夹
#│   └── wrapper/
#│       ├── gradle-wrapper.jar
#│       └── gradle-wrapper.properties
#├── build.gradle.kts           # 根项目构建配置
#├── settings.gradle.kts        # 项目模块声明
#├── gradlew                    # Unix 启动脚本
#└── gradlew.bat                # Windows 启动脚本

# 创建 smart_alarm 项目结构
mkdir -p ./app/src/main/java/io/github/av10086/smartalarm
mkdir -p ./app/src/main/res
mkdir -p ./gradle/wrapper
mkdir -p ./app/libs
mkdir -p ./app/src/main/java/io/github/av10086/smartalarm
mkdir -p app/src/main/res/values

# 创建所有文件
touch ./app/libs/.gitkeep
touch ./app/src/main/java/io/github/av10086/smartalarm/MainActivity.kt
touch ./app/src/main/java/io/github/av10086/smartalarm/ModuleMain.kt
touch ./app/src/main/AndroidManifest.xml
touch ./build.gradle.kts
touch ./settings.gradle.kts
touch ./gradle/wrapper/gradle-wrapper.jar
touch ./gradle/wrapper/gradle-wrapper.properties
touch ./gradlew
touch ./gradlew.bat
touch ./app/build.gradle.kts
touch ./app/src/main/res/values/arrays.xml
touch ./gradle.properties

echo "✅ smart_alarm 项目结构创建完成！"