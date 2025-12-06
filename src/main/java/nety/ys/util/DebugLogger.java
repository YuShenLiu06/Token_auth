package nety.ys.util;

import nety.ys.TokenAuthMod;
import nety.ys.config.ModConfig;
import nety.ys.config.SimpleConfigManager;

/**
 * 调试日志管理器
 * 根据配置决定是否输出调试信息
 * 
 * @author nety.ys
 */
public class DebugLogger {
    
    /**
     * 输出调试信息
     * 只有在调试模式下才会输出
     * 
     * @param message 日志消息
     * @param args 参数
     */
    public static void debug(String message, Object... args) {
        if (isDebugMode()) {
            TokenAuthMod.LOGGER.info("[DEBUG] " + message, args);
        }
    }
    
    /**
     * 输出调试信息
     * 只有在调试模式下才会输出
     * 
     * @param message 日志消息
     */
    public static void debug(String message) {
        if (isDebugMode()) {
            TokenAuthMod.LOGGER.info("[DEBUG] " + message);
        }
    }
    
    /**
     * 输出调试信息（带异常）
     * 只有在调试模式下才会输出
     * 
     * @param message 日志消息
     * @param throwable 异常对象
     */
    public static void debug(String message, Throwable throwable) {
        if (isDebugMode()) {
            TokenAuthMod.LOGGER.info("[DEBUG] " + message, throwable);
        }
    }
    
    /**
     * 输出详细信息
     * 用于记录详细的认证过程信息，在调试模式下输出
     * 
     * @param message 日志消息
     * @param args 参数
     */
    public static void verbose(String message, Object... args) {
        if (isDebugMode()) {
            TokenAuthMod.LOGGER.info("[VERBOSE] " + message, args);
        }
    }
    
    /**
     * 输出详细信息
     * 用于记录详细的认证过程信息，在调试模式下输出
     * 
     * @param message 日志消息
     */
    public static void verbose(String message) {
        if (isDebugMode()) {
            TokenAuthMod.LOGGER.info("[VERBOSE] " + message);
        }
    }
    
    /**
     * 输出详细信息（带异常）
     * 用于记录详细的认证过程信息，在调试模式下输出
     * 
     * @param message 日志消息
     * @param throwable 异常对象
     */
    public static void verbose(String message, Throwable throwable) {
        if (isDebugMode()) {
            TokenAuthMod.LOGGER.info("[VERBOSE] " + message, throwable);
        }
    }
    
    /**
     * 输出认证过程信息
     * 在调试模式下输出详细信息，否则只输出简要信息
     * 
     * @param message 日志消息
     * @param args 参数
     */
    public static void auth(String message, Object... args) {
        if (isDebugMode()) {
            TokenAuthMod.LOGGER.info("[AUTH] " + message, args);
        }
    }
    
    /**
     * 输出认证过程信息
     * 在调试模式下输出详细信息，否则只输出简要信息
     * 
     * @param message 日志消息
     */
    public static void auth(String message) {
        if (isDebugMode()) {
            TokenAuthMod.LOGGER.info("[AUTH] " + message);
        }
    }
    
    /**
     * 输出认证过程信息（带异常）
     * 在调试模式下输出详细信息，否则只输出简要信息
     * 
     * @param message 日志消息
     * @param throwable 异常对象
     */
    public static void auth(String message, Throwable throwable) {
        if (isDebugMode()) {
            TokenAuthMod.LOGGER.info("[AUTH] " + message, throwable);
        }
    }
    
    /**
     * 检查是否启用了调试模式
     * 
     * @return 如果启用了调试模式则返回true
     */
    private static boolean isDebugMode() {
        try {
            SimpleConfigManager configManager = (SimpleConfigManager) TokenAuthMod.getInstance().getConfigManager();
            if (configManager != null) {
                ModConfig.ServerConfig config = configManager.getServerConfig();
                return config != null && config.debugMode;
            }
        } catch (Exception e) {
            // 如果获取配置失败，默认不输出调试信息
        }
        return false;
    }
    
    /**
     * 输出CSV记录相关的调试信息
     * 只有在调试模式下才会输出
     * 
     * @param message 日志消息
     * @param args 参数
     */
    public static void csv(String message, Object... args) {
        if (isDebugMode()) {
            TokenAuthMod.LOGGER.info("[CSV] " + message, args);
        }
    }
    
    /**
     * 输出CSV记录相关的调试信息
     * 只有在调试模式下才会输出
     * 
     * @param message 日志消息
     */
    public static void csv(String message) {
        if (isDebugMode()) {
            TokenAuthMod.LOGGER.info("[CSV] " + message);
        }
    }
    
    /**
     * 输出邮件警报相关的调试信息
     * 只有在调试模式下才会输出
     * 
     * @param message 日志消息
     * @param args 参数
     */
    public static void email(String message, Object... args) {
        if (isDebugMode()) {
            TokenAuthMod.LOGGER.info("[EMAIL] " + message, args);
        }
    }
    
    /**
     * 输出邮件警报相关的调试信息
     * 只有在调试模式下才会输出
     * 
     * @param message 日志消息
     */
    public static void email(String message) {
        if (isDebugMode()) {
            TokenAuthMod.LOGGER.info("[EMAIL] " + message);
        }
    }
}