package nety.ys.config;

// 暂时移除这些依赖，因为它们无法正确解析
// import com.electronwill.nightconfig.core.Config;
// import com.electronwill.nightconfig.core.file.FileConfig;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

/**
 * 模组配置类
 * 定义服务器和客户端的配置数据结构
 * 
 * @author nety.ys
 */
public class ModConfig {
    
    /**
     * 服务器配置类
     */
    public static class ServerConfig {
        // 认证设置
        public boolean enabled = true;
        public String sharedSecret = "";
        public long timeWindow = 30000; // 30秒
        public int challengeSize = 16;
        public long responseTimeout = 5000; // 5秒
        
        // 安全设置
        public int maxAttemptsPerIP = 5;
        public int blockDurationMinutes = 30;
        public boolean enableIPWhitelist = false;
        public List<String> ipWhitelist = List.of("127.0.0.1", "::1");
        
        // 日志设置
        public boolean enableAuthLogging = true;
        public boolean logSuccessfulAuth = true;
        public boolean logFailedAttempts = true;
        
        /**
         * 获取共享密钥的字节数组形式
         * 
         * @return 共享密钥字节数组
         */
        public byte[] getSharedSecretBytes() {
            if (sharedSecret.isEmpty()) {
                return new byte[0];
            }
            try {
                return Base64.getDecoder().decode(sharedSecret);
            } catch (IllegalArgumentException e) {
                return new byte[0];
            }
        }
        
        /**
         * 检查共享密钥是否已配置
         * 
         * @return 如果共享密钥已配置且有效则返回true
         */
        public boolean isSharedSecretConfigured() {
            byte[] secret = getSharedSecretBytes();
            return secret.length > 0;
        }
    }
    
    /**
     * 客户端配置类
     */
    public static class ClientConfig {
        // 认证设置
        public String sharedSecret = "";
        public boolean autoRefresh = false;
        
        // 连接设置
        public long timeout = 10000; // 10秒
        public int retryAttempts = 3;
        
        /**
         * 获取共享密钥的字节数组形式
         * 
         * @return 共享密钥字节数组
         */
        public byte[] getSharedSecretBytes() {
            if (sharedSecret.isEmpty()) {
                return new byte[0];
            }
            try {
                return Base64.getDecoder().decode(sharedSecret);
            } catch (IllegalArgumentException e) {
                return new byte[0];
            }
        }
        
        /**
         * 检查共享密钥是否已配置
         * 
         * @return 如果共享密钥已配置且有效则返回true
         */
        public boolean isSharedSecretConfigured() {
            byte[] secret = getSharedSecretBytes();
            return secret.length > 0;
        }
    }
    
    /**
     * 服务器配置默认值
     */
    public static final ServerConfig SERVER_DEFAULTS = new ServerConfig();
    
    /**
     * 客户端配置默认值
     */
    public static final ClientConfig CLIENT_DEFAULTS = new ClientConfig();
    
    /**
     * 获取配置目录路径
     * 
     * @return 配置目录路径
     */
    public static Path getConfigDir() {
        return FabricLoader.getInstance().getConfigDir().resolve("token-auth");
    }
    
    /**
     * 确保配置目录存在
     */
    public static void ensureConfigDirExists() {
        Path configDir = getConfigDir();
        File dir = configDir.toFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
}