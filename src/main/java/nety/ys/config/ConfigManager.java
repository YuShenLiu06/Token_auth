package nety.ys.config;

import com.electronwill.nightconfig.core.file.FileConfig;
import nety.ys.TokenAuthMod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

/**
 * 配置管理器类
 * 负责加载、保存和管理服务器和客户端的配置文件
 * 
 * @author nety.ys
 */
public class ConfigManager {
    
    /**
     * 日志记录器
     */
    private static final Logger LOGGER = LoggerFactory.getLogger("token-auth-config");
    
    /**
     * 服务器配置文件路径
     */
    private static final String SERVER_CONFIG_FILE = "token-auth-server.toml";
    
    /**
     * 客户端配置文件路径
     */
    private static final String CLIENT_CONFIG_FILE = "token-auth-client.toml";
    
    /**
     * 服务器配置实例
     */
    private ModConfig.ServerConfig serverConfig;
    
    /**
     * 客户端配置实例
     */
    private ModConfig.ClientConfig clientConfig;
    
    /**
     * 服务器配置文件对象
     */
    private FileConfig serverConfigFile;
    
    /**
     * 客户端配置文件对象
     */
    private FileConfig clientConfigFile;
    
    /**
     * 获取服务器配置
     * 
     * @return 服务器配置实例
     */
    public ModConfig.ServerConfig getServerConfig() {
        return serverConfig;
    }
    
    /**
     * 获取客户端配置
     * 
     * @return 客户端配置实例
     */
    public ModConfig.ClientConfig getClientConfig() {
        return clientConfig;
    }
    
    /**
     * 加载服务器配置
     */
    public void loadServerConfig() {
        try {
            // 确保配置目录存在
            ModConfig.ensureConfigDirExists();
            
            // 获取配置文件路径
            Path configPath = ModConfig.getConfigDir().resolve(SERVER_CONFIG_FILE);
            
            // 创建配置文件对象
            serverConfigFile = FileConfig.of(configPath);
            
            // 加载配置文件
            serverConfigFile.load();
            
            // 从配置文件读取服务器配置
            serverConfig = loadServerConfigFromFile(serverConfigFile);
            
            LOGGER.info("服务器配置加载成功: {}", configPath);
            
            // 如果配置文件不存在或为空，创建默认配置并保存
            if (!configPath.toFile().exists() || serverConfigFile.isEmpty()) {
                LOGGER.info("服务器配置文件不存在或为空，创建默认配置");
                serverConfig = new ModConfig.ServerConfig();
                saveServerConfig();
            }
        } catch (Exception e) {
            LOGGER.error("加载服务器配置失败，使用默认配置", e);
            serverConfig = new ModConfig.ServerConfig();
        }
    }
    
    /**
     * 加载客户端配置
     */
    public void loadClientConfig() {
        try {
            // 确保配置目录存在
            ModConfig.ensureConfigDirExists();
            
            // 获取配置文件路径
            Path configPath = ModConfig.getConfigDir().resolve(CLIENT_CONFIG_FILE);
            
            // 创建配置文件对象
            clientConfigFile = FileConfig.of(configPath);
            
            // 加载配置文件
            clientConfigFile.load();
            
            // 从配置文件读取客户端配置
            clientConfig = loadClientConfigFromFile(clientConfigFile);
            
            LOGGER.info("客户端配置加载成功: {}", configPath);
            
            // 如果配置文件不存在或为空，创建默认配置并保存
            if (!configPath.toFile().exists() || clientConfigFile.isEmpty()) {
                LOGGER.info("客户端配置文件不存在或为空，创建默认配置");
                clientConfig = new ModConfig.ClientConfig();
                saveClientConfig();
            }
        } catch (Exception e) {
            LOGGER.error("加载客户端配置失败，使用默认配置", e);
            clientConfig = new ModConfig.ClientConfig();
        }
    }
    
    /**
     * 保存服务器配置
     */
    public void saveServerConfig() {
        try {
            if (serverConfigFile == null) {
                Path configPath = ModConfig.getConfigDir().resolve(SERVER_CONFIG_FILE);
                serverConfigFile = FileConfig.of(configPath);
            }
            
            // 将服务器配置写入配置文件
            saveServerConfigToFile(serverConfigFile, serverConfig);
            
            // 保存到文件
            serverConfigFile.save();
            
            LOGGER.info("服务器配置已保存");
        } catch (Exception e) {
            LOGGER.error("保存服务器配置失败", e);
        }
    }
    
    /**
     * 保存客户端配置
     */
    public void saveClientConfig() {
        try {
            if (clientConfigFile == null) {
                Path configPath = ModConfig.getConfigDir().resolve(CLIENT_CONFIG_FILE);
                clientConfigFile = FileConfig.of(configPath);
            }
            
            // 将客户端配置写入配置文件
            saveClientConfigToFile(clientConfigFile, clientConfig);
            
            // 保存到文件
            clientConfigFile.save();
            
            LOGGER.info("客户端配置已保存");
        } catch (Exception e) {
            LOGGER.error("保存客户端配置失败", e);
        }
    }
    
    /**
     * 重新加载服务器配置
     */
    public void reloadServerConfig() {
        if (serverConfigFile != null) {
            serverConfigFile.close();
        }
        loadServerConfig();
        LOGGER.info("服务器配置已重新加载");
    }
    
    /**
     * 重新加载客户端配置
     */
    public void reloadClientConfig() {
        if (clientConfigFile != null) {
            clientConfigFile.close();
        }
        loadClientConfig();
        LOGGER.info("客户端配置已重新加载");
    }
    
    /**
     * 从文件加载服务器配置
     * 
     * @param config 配置文件对象
     * @return 服务器配置实例
     */
    private ModConfig.ServerConfig loadServerConfigFromFile(FileConfig config) {
        ModConfig.ServerConfig serverConfig = new ModConfig.ServerConfig();
        
        // 认证设置
        serverConfig.enabled = config.getOrElse("authentication.enabled", true);
        serverConfig.sharedSecret = config.getOrElse("authentication.sharedSecret", "");
        serverConfig.timeWindow = config.getOrElse("authentication.timeWindow", 30000L);
        serverConfig.challengeSize = config.getOrElse("authentication.challengeSize", 16);
        serverConfig.responseTimeout = config.getOrElse("authentication.responseTimeout", 5000L);
        
        // 安全设置
        serverConfig.maxAttemptsPerIP = config.getOrElse("security.maxAttemptsPerIP", 5);
        serverConfig.blockDurationMinutes = config.getOrElse("security.blockDurationMinutes", 30);
        serverConfig.enableIPWhitelist = config.getOrElse("security.enableIPWhitelist", false);
        serverConfig.ipWhitelist = config.getOrElse("security.ipWhitelist", serverConfig.ipWhitelist);
        
        // 日志设置
        serverConfig.enableAuthLogging = config.getOrElse("logging.enableAuthLogging", true);
        serverConfig.logSuccessfulAuth = config.getOrElse("logging.logSuccessfulAuth", true);
        serverConfig.logFailedAttempts = config.getOrElse("logging.logFailedAttempts", true);
        
        return serverConfig;
    }
    
    /**
     * 从文件加载客户端配置
     * 
     * @param config 配置文件对象
     * @return 客户端配置实例
     */
    private ModConfig.ClientConfig loadClientConfigFromFile(FileConfig config) {
        ModConfig.ClientConfig clientConfig = new ModConfig.ClientConfig();
        
        // 认证设置
        clientConfig.sharedSecret = config.getOrElse("authentication.sharedSecret", "");
        clientConfig.autoRefresh = config.getOrElse("authentication.autoRefresh", false);
        
        // 连接设置
        clientConfig.timeout = config.getOrElse("connection.timeout", 10000L);
        clientConfig.retryAttempts = config.getOrElse("connection.retryAttempts", 3);
        
        return clientConfig;
    }
    
    /**
     * 将服务器配置保存到文件
     * 
     * @param config 配置文件对象
     * @param serverConfig 服务器配置实例
     */
    private void saveServerConfigToFile(FileConfig config, ModConfig.ServerConfig serverConfig) {
        // 认证设置
        config.set("authentication.enabled", serverConfig.enabled);
        config.set("authentication.sharedSecret", serverConfig.sharedSecret);
        config.set("authentication.timeWindow", serverConfig.timeWindow);
        config.set("authentication.challengeSize", serverConfig.challengeSize);
        config.set("authentication.responseTimeout", serverConfig.responseTimeout);
        
        // 安全设置
        config.set("security.maxAttemptsPerIP", serverConfig.maxAttemptsPerIP);
        config.set("security.blockDurationMinutes", serverConfig.blockDurationMinutes);
        config.set("security.enableIPWhitelist", serverConfig.enableIPWhitelist);
        config.set("security.ipWhitelist", serverConfig.ipWhitelist);
        
        // 日志设置
        config.set("logging.enableAuthLogging", serverConfig.enableAuthLogging);
        config.set("logging.logSuccessfulAuth", serverConfig.logSuccessfulAuth);
        config.set("logging.logFailedAttempts", serverConfig.logFailedAttempts);
    }
    
    /**
     * 将客户端配置保存到文件
     * 
     * @param config 配置文件对象
     * @param clientConfig 客户端配置实例
     */
    private void saveClientConfigToFile(FileConfig config, ModConfig.ClientConfig clientConfig) {
        // 认证设置
        config.set("authentication.sharedSecret", clientConfig.sharedSecret);
        config.set("authentication.autoRefresh", clientConfig.autoRefresh);
        
        // 连接设置
        config.set("connection.timeout", clientConfig.timeout);
        config.set("connection.retryAttempts", clientConfig.retryAttempts);
    }
}