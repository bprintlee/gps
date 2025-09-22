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

### 2. 保持MQTT服务声明

**文件**: `app/src/main/AndroidManifest.xml`
```xml
<!-- MQTT服务 -->
<service
    android:name="org.eclipse.paho.android.service.MqttService"
    android:enabled="true"
    android:exported="false" />
```

### 3. 在MqttManager中添加Android 15兼容性处理

**文件**: `app/src/main/java/com/gpstracker/app/utils/MqttManager.kt`

```kotlin
fun connect() {
    try {
        // Android 15兼容性处理
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            Log.d("MqttManager", "检测到Android 15，使用兼容性处理")
            connectWithAndroid15Compatibility()
            return
        }
        // 正常的连接流程...
    } catch (e: Exception) {
        // 错误处理...
    }
}

private fun connectWithAndroid15Compatibility() {
    // 创建简化的MQTT客户端，禁用自动重连
    mqttClient = MqttAndroidClient(context, serverUri, clientId)
    
    val options = MqttConnectOptions().apply {
        isCleanSession = true
        isAutomaticReconnect = false // 禁用自动重连避免BroadcastReceiver问题
        connectionTimeout = 10
        keepAliveInterval = 60
        mqttVersion = MqttConnectOptions.MQTT_VERSION_3_1_1
    }
    
    // 简化的回调，不处理连接丢失重连
    mqttClient?.setCallback(object : MqttCallback {
        override fun connectionLost(cause: Throwable?) {
            Log.w("MqttManager", "Android 15兼容模式：连接丢失", cause)
            isConnecting = false
            lastConnectionState = "连接丢失"
        }
        // 其他回调方法...
    })
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
- 需要自定义包装器来处理版本兼容性

### 解决方案优势

1. **向后兼容**: 支持Android 12及以下版本
2. **向前兼容**: 支持Android 15的新安全要求
3. **最小侵入**: 只修改必要的代码，不影响其他功能
4. **错误处理**: 包含异常处理，避免应用崩溃

## 测试验证

修复后，应用应该能够：
1. 在Android 15设备上正常启动
2. 成功连接MQTT服务器
3. 正常发送GPS位置数据
4. 不再出现SecurityException崩溃

## 相关文件

- `app/build.gradle` - 更新SDK版本
- `app/src/main/AndroidManifest.xml` - 保持MQTT服务声明
- `app/src/main/java/com/gpstracker/app/utils/MqttManager.kt` - 添加Android 15兼容性处理

## 注意事项

1. 此修复专门针对Android 15的MQTT兼容性问题
2. 建议在多种Android版本上测试以确保兼容性
3. 如果使用其他MQTT库，可能需要类似的修复
4. 定期检查MQTT库更新，看是否有官方修复
