# 约束系统集成指南

## 概述

本指南说明了如何将Player Constraint API集成到Token Auth Mod中，为未认证的玩家提供行为约束功能。

## 实现的功能

### 1. 认证约束条件
- **文件**: [`AuthenticationConstraintCondition.java`](src/main/java/nety/ys/server/constraint/AuthenticationConstraintCondition.java:1)
- **功能**: 检查玩家是否已认证，如果未认证则应用约束
- **优先级**: 10（高优先级，确保认证约束优先执行）

### 2. 约束管理器
- **文件**: [`ConstraintManager.java`](src/main/java/nety/ys/server/constraint/ConstraintManager.java:1)
- **功能**: 
  - 初始化约束系统
  - 为未认证玩家添加约束
  - 为已认证玩家移除约束
  - 检查玩家约束状态

### 3. 集成点

#### 3.1 玩家登录时添加约束
- **文件**: [`AuthEventHandler.java`](src/main/java/nety/ys/server/events/AuthEventHandler.java:1)
- **位置**: [`onPlayerJoin()`](src/main/java/nety/ys/server/events/AuthEventHandler.java:26) 方法
- **逻辑**: 当玩家未认证时，在发送认证挑战前添加约束

#### 3.2 认证成功后移除约束
- **文件**: [`AuthPacketHandler.java`](src/main/java/nety/ys/server/AuthPacketHandler.java:1)
- **位置**: [`onAuthenticationSuccess()`](src/main/java/nety/ys/server/AuthPacketHandler.java:137) 方法
- **逻辑**: 在标记玩家为已认证后移除所有约束

#### 3.3 模组初始化
- **文件**: [`TokenAuthMod.java`](src/main/java/nety/ys/TokenAuthMod.java:1)
- **位置**: [`initializeServer()`](src/main/java/nety/ys/TokenAuthMod.java:83) 方法
- **逻辑**: 在服务器初始化时初始化约束系统

## 约束类型

系统为未认证玩家应用以下约束：

1. **MOVEMENT** - 限制玩家移动
2. **CHAT** - 限制玩家聊天
3. **COMMAND** - 限制玩家执行命令
4. **BLOCK_INTERACTION** - 限制玩家与方块交互
5. **BLOCK_BREAKING** - 限制玩家破坏方块
6. **ENTITY_INTERACTION** - 限制玩家与实体交互
7. **ENTITY_ATTACKING** - 限制玩家攻击实体
8. **ITEM_USING** - 限制玩家使用物品
9. **ITEM_DROPPING** - 限制玩家丢弃物品
10. **ITEM_MOVING** - 限制玩家在物品栏中移动物品

## 使用流程

### 1. 玩家登录流程
```
玩家加入服务器
    ↓
检查玩家认证状态
    ↓
如果未认证：
    - 检查约束系统是否可用
    - 如果可用：添加约束
    - 发送认证挑战
    ↓
玩家响应挑战
    ↓
验证令牌
    ↓
如果验证成功：
    - 标记为已认证
    - 如果约束系统可用：移除所有约束
    ↓
玩家可以正常游戏
```

### 2. 约束系统可用性检查
系统在以下位置检查constraint模组的可用性：
- **模组初始化时**：在 [`TokenAuthMod.initializeServer()`](src/main/java/nety/ys/TokenAuthMod.java:83) 中检查
- **添加约束时**：在 [`AuthEventHandler.onPlayerJoin()`](src/main/java/nety/ys/server/events/AuthEventHandler.java:26) 中检查
- **移除约束时**：在 [`AuthPacketHandler.onAuthenticationSuccess()`](src/main/java/nety/ys/server/AuthPacketHandler.java:137) 中检查

如果constraint模组不可用，系统会记录调试日志并跳过相关约束操作，不会影响正常的认证流程。

### 2. 约束应用时机
- **添加约束**: 玩家加入服务器且未认证时
- **移除约束**: 玩家认证成功时
- **清理约束**: 玩家断开连接时

## 配置要求

### 1. 依赖配置
在 [`fabric.mod.json`](src/main/resources/fabric.mod.json:1) 中添加了服务端专用的constraint依赖：
```json
"depends": {
    "fabricloader": ">=0.14.21",
    "minecraft": "~1.19.2",
    "java": ">=17",
    "fabric-api": ">=0.76.0+1.19.2"
},
"custom": {
    "server": {
        "depends": {
            "constraint": ">=1.0.0"
        }
    }
}
```

这样配置确保constraint模组只在服务端环境中作为必需依赖，客户端不需要安装constraint模组也能正常运行。

### 2. 约束源标识符
- **标识符**: `token_auth:auth_constraint`
- **用途**: 标识约束来源，便于管理和调试

## 日志记录

系统提供详细的日志记录：
- 约束添加/移除事件
- 认证状态检查
- 错误处理和异常情况

## 错误处理

1. **约束添加失败**: 记录错误日志，不影响认证流程
2. **约束移除失败**: 记录错误日志，不影响玩家正常游戏
3. **约束系统初始化失败**: 记录错误日志，系统继续运行但无约束功能
4. **约束模组不可用**: 记录调试日志，跳过约束操作，正常运行认证流程

## 测试建议

### 1. 基本功能测试
- 测试未认证玩家是否被正确约束
- 测试认证成功后约束是否被移除
- 测试各种约束类型是否生效

### 2. 边界情况测试
- 测试约束系统初始化失败的情况
- 测试玩家在约束期间断开连接
- 测试认证超时的情况

### 3. 性能测试
- 测试大量玩家同时登录时的约束处理
- 测试约束添加/移除的性能影响

## 注意事项

1. **依赖关系**: constraint模组仅在服务端需要，客户端不需要安装
2. **兼容性**: 与其他可能使用约束系统的模组兼容
3. **优先级**: 认证约束条件设置高优先级，确保优先执行
4. **资源清理**: 确保在玩家断开连接时正确清理约束
5. **可用性检查**: 系统会自动检测constraint模组的可用性，优雅降级

## 故障排除

### 1. 约束未生效
- 检查constraint模组是否正确安装
- 检查日志中是否有约束系统初始化错误
- 检查玩家认证状态是否正确

### 2. 约束未移除
- 检查认证成功事件是否正确触发
- 检查约束移除过程中是否有异常
- 检查玩家UUID是否一致

### 3. 性能问题
- 检查约束添加/移除是否过于频繁
- 检查约束条件检查逻辑是否优化
- 监控服务器资源使用情况

## 扩展功能

未来可以考虑添加以下功能：
1. 可配置的约束类型
2. 约束等级系统
3. 约束时间限制
4. 约束例外规则
5. 管理员命令接口