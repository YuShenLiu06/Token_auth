# 邮件警报功能故障排除指南

## 问题描述

邮件警报功能在本地环境正常工作，但在云端服务器环境中无法发送邮件。

## 可能的原因分析

### 1. 网络防火墙/安全组限制

**问题描述**：
云服务提供商通常会限制出站SMTP端口的访问，以防止垃圾邮件。

**常见限制端口**：
- 端口 25：许多云服务商完全阻止
- 端口 587：部分云服务商可能限制
- 端口 465：部分云服务商可能限制

**解决方案**：
1. 检查云服务商的安全组/防火墙设置
2. 尝试使用不同的SMTP端口
3. 考虑使用云服务商提供的邮件服务（如AWS SES、阿里云邮件推送）

### 2. SMTP端口配置问题

**问题描述**：
配置文件中使用的是端口25，这是最容易被阻止的端口。

**解决方案**：
1. **修改为587端口（STARTTLS）**：
   ```properties
   email.smtpPort=587
   ```

2. **修改为465端口（SSL/TLS）**：
   ```properties
   email.smtpPort=465
   ```

3. **根据邮箱服务商推荐配置**：
   - Gmail: 587 (STARTTLS) 或 465 (SSL/TLS)
   - QQ邮箱: 587 (STARTTLS) 或 465 (SSL/TLS)
   - 163邮箱: 587 (STARTTLS) 或 465 (SSL/TLS)
   - Outlook: 587 (STARTTLS)

### 3. DNS解析问题

**问题描述**：
云端服务器可能无法正确解析SMTP服务器地址。

**解决方案**：
1. 检查DNS配置：
   ```bash
   nslookup smtp.gmail.com
   ```

2. 尝试使用IP地址代替域名（不推荐，仅用于测试）

3. 配置自定义DNS服务器（如8.8.8.8）

### 4. SSL/TLS证书验证问题

**问题描述**：
云端Java环境可能缺少必要的根证书，导致SSL/TLS握手失败。

**解决方案**：
1. 更新Java证书库：
   ```bash
   sudo update-ca-certificates
   ```

2. 导入邮箱服务商的证书（高级操作）

3. 临时禁用证书验证（仅用于测试，不推荐生产环境）

### 5. 邮箱服务商限制

**问题描述**：
邮箱服务商可能限制从云服务器IP发送邮件。

**常见限制**：
- IP地址被标记为可疑
- 发送频率限制
- 地理位置限制

**解决方案**：
1. 使用企业邮箱服务（如Google Workspace、Microsoft 365）
2. 申请IP白名单
3. 使用云服务商提供的邮件服务

### 6. JavaMail配置问题

**问题描述**：
JavaMail配置可能与云端环境不兼容。

**解决方案**：
1. 添加更多兼容性配置：
   ```properties
   email.smtpPort=587
   # 在代码中自动配置以下属性
   mail.smtp.ssl.protocols=TLSv1.2 TLSv1.3
   mail.smtp.ssl.enable=false
   mail.smtp.starttls.required=true
   mail.smtp.connectiontimeout=10000
   mail.smtp.timeout=15000
   ```

## 排查步骤

### 1. 启用调试模式

启用调试模式以获取详细日志：
```
/debugstatus  # 检查当前调试状态
```

在配置文件中启用调试模式：
```properties
logging.debugMode=true
```

### 2. 使用测试命令

使用内置的邮件测试命令：
```
/token test-email
```

### 3. 检查日志

查找以下关键日志信息：
- `[EMAIL]` 前缀的邮件相关日志
- JavaMail的调试输出
- 网络连接异常

### 4. 网络连通性测试

测试SMTP服务器连通性：
```bash
telnet smtp.gmail.com 587
```

或使用openssl测试：
```bash
openssl s_client -connect smtp.gmail.com:587 -starttls smtp
```

## 推荐配置

### Gmail配置（推荐）

```properties
email.enableEmailAlerts=true
email.smtpHost=smtp.gmail.com
email.smtpPort=587
email.smtpUsername=your-email@gmail.com
email.smtpPassword=your-app-password  # 使用应用专用密码
email.fromAddress=your-email@gmail.com
email.toAddress=recipient@example.com
```

### 企业邮箱配置

```properties
email.enableEmailAlerts=true
email.smtpHost=smtp.office365.com
email.smtpPort=587
email.smtpUsername=your-email@company.com
email.smtpPassword=your-password
email.fromAddress=your-email@company.com
email.toAddress=admin@company.com
```

## 云服务商特定解决方案

### AWS EC2

1. 在安全组中添加出站规则：
   - 协议：TCP
   - 端口范围：587
   - 目标：0.0.0.0/0

2. 考虑使用AWS SES服务

### 阿里云ECS

1. 在安全组中添加出站规则：
   - 协议：TCP
   - 端口范围：587
   - 目标：0.0.0.0/0

2. 考虑使用阿里云邮件推送服务

### 腾讯云CVM

1. 在安全组中添加出站规则：
   - 协议：TCP
   - 端口范围：587
   - 目标：0.0.0.0/0

2. 考虑使用腾讯云邮件服务

## 替代方案

如果上述方法都无法解决问题，可以考虑以下替代方案：

1. **使用云服务商提供的邮件服务**：
   - AWS SES
   - 阿里云邮件推送
   - 腾讯云邮件服务

2. **使用第三方邮件API服务**：
   - SendGrid
   - Mailgun
   - Postmark

3. **使用Webhook通知**：
   - 配置Webhook URL
   - 发送HTTP请求代替邮件

## 常见错误及解决方法

### 错误：Connection timed out

**原因**：网络连接超时，可能是防火墙阻止或网络问题

**解决**：
1. 检查防火墙设置
2. 尝试不同端口
3. 检查DNS解析

### 错误：Authentication failed

**原因**：用户名或密码错误

**解决**：
1. 检查邮箱账号和密码
2. 对于Gmail，使用应用专用密码
3. 检查是否开启了"两步验证"

### 错误：SSLHandshakeException

**原因**：SSL/TLS证书验证失败

**解决**：
1. 更新Java证书库
2. 检查系统时间是否正确
3. 尝试不同的加密协议

## 联系支持

如果问题仍然存在，请提供以下信息：

1. 云服务商类型和地区
2. 使用的邮箱服务商
3. 完整的错误日志
4. 网络连通性测试结果
5. 邮件配置（隐藏敏感信息）

这样可以帮助更快地定位和解决问题。