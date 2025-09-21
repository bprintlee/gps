# GPS行程跟踪功能实现总结

## 功能概述

已成功实现GPS数据按行程分组的功能，解决了之前将所有数据导出到一个文件的问题。现在系统能够：

1. **自动行程管理**：根据室内/室外状态自动开始和结束行程
2. **按行程导出**：每个行程生成独立的GPX文件
3. **行程信息显示**：在主界面显示当前行程和总行程数
4. **行程选择导出**：可以查看和选择特定行程进行导出

## 实现细节

### 1. 数据库结构更新

**文件**: `app/src/main/java/com/gpstracker/app/model/GpsData.kt`
- 添加了 `tripId: String?` 字段用于标记GPS数据所属的行程

**文件**: `app/src/main/java/com/gpstracker/app/database/GpsDatabase.kt`
- 数据库版本从1升级到2
- 添加了 `trip_id` 列
- 实现了数据库升级逻辑（从版本1到版本2）
- 新增方法：
  - `getGpsDataByTripId(tripId: String)`: 按行程ID查询GPS数据
  - `getAllTripIds()`: 获取所有行程ID列表

### 2. GPS服务行程管理

**文件**: `app/src/main/java/com/gpstracker/app/service/GpsTrackingService.kt`
- 添加行程管理变量：
  - `currentTripId: String?`: 当前行程ID
  - `isTripActive: Boolean`: 行程是否活跃
- 实现自动行程管理逻辑：
  - 从室内状态切换到室外/活跃/驾驶状态时自动开始新行程
  - 从室外/活跃/驾驶状态切换到室内状态时自动结束当前行程
- 新增公共方法：
  - `getCurrentTripId()`: 获取当前行程ID
  - `isTripActive()`: 检查行程是否活跃
  - `getAllTripIds()`: 获取所有行程ID
  - `getGpsDataByTripId(tripId: String)`: 获取特定行程的GPS数据
  - `exportTripGpx(tripId: String)`: 导出特定行程的GPX文件

### 3. GPX导出功能更新

**文件**: `app/src/main/java/com/gpstracker/app/utils/GpxExporter.kt`
- 更新 `exportFromDatabase()` 方法：现在按行程分组导出，为每个行程创建单独的GPX文件
- 新增 `exportTripGpx()` 方法：导出特定行程的GPX文件
- 新增 `createNewGpxFileForTrip()` 方法：为特定行程创建GPX文件头

### 4. 用户界面更新

**主界面** (`app/src/main/java/com/gpstracker/app/MainActivity.kt`):
- 添加行程信息显示：
  - 当前行程ID
  - 总行程数
- 实时更新行程状态

**主界面布局** (`app/src/main/res/layout/activity_main.xml`):
- 添加行程信息显示控件

**导出界面** (`app/src/main/java/com/gpstracker/app/ExportActivity.kt`):
- 添加"查看行程"按钮
- 实现行程选择对话框
- 支持导出特定行程

**导出界面布局** (`app/src/main/res/layout/activity_export.xml`):
- 重新组织按钮布局
- 添加"查看行程"按钮

## 行程管理逻辑

### 自动行程开始
当用户从室内状态切换到以下任一状态时，系统自动开始新行程：
- 室外状态 (OUTDOOR)
- 活跃状态 (ACTIVE)  
- 驾驶状态 (DRIVING)

### 自动行程结束
当用户从以下任一状态切换到室内状态时，系统自动结束当前行程：
- 室外状态 (OUTDOOR)
- 活跃状态 (ACTIVE)
- 驾驶状态 (DRIVING)

### 行程ID生成
行程ID格式：`trip_yyyyMMdd_HHmmss`
例如：`trip_20241201_143022`

## 文件命名规则

### GPX文件命名
- 按行程导出：`{tripId}.gpx`
- 例如：`trip_20241201_143022.gpx`

### GPX文件内容
每个行程的GPX文件包含：
- 行程元数据（行程ID、开始时间、结束时间）
- 该行程的所有GPS点数据
- 每个GPS点的详细信息（位置、时间、状态、精度）

## 使用方法

### 1. 自动行程跟踪
1. 启动GPS跟踪服务
2. 在室内和室外之间移动
3. 系统会自动开始和结束行程
4. 在主界面查看当前行程和总行程数

### 2. 查看行程
1. 进入导出界面
2. 点击"查看行程"按钮
3. 查看所有行程列表，包括：
   - 行程ID
   - 开始时间
   - 结束时间
   - GPS点数量

### 3. 导出特定行程
1. 在行程列表中选择要导出的行程
2. 系统会生成该行程的独立GPX文件
3. 文件保存在应用文档目录中

### 4. 导出所有行程
1. 点击"按行程导出"按钮
2. 系统会为每个行程生成独立的GPX文件

## 技术特点

### 1. 数据库兼容性
- 支持从旧版本数据库升级
- 新字段为可选字段，不影响现有数据

### 2. 性能优化
- 行程管理逻辑集成在现有的状态检查中
- 最小化额外的数据库查询

### 3. 用户体验
- 自动行程管理，无需用户干预
- 清晰的行程信息显示
- 灵活的导出选项

## 测试建议

1. **基本功能测试**：
   - 启动GPS跟踪
   - 在室内外之间移动
   - 观察行程自动开始/结束

2. **导出功能测试**：
   - 查看行程列表
   - 导出特定行程
   - 验证GPX文件内容

3. **数据完整性测试**：
   - 检查GPS数据是否正确关联到行程
   - 验证行程时间范围
   - 确认GPX文件格式正确

## 总结

成功实现了GPS数据按行程分组的功能，解决了用户提出的需求：

✅ **记录到数据库时标记行程**：每个GPS数据点都包含行程ID
✅ **行程结束标记**：当状态切换时自动结束行程
✅ **按行程生成GPX文件**：每个行程生成独立的GPX文件
✅ **用户界面支持**：提供行程查看和选择导出功能

现在系统能够正确地将连续的GPS数据按行程进行分组，每个行程都有独立的GPX文件，完全符合用户的需求。
