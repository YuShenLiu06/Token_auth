package nety.ys.util;

import nety.ys.TokenAuthMod;
import nety.ys.config.ModConfig;
import nety.ys.config.SimpleConfigManager;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 认证失败CSV记录器
 * 用于将未通过认证的玩家数据记录到CSV文件中
 * 
 * @author nety.ys
 */
public class FailedAuthLogger {
    
    private static final ReentrantLock fileLock = new ReentrantLock();
    private static final String CSV_HEADER = "玩家名称,登录时间,IP地址,地理位置";
    
    /**
     * 记录认证失败信息到CSV文件
     * 
     * @param playerName 玩家名称
     * @param ipAddress IP地址
     * @param reason 失败原因
     */
    public static void logFailedAuth(String playerName, InetAddress ipAddress, String reason) {
        DebugLogger.csv("收到认证失败记录请求: 玩家={}, IP={}, 原因={}", playerName, ipAddress.getHostAddress(), reason);
        
        SimpleConfigManager configManager = (SimpleConfigManager) TokenAuthMod.getInstance().getConfigManager();
        ModConfig.ServerConfig config = configManager.getServerConfig();
        
        // 检查是否启用了CSV记录
        if (!config.enableCSVLogging) {
            DebugLogger.csv("CSV记录功能已禁用，跳过记录");
            return;
        }
        
        DebugLogger.csv("CSV记录功能已启用，开始记录认证失败信息");
        
        try {
            fileLock.lock();
            
            // 获取CSV文件路径
            Path csvPath = getCSVFilePath(config.csvFileName);
            DebugLogger.csv("CSV文件路径: {}", csvPath.toAbsolutePath());
            
            // 检查文件是否存在，如果不存在则创建并写入标题行
            boolean fileExists = Files.exists(csvPath);
            DebugLogger.csv("CSV文件是否存在: {}", fileExists);
            
            if (!fileExists) {
                DebugLogger.csv("CSV文件不存在，创建新文件");
                // 确保目录存在
                Files.createDirectories(csvPath.getParent());
                DebugLogger.csv("确保目录存在: {}", csvPath.getParent().toAbsolutePath());
                
                // 创建文件并写入标题行
                try (BufferedWriter writer = Files.newBufferedWriter(csvPath)) {
                    writer.write(CSV_HEADER);
                    writer.newLine();
                    DebugLogger.csv("已写入CSV文件标题行");
                }
            }
            
            // 获取当前时间
            String loginTime = IPGeolocationUtil.getCurrentChinaTime();
            DebugLogger.csv("当前登录时间: {}", loginTime);
            
            // 获取地理位置信息
            String geoLocation = "未知位置";
            if (config.includeGeoLocation) {
                DebugLogger.csv("正在获取IP {} 的地理位置信息...", ipAddress.getHostAddress());
                IPGeolocationUtil.GeoLocationInfo geoInfo = IPGeolocationUtil.getGeoLocation(ipAddress);
                geoLocation = geoInfo.getFullLocation();
                DebugLogger.csv("获取地理位置信息成功: {}", geoLocation);
            } else {
                DebugLogger.csv("已禁用地理位置信息获取");
            }
            
            // 构建CSV行
            String csvLine = String.format("%s,%s,%s,%s",
                escapeCSVField(playerName),
                escapeCSVField(loginTime),
                escapeCSVField(ipAddress.getHostAddress()),
                escapeCSVField(geoLocation)
            );
            
            DebugLogger.csv("准备写入CSV行: {}", csvLine);
            
            // 写入CSV文件
            try (BufferedWriter writer = Files.newBufferedWriter(csvPath,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                writer.write(csvLine);
                writer.newLine();
                DebugLogger.csv("CSV行写入成功");
            }
            
            DebugLogger.csv("已将玩家 {} 的认证失败信息记录到CSV文件", playerName);
            
        } catch (IOException e) {
            TokenAuthMod.LOGGER.error("写入认证失败CSV记录时出错", e);
        } finally {
            fileLock.unlock();
        }
    }
    
    /**
     * 获取CSV文件的完整路径
     * 
     * @param fileName 文件名
     * @return 文件路径
     */
    private static Path getCSVFilePath(String fileName) {
        // 清理文件名，移除可能的引号
        if (fileName != null) {
            fileName = fileName.trim();
            // 移除文件名两端的引号
            if (fileName.startsWith("\"") && fileName.endsWith("\"")) {
                fileName = fileName.substring(1, fileName.length() - 1);
            }
        }
        
        // 确保文件名以.csv结尾
        if (fileName == null || fileName.isEmpty()) {
            fileName = "failed_auth_attempts.csv";
        } else if (!fileName.toLowerCase().endsWith(".csv")) {
            fileName += ".csv";
        }
        
        // 获取配置目录
        Path configDir = ModConfig.getConfigDir();
        return configDir.resolve(fileName);
    }
    
    /**
     * 转义CSV字段，处理包含逗号、引号或换行符的情况
     * 
     * @param field 字段值
     * @return 转义后的字段值
     */
    private static String escapeCSVField(String field) {
        if (field == null) {
            return "";
        }
        
        // 如果字段包含逗号、引号或换行符，则需要用引号包围，并将内部的引号转义
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        
        return field;
    }
    
    /**
     * 检查CSV文件是否存在
     * 
     * @return 如果文件存在则返回true
     */
    public static boolean csvFileExists() {
        SimpleConfigManager configManager = (SimpleConfigManager) TokenAuthMod.getInstance().getConfigManager();
        ModConfig.ServerConfig config = configManager.getServerConfig();
        Path csvPath = getCSVFilePath(config.csvFileName);
        return Files.exists(csvPath);
    }
    
    /**
     * 获取CSV文件路径字符串
     *
     * @return CSV文件路径
     */
    public static String getCSVFilePathString() {
        SimpleConfigManager configManager = (SimpleConfigManager) TokenAuthMod.getInstance().getConfigManager();
        ModConfig.ServerConfig config = configManager.getServerConfig();
        return getCSVFilePath(config.csvFileName).toString();
    }
}