package nety.ys.client;

import nety.ys.TokenAuthMod;
import nety.ys.config.ModConfig;
import nety.ys.crypto.DynamicTokenGenerator;

/**
 * 客户端令牌管理器
 * 负责管理客户端的令牌生成和响应
 * 
 * @author nety.ys
 */
public class ClientTokenManager {
    
    /**
     * 动态令牌生成器
     */
    private static DynamicTokenGenerator tokenGenerator;
    
    /**
     * 是否已初始化
     */
    private static boolean initialized = false;
    
    /**
     * 初始化客户端令牌管理器
     */
    public static void initialize() {
        if (initialized) {
            TokenAuthMod.LOGGER.warn("客户端令牌管理器已经初始化");
            return;
        }
        
        try {
            // 获取客户端配置
            ModConfig.ClientConfig config = TokenAuthMod.getInstance().getConfigManager().getClientConfig();
            
            // 检查共享密钥是否已配置
            if (!config.isSharedSecretConfigured()) {
                TokenAuthMod.LOGGER.warn("客户端共享密钥未配置，令牌管理器无法正常工作");
                return;
            }
            
            // 初始化令牌生成器
            tokenGenerator = new DynamicTokenGenerator(config.getSharedSecretBytes());
            
            initialized = true;
            TokenAuthMod.LOGGER.info("客户端令牌管理器初始化完成");
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("初始化客户端令牌管理器时出错", e);
        }
    }
    
    /**
     * 生成令牌响应
     * 
     * @param challenge 挑战数据
     * @param timestamp 时间戳
     * @return 令牌响应，如果生成失败则返回null
     */
    public static byte[] generateTokenResponse(byte[] challenge, long timestamp) {
        if (!initialized) {
            TokenAuthMod.LOGGER.error("客户端令牌管理器未初始化，无法生成令牌响应");
            return null;
        }
        
        if (tokenGenerator == null) {
            TokenAuthMod.LOGGER.error("令牌生成器未初始化，无法生成令牌响应");
            return null;
        }
        
        try {
            return tokenGenerator.generateToken(challenge, timestamp);
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("生成令牌响应时出错", e);
            return null;
        }
    }
    
    /**
     * 检查是否已初始化
     * 
     * @return 如果已初始化则返回true
     */
    public static boolean isInitialized() {
        return initialized;
    }
    
    /**
     * 重新初始化客户端令牌管理器
     */
    public static void reinitialize() {
        initialized = false;
        tokenGenerator = null;
        initialize();
    }
    
    /**
     * 获取令牌生成器
     * 
     * @return 令牌生成器实例，如果未初始化则返回null
     */
    public static DynamicTokenGenerator getTokenGenerator() {
        return tokenGenerator;
    }
}