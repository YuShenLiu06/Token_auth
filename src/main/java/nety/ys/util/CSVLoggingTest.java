package nety.ys.util;

import nety.ys.TokenAuthMod;
import nety.ys.config.ModConfig;
import nety.ys.config.SimpleConfigManager;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * CSV记录功能测试类
 * 用于验证CSV记录功能是否正常工作
 * 
 * @author nety.ys
 */
public class CSVLoggingTest {
    
    /**
     * 测试CSV记录功能
     * 
     * @return 如果测试通过则返回true
     */
    public static boolean testCSVLogging() {
        try {
            TokenAuthMod.LOGGER.info("开始测试CSV记录功能...");
            
            // 创建测试数据
            String testPlayerName = "TestPlayer";
            InetAddress testIP = InetAddress.getByName("8.8.8.8"); // 使用Google DNS作为测试IP
            String testReason = "测试认证失败";
            
            // 获取配置
            SimpleConfigManager configManager = (SimpleConfigManager) TokenAuthMod.getInstance().getConfigManager();
            ModConfig.ServerConfig config = configManager.getServerConfig();
            
            // 临时启用CSV记录
            boolean originalCSVLogging = config.enableCSVLogging;
            config.enableCSVLogging = true;
            
            try {
                // 记录测试数据
                FailedAuthLogger.logFailedAuth(testPlayerName, testIP, testReason);
                
                // 检查CSV文件是否存在
                if (!FailedAuthLogger.csvFileExists()) {
                    TokenAuthMod.LOGGER.error("CSV文件未创建");
                    return false;
                }
                
                // 读取CSV文件内容
                Path csvPath = Paths.get(FailedAuthLogger.getCSVFilePathString());
                String content = Files.readString(csvPath);
                
                // 检查内容是否包含测试数据
                if (!content.contains(testPlayerName) || !content.contains(testIP.getHostAddress())) {
                    TokenAuthMod.LOGGER.error("CSV文件内容不包含测试数据");
                    return false;
                }
                
                TokenAuthMod.LOGGER.info("CSV记录功能测试通过");
                return true;
                
            } finally {
                // 恢复原始配置
                config.enableCSVLogging = originalCSVLogging;
            }
            
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("CSV记录功能测试失败", e);
            return false;
        }
    }
    
    /**
     * 测试IP地理位置解析功能
     * 
     * @return 如果测试通过则返回true
     */
    public static boolean testIPGeolocation() {
        try {
            TokenAuthMod.LOGGER.info("开始测试IP地理位置解析功能...");
            
            // 使用测试IP
            InetAddress testIP = InetAddress.getByName("8.8.8.8");
            
            // 获取地理位置信息
            IPGeolocationUtil.GeoLocationInfo geoInfo = IPGeolocationUtil.getGeoLocation(testIP);
            
            // 检查状态
            if (!"success".equals(geoInfo.getStatus())) {
                TokenAuthMod.LOGGER.warn("IP地理位置解析失败: {}", geoInfo.getMessage());
                return false;
            }
            
            // 检查是否有位置信息
            String fullLocation = geoInfo.getFullLocation();
            if (fullLocation == null || fullLocation.isEmpty() || "未知位置".equals(fullLocation)) {
                TokenAuthMod.LOGGER.warn("未获取到有效的地理位置信息");
                return false;
            }
            
            TokenAuthMod.LOGGER.info("IP地理位置解析功能测试通过，位置: {}", fullLocation);
            return true;
            
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("IP地理位置解析功能测试失败", e);
            return false;
        }
    }
    
    /**
     * 运行所有测试
     * 
     * @return 如果所有测试都通过则返回true
     */
    public static boolean runAllTests() {
        boolean csvTestPassed = testCSVLogging();
        boolean geoTestPassed = testIPGeolocation();
        
        TokenAuthMod.LOGGER.info("=== CSV记录功能测试结果 ===");
        TokenAuthMod.LOGGER.info("CSV记录测试: {}", csvTestPassed ? "§a通过" : "§c失败");
        TokenAuthMod.LOGGER.info("IP地理位置解析测试: {}", geoTestPassed ? "§a通过" : "§c失败");
        
        return csvTestPassed && geoTestPassed;
    }
}