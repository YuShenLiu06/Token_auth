# TokenAuth 邮件警报配置指南

## 概述

TokenAuth 邮件警报功能可以在检测到认证失败或认证超时时自动发送邮件通知给管理员。本指南将详细介绍如何配置此功能。

## 配置文件位置

邮件警报配置位于 `run/config/token-auth/token-auth-server.properties` 文件中。

## 配置项详解

### 基本开关

```properties
# 是否启用邮件警报功能
email.enableEmailAlerts=false
```

- **说明**: 控制是否启用邮件警报功能
- **默认值**: `false`
- **可选值**: `true` (启用) 或 `false` (禁用)
- **注意**: 必须设置为 `true` 才会发送邮件警报

### 服务器信息

```properties
# 服务器名称
email.serverName=Minecraft服务器
```

- **说明**: 在邮件中显示的服务器名称
- **默认值**: `Minecraft服务器`
- **建议**: 设置为您的服务器实际名称，如 "我的生存服务器"

### SMTP 服务器配置

```properties
# SMTP服务器地址
email.smtpHost=

# SMTP端口
email.smtpPort=587
```

- **smtpHost**: SMTP服务器地址
  - **默认值**: 空字符串
  - **示例**: 
    - Gmail: `smtp.gmail.com`
    - QQ邮箱: `smtp.qq.com`
    - 163邮箱: `smtp.163.com`
    - Outlook: `smtp-mail.outlook.com`

- **smtpPort**: SMTP服务器端口
  - **默认值**: `587`
  - **常用端口**:
    - 587 (TLS/STARTTLS，推荐)
    - 465 (SSL)
    - 25 (不加密，不推荐)

### SMTP 认证配置

```properties
# SMTP用户名
email.smtpUsername=

# SMTP密码
email.smtpPassword=
```

- **smtpUsername**: SMTP服务器登录用户名
  - **默认值**: 空字符串
  - **注意**: 通常是完整的邮箱地址，如 `your-email@example.com`

- **smtpPassword**: SMTP服务器登录密码
  - **默认值**: 空字符串
  - **注意**: 
    - 对于Gmail，可能需要使用"应用专用密码"
    - 对于QQ邮箱，可能需要开启SMTP服务并获取授权码

### 邮件地址配置

```properties
# 发件人邮箱
email.fromAddress=

# 收件人邮箱
email.toAddress=
```

- **fromAddress**: 发件人邮箱地址

### Outlook/Office 365配置

```properties
email.smtpHost=smtp.office365.com
email.smtpPort=587
email.smtpUsername=your-outlook@outlook.com
email.smtpPassword=your-outlook-password
email.fromAddress=your-outlook@outlook.com
email.toAddress=admin@example.com
```

**Outlook特别说明**:
- **SMTP服务器**: `smtp.office365.com`
- **端口**: `587`
- **加密**: 需要SSL/TLS加密
- **用户名**: 完整的Outlook邮箱地址（如 `yourname@outlook.com`）
- **密码**: 与登录Outlook网站相同的密码
- **注意**: 如果启用了两步验证，可能需要使用应用专用密码
  - **默认值**: 空字符串
  - **要求**: 必须是有效的邮箱格式
  - **注意**: 通常与SMTP用户名相同

- **toAddress**: 接收警报的邮箱地址
  - **默认值**: 空字符串
  - **要求**: 必须是有效的邮箱格式
  - **注意**: 可以是与管理员相同的邮箱，也可以是不同的邮箱

### 地理位置配置

```properties
# 是否包含地理位置信息（需要网络请求）
includeGeoLocation = true
```

- **说明**: 控制是否在警报邮件中包含IP地址的地理位置信息
- **默认值**: `true`
- **可选值**: `true` (包含) 或 `false` (不包含)
- **注意**: 启用此功能需要网络请求，可能会稍微增加处理时间

## 常见邮箱服务配置示例

### Gmail 配置

```properties
email.smtpHost=smtp.gmail.com
email.smtpPort=587
email.smtpUsername=your-gmail@gmail.com
email.smtpPassword=your-app-password  # 使用应用专用密码，不是账户密码
email.fromAddress=your-gmail@gmail.com
email.toAddress=admin@example.com
```

### QQ邮箱配置

```properties
email.smtpHost=smtp.qq.com
email.smtpPort=587
email.smtpUsername=your-qq@qq.com
email.smtpPassword=your-authorization-code  # 使用授权码，不是QQ密码
email.fromAddress=your-qq@qq.com
email.toAddress=admin@example.com
```

### 163邮箱配置

```properties
email.smtpHost=smtp.163.com
email.smtpPort=587
email.smtpUsername=your-163@163.com
email.smtpPassword=your-password
email.fromAddress=your-163@163.com
email.toAddress=admin@example.com
```

## 安全注意事项

1. **密码安全**:
   - 不要在配置文件中使用明文密码
   - 考虑使用应用专用密码或授权码
   - 确保配置文件权限设置正确，只有服务器管理员可读

2. **邮箱安全**:
   - 为警报功能创建专门的邮箱账户
   - 启用邮箱的两步验证
   - 定期更换密码

3. **服务器安全**:
   - 限制配置文件的访问权限
   - 定期检查配置文件是否被修改

## 故障排除

### 邮件发送失败

1. **检查网络连接**:
   - 确保服务器可以访问SMTP服务器
   - 检查防火墙设置

2. **验证配置**:
   - 确认SMTP服务器地址和端口正确
   - 检查用户名和密码是否正确

3. **查看日志**:
   - 检查服务器日志中的错误信息
   - 查找与邮件发送相关的错误

### 常见错误及解决方案

1. **认证失败**:
   - 检查用户名和密码
   - 对于Gmail，确保使用应用专用密码
   - 对于QQ邮箱，确保使用授权码

2. **连接超时**:
   - 检查SMTP服务器地址和端口
   - 确认网络连接正常

3. **邮件被拒收**:
   - 检查发件人邮箱地址格式
   - 确认收件人邮箱地址正确

## 测试配置

完成配置后，可以使用以下步骤测试邮件功能：

1. 设置 `email.enableEmailAlerts=true`
2. 保存配置文件
3. 重启服务器或重新加载配置
4. 使用无效的令牌尝试登录服务器，触发认证失败
5. 检查指定邮箱是否收到警报邮件

## 邮件内容示例

```
致我的生存服务器:

您的服务器貌似遭遇了非法用户的闯入，详细信息如下

玩家名称: TestPlayer
时间: 2025/12/06 20:30:15
IP地址: 192.168.1.100
地理位置: 中国 上海市 (China Mobile)
原因: 认证失败

——Token_auth
```

## 总结

正确配置邮件警报功能后，您将在以下情况收到邮件通知：
- 玩家认证失败
- 玩家认证超时

这些警报将帮助您及时发现潜在的安全威胁，保护服务器安全。