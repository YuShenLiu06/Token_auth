package nety.ys.util;

import nety.ys.TokenAuthMod;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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
                TokenAuthMod.LOGGER.info("正在发送非法闯入报告邮件...");
                TokenAuthMod.LOGGER.info("邮件配置检查 - SMTP服务器: {}, 端口: {}, 用户名: {}, 发件人: {}, 收件人: {}", 
                    config.getSmtpHost(), config.getSmtpPort(), config.getUsername(), config.getFromAddress(), config.getToAddress());
                
                // 验证配置
                if (!config.isValid()) {
                    TokenAuthMod.LOGGER.error("邮件配置无效，无法发送邮件");
                    return false;
                }
                
                // 构建邮件内容
                String emailContent = buildEmailContent(serverName, playerName, timestamp, ipAddress, location, reason);
                TokenAuthMod.LOGGER.info("邮件内容: {}", emailContent);
                
                // 发送邮件
                boolean success = sendEmailWithJavaMail(config, "TokenAuth 非法闯入警报 - " + serverName, emailContent);
                
                if (success) {
                    TokenAuthMod.LOGGER.info("非法闯入报告邮件发送成功");
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
            TokenAuthMod.LOGGER.info("使用JavaMail API发送邮件到: {}", config.getToAddress());
            
            // 创建邮件会话属性
            Properties props = new Properties();
            props.put("mail.smtp.host", config.getSmtpHost());
            props.put("mail.smtp.port", config.getSmtpPort());
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.ssl.trust", "*");
            props.put("mail.smtp.connectiontimeout", "10000");
            props.put("mail.smtp.timeout", "15000");
            
            // 添加更多SSL/TLS配置以提高兼容性
            props.put("mail.smtp.ssl.protocols", "TLSv1.2 TLSv1.3");
            props.put("mail.smtp.ssl.enable", "false"); // 我们使用STARTTLS而不是直接SSL
            props.put("mail.smtp.starttls.required", "true");
            
            // Gmail特定配置
            if (config.getSmtpHost().contains("gmail.com")) {
                props.put("mail.smtp.ssl.socketFactory", "javax.net.ssl.SSLSocketFactory");
                props.put("mail.smtp.socketFactory.port", config.getSmtpPort());
                props.put("mail.smtp.socketFactory.fallback", "false");
                props.put("mail.smtp.ssl.socketFactory.port", config.getSmtpPort());
            }
            
            // 添加调试信息
            props.put("mail.debug", "true");
            
            // 创建邮件会话
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(config.getUsername(), config.getPassword());
                }
            });
            
            // 启用调试模式
            session.setDebug(true);
            
            // 创建邮件消息
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(config.getFromAddress()));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(config.getToAddress()));
            message.setSubject(subject);
            message.setText(content);
            message.setSentDate(new java.util.Date());
            
            // 发送邮件
            Transport.send(message);
            
            TokenAuthMod.LOGGER.info("邮件发送成功");
            return true;
            
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("使用JavaMail发送邮件时出错", e);
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
        emailExecutor.shutdown();
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
        
        public EmailConfig(String smtpHost, String smtpPort, String username, 
                          String password, String fromAddress, String toAddress) {
            this.smtpHost = smtpHost;
            this.smtpPort = smtpPort;
            this.username = username;
            this.password = password;
            this.fromAddress = fromAddress;
            this.toAddress = toAddress;
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