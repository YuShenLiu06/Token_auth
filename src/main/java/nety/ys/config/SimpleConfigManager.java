package nety.ys.config;

import nety.ys.TokenAuthMod;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * 简单配置管理器
 * 不依赖外部库，使用Java标准库
 * 
 * @author nety.ys
 */
public class SimpleConfigManager {
    
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
                TokenAuthMod.LOGGER.info("服务器配置加载成功: {}", configPath);
            } else {
                TokenAuthMod.LOGGER.info("服务器配置文件不存在，创建默认配置");
            }
            
            // 从属性文件读取服务器配置
            serverConfig = loadServerConfigFromProperties(props);
            
            // 如果配置文件不存在或为空，创建默认配置并保存
            if (!configPath.toFile().exists() || props.isEmpty()) {
                TokenAuthMod.LOGGER.info("服务器配置文件不存在或为空，创建默认配置");
                serverConfig = new ModConfig.ServerConfig();
                saveServerConfig();
            }
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("加载服务器配置失败，使用默认配置", e);
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
                TokenAuthMod.LOGGER.info("客户端配置加载成功: {}", configPath);
            } else {
                TokenAuthMod.LOGGER.info("客户端配置文件不存在，创建默认配置");
            }
            
            // 从属性文件读取客户端配置
            clientConfig = loadClientConfigFromProperties(props);
            
            // 如果配置文件不存在或为空，创建默认配置并保存
            if (!configPath.toFile().exists() || props.isEmpty()) {
                TokenAuthMod.LOGGER.info("客户端配置文件不存在或为空，创建默认配置");
                clientConfig = new ModConfig.ClientConfig();
                saveClientConfig();
            }
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("加载客户端配置失败，使用默认配置", e);
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
            
            TokenAuthMod.LOGGER.info("服务器配置已保存");
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("保存服务器配置失败", e);
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
            
            TokenAuthMod.LOGGER.info("客户端配置已保存");
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("保存客户端配置失败", e);
        }
    }
    
    /**
     * 重新加载服务器配置
     */
    public void reloadServerConfig() {
        loadServerConfig();
        TokenAuthMod.LOGGER.info("服务器配置已重新加载");
    }
    
    /**
     * 重新加载客户端配置
     */
    public void reloadClientConfig() {
        loadClientConfig();
        TokenAuthMod.LOGGER.info("客户端配置已重新加载");
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
        
        // 日志设置
        props.setProperty("logging.enableAuthLogging", String.valueOf(serverConfig.enableAuthLogging));
        props.setProperty("logging.logSuccessfulAuth", String.valueOf(serverConfig.logSuccessfulAuth));
        props.setProperty("logging.logFailedAttempts", String.valueOf(serverConfig.logFailedAttempts));
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