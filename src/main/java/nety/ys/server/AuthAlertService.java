package nety.ys.server;

import nety.ys.TokenAuthMod;
import nety.ys.config.ModConfig;
import nety.ys.config.SimpleConfigManager;
import nety.ys.util.EmailNotifier;
import nety.ys.util.IPGeolocationUtil;
import nety.ys.util.DebugLogger;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 认证警报服务
 * 负责处理认证失败和超时事件，并发送邮件通知
 * 使用独立线程处理邮件和地理位置请求，避免阻塞主线程
 *
 * @author nety.ys
 */
public class AuthAlertService {
    
    // 使用独立线程池处理警报服务，避免阻塞主线程
    private static final ExecutorService alertExecutor = Executors.newFixedThreadPool(2);
    
    /**
     * 发送认证失败警报邮件
     * 
     * @param playerName 玩家名称
     * @param ipAddress IP地址
     * @param reason 失败原因
     */
    public static CompletableFuture<Void> sendAuthFailureAlert(String playerName, InetAddress ipAddress, String reason) {
        return CompletableFuture.runAsync(() -> {
            try {
                DebugLogger.email("准备发送认证失败警报邮件 - 玩家: {}, IP: {}, 原因: {}",
                    playerName, ipAddress.getHostAddress(), reason);
                
                // 获取服务器配置
                SimpleConfigManager configManager = (SimpleConfigManager) TokenAuthMod.getInstance().getConfigManager();
                ModConfig.ServerConfig config = configManager.getServerConfig();
                
                // 检查是否启用了邮件警报
                if (!config.enableEmailAlerts) {
                    DebugLogger.email("邮件警报功能已禁用，跳过发送认证失败邮件");
                    return;
                }
                
                DebugLogger.email("邮件警报功能已启用，继续处理认证失败警报");
                
                // 检查邮件配置是否有效
                if (!isEmailConfigValid(config)) {
                    TokenAuthMod.LOGGER.warn("邮件配置无效，无法发送认证失败警报");
                    return;
                }
                
                // 获取当前时间
                String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss"));
                DebugLogger.email("获取当前时间: {}", timestamp);
                
                // 异步获取地理位置信息
                String location = "未知位置";
                if (config.includeGeoLocation) {
                    try {
                        DebugLogger.email("正在获取IP {} 的地理位置信息...", ipAddress.getHostAddress());
                        IPGeolocationUtil.GeoLocationInfo geoInfo = IPGeolocationUtil.getGeoLocation(ipAddress);
                        location = geoInfo.getFullLocation();
                        DebugLogger.email("获取地理位置信息成功: {}", location);
                    } catch (Exception e) {
                        TokenAuthMod.LOGGER.error("获取IP {} 地理位置信息失败", ipAddress.getHostAddress(), e);
                    }
                } else {
                    DebugLogger.email("地理位置信息获取已禁用");
                }
                
                // 创建邮件配置
                EmailNotifier.EmailConfig emailConfig = new EmailNotifier.EmailConfig(
                    config.smtpHost,
                    config.smtpPort,
                    config.smtpUsername,
                    config.smtpPassword,
                    config.emailFromAddress,
                    config.emailToAddress,
                    config.enableSSL
                );
                
                DebugLogger.email("创建邮件配置完成，准备发送邮件");
                
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
                        DebugLogger.email("认证失败警报邮件发送成功");
                    } else {
                        TokenAuthMod.LOGGER.warn("认证失败警报邮件发送失败");
                    }
                });
                
            } catch (Exception e) {
                TokenAuthMod.LOGGER.error("发送认证失败警报时出错", e);
            }
        }, alertExecutor);
    }
    
    /**
     * 发送认证超时警报邮件
     * 
     * @param playerName 玩家名称
     * @param ipAddress IP地址
     */
    public static CompletableFuture<Void> sendAuthTimeoutAlert(String playerName, InetAddress ipAddress) {
        DebugLogger.email("准备发送认证超时警报邮件 - 玩家: {}, IP: {}",
                playerName, ipAddress.getHostAddress());
        return sendAuthFailureAlert(playerName, ipAddress, "认证超时");
    }
    
    /**
     * 关闭警报服务执行器
     */
    public static void shutdown() {
        TokenAuthMod.LOGGER.info("正在关闭认证警报服务...");
        
        if (alertExecutor != null && !alertExecutor.isShutdown()) {
            alertExecutor.shutdown();
            try {
                // 等待当前任务完成，最多等待10秒
                if (!alertExecutor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS)) {
                    TokenAuthMod.LOGGER.warn("警报服务执行器未能在10秒内正常关闭，强制关闭");
                    alertExecutor.shutdownNow();
                    
                    // 再给5秒时间让任务响应中断
                    if (!alertExecutor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                        TokenAuthMod.LOGGER.error("警报服务执行器强制关闭失败");
                    }
                } else {
                    TokenAuthMod.LOGGER.info("认证警报服务已正常关闭");
                }
            } catch (InterruptedException e) {
                TokenAuthMod.LOGGER.warn("等待警报服务执行器关闭时被中断，强制关闭");
                alertExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
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
        
        DebugLogger.email("邮件配置验证结果: {}", isValid);
        
        if (!isValid) {
            TokenAuthMod.LOGGER.warn("邮件配置项检查:");
            DebugLogger.email("  SMTP主机: {}", config.smtpHost != null ? config.smtpHost : "null");
            DebugLogger.email("  SMTP端口: {}", config.smtpPort != null ? config.smtpPort : "null");
            DebugLogger.email("  SMTP用户名: {}", config.smtpUsername != null ? config.smtpUsername : "null");
            DebugLogger.email("  SMTP密码: {}", config.smtpPassword != null && !config.smtpPassword.isEmpty() ? "[已设置]" : "[空]");
            DebugLogger.email("  发件人地址: {}", config.emailFromAddress != null ? config.emailFromAddress : "null");
            DebugLogger.email("  收件人地址: {}", config.emailToAddress != null ? config.emailToAddress : "null");
        }
        
        return isValid;
    }
}