package nety.ys.util;

import nety.ys.TokenAuthMod;
import nety.ys.config.SimpleConfigManager;

/**
 * 调试日志测试类
 * 用于验证调试模式开关是否正常工作
 * 
 * @author nety.ys
 */
public class DebugLoggerTest {
    
    /**
     * 测试调试日志功能
     */
    public static void testDebugLogging() {
        TokenAuthMod.LOGGER.info("=== 开始测试调试日志功能 ===");
        
        // 测试不同类型的调试日志
        DebugLogger.debug("这是一条调试信息测试");
        DebugLogger.debug("这是带参数的调试信息测试: 参数1={}, 参数2={}", "值1", "值2");
        
        DebugLogger.verbose("这是一条详细信息测试");
        DebugLogger.verbose("这是带参数的详细信息测试: 参数={}", "测试值");
        
        DebugLogger.auth("这是一条认证过程信息测试");
        DebugLogger.auth("这是带参数的认证信息测试: 玩家={}", "测试玩家");
        
        DebugLogger.csv("这是一条CSV记录信息测试");
        DebugLogger.csv("这是带参数的CSV信息测试: 文件={}", "test.csv");
        
        DebugLogger.email("这是一条邮件信息测试");
        DebugLogger.email("这是带参数的邮件信息测试: 收件人={}", "test@example.com");
        
        TokenAuthMod.LOGGER.info("=== 调试日志功能测试完成 ===");
        TokenAuthMod.LOGGER.info("请检查配置文件中的 logging.debugMode 设置");
        TokenAuthMod.LOGGER.info("如果设置为 true，则应该看到 [DEBUG]、[VERBOSE]、[AUTH]、[CSV]、[EMAIL] 前缀的日志");
        TokenAuthMod.LOGGER.info("如果设置为 false，则这些日志不应该显示");
    }
    
    /**
     * 检查当前调试模式状态
     */
    public static void checkDebugModeStatus() {
        try {
            SimpleConfigManager configManager = (SimpleConfigManager) TokenAuthMod.getInstance().getConfigManager();
            if (configManager != null) {
                boolean debugMode = configManager.getServerConfig().debugMode;
                TokenAuthMod.LOGGER.info("当前调试模式状态: {}", debugMode ? "启用" : "禁用");
            } else {
                TokenAuthMod.LOGGER.warn("无法获取配置管理器");
            }
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("检查调试模式状态时出错", e);
        }
    }
}