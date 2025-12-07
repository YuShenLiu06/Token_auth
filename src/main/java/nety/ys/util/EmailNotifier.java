package nety.ys.util;

import nety.ys.TokenAuthMod;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 邮件通知工具类
 * 负责发送认证失败和超时的警报邮件
 * 使用标准JavaMail API，更加可靠和兼容
 * 
 * @author nety.ys
 */
public class EmailNotifier {
    
    private static final ExecutorService emailExecutor = Executors.newSingleThreadExecutor();
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})$");
    
    /**
     * 发送非法闯入报告邮件
     * 
     * @param serverName 服务器名称
     * @param playerName 玩家名称
     * @param timestamp 时间戳
     * @param ipAddress IP地址
     * @param location 地理位置
     * @param reason 原因（认证失败或认证超时）
     * @param config 邮件配置
     * @return CompletableFuture<Boolean> 表示发送是否成功
     */
    public static CompletableFuture<Boolean> sendIntrusionAlert(
            String serverName,
            String playerName,
            String timestamp,
            String ipAddress,
            String location,
            String reason,
            EmailConfig config) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                DebugLogger.email("正在发送非法闯入报告邮件...");
                DebugLogger.email("邮件配置检查 - SMTP服务器: {}, 端口: {}, 用户名: {}, 发件人: {}, 收件人: {}",
                    config.getSmtpHost(), config.getSmtpPort(), config.getUsername(), config.getFromAddress(), config.getToAddress());
                
                // 验证配置
                if (!config.isValid()) {
                    TokenAuthMod.LOGGER.error("邮件配置无效，无法发送邮件");
                    return false;
                }
                
                // 构建邮件内容
                String emailContent = buildEmailContent(serverName, playerName, timestamp, ipAddress, location, reason);
                DebugLogger.email("邮件内容: {}", emailContent);
                
                // 发送邮件
                boolean success = sendEmailWithJavaMail(config, "TokenAuth 非法闯入警报 - " + serverName, emailContent);
                
                if (success) {
                    DebugLogger.email("非法闯入报告邮件发送成功");
                } else {
                    TokenAuthMod.LOGGER.error("非法闯入报告邮件发送失败");
                }
                
                return success;
                
            } catch (Exception e) {
                TokenAuthMod.LOGGER.error("发送非法闯入报告邮件时出错", e);
                return false;
            }
        }, emailExecutor);
    }
    
    /**
     * 使用JavaMail API发送邮件
     * 
     * @param config 邮件配置
     * @param subject 邮件主题
     * @param content 邮件内容
     * @return 是否发送成功
     */
    private static boolean sendEmailWithJavaMail(EmailConfig config, String subject, String content) {
        try {
            DebugLogger.email("使用JavaMail API发送邮件到: {}, SSL启用: {}", config.getToAddress(), config.isEnableSSL());
            DebugLogger.email("邮件主题: {}", subject);
            DebugLogger.email("邮件内容预览: {}", content.length() > 100 ? content.substring(0, 100) + "..." : content);
            
            // 创建邮件会话属性
            Properties props = new Properties();
            props.put("mail.smtp.host", config.getSmtpHost());
            props.put("mail.smtp.port", config.getSmtpPort());
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.connectiontimeout", "10000");
            props.put("mail.smtp.timeout", "15000");
            
            // 添加更多SSL/TLS配置以提高兼容性
            props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
            
            // 根据SSL配置设置不同的加密方式
            if (config.isEnableSSL()) {
                // 启用直接SSL连接
                props.put("mail.smtp.ssl.enable", "true");
                props.put("mail.smtp.starttls.enable", "false");
                props.put("mail.smtp.starttls.required", "false");
                props.put("mail.smtp.ssl.trust", "*");
                props.put("mail.smtp.ssl.socketFactory", "javax.net.ssl.SSLSocketFactory");
                props.put("mail.smtp.socketFactory.port", config.getSmtpPort());
                props.put("mail.smtp.socketFactory.fallback", "false");
                DebugLogger.email("配置为SSL模式，端口: {}", config.getSmtpPort());
            } else {
                // 使用STARTTLS
                props.put("mail.smtp.ssl.enable", "false");
                props.put("mail.smtp.starttls.enable", "true");
                props.put("mail.smtp.starttls.required", "true");
                props.put("mail.smtp.ssl.trust", "*");
                DebugLogger.email("配置为STARTTLS模式，端口: {}", config.getSmtpPort());
            }
            
            // Gmail特定配置（如果启用了SSL，则不需要额外配置）
            if (!config.isEnableSSL() && config.getSmtpHost().contains("gmail.com")) {
                props.put("mail.smtp.ssl.socketFactory", "javax.net.ssl.SSLSocketFactory");
                props.put("mail.smtp.socketFactory.port", config.getSmtpPort());
                props.put("mail.smtp.socketFactory.fallback", "false");
                props.put("mail.smtp.ssl.socketFactory.port", config.getSmtpPort());
                DebugLogger.email("检测到Gmail SMTP，应用特定配置");
            }
            
            // 添加调试信息
            props.put("mail.debug", "true");
            
            DebugLogger.email("正在创建邮件会话，SMTP服务器: {}:{}", config.getSmtpHost(), config.getSmtpPort());
            DebugLogger.email("认证用户名: {}", config.getUsername());
            
            // 创建邮件会话
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    DebugLogger.email("正在使用SMTP认证: {}", config.getUsername());
                    return new PasswordAuthentication(config.getUsername(), config.getPassword());
                }
            });
            
            // 启用调试模式
            session.setDebug(true);
            DebugLogger.email("邮件会话调试模式已启用");
            
            // 创建邮件消息
            DebugLogger.email("正在创建邮件消息...");
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(config.getFromAddress()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(config.getToAddress()));
            message.setSubject(subject);
            message.setText(content);
            message.setSentDate(new java.util.Date());
            
            DebugLogger.email("邮件消息创建完成，发件人: {}, 收件人: {}",
                config.getFromAddress(), config.getToAddress());
            
            // 发送邮件
            DebugLogger.email("开始发送邮件...");
            Transport.send(message);
            
            DebugLogger.email("邮件发送成功");
            return true;
            
        } catch (MessagingException e) {
            if (e.getMessage() != null && e.getMessage().contains("Authentication failed")) {
                TokenAuthMod.LOGGER.error("SMTP认证失败，请检查用户名和密码: {}", e.getMessage());
                DebugLogger.email("SMTP认证失败详情: {}", e.toString());
            } else if (e instanceof SendFailedException ||
                      (e.getMessage() != null && e.getMessage().contains("Invalid addresses"))) {
                TokenAuthMod.LOGGER.error("邮件发送失败，可能是收件人地址无效: {}", e.getMessage());
                DebugLogger.email("邮件发送失败详情: {}", e.toString());
            } else {
                TokenAuthMod.LOGGER.error("邮件消息处理错误: {}", e.getMessage());
                DebugLogger.email("邮件消息处理错误详情: {}", e.toString());
            }
            return false;
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("使用JavaMail发送邮件时出现未知错误", e);
            DebugLogger.email("未知错误详情: {}", e.toString());
            return false;
        }
    }
    
    /**
     * 构建邮件内容
     * 
     * @param serverName 服务器名称
     * @param playerName 玩家名称
     * @param timestamp 时间戳
     * @param ipAddress IP地址
     * @param location 地理位置
     * @param reason 原因
     * @return 邮件内容
     */
    private static String buildEmailContent(
            String serverName,
            String playerName,
            String timestamp,
            String ipAddress,
            String location,
            String reason) {
        
        StringBuilder content = new StringBuilder();
        content.append("致").append(serverName).append(":\n\n");
        content.append("您的服务器貌似遭遇了非法用户的闯入，详细信息如下\n\n");
        content.append("玩家名称: ").append(playerName).append("\n");
        content.append("时间: ").append(timestamp).append("\n");
        content.append("IP地址: ").append(ipAddress).append("\n");
        content.append("地理位置: ").append(location).append("\n");
        content.append("原因: ").append(reason).append("\n\n");
        content.append("——Token_auth");
        
        return content.toString();
    }
    
    /**
     * 验证邮箱地址格式
     * 
     * @param email 邮箱地址
     * @return 是否有效
     */
    private static boolean isValidEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }
    
    /**
     * 关闭邮件执行器
     */
    public static void shutdown() {
        TokenAuthMod.LOGGER.info("正在关闭邮件通知服务...");
        
        if (emailExecutor != null && !emailExecutor.isShutdown()) {
            emailExecutor.shutdown();
            try {
                // 等待当前任务完成，最多等待10秒
                if (!emailExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                    TokenAuthMod.LOGGER.warn("邮件执行器未能在10秒内正常关闭，强制关闭");
                    emailExecutor.shutdownNow();
                    
                    // 再给5秒时间让任务响应中断
                    if (!emailExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                        TokenAuthMod.LOGGER.error("邮件执行器强制关闭失败");
                    }
                } else {
                    TokenAuthMod.LOGGER.info("邮件通知服务已正常关闭");
                }
            } catch (InterruptedException e) {
                TokenAuthMod.LOGGER.warn("等待邮件执行器关闭时被中断，强制关闭");
                emailExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * 邮件配置类
     */
    public static class EmailConfig {
        private final String smtpHost;
        private final String smtpPort;
        private final String username;
        private final String password;
        private final String fromAddress;
        private final String toAddress;
        private final boolean enableSSL;
        
        public EmailConfig(String smtpHost, String smtpPort, String username,
                          String password, String fromAddress, String toAddress, boolean enableSSL) {
            this.smtpHost = smtpHost;
            this.smtpPort = smtpPort;
            this.username = username;
            this.password = password;
            this.fromAddress = fromAddress;
            this.toAddress = toAddress;
            this.enableSSL = enableSSL;
        }
        
        /**
         * 兼容性构造函数，默认不启用SSL
         */
        public EmailConfig(String smtpHost, String smtpPort, String username,
                          String password, String fromAddress, String toAddress) {
            this(smtpHost, smtpPort, username, password, fromAddress, toAddress, false);
        }
        
        public String getSmtpHost() {
            return smtpHost;
        }
        
        public String getSmtpPort() {
            return smtpPort;
        }
        
        public String getUsername() {
            return username;
        }
        
        public String getPassword() {
            return password;
        }
        
        public String getFromAddress() {
            return fromAddress;
        }
        
        public String getToAddress() {
            return toAddress;
        }
        
        public boolean isEnableSSL() {
            return enableSSL;
        }
        
        /**
         * 检查邮件配置是否有效
         * 
         * @return 如果配置有效则返回true
         */
        public boolean isValid() {
            return smtpHost != null && !smtpHost.isEmpty() &&
                   smtpPort != null && !smtpPort.isEmpty() &&
                   username != null && !username.isEmpty() &&
                   password != null && !password.isEmpty() &&
                   fromAddress != null && !fromAddress.isEmpty() && isValidEmail(fromAddress) &&
                   toAddress != null && !toAddress.isEmpty() && isValidEmail(toAddress);
        }
    }
}