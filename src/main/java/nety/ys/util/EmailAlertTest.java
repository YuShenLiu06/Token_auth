package nety.ys.util;

import nety.ys.TokenAuthMod;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * 邮件警报测试类
 * 用于测试邮件发送功能
 * 
 * @author nety.ys
 */
public class EmailAlertTest {
    
    /**
     * 测试邮件发送功能
     * 
     * @param smtpHost SMTP服务器地址
     * @param smtpPort SMTP端口
     * @param username SMTP用户名
     * @param password SMTP密码
     * @param fromAddress 发件人邮箱
     * @param toAddress 收件人邮箱
     * @param serverName 服务器名称
     * @return CompletableFuture<Boolean> 表示测试是否成功
     */
    public static CompletableFuture<Boolean> testEmailSending(
            String smtpHost,
            String smtpPort,
            String username,
            String password,
            String fromAddress,
            String toAddress,
            String serverName) {
        
        return CompletableFuture.supplyAsync(() -> {
            try {
                TokenAuthMod.LOGGER.info("开始测试邮件发送功能...");
                
                // 创建邮件配置
                EmailNotifier.EmailConfig emailConfig = new EmailNotifier.EmailConfig(
                    smtpHost, smtpPort, username, password, fromAddress, toAddress, false);
                
                // 验证配置
                if (!emailConfig.isValid()) {
                    TokenAuthMod.LOGGER.error("邮件配置无效，测试失败");
                    return false;
                }
                
                // 获取当前时间
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
                
                // 发送测试邮件
                CompletableFuture<Boolean> result = EmailNotifier.sendIntrusionAlert(
                    serverName,
                    "TestPlayer",
                    timestamp,
                    "127.0.0.1",
                    "本地测试 (reserved range)",
                    "测试邮件功能",
                    emailConfig);
                
                // 等待发送完成
                Boolean success = result.get();
                
                if (success) {
                    TokenAuthMod.LOGGER.info("邮件发送测试成功");
                } else {
                    TokenAuthMod.LOGGER.error("邮件发送测试失败");
                }
                
                return success;
                
            } catch (Exception e) {
                TokenAuthMod.LOGGER.error("测试邮件发送功能时出错", e);
                return false;
            }
        });
    }
    
    /**
     * 测试邮件配置
     * 
     * @param emailConfig 邮件配置
     * @return 配置是否有效
     */
    public static boolean validateEmailConfig(EmailNotifier.EmailConfig emailConfig) {
        if (emailConfig == null) {
            TokenAuthMod.LOGGER.error("邮件配置为null");
            return false;
        }
        
        if (!emailConfig.isValid()) {
            TokenAuthMod.LOGGER.error("邮件配置无效");
            return false;
        }
        
        TokenAuthMod.LOGGER.info("邮件配置验证通过");
        return true;
    }
}