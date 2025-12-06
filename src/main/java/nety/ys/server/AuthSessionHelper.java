package nety.ys.server;

import net.minecraft.server.network.ServerPlayerEntity;
import nety.ys.TokenAuthMod;
import nety.ys.crypto.DynamicTokenGenerator;
import nety.ys.util.DebugLogger;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;

/**
 * 认证会话辅助类
 * 提供会话查找和验证的辅助方法
 * 
 * @author nety.ys
 */
public class AuthSessionHelper {
    
    /**
     * 根据玩家查找对应的认证会话
     * 
     * @param player 玩家实体
     * @return 对应的认证会话，如果找不到则返回null
     */
    public static AuthSessionManager.AuthSession findSessionByPlayer(ServerPlayerEntity player) {
        try {
            InetAddress playerAddress = ((InetSocketAddress) player.networkHandler.connection.getAddress()).getAddress();
            
            // 获取所有活跃会话
            Collection<AuthSessionManager.AuthSession> sessions = getAllActiveSessions();
            
            DebugLogger.debug("查找玩家会话 - 玩家: {}, IP: {}, 活跃会话数: {}",
                player.getName().getString(), playerAddress.toString(), sessions.size());
            
            // 查找匹配IP地址的会话
            for (AuthSessionManager.AuthSession session : sessions) {
                DebugLogger.debug("检查会话 - IP: {}, 时间戳: {}",
                    session.getAddress().toString(), session.getTimestamp());
                
                if (session.getAddress().equals(playerAddress)) {
                    DebugLogger.debug("找到匹配的会话 - 玩家: {}, IP: {}, 会话时间戳: {}",
                        player.getName().getString(), playerAddress.toString(), session.getTimestamp());
                    return session;
                }
            }
            
            TokenAuthMod.LOGGER.warn("未找到匹配的会话 - 玩家: {}, IP: {}", 
                player.getName().getString(), playerAddress.toString());
            return null;
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("查找玩家会话时出错", e);
            return null;
        }
    }
    
    /**
     * 验证共享密钥配置
     * 
     * @return 如果密钥配置有效则返回true
     */
    public static boolean validateSharedSecret() {
        try {
            nety.ys.config.ModConfig.ServerConfig config = nety.ys.TokenAuthMod.getInstance().getConfigManager().getServerConfig();
            
            if (!config.isSharedSecretConfigured()) {
                TokenAuthMod.LOGGER.error("服务器共享密钥未配置或无效");
                return false;
            }
            
            byte[] secretBytes = config.getSharedSecretBytes();
            DebugLogger.debug("服务器共享密钥已配置，长度: {} 字节", secretBytes.length);
            
            // 尝试创建令牌生成器以验证密钥有效性
            new DynamicTokenGenerator(secretBytes);
            
            return true;
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("验证共享密钥时出错", e);
            return false;
        }
    }
    
    /**
     * 获取所有活跃会话（用于调试）
     * 
     * @return 所有活跃会话的集合
     */
    public static Collection<AuthSessionManager.AuthSession> getAllActiveSessions() {
        try {
            // 使用反射获取私有字段
            java.lang.reflect.Field field = AuthSessionManager.class.getDeclaredField("activeSessions");
            field.setAccessible(true);
            @SuppressWarnings("unchecked")
            Map<String, AuthSessionManager.AuthSession> sessions = 
                (Map<String, AuthSessionManager.AuthSession>) field.get(null);
            return sessions.values();
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("获取活跃会话时出错", e);
            return java.util.Collections.emptyList();
        }
    }
    
    /**
     * 打印所有活跃会话信息（用于调试）
     */
    public static void debugPrintAllSessions() {
        try {
            Collection<AuthSessionManager.AuthSession> sessions = getAllActiveSessions();
            DebugLogger.debug("=== 当前活跃会话 (总数: {}) ===", sessions.size());
            
            for (AuthSessionManager.AuthSession session : sessions) {
                DebugLogger.debug("会话 - ID: {}, IP: {}, 时间戳: {}, 挑战长度: {}",
                    session.getConnectionId(),
                    session.getAddress().toString(),
                    session.getTimestamp(),
                    session.getChallenge().length);
            }
            
            DebugLogger.debug("=== 会话信息打印完成 ===");
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("打印会话信息时出错", e);
        }
    }
}