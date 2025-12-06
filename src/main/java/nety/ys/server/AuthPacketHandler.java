package nety.ys.server;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.network.ServerPlayerEntity;
import nety.ys.TokenAuthMod;
import nety.ys.config.ModConfig;
import nety.ys.network.packets.ChallengePacket;
import nety.ys.network.packets.TokenResponsePacket;
import nety.ys.server.constraint.ConstraintManager;
import nety.ys.util.FailedAuthLogger;
import nety.ys.config.SimpleConfigManager;
import nety.ys.server.AuthAlertService;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * 服务端认证数据包处理器
 * 负责处理客户端发送的令牌响应
 * 
 * @author nety.ys
 */
public class AuthPacketHandler {
    
    /**
     * 处理客户端令牌响应
     * 
     * @param packet 令牌响应数据包
     * @param player 玩家实体
     * @param responseSender 响应发送器
     */
    public static void handleTokenResponse(TokenResponsePacket packet, ServerPlayerEntity player, PacketSender responseSender) {
        try {
            TokenAuthMod.LOGGER.info("收到玩家 {} 的令牌响应", player.getName().getString());
            
            // 获取玩家IP地址
            InetAddress playerAddress = ((InetSocketAddress) player.networkHandler.connection.getAddress()).getAddress();
            
            // 获取服务器配置
            SimpleConfigManager configManager = (SimpleConfigManager) TokenAuthMod.getInstance().getConfigManager();
            ModConfig.ServerConfig config = configManager.getServerConfig();
            
            // 检查认证是否启用
            if (!config.enabled) {
                TokenAuthMod.LOGGER.warn("收到令牌响应，但认证系统已禁用");
                return;
            }
            
            TokenAuthMod.LOGGER.info("开始验证玩家 {} 的令牌响应", player.getName().getString());
            TokenAuthMod.LOGGER.info("客户端发送的令牌: {}", java.util.Base64.getEncoder().encodeToString(packet.getTokenResponse()));
            TokenAuthMod.LOGGER.info("挑战时间戳: {}", packet.getChallengeTimestamp());
            
            // 获取会话信息以便调试
            AuthSessionManager.AuthSession session = AuthSessionHelper.findSessionByPlayer(player);
            if (session != null) {
                TokenAuthMod.LOGGER.info("服务器会话挑战: {}", java.util.Base64.getEncoder().encodeToString(session.getChallenge()));
                TokenAuthMod.LOGGER.info("服务器会话时间戳: {}", session.getTimestamp());
            } else {
                // 打印所有活跃会话用于调试
                AuthSessionHelper.debugPrintAllSessions();
            }
            
            // 验证令牌响应
            boolean isValid = AuthSessionManager.verifyTokenResponse(
                player.getUuid().toString(),
                packet.getTokenResponse(),
                packet.getChallengeTimestamp(),
                playerAddress
            );
            
            if (isValid) {
                // 认证成功
                TokenAuthMod.LOGGER.info("玩家 {} 认证成功", player.getName().getString());
                onAuthenticationSuccess(player);
                
                // 发送认证成功结果给客户端
                new nety.ys.network.packets.AuthResultPacket(true, "认证成功，正在进入游戏...").send(player);
            } else {
                // 认证失败
                TokenAuthMod.LOGGER.warn("玩家 {} 认证失败：令牌验证失败", player.getName().getString());
                onAuthenticationFailure(player, "令牌验证失败");
                
                // 发送认证失败结果给客户端
                new nety.ys.network.packets.AuthResultPacket(false, "认证失败，请检查客户端配置").send(player);
            }
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("处理令牌响应时出错", e);
            onAuthenticationFailure(player, "处理令牌响应时出错: " + e.getMessage());
        }
    }
    
    /**
     * 发送挑战给客户端
     *
     * @param player 目标玩家
     * @return 如果成功发送挑战则返回true
     */
    public static boolean sendChallengeToClient(ServerPlayerEntity player) {
        return sendChallengeToClient(player, null);
    }
    
    /**
     * 发送挑战给客户端
     *
     * @param player 目标玩家
     * @param connectionId 连接ID（可选）
     * @return 如果成功发送挑战则返回true
     */
    public static boolean sendChallengeToClient(ServerPlayerEntity player, String connectionId) {
        try {
            // 获取玩家IP地址
            InetAddress playerAddress = ((InetSocketAddress) player.networkHandler.connection.getAddress()).getAddress();
            
            // 创建认证会话
            String sessionId = connectionId != null ? connectionId : player.getUuid().toString();
            AuthSessionManager.AuthSession session = AuthSessionManager.createSession(
                sessionId,
                playerAddress
            );
            
            if (session == null) {
                TokenAuthMod.LOGGER.error("无法为玩家 {} 创建认证会话", player.getName().getString());
                return false;
            }
            
            // 创建挑战数据包
            ChallengePacket challengePacket = new ChallengePacket(
                session.getChallenge(),
                session.getTimestamp()
            );
            
            // 发送挑战给客户端
            challengePacket.send(player);
            
            TokenAuthMod.LOGGER.info("已向玩家 {} 发送认证挑战", player.getName().getString());
            return true;
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("发送挑战给客户端时出错", e);
            return false;
        }
    }
    
    /**
     * 认证成功处理
     * 
     * @param player 玩家实体
     */
    private static void onAuthenticationSuccess(ServerPlayerEntity player) {
        SimpleConfigManager configManager = (SimpleConfigManager) TokenAuthMod.getInstance().getConfigManager();
        ModConfig.ServerConfig config = configManager.getServerConfig();
        
        // 记录成功日志
        if (config.enableAuthLogging && config.logSuccessfulAuth) {
            TokenAuthMod.LOGGER.info("玩家 {} 认证成功", player.getName().getString());
        }
        
        // 标记玩家为已认证
        AuthSessionManager.markPlayerAsAuthenticated(player.getUuid().toString());
        
        // 移除玩家的约束（如果约束系统可用）
        try {
            // 检查约束系统是否可用
            Class.forName("nety.ys.constraint.api.ConstraintAPI");
            ConstraintManager.removeConstraintsFromPlayer(player);
            TokenAuthMod.LOGGER.info("已为已认证玩家 {} 移除约束", player.getName().getString());
        } catch (ClassNotFoundException e) {
            TokenAuthMod.LOGGER.debug("约束系统不可用，跳过约束移除");
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("为玩家移除约束时出错", e);
        }
        
        // 继续正常的游戏流程
        // 这里可能需要通知服务器继续处理玩家的登录
    }
    
    /**
     * 认证失败处理
     * 
     * @param player 玩家实体
     * @param reason 失败原因
     */
    private static void onAuthenticationFailure(ServerPlayerEntity player, String reason) {
        SimpleConfigManager configManager = (SimpleConfigManager) TokenAuthMod.getInstance().getConfigManager();
        ModConfig.ServerConfig config = configManager.getServerConfig();
        
        // 记录失败日志
        if (config.enableAuthLogging && config.logFailedAttempts) {
            TokenAuthMod.LOGGER.warn("玩家 {} 认证失败: {}", player.getName().getString(), reason);
        }
        
        // 获取玩家IP地址
        InetAddress playerAddress = ((InetSocketAddress) player.networkHandler.connection.getAddress()).getAddress();
        
        // 记录到CSV文件
        
        // 发送认证失败警报邮件
        AuthAlertService.sendAuthFailureAlert(player.getName().getString(), playerAddress, reason);
        FailedAuthLogger.logFailedAuth(player.getName().getString(), playerAddress, reason);
        
        // 增加失败尝试次数
        int attempts = AuthSessionManager.incrementFailedAttempt(playerAddress.toString());
        
        // 检查是否需要阻止IP
        if (attempts >= config.maxAttemptsPerIP) {
            AuthSessionManager.blockIPAddress(playerAddress.toString(), config.blockDurationMinutes);
            TokenAuthMod.LOGGER.warn("IP地址 {} 已被阻止，原因：认证失败次数过多", playerAddress.toString());
        }
        
        // 断开玩家连接
        player.networkHandler.disconnect(net.minecraft.text.Text.literal("认证失败: " + reason));
    }
}