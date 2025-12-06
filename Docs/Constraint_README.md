# Player Constraint API

一个灵活的玩家行为约束API，允许其他模组对玩家进行活动约束。

## 概述

这个模组提供了一个通用的玩家约束系统，类似于EasyAuth的约束机制，但更加灵活和可扩展。其他模组可以使用这个API来约束玩家的各种行为，如移动、聊天、命令执行、方块交互等。

## 特性

- 🎯 **多种约束类型**：支持移动、聊天、命令、方块交互、实体交互、物品操作等多种约束
- 🔧 **灵活的约束条件**：允许模组定义自己的约束条件逻辑
- 📡 **事件系统**：提供完整的事件监听机制
- 🎛️ **可配置**：支持详细的配置选项
- 🔌 **模组间通信**：提供简单的API供其他模组使用
- 🎨 **可扩展**：支持自定义约束类型和条件

## 快速开始

### 1. 添加依赖

在你的`fabric.mod.json`中添加依赖：

```json
{
  "depends": {
    "constraint": ">=1.0.0"
  }
}
```

### 2. 基本使用

```java
import nety.ys.constraint.api.ConstraintAPI;
import nety.ys.constraint.api.ConstraintType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

// 为玩家添加移动约束
ServerPlayerEntity player = ...;
Identifier source = new Identifier("mymod", "auth");
ConstraintAPI.addConstraint(player, ConstraintType.MOVEMENT, source);

// 移除约束
ConstraintAPI.removeConstraint(player, ConstraintType.MOVEMENT, source);
```

### 3. 添加约束条件

```java
import nety.ys.constraint.api.ConstraintCondition;
import nety.ys.constraint.api.ConstraintContext;
import nety.ys.constraint.api.ConstraintType;

// 创建认证约束条件
ConstraintCondition authCondition = new ConstraintCondition() {
    @Override
    public boolean shouldConstrain(ServerPlayerEntity player, ConstraintType constraintType, ConstraintContext context) {
        // 检查玩家是否已认证
        return !isPlayerAuthenticated(player);
    }
    
    @Override
    public Identifier getIdentifier() {
        return new Identifier("mymod", "auth_check");
    }
    
    @Override
    public int getPriority() {
        return 10; // 高优先级
    }
};

// 添加约束条件
ConstraintAPI.addConstraintCondition(authCondition);
```

## 约束类型

API提供了以下预定义的约束类型：

| 约束类型 | 描述 |
|-----------|------|
| `MOVEMENT` | 限制玩家移动 |
| `CHAT` | 限制玩家聊天 |
| `COMMAND` | 限制玩家执行命令 |
| `BLOCK_INTERACTION` | 限制玩家与方块交互 |
| `BLOCK_BREAKING` | 限制玩家破坏方块 |
| `ENTITY_INTERACTION` | 限制玩家与实体交互 |
| `ENTITY_ATTACKING` | 限制玩家攻击实体 |
| `ITEM_USING` | 限制玩家使用物品 |
| `ITEM_DROPPING` | 限制玩家丢弃物品 |
| `ITEM_MOVING` | 限制玩家在物品栏中移动物品 |
| `CUSTOM` | 自定义约束类型 |

## 高级用法

### 监听约束事件

```java
import nety.ys.constraint.api.ConstraintEvent;

// 监听约束应用事件
ConstraintEvent.CONSTRAINT_APPLIED.register((player, constraintType, context) -> {
    System.out.println("玩家 " + player.getName().getString() + " 被约束了: " + constraintType);
});

// 监听约束移除事件
ConstraintEvent.CONSTRAINT_REMOVED.register((player, constraintType, context) -> {
    System.out.println("玩家 " + player.getName().getString() + " 的约束被移除了: " + constraintType);
});
```

### 检查玩家约束状态

```java
import nety.ys.constraint.api.ConstraintState;

ConstraintState state = ConstraintAPI.getConstraintState(player);

// 检查是否被约束
if (state.isConstrained()) {
    // 获取所有活跃的约束
    for (ConstraintType type : state.getActiveConstraints()) {
        System.out.println("约束类型: " + type);
        
        // 获取约束来源
        Identifier source = state.getConstraintSource(type);
        System.out.println("来源: " + source);
        
        // 获取约束剩余时间
        long remainingTime = state.getRemainingTime(type);
        if (remainingTime > 0) {
            System.out.println("剩余时间: " + remainingTime + "ms");
        }
    }
}
```

### 使用约束上下文

```java
import nety.ys.constraint.api.ConstraintContext;

// 创建命令约束上下文
ConstraintContext commandContext = ConstraintContext.forCommand("/help");
boolean shouldConstrain = ConstraintAPI.shouldConstrain(player, ConstraintType.COMMAND, commandContext);

// 创建方块交互上下文
BlockPos pos = new BlockPos(x, y, z);
ConstraintContext blockContext = ConstraintContext.forBlockInteraction(pos);
boolean shouldConstrain = ConstraintAPI.shouldConstrain(player, ConstraintType.BLOCK_INTERACTION, blockContext);

// 创建自定义上下文
ConstraintContext customContext = new ConstraintContext()
    .set("customKey", "customValue")
    .set("anotherKey", 123);
```

## 内置约束条件

### 认证约束条件

```java
import nety.ys.constraint.condition.AuthenticationCondition;

// 添加认证约束条件
ConstraintAPI.addConstraintCondition(new AuthenticationCondition());
```

### 权限约束条件

```java
import nety.ys.constraint.condition.PermissionCondition;

// 创建权限约束条件（需要权限才不约束）
PermissionCondition requirePermission = new PermissionCondition("my.permission", true);

// 创建权限约束条件（有权限才约束）
PermissionCondition excludePermission = new PermissionCondition("my.permission", false);

// 添加约束条件
ConstraintAPI.addConstraintCondition(requirePermission);
ConstraintAPI.addConstraintCondition(excludePermission);
```

## 配置

API支持详细的配置选项：

```java
import nety.ys.constraint.config.ConstraintConfig;

ConstraintConfig config = new ConstraintConfig();

// 配置移动约束
config.setTeleportationTimeoutMs(20);
config.setSetInvulnerable(true);
config.setSetInvisible(true);

// 配置命令约束
config.setAllowCommands(false);
config.setAllowedCommands(new String[]{"help", "rules"});

// 配置聊天约束
config.setAllowChat(false);

// 配置其他约束
config.setAllowBlockInteraction(false);
config.setAllowBlockBreaking(false);
config.setAllowEntityInteraction(false);
config.setAllowEntityAttacking(false);
config.setAllowItemUsing(false);
config.setAllowItemDropping(false);
config.setAllowItemMoving(false);
```

## 与EasyAuth集成

这个API设计为与EasyAuth等认证模组兼容。你可以创建一个适配器来集成EasyAuth：

```java
public class EasyAuthAdapter {
    public static void integrateEasyAuth() {
        // 创建认证约束条件
        ConstraintCondition authCondition = ConstraintAPI.createAuthCondition(
            new Identifier("easyauth", "auth_check"),
            player -> {
                // 检查EasyAuth认证状态
                if (player instanceof PlayerAuth) {
                    return ((PlayerAuth) player).easyAuth$isAuthenticated();
                }
                return false;
            }
        );
        
        // 添加约束条件
        ConstraintAPI.addConstraintCondition(authCondition);
    }
}
```

## 最佳实践

1. **使用有意义的来源标识符**：使用你的模组ID作为约束来源，便于管理和调试
2. **设置合适的优先级**：认证条件应该有高优先级，其他条件按需设置
3. **监听约束事件**：通过事件系统了解约束状态变化，进行相应的处理
4. **提供用户反馈**：当玩家被约束时，提供清晰的提示信息
5. **清理约束**：在玩家离开或认证成功时及时清理约束

## 示例项目

查看`src/main/java/nety/ys/constraint/example/ExampleUsage.java`获取更多使用示例。

## 许可证

MIT License

## 贡献

欢迎提交Issue和Pull Request！

## 致谢

- 灵感来源于EasyAuth模组的约束机制
- 感谢Fabric MC模组开发社区的支持