package nety.ys.client;

import nety.ys.TokenAuthMod;
import nety.ys.config.ModConfig;
import nety.ys.network.packets.ChallengePacket;
import nety.ys.network.packets.TokenResponsePacket;

/**
 * 客户端认证数据包处理器
 * 负责处理服务器发送的挑战数据并生成响应
 * 
 * @author nety.ys
 */
public class ClientPacketHandler {
    
    /**
     * 处理服务器挑战
     * 
     * @param packet 挑战数据包
     */
    public static void handleServerChallenge(ChallengePacket packet) {
        try {
            TokenAuthMod.LOGGER.info("开始处理服务器挑战");
            
            // 获取客户端配置
            ModConfig.ClientConfig config = TokenAuthMod.getInstance().getConfigManager().getClientConfig();
            
            // 检查共享密钥是否已配置
            if (!config.isSharedSecretConfigured()) {
                TokenAuthMod.LOGGER.error("客户端共享密钥未配置，无法处理服务器挑战");
                return;
            }
            
            // 检查挑战是否过期
            if (packet.isExpired(config.timeout)) {
                TokenAuthMod.LOGGER.warn("收到过期的服务器挑战，时间戳: {}, 当前时间: {}",
                    packet.getTimestamp(), System.currentTimeMillis());
                return;
            }
            
            TokenAuthMod.LOGGER.info("挑战数据有效，开始生成令牌响应");
            TokenAuthMod.LOGGER.info("服务器挑战: {}", java.util.Base64.getEncoder().encodeToString(packet.getChallenge()));
            TokenAuthMod.LOGGER.info("挑战时间戳: {}", packet.getTimestamp());
            
            // 检查客户端令牌管理器状态
            if (!ClientTokenManager.isInitialized()) {
                TokenAuthMod.LOGGER.error("客户端令牌管理器未初始化");
                return;
            }
            
            // 更新认证状态为处理中
            AuthStateManager.setState(AuthStateManager.AuthState.PROCESSING);
            AuthStateManager.setStatusMessage("生成令牌响应...");
            
            // 生成令牌响应
            byte[] response = ClientTokenManager.generateTokenResponse(
                packet.getChallenge(),
                packet.getTimestamp()
            );
            
            if (response == null || response.length == 0) {
                TokenAuthMod.LOGGER.error("生成令牌响应失败");
                return;
            }
            
            TokenAuthMod.LOGGER.info("令牌响应生成成功，长度: {} 字节", response.length);
            TokenAuthMod.LOGGER.info("客户端生成的令牌: {}", java.util.Base64.getEncoder().encodeToString(response));
            TokenAuthMod.LOGGER.info("令牌生成器状态: {}", ClientTokenManager.getTokenGenerator() != null ? "已初始化" : "未初始化");
            
            // 创建令牌响应数据包
            TokenResponsePacket responsePacket = new TokenResponsePacket(
                response,
                packet.getTimestamp()
            );
            
            // 发送响应给服务器
            responsePacket.send();
            
            TokenAuthMod.LOGGER.info("已向服务器发送令牌响应");
            
            // 更新认证状态为等待结果
            AuthStateManager.setState(AuthStateManager.AuthState.WAITING_RESULT);
            AuthStateManager.setStatusMessage("等待服务器验证...");
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("处理服务器挑战时出错", e);
        }
    }
    
    /**
     * 处理认证结果
     * 
     * @param success 是否认证成功
     * @param message 认证结果消息
     */
    public static void handleAuthResult(boolean success, String message) {
        if (success) {
            TokenAuthMod.LOGGER.info("服务器认证成功: {}", message);
        } else {
            TokenAuthMod.LOGGER.warn("服务器认证失败: {}", message);
        }
    }
    
    /**
     * 处理连接错误
     * 
     * @param error 错误消息
     */
    public static void handleConnectionError(String error) {
        TokenAuthMod.LOGGER.error("连接错误: {}", error);
    }
}