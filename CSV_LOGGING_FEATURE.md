# CSV记录功能说明

## 功能概述

新增的CSV记录功能可以将未通过认证的玩家数据记录到本地CSV文件中，包括玩家名称、登录时间、IP地址和IP地址解析的地理位置信息。

## 功能特点

1. **自动记录**：当玩家认证失败时，自动将相关信息记录到CSV文件
2. **地理位置解析**：使用ip-api.com API获取IP地址的地理位置信息
3. **可配置**：可以通过配置文件启用/禁用CSV记录功能
4. **线程安全**：使用文件锁确保多线程环境下的数据安全

## 配置选项

在服务器配置文件 `token-auth-server.toml` 中，有以下新增配置项：

```toml
[csv_logging]
# 是否启用CSV记录功能（默认关闭，需要手动启用）
enableCSVLogging = false
# CSV文件名（自动添加.csv后缀）
csvFileName = "failed_auth_attempts.csv"
# 是否包含地理位置信息（需要网络请求）
includeGeoLocation = true
# 是否在认证超时时也记录到CSV文件
logTimeoutAttempts = true
```

## 命令使用

新增了以下命令来管理CSV记录功能（需要OP 3级权限）：

- `/csvstatus` - 显示CSV记录状态和文件路径
- `/csvenable` - 启用CSV记录功能
- `/csvdisable` - 禁用CSV记录功能
- `/csvtest` - 测试CSV记录功能是否正常工作

5. 使用 `/csvtimeout` 命令切换是否记录认证超时到CSV文件
## CSV文件格式

CSV文件包含以下列：

| 列名 | 说明 |
|------|------|
| 玩家名称 | 尝试登录但认证失败的玩家名称 |
| 登录时间 | 登录尝试的时间（YY/MM/DD HH:mm:ss，中国大陆时间） |
| IP地址 | 玩家的IP地址 |
| 地理位置 | IP地址解析的地理位置信息 |

示例CSV内容：
```
玩家名称,登录时间,IP地址,地理位置
TestPlayer,23/12/06 19:30:45,192.168.1.1,中国 广东省 深圳市
AnotherPlayer,23/12/06 20:15:22,8.8.8.8,美国 加利福尼亚州 山景城
```

## 使用方法

1. **启用CSV记录功能**：
   ```
   /csvenable
   ```

2. **查看CSV记录状态**：
   ```
   /csvstatus
   ```
当有玩家认证失败或认证超时时，系统会自动记录到CSV文件

3. **测试功能是否正常**：
   ```
   /csvtest
   ```

4. **禁用CSV记录功能**：
   ```
   /csvdisable
   ```

## 注意事项

1. CSV记录功能默认是关闭的，需要手动启用
2. 地理位置解析需要网络连接，如果网络不可用可能会影响性能
3. CSV文件保存在服务器的配置目录中（通常是 `config/token-auth/`）
4. 文件操作使用锁机制，确保在高并发情况下的数据完整性
5. 如果IP地址无法解析地理位置，将显示"未知位置"

## 故障排除

如果CSV记录功能不工作，请检查：

1. 是否已启用CSV记录功能（使用 `/csvstatus` 命令检查）
2. 服务器是否有写入配置目录的权限
3. 网络连接是否正常（如果启用了地理位置解析）
4. 使用 `/csvtest` 命令进行功能测试

## 技术实现

该功能由以下组件实现：

1. `IPGeolocationUtil` - IP地址地理位置解析工具类
2. `FailedAuthLogger` - CSV记录器类
3. `CSVLoggingTest` - 功能测试类
4. `TokenCommandSimple` - 简化的命令处理器

所有组件都遵循了KISS、YAGNI和DRY原则，确保代码简洁、高效和可维护。