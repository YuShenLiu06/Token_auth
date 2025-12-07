# Token Auth Mod 命令参考指南

本文档详细介绍了 Token Auth Mod 提供的所有管理命令及其用法。

## 权限要求

所有 Token Auth Mod 命令都需要 **OP 3级权限** 才能执行。

## 主要命令

### `/token` - 令牌管理命令

这是 Token Auth Mod 的主要管理命令，提供多种子命令用于管理认证系统。

#### `/token reload`
重新加载 Token Auth Mod 的配置文件。

**用法**: `/token reload`

**功能**: 
- 重新读取服务器配置文件
- 应用新的配置设置，无需重启服务器
- 如果配置文件有错误，会显示错误信息

**示例**:
```
/token reload
```

#### `/token generate-key`
生成一个新的共享密钥，用于服务器和客户端之间的加密通信。

**用法**: `/token generate-key`

**功能**:
- 生成一个安全的随机共享密钥
- 在聊天中显示密钥，并支持点击复制
- 提示将密钥配置到服务器和客户端配置文件中

**示例**:
```
/token generate-key
```

#### `/token status`
显示 Token Auth Mod 的当前状态和配置信息。

**用法**: `/token status`

**功能**:
- 显示认证系统启用状态
- 显示共享密钥配置状态
- 显示活跃会话数和已认证玩家数
- 显示被阻止的IP数量
- 显示安全设置（最大尝试次数、阻止持续时间等）
- 显示日志和CSV记录状态
- 显示邮件警报状态和配置信息

**示例**:
```
/token status
```

#### `/token block-ip <IP地址> [分钟]`
阻止指定的IP地址访问服务器。

**用法**: `/token block-ip <IP地址> [分钟]`

**参数**:
- `IP地址`: 要阻止的IP地址
- `分钟`: (可选) 阻止持续时间，默认为30分钟，范围1-1440分钟

**功能**:
- 立即阻止指定IP地址的访问
- 可自定义阻止时间
- 被阻止的IP尝试连接时会被拒绝

**示例**:
```
/token block-ip 192.168.1.100 60
/token block-ip 10.0.0.5
```

#### `/token unblock-ip <IP地址>`
解除对指定IP地址的阻止。

**用法**: `/token unblock-ip <IP地址>`

**参数**:
- `IP地址`: 要解除阻止的IP地址

**功能**:
- 从阻止列表中移除指定IP地址
- 该IP地址将可以正常连接服务器

**示例**:
```
/token unblock-ip 192.168.1.100
```

#### `/token list-blocked-ips`
列出所有当前被阻止的IP地址。

**用法**: `/token list-blocked-ips`

**功能**:
- 显示所有被阻止的IP地址列表
- 如果没有IP被阻止，会显示相应提示

**示例**:
```
/token list-blocked-ips
```

#### `/token list-authenticated`
列出所有当前已通过认证的玩家。

**用法**: `/token list-authenticated`

**功能**:
- 显示所有已通过认证的在线玩家列表
- 如果没有已认证的玩家，会显示相应提示

**示例**:
```
/token list-authenticated
```

#### `/token remove-auth <玩家名>`
移除指定玩家的认证状态，强制其重新认证。

**用法**: `/token remove-auth <玩家名>`

**参数**:
- `玩家名`: 要移除认证状态的玩家名称（必须是在线玩家）

**功能**:
- 移除指定玩家的认证状态
- 玩家下次进行需要认证的操作时必须重新认证
- 可用于测试或强制玩家重新验证身份

**示例**:
```
/token remove-auth Steve
/token remove-auth Alex
```

#### `/token enable-email`
启用邮件警报功能。

**用法**: `/token enable-email`

**功能**:
- 启用认证失败和超时的邮件警报
- 当玩家认证失败或超时时，会发送邮件通知
- 需要先在配置文件中正确设置SMTP参数

**示例**:
```
/token enable-email
```

#### `/token disable-email`
禁用邮件警报功能。

**用法**: `/token disable-email`

