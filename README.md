# Token Auth Mod

一个基于HMAC-SHA256的动态令牌认证系统，为Minecraft服务器提供增强的安全保护，支持玩家行为约束功能。

## 🌟 特性

### 🔐 认证系统
- **动态令牌认证**：基于HMAC-SHA256算法的安全认证机制
- **挑战-响应模式**：防止重放攻击和时间戳攻击
- **时间窗口验证**：灵活的时间容错机制
- **IP阻止机制**：自动阻止多次认证失败的IP地址
- **会话管理**：完整的认证会话生命周期管理

### ⛓️ 玩家约束
- **多种约束类型**：移动、聊天、命令、方块交互等10种约束
- **智能约束条件**：基于认证状态的动态约束应用
- **优雅降级**：约束系统不可用时自动跳过，不影响认证流程
- **服务端专用**：约束功能仅在服务端生效，客户端无需额外依赖

### 🛡️ 安全特性
- **加密通信**：所有认证数据均经过加密传输
- **防暴力破解**：IP阻止和失败次数限制
- **会话超时**：自动清理过期会话
- **日志审计**：详细的认证事件日志记录

## 📋 系统要求

- **Minecraft**: 1.19.2
- **Fabric Loader**: >=0.14.21
- **Fabric API**: >=0.76.0+1.19.2
- **Java**: >=17
- **服务端可选依赖**: Constraint API >=1.0.0 (用于玩家约束功能)

## 🚀 安装指南

### 服务端安装
1. 下载最新版本的Token Auth Mod
2. 将jar文件放入服务器的`mods`文件夹
3. 如果需要约束功能，同时安装Constraint API模组
4. 重启服务器

### 客户端安装
1. 下载与服务器版本相同的Token Auth Mod
2. 将jar文件放入客户端的`mods`文件夹
3. 启动游戏

## ⚙️ 配置

### 服务端配置文件位置
```
config/token-auth-server.toml
```

### 主要配置选项
```toml
[server]
# 是否启用认证系统
enabled = true

# 共享密钥（用于HMAC签名）
shared_secret = "your_secret_key_here"

# 认证超时时间（毫秒）
response_timeout = 30000

# 最大失败尝试次数
max_attempts_per_ip = 5

# IP阻止持续时间（分钟）
block_duration_minutes = 10
```

### 客户端配置文件位置
```
config/token-auth-client.toml
```

## 🔧 使用方法

### 管理员命令
Token Auth Mod 提供了丰富的管理命令，用于配置和监控系统状态。

**主要命令**:
```
/token reload - 重新加载配置
/token generate-key - 生成新的共享密钥
/token status - 查看系统状态
/token block-ip <IP> [分钟] - 阻止IP地址
/token unblock-ip <IP> - 解除IP阻止
/token list-blocked-ips - 列出被阻止的IP
/token list-authenticated - 列出已认证玩家
/token remove-auth <玩家> - 移除玩家认证状态
```

**CSV记录命令**:
```
/csvtest - 测试CSV记录功能
/csvstatus - 查看CSV记录状态
/csvenable - 启用CSV记录
/csvdisable - 禁用CSV记录
/csvtimeout - 切换超时记录
```

**调试命令**:
```
/debugtest - 测试调试日志功能
/debugstatus - 查看调试模式状态
```

📖 **完整命令参考**: 详细的命令说明和使用示例请参考 [命令参考指南](Docs/COMMAND_REFERENCE.md)

### 认证流程
1. 玩家加入服务器
2. 服务器发送认证挑战
3. 客户端生成动态令牌响应
4. 服务器验证令牌
5. 认证成功后玩家可正常游戏

### 约束功能（可选）
如果安装了Constraint API，未认证玩家将受到以下约束：
- 无法移动
- 无法聊天
- 无法执行命令
- 无法与方块/实体交互
- 无法使用/丢弃物品

认证成功后所有约束自动移除。

## 🏗️ 开发信息

### 项目结构
```
src/main/java/nety/ys/
├── TokenAuthMod.java              # 主模组类
├── client/                       # 客户端相关代码
│   ├── ClientInitializer.java
│   ├── ClientPacketHandler.java
│   └── ClientTokenManager.java
├── server/                       # 服务端相关代码
│   ├── AuthSessionManager.java
│   ├── AuthPacketHandler.java
│   ├── commands/
│   ├── events/
│   └── constraint/               # 约束系统
│       ├── ConstraintManager.java
│       └── AuthenticationConstraintCondition.java
├── network/                      # 网络通信
│   ├── PacketRegistry.java
│   └── packets/
├── config/                       # 配置管理
├── crypto/                       # 加密算法
└── util/                         # 工具类
```

### 核心组件

#### 认证会话管理器
负责管理服务器端的认证会话、IP阻止和玩家认证状态。

#### 约束管理器
提供玩家行为约束功能，支持多种约束类型和条件检查。

#### 网络数据包系统
处理客户端和服务端之间的认证数据传输。

## 🐛 故障排除

### 常见问题

**Q: 玩家无法通过认证**
A: 检查以下几点：
- 服务器和客户端的模组版本是否一致
- 共享密钥配置是否正确
- 网络连接是否正常
- 查看服务器日志中的详细错误信息

**Q: 约束功能不生效**
A: 确认以下事项：
- 服务端是否安装了Constraint API模组
- 查看日志中是否有约束系统初始化错误
- 检查fabric.mod.json中的依赖配置

**Q: 认证超时**
A: 可能的原因：
- 网络延迟过高
- 服务器性能不足
- response_timeout配置过短

### 日志级别
在配置文件中设置日志级别以获取更多信息：
```toml
[logging]
level = "DEBUG"  # DEBUG, INFO, WARN, ERROR
```

## 🔗 相关链接

- **GitHub仓库**: [https://github.com/nety-ys/token-auth-mod](https://github.com/nety-ys/token-auth-mod)
- **问题反馈**: [GitHub Issues](https://github.com/nety-ys/token-auth-mod/issues)
- **命令参考**: [命令参考指南](Docs/COMMAND_REFERENCE.md)
- **邮件警报配置**: [邮件警报配置指南](Docs/Email_Alert_Configuration_Guide.md)
- **邮件故障排除**: [邮件功能故障排除](Docs/EMAIL_TROUBLESHOOTING.md)
- **Constraint API**: [Player Constraint API](Constraint_README.md)
- **调试日志指南**: [调试日志指南](Docs/DEBUG_LOGGING_GUIDE.md)
- **CSV日志功能**: [CSV日志功能说明](Docs/CSV_LOGGING_FEATURE.md)

## 📄 许可证

本项目采用 MIT 许可证 - 详见 [LICENSE](LICENSE) 文件

## 🤝 贡献

欢迎提交Pull Request和Issue！

### 开发环境设置
1. 克隆仓库
2. 使用IntelliJ IDEA或Eclipse导入项目
3. 运行`gradlew genSources`生成源代码
4. 运行`gradlew build`构建项目

### 代码规范
- 使用Java 17特性
- 遵循Google Java Style Guide
- 添加适当的注释和文档
- 确保所有测试通过

## 📊 更新日志

### v1.0.0
- 初始发布
- 基础认证功能
- 约束系统集成
- 完整的配置系统

## 🙏 致谢

- 感谢Fabric MC模组开发社区的支持
- 灵感来源于EasyAuth模组的认证机制
- 感谢所有贡献者和测试用户

---

**注意**: 本模组仅用于教育和研究目的，请遵守相关法律法规和服务器使用条款。
