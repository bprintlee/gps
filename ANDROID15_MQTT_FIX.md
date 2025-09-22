# Android 15 MQTT兼容性修复

## 问题描述

在Android 15 (API 35)上，应用在启动GPS跟踪时发生崩溃，错误信息如下：

```
SecurityException: com.gpstracker.app: One of RECEIVER_EXPORTED or RECEIVER_NOT_EXPORTED should be specified when a receiver isn't being registered exclusively for system broadcasts
```

## 问题原因

Android 15引入了新的安全限制，要求所有BroadcastReceiver注册时必须明确指定`RECEIVER_EXPORTED`或`RECEIVER_NOT_EXPORTED`标志。MQTT客户端库中的`AlarmPingSender`在注册BroadcastReceiver时没有指定这些标志，导致应用崩溃。

## 修复方案

### 1. 更新目标SDK版本

**文件**: `app/build.gradle`
```gradle
android {
    compileSdk 35
    defaultConfig {
        targetSdk 35
    }
}
```

### 2. 使用兼容Android 15的MQTT库

**文件**: `app/build.gradle`
```gradle
repositories {
    maven { url 'https://jitpack.io' }
}

dependencies {
    // 使用兼容Android 15的社区版本MQTT库
    implementation 'org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5'
    implementation 'com.github.hannesa2:paho.mqtt.android:4.2.3'
}
```

### 3. 保持MQTT服务声明

**文件**: `app/src/main/AndroidManifest.xml`
```xml
<!-- MQTT服务 -->
<service
    android:name="org.eclipse.paho.android.service.MqttService"
    android:enabled="true"
    android:exported="false" />
```

### 4. 恢复完整的MQTT功能

**文件**: `app/src/main/java/com/gpstracker/app/utils/MqttManager.kt`

使用新的兼容库后，MqttManager可以正常工作，无需特殊的Android 15兼容性处理：

```kotlin
fun connect() {
    try {
        // 检查网络连接
        if (!isNetworkAvailable()) {
            Log.w("MqttManager", "网络不可用，跳过MQTT连接")
            return
        }
        
        // 正常的MQTT连接流程
        mqttClient = MqttAndroidClient(context, serverUri, clientId)
        
        val options = MqttConnectOptions().apply {
            isCleanSession = true
            isAutomaticReconnect = true // 可以正常使用自动重连
            connectionTimeout = 10
            keepAliveInterval = 60
            mqttVersion = MqttConnectOptions.MQTT_VERSION_3_1_1
        }
        
        // 正常的回调处理
        mqttClient?.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                Log.w("MqttManager", "连接丢失", cause)
                // 自动重连会处理连接丢失
            }
            // 其他回调方法...
        })
    } catch (e: Exception) {
        // 错误处理...
    }
}
```

## 技术细节

### Android 15安全限制

Android 15要求所有BroadcastReceiver注册时必须明确指定导出状态：

- `RECEIVER_EXPORTED`: 允许其他应用发送广播到此接收器
- `RECEIVER_NOT_EXPORTED`: 只允许应用内部或系统发送广播到此接收器

### MQTT库兼容性

Eclipse Paho MQTT库在Android 15上存在兼容性问题，特别是：
- `AlarmPingSender`类注册BroadcastReceiver时未指定导出标志
- 官方库未及时更新以支持Android 15的新安全要求

### 解决方案优势

1. **使用社区维护的兼容库**: `hannesa2/paho.mqtt.android`已针对Android 14+进行适配
2. **完整功能支持**: 保持所有MQTT功能，包括自动重连、心跳等
3. **向后兼容**: 支持所有Android版本
4. **向前兼容**: 支持Android 15的新安全要求
5. **最小侵入**: 只需更换依赖库，无需修改业务逻辑

## 测试验证

修复后，应用应该能够：
1. 在Android 15设备上正常启动
2. 成功连接MQTT服务器
3. 正常发送GPS位置数据
4. 不再出现SecurityException崩溃

## 相关文件

- `app/build.gradle` - 更新SDK版本和MQTT依赖库
- `app/src/main/AndroidManifest.xml` - 保持MQTT服务声明
- `app/src/main/java/com/gpstracker/app/utils/MqttManager.kt` - 恢复完整MQTT功能

## 注意事项

1. 此修复专门针对Android 15的MQTT兼容性问题
2. 建议在多种Android版本上测试以确保兼容性
3. 如果使用其他MQTT库，可能需要类似的修复
4. 定期检查MQTT库更新，看是否有官方修复