**功能**:
- 禁用认证失败和超时的邮件警报
- 认证失败或超时将不再发送邮件通知
- 邮件配置保持不变，可以随时重新启用

**示例**:
```
/token disable-email
```

#### `/token test-email`
发送一封测试邮件，验证邮件配置是否正确。

**用法**: `/token test-email`

**功能**:
- 发送一封测试邮件到配置的收件人地址
- 验证SMTP服务器连接和认证是否正常
- 帮助排查邮件发送问题

**示例**:
```
/token test-email
```

## CSV记录命令

### `/csvtest`
测试CSV记录功能是否正常工作。

**用法**: `/csvtest`

**功能**:
- 执行CSV记录功能的全面测试
- 验证文件创建、写入和读取功能
- 测试完成后会显示测试结果

**示例**:
```
/csvtest
```

### `/csvstatus`
显示CSV记录功能的当前状态和配置。

**用法**: `/csvstatus`

**功能**:
- 显示CSV记录是否启用
- 显示CSV文件名和路径
- 显示是否包含地理位置信息
- 显示CSV文件是否存在

**示例**:
```
/csvstatus
```

### `/csvenable`
启用CSV记录功能。

**用法**: `/csvenable`

**功能**:
- 启用认证失败信息的CSV记录
- 所有认证失败尝试将被记录到CSV文件中
- 便于后续分析和审计

**示例**:
```
/csvenable
```

### `/csvdisable`
禁用CSV记录功能。

**用法**: `/csvdisable`

**功能**:
- 禁用认证失败信息的CSV记录
- 不再记录新的认证失败尝试到CSV文件
- 已有的CSV文件会被保留

**示例**:
```
/csvdisable
```

### `/csvtimeout`
切换认证超时记录的开关状态。

**用法**: `/csvtimeout`

**功能**:
- 切换是否将认证超时尝试记录到CSV文件
- 在启用和禁用之间切换
- 显示当前状态和操作结果

**示例**:
```
/csvtimeout
```

## 调试命令

### `/debugtest`
测试调试日志功能是否正常工作。

**用法**: `/debugtest`

**功能**:
- 执行调试日志功能的测试
- 验证各种调试级别的日志输出
- 测试完成后会提示检查服务器日志

**示例**:
```
/debugtest
```

### `/debugstatus`
显示调试模式的当前状态。

**用法**: `/debugstatus`

**功能**:
- 显示调试模式是否启用
- 解释调试模式的作用和影响
- 说明不同调试级别日志的显示条件

**示例**:
```
/debugstatus
```

## 使用示例

### 常见管理任务

1. **初次设置**:
   ```
   /token generate-key    // 生成共享密钥
   /token status          // 检查系统状态
   ```

2. **日常管理**:
   ```
   /token status          // 查看系统状态
   /token list-authenticated    // 查看已认证玩家
   /token list-blocked-ips      // 查看被阻止的IP
   ```

3. **安全管理**:
   ```
   /token block-ip 192.168.1.100 60    // 阻止可疑IP
   /token remove-auth SuspiciousPlayer  // 强制重新认证
   /token reload                       // 重载安全配置
   ```

4. **日志和调试**:
   ```
   /csvtest            // 测试CSV记录
   /csvstatus          // 查看CSV状态
   /debugtest          // 测试调试日志
   /debugstatus        // 查看调试状态
   ```

## 注意事项

1. 所有命令都需要OP 3级权限
2. 配置更改（如`/token reload`）会立即生效，无需重启服务器
3. 生成的密钥需要同时配置到服务器和客户端才能正常工作
4. CSV记录功能可以帮助分析认证失败模式和潜在的安全威胁
5. 调试模式会产生大量日志，建议仅在排查问题时启用

## 故障排除

如果命令执行失败，请检查：

1. 是否有足够的权限（OP 3级）
2. 命令语法是否正确
3. 相关配置文件是否存在且格式正确
4. 服务器日志中是否有更详细的错误信息

更多详细信息，请参考其他相关文档。