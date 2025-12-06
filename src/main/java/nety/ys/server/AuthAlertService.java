package nety.ys.server;

import nety.ys.TokenAuthMod;
import nety.ys.config.ModConfig;
import nety.ys.config.SimpleConfigManager;
import nety.ys.util.EmailNotifier;
import nety.ys.util.IPGeolocationUtil;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 认证警报服务
 * 负责处理认证失败和超时事件，并发送邮件通知
 * 
 * @author nety.ys
 */
public class AuthAlertService {
    
    /**
     * 发送认证失败警报邮件
     * 
     * @param playerName 玩家名称
     * @param ipAddress IP地址
     * @param reason 失败原因
     */
    public static void sendAuthFailureAlert(String playerName, InetAddress ipAddress, String reason) {
        try {
            TokenAuthMod.LOGGER.info("准备发送认证失败警报邮件 - 玩家: {}, IP: {}, 原因: {}", 
                playerName, ipAddress.getHostAddress(), reason);
            
            // 获取服务器配置
            SimpleConfigManager configManager = (SimpleConfigManager) TokenAuthMod.getInstance().getConfigManager();
            ModConfig.ServerConfig config = configManager.getServerConfig();
            
            // 检查是否启用了邮件警报
            if (!config.enableEmailAlerts) {
                TokenAuthMod.LOGGER.info("邮件警报功能已禁用，跳过发送认证失败邮件");
                return;
            }
            
            TokenAuthMod.LOGGER.info("邮件警报功能已启用，继续处理认证失败警报");
            
            // 检查邮件配置是否有效
            if (!isEmailConfigValid(config)) {
                TokenAuthMod.LOGGER.warn("邮件配置无效，无法发送认证失败警报");
                return;
            }
            
            // 获取当前时间
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
            TokenAuthMod.LOGGER.info("获取当前时间: {}", timestamp);
            
            // 获取地理位置信息
            String location = "未知位置";
            if (config.includeGeoLocation) {
                try {
                    TokenAuthMod.LOGGER.info("正在获取IP {} 的地理位置信息...", ipAddress.getHostAddress());
                    IPGeolocationUtil.GeoLocationInfo geoInfo = IPGeolocationUtil.getGeoLocation(ipAddress);
                    location = geoInfo.getFullLocation();
                    TokenAuthMod.LOGGER.info("获取地理位置信息成功: {}", location);
                } catch (Exception e) {
                    TokenAuthMod.LOGGER.error("获取IP {} 地理位置信息失败", ipAddress.getHostAddress(), e);
                }
            } else {
                TokenAuthMod.LOGGER.info("地理位置信息获取已禁用");
            }
            
            // 创建邮件配置
            EmailNotifier.EmailConfig emailConfig = new EmailNotifier.EmailConfig(
                config.smtpHost,
                config.smtpPort,
                config.smtpUsername,
                config.smtpPassword,
                config.emailFromAddress,
                config.emailToAddress
            );
            
            TokenAuthMod.LOGGER.info("创建邮件配置完成，准备发送邮件");
            
            // 发送邮件
            EmailNotifier.sendIntrusionAlert(
                config.serverName,
                playerName,
                timestamp,
                ipAddress.getHostAddress(),
                location,
                reason,
                emailConfig
            ).thenAccept(success -> {
                if (success) {
                    TokenAuthMod.LOGGER.info("认证失败警报邮件发送成功");
                } else {
                    TokenAuthMod.LOGGER.warn("认证失败警报邮件发送失败");
                }
            });
            
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("发送认证失败警报时出错", e);
        }
    }
    
    /**
     * 发送认证超时警报邮件
     * 
     * @param playerName 玩家名称
     * @param ipAddress IP地址
     */
    public static void sendAuthTimeoutAlert(String playerName, InetAddress ipAddress) {
        TokenAuthMod.LOGGER.info("准备发送认证超时警报邮件 - 玩家: {}, IP: {}", 
                playerName, ipAddress.getHostAddress());
        sendAuthFailureAlert(playerName, ipAddress, "认证超时");
    }
    
    /**
     * 检查邮件配置是否有效
     * 
     * @param config 服务器配置
     * @return 如果邮件配置有效则返回true
     */
    private static boolean isEmailConfigValid(ModConfig.ServerConfig config) {
        boolean isValid = config.smtpHost != null && !config.smtpHost.isEmpty() &&
               config.smtpPort != null && !config.smtpPort.isEmpty() &&
               config.smtpUsername != null && !config.smtpUsername.isEmpty() &&
               config.smtpPassword != null && !config.smtpPassword.isEmpty() &&
               config.emailFromAddress != null && !config.emailFromAddress.isEmpty() &&
               config.emailToAddress != null && !config.emailToAddress.isEmpty();
        
        TokenAuthMod.LOGGER.info("邮件配置验证结果: {}", isValid);
        
        if (!isValid) {
            TokenAuthMod.LOGGER.warn("邮件配置项检查:");
            TokenAuthMod.LOGGER.warn("  SMTP主机: {}", config.smtpHost != null ? config.smtpHost : "null");
            TokenAuthMod.LOGGER.warn("  SMTP端口: {}", config.smtpPort != null ? config.smtpPort : "null");
            TokenAuthMod.LOGGER.warn("  SMTP用户名: {}", config.smtpUsername != null ? config.smtpUsername : "null");
            TokenAuthMod.LOGGER.warn("  SMTP密码: {}", config.smtpPassword != null && !config.smtpPassword.isEmpty() ? "[已设置]" : "[空]");
            TokenAuthMod.LOGGER.warn("  发件人地址: {}", config.emailFromAddress != null ? config.emailFromAddress : "null");
            TokenAuthMod.LOGGER.warn("  收件人地址: {}", config.emailToAddress != null ? config.emailToAddress : "null");
        }
        
        return isValid;
    }
}