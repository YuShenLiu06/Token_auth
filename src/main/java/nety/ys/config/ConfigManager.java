package nety.ys.config;

import nety.ys.TokenAuthMod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

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
     * 服务器配置实例
     */
    private ModConfig.ServerConfig serverConfig;
    
    /**
     * 客户端配置实例
     */
    private ModConfig.ClientConfig clientConfig;
    
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
            Path configPath = ModConfig.getConfigDir().resolve("token-auth-server.properties");
            
            // 创建配置文件对象
            Properties props = new Properties();
            
            // 如果配置文件存在，则加载
            if (configPath.toFile().exists()) {
                try (InputStream input = Files.newInputStream(configPath)) {
                    props.load(input);
                }
                LOGGER.info("服务器配置加载成功: {}", configPath);
            } else {
                LOGGER.info("服务器配置文件不存在，创建默认配置");
            }
            
            // 从属性文件读取服务器配置
            serverConfig = loadServerConfigFromProperties(props);
            
            // 如果配置文件不存在或为空，创建默认配置并保存
            if (!configPath.toFile().exists() || props.isEmpty()) {
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
            Path configPath = ModConfig.getConfigDir().resolve("token-auth-client.properties");
            
            // 创建配置文件对象
            Properties props = new Properties();
            
            // 如果配置文件存在，则加载
            if (configPath.toFile().exists()) {
                try (InputStream input = Files.newInputStream(configPath)) {
                    props.load(input);
                }
                LOGGER.info("客户端配置加载成功: {}", configPath);
            } else {
                LOGGER.info("客户端配置文件不存在，创建默认配置");
            }
            
            // 从属性文件读取客户端配置
            clientConfig = loadClientConfigFromProperties(props);
            
            // 如果配置文件不存在或为空，创建默认配置并保存
            if (!configPath.toFile().exists() || props.isEmpty()) {
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
            // 获取配置文件路径
            Path configPath = ModConfig.getConfigDir().resolve("token-auth-server.properties");
            
            // 创建配置文件对象
            Properties props = new Properties();
            
            // 将服务器配置写入属性文件
            saveServerConfigToProperties(props, serverConfig);
            
            // 保存到文件
            try (OutputStream output = Files.newOutputStream(configPath)) {
                props.store(output, "Token Auth Mod Server Configuration");
            }
            
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
            // 获取配置文件路径
            Path configPath = ModConfig.getConfigDir().resolve("token-auth-client.properties");
            
            // 创建配置文件对象
            Properties props = new Properties();
            
            // 将客户端配置写入属性文件
            saveClientConfigToProperties(props, clientConfig);
            
            // 保存到文件
            try (OutputStream output = Files.newOutputStream(configPath)) {
                props.store(output, "Token Auth Mod Client Configuration");
            }
            
            LOGGER.info("客户端配置已保存");
        } catch (Exception e) {
            LOGGER.error("保存客户端配置失败", e);
        }
    }
    
    /**
     * 重新加载服务器配置
     */
    public void reloadServerConfig() {
        loadServerConfig();
        LOGGER.info("服务器配置已重新加载");
    }
    
    /**
     * 重新加载客户端配置
     */
    public void reloadClientConfig() {
        loadClientConfig();
        LOGGER.info("客户端配置已重新加载");
    }
    
    /**
     * 从属性文件加载服务器配置
     * 
     * @param props 属性文件对象
     * @return 服务器配置实例
     */
    private ModConfig.ServerConfig loadServerConfigFromProperties(Properties props) {
        ModConfig.ServerConfig serverConfig = new ModConfig.ServerConfig();
        
        // 认证设置
        serverConfig.enabled = Boolean.parseBoolean(props.getProperty("authentication.enabled", "true"));
        serverConfig.sharedSecret = props.getProperty("authentication.sharedSecret", "");
        serverConfig.timeWindow = Long.parseLong(props.getProperty("authentication.timeWindow", "30000"));
        serverConfig.challengeSize = Integer.parseInt(props.getProperty("authentication.challengeSize", "16"));
        serverConfig.responseTimeout = Long.parseLong(props.getProperty("authentication.responseTimeout", "5000"));
        
        // 安全设置
        serverConfig.maxAttemptsPerIP = Integer.parseInt(props.getProperty("security.maxAttemptsPerIP", "5"));
        serverConfig.blockDurationMinutes = Integer.parseInt(props.getProperty("security.blockDurationMinutes", "30"));
        serverConfig.enableIPWhitelist = Boolean.parseBoolean(props.getProperty("security.enableIPWhitelist", "false"));
        // IP白名单暂时保持默认值
        
        // 日志设置
        serverConfig.enableAuthLogging = Boolean.parseBoolean(props.getProperty("logging.enableAuthLogging", "true"));
        serverConfig.logSuccessfulAuth = Boolean.parseBoolean(props.getProperty("logging.logSuccessfulAuth", "true"));
        serverConfig.logFailedAttempts = Boolean.parseBoolean(props.getProperty("logging.logFailedAttempts", "true"));
        
        // CSV记录设置
        serverConfig.enableCSVLogging = Boolean.parseBoolean(props.getProperty("enableCSVLogging", "false"));
        serverConfig.csvFileName = props.getProperty("csvFileName", "failed_auth_attempts.csv");
        serverConfig.logTimeoutAttempts = Boolean.parseBoolean(props.getProperty("logTimeoutAttempts", "true"));
        serverConfig.includeGeoLocation = Boolean.parseBoolean(props.getProperty("includeGeoLocation", "true"));
        
        // 邮件警报设置
        serverConfig.enableEmailAlerts = Boolean.parseBoolean(props.getProperty("email.enableEmailAlerts", "false"));
        serverConfig.serverName = props.getProperty("email.serverName", "Minecraft服务器");
        serverConfig.smtpHost = props.getProperty("email.smtpHost", "");
        serverConfig.smtpPort = props.getProperty("email.smtpPort", "587");
        serverConfig.enableSSL = Boolean.parseBoolean(props.getProperty("email.enableSSL", "false"));
        serverConfig.smtpUsername = props.getProperty("email.smtpUsername", "");
        serverConfig.smtpPassword = props.getProperty("email.smtpPassword", "");
        serverConfig.emailFromAddress = props.getProperty("email.fromAddress", "");
        serverConfig.emailToAddress = props.getProperty("email.toAddress", "");
        
        return serverConfig;
    }
    
    /**
     * 从属性文件加载客户端配置
     * 
     * @param props 属性文件对象
     * @return 客户端配置实例
     */
    private ModConfig.ClientConfig loadClientConfigFromProperties(Properties props) {
        ModConfig.ClientConfig clientConfig = new ModConfig.ClientConfig();
        
        // 认证设置
        clientConfig.sharedSecret = props.getProperty("authentication.sharedSecret", "");
        clientConfig.autoRefresh = Boolean.parseBoolean(props.getProperty("authentication.autoRefresh", "false"));
        
        // 连接设置
        clientConfig.timeout = Long.parseLong(props.getProperty("connection.timeout", "10000"));
        clientConfig.retryAttempts = Integer.parseInt(props.getProperty("connection.retryAttempts", "3"));
        
        return clientConfig;
    }
    
    /**
     * 将服务器配置保存到属性文件
     * 
     * @param props 属性文件对象
     * @param serverConfig 服务器配置实例
     */
    private void saveServerConfigToProperties(Properties props, ModConfig.ServerConfig serverConfig) {
        // 认证设置
        props.setProperty("authentication.enabled", String.valueOf(serverConfig.enabled));
        props.setProperty("authentication.sharedSecret", serverConfig.sharedSecret);
        props.setProperty("authentication.timeWindow", String.valueOf(serverConfig.timeWindow));
        props.setProperty("authentication.challengeSize", String.valueOf(serverConfig.challengeSize));
        props.setProperty("authentication.responseTimeout", String.valueOf(serverConfig.responseTimeout));
        
        // 安全设置
        props.setProperty("security.maxAttemptsPerIP", String.valueOf(serverConfig.maxAttemptsPerIP));
        props.setProperty("security.blockDurationMinutes", String.valueOf(serverConfig.blockDurationMinutes));
        props.setProperty("security.enableIPWhitelist", String.valueOf(serverConfig.enableIPWhitelist));
        // IP白名单暂时不保存
        
        
        // 邮件警报设置
        props.setProperty("email.enableEmailAlerts", String.valueOf(serverConfig.enableEmailAlerts));
        props.setProperty("email.serverName", serverConfig.serverName);
        props.setProperty("email.smtpHost", serverConfig.smtpHost);
        props.setProperty("email.smtpPort", serverConfig.smtpPort);
        props.setProperty("email.enableSSL", String.valueOf(serverConfig.enableSSL));
        props.setProperty("email.smtpUsername", serverConfig.smtpUsername);
        props.setProperty("email.smtpPassword", serverConfig.smtpPassword);
        props.setProperty("email.fromAddress", serverConfig.emailFromAddress);
        props.setProperty("email.toAddress", serverConfig.emailToAddress);
        // 日志设置
        props.setProperty("logging.enableAuthLogging", String.valueOf(serverConfig.enableAuthLogging));
        props.setProperty("logging.logSuccessfulAuth", String.valueOf(serverConfig.logSuccessfulAuth));
        props.setProperty("logging.logFailedAttempts", String.valueOf(serverConfig.logFailedAttempts));
        
        // CSV记录设置
        props.setProperty("enableCSVLogging", String.valueOf(serverConfig.enableCSVLogging));
        props.setProperty("csvFileName", serverConfig.csvFileName);
        props.setProperty("logTimeoutAttempts", String.valueOf(serverConfig.logTimeoutAttempts));
        props.setProperty("includeGeoLocation", String.valueOf(serverConfig.includeGeoLocation));
    }
    
    /**
     * 将客户端配置保存到属性文件
     * 
     * @param props 属性文件对象
     * @param clientConfig 客户端配置实例
     */
    private void saveClientConfigToProperties(Properties props, ModConfig.ClientConfig clientConfig) {
        // 认证设置
        props.setProperty("authentication.sharedSecret", clientConfig.sharedSecret);
        props.setProperty("authentication.autoRefresh", String.valueOf(clientConfig.autoRefresh));
        
        // 连接设置
        props.setProperty("connection.timeout", String.valueOf(clientConfig.timeout));
        props.setProperty("connection.retryAttempts", String.valueOf(clientConfig.retryAttempts));
    }
}