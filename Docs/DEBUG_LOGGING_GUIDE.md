# 调试日志功能使用指南

## 概述

本功能为Token Auth Mod添加了调试模式支持，允许用户在调试模式和平常模式之间切换，从而控制日志输出的详细程度。

## 功能特点

1. **调试模式开关**：在配置文件中控制是否启用调试模式
2. **分类日志输出**：不同类型的日志使用不同前缀，便于识别
3. **性能优化**：非调试模式下减少不必要的日志输出，提高性能

## 配置说明

在 `run/config/token-auth/token-auth-server.properties` 文件中添加了以下配置项：

```properties
# 日志设置
logging.enableAuthLogging=true
logging.logFailedAttempts=true
logging.logSuccessfulAuth=true
logging.debugMode=false  # 调试模式开关，默认关闭
```

- `logging.debugMode=true`：启用调试模式，显示所有详细日志
- `logging.debugMode=false`：禁用调试模式，只显示重要日志

## 日志类型

调试模式下，日志会显示以下前缀：

- `[DEBUG]`：调试信息，用于开发和故障排除
- `[VERBOSE]`：详细信息，记录详细的处理过程
- `[AUTH]`：认证过程信息，记录认证相关操作
- `[CSV]`：CSV记录信息，记录CSV文件操作
- `[EMAIL]`：邮件警报信息，记录邮件发送状态

## 使用方法

### 1. 配置调试模式

编辑 `run/config/token-auth/token-auth-server.properties` 文件：

```properties
# 启用调试模式
logging.debugMode=true
```

### 2. 测试调试功能

使用游戏内命令测试调试功能：

```
/debugtest    # 测试调试日志功能
/debugstatus  # 查看当前调试模式状态
```

### 3. 重启服务器

修改配置后需要重启服务器使设置生效。

## 代码实现

### 1. DebugLogger 工具类

创建了 `src/main/java/nety/ys/util/DebugLogger.java`，提供统一的调试日志接口：

- `debug()`：输出调试信息
- `verbose()`：输出详细信息
- `auth()`：输出认证过程信息
- `csv()`：输出CSV记录信息
- `email()`：输出邮件警报信息

### 2. 配置支持

修改了以下类以支持调试模式配置：

- `ModConfig.ServerConfig`：添加了 `debugMode` 字段
- `SimpleConfigManager`：添加了调试模式的加载和保存逻辑

### 3. 日志替换

将以下类中的详细日志替换为条件输出：

- `FailedAuthLogger`：CSV记录相关日志
- `AuthAlertService`：邮件警报相关日志
- `AuthPacketHandler`：认证数据包处理日志
- `AuthSessionManager`：会话管理日志
- `AuthEventHandler`：认证事件处理日志
- `ConstraintManager`：约束管理日志

## 性能影响

- **调试模式启用**：日志输出量增加，但提供详细的调试信息
- **调试模式禁用**：日志输出量减少，提高服务器性能

## 故障排除

如果调试功能不工作：

1. 检查配置文件是否正确设置
2. 确认服务器已重启
3. 使用 `/debugstatus` 命令检查当前状态
4. 查看服务器日志确认配置是否生效

## 示例输出

### 调试模式启用时：

```
[INFO] Token Auth Mod 正在初始化...
[DEBUG] 服务器启动中，认证系统准备就绪
[DEBUG] 约束系统初始化成功
[INFO] Token Auth Mod 初始化完成！
```

### 调试模式禁用时：

```
[INFO] Token Auth Mod 正在初始化...
[INFO] Token Auth Mod 初始化完成！
```

## 注意事项

1. 调试模式主要用于开发和故障排除，生产环境建议关闭
2. 调试模式会增加日志文件大小，注意磁盘空间
3. 某些关键日志（如错误和警告）不受调试模式影响，始终会显示