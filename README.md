<<<<<<< HEAD
# GPS跟踪器 Android应用

一个智能的GPS位置跟踪Android应用程序，能够根据用户的活动状态自动调整跟踪策略以节省电量。

## 功能特性

### 核心功能
- **GPS位置采集**: 定期采集用户GPS位置信息
- **GPX文件生成**: 将位置数据保存为标准GPX格式文件
- **文件导出**: 支持导出GPX文件到设备存储

### 智能状态检测
应用通过多种传感器自动判断用户状态，实现智能省电：

#### 室内/室外状态判断
- **室内状态**: 30秒内无法获取GPS信号时自动进入
- **室外状态**: 能够稳定获取GPS信号时进入

#### 活跃状态检测
- **步数检测**: 当步数超过20步时进入活跃状态
- **驾驶检测**: 加速度超过阈值时判断为驾驶状态

### 状态说明
- **室内模式**: 停止GPS跟踪，节省电量
- **室外模式**: 正常GPS跟踪
- **活跃状态**: 开始GPS跟踪检测
- **驾驶状态**: 持续GPS跟踪

## 技术架构

### 主要组件
- **MainActivity**: 主界面，控制跟踪开始/停止
- **GpsTrackingService**: 后台服务，处理GPS数据采集
- **GpxExporter**: GPX文件生成和导出工具
- **TrackingState**: 状态枚举（室内/室外/活跃/驾驶）

### 传感器集成
- **GPS传感器**: 位置信息采集
- **加速度传感器**: 运动状态检测
- **计步器传感器**: 步数统计

### 权限要求
- `ACCESS_FINE_LOCATION`: 精确位置权限
- `ACCESS_COARSE_LOCATION`: 粗略位置权限
- `ACCESS_BACKGROUND_LOCATION`: 后台位置权限
- `ACTIVITY_RECOGNITION`: 活动识别权限
- `BODY_SENSORS`: 传感器权限
- `WRITE_EXTERNAL_STORAGE`: 存储权限

## 项目结构

```
app/
├── src/main/
│   ├── java/com/gpstracker/app/
│   │   ├── MainActivity.kt              # 主界面
│   │   ├── model/
│   │   │   └── GpsData.kt              # 数据模型
│   │   ├── service/
│   │   │   └── GpsTrackingService.kt   # GPS跟踪服务
│   │   └── utils/
│   │       └── GpxExporter.kt          # GPX导出工具
│   ├── res/                            # 资源文件
│   └── AndroidManifest.xml             # 应用清单
├── build.gradle                        # 应用构建配置
└── proguard-rules.pro                  # 代码混淆规则

.github/workflows/
└── android.yml                         # GitHub Actions配置

gradle/                                 # Gradle包装器
build.gradle                           # 项目构建配置
settings.gradle                        # 项目设置
gradle.properties                      # Gradle属性
```

## 构建和部署

### 本地构建
```bash
# 克隆项目
git clone https://github.com/bprintlee/gps.git
cd gps

# 构建Debug版本
./gradlew assembleDebug

# 构建Release版本
./gradlew assembleRelease
```

### GitHub Actions自动构建
项目配置了GitHub Actions工作流，在以下情况下自动构建：
- 推送到main或develop分支
- 创建Pull Request到main分支

构建产物：
- `app-debug-apk`: Debug版本APK
- `app-release-apk`: Release版本APK

## 使用说明

### 首次使用
1. 安装应用后首次启动
2. 授予必要的位置和传感器权限
3. 点击"开始跟踪"按钮启动GPS跟踪

### 状态监控
- 应用会显示当前跟踪状态
- 实时显示GPS信号状态
- 显示最后获取的位置信息

### 数据导出
- 点击"导出GPX"按钮导出位置数据
- GPX文件保存在设备文档目录
- 文件按日期命名：`gps_track_yyyy-MM-dd.gpx`

## 省电策略

### 智能状态切换
- **室内状态**: 完全停止GPS跟踪，仅监控传感器
- **室外状态**: 正常GPS跟踪频率
- **活跃状态**: 根据运动情况调整跟踪频率
- **驾驶状态**: 保持高频GPS跟踪

### 传感器优化
- 使用低功耗传感器监听
- 智能判断用户活动状态
- 避免不必要的GPS调用

## 开发说明

### 环境要求
- Android Studio Arctic Fox或更高版本
- JDK 17
- Android SDK API 24+
- Gradle 8.4+

### 依赖库
- AndroidX Core KTX
- Material Design Components
- Google Play Services Location
- WorkManager (后台任务)

### 代码规范
- 使用Kotlin语言开发
- 遵循Android开发最佳实践
- 使用协程处理异步操作
- 实现适当的错误处理

## 版本历史

### v1.0.0 (当前版本)
- 基础GPS跟踪功能
- 智能状态检测
- GPX文件生成和导出
- GitHub Actions自动构建

## 许可证

本项目采用MIT许可证，详见LICENSE文件。

## 贡献

欢迎提交Issue和Pull Request来改进这个项目。

## 联系方式

如有问题或建议，请通过GitHub Issues联系。
=======
# gps
>>>>>>> a7f12815049fb94dd8786954857ad4877af197ad
