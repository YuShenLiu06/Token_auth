package nety.ys.server.events;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import nety.ys.TokenAuthMod;
import nety.ys.server.AuthSessionManager;
import nety.ys.server.constraint.ConstraintManager;

/**
 * 认证事件处理器
 * 处理服务器端的各种认证相关事件
 * 
 * @author nety.ys
 */
public class AuthEventHandler {
    
    /**
     * 玩家加入服务器事件处理
     * 
     * @param handler 网络处理器
     * @param sender 数据包发送器
     * @param server 服务器实例
     */
    public static void onPlayerJoin(ServerPlayNetworkHandler handler, PacketSender sender, MinecraftServer server) {
        ServerPlayerEntity player = handler.player;
        
        TokenAuthMod.LOGGER.info("玩家 {} (UUID: {}) 尝试加入服务器", player.getName().getString(), player.getUuid().toString());
        
        // 检查玩家是否已通过认证
        if (AuthSessionManager.isPlayerAuthenticated(player.getUuid().toString())) {
            TokenAuthMod.LOGGER.info("玩家 {} 已通过认证，允许加入", player.getName().getString());
        } else {
            TokenAuthMod.LOGGER.info("玩家 {} 未通过认证，发送认证挑战", player.getName().getString());
            
            // 为未认证玩家添加约束（如果约束系统可用）
            try {
                // 检查约束系统是否可用
                Class.forName("nety.ys.constraint.api.ConstraintAPI");
                ConstraintManager.applyConstraintsToPlayer(player);
                TokenAuthMod.LOGGER.info("已为未认证玩家 {} 添加约束", player.getName().getString());
            } catch (ClassNotFoundException e) {
                TokenAuthMod.LOGGER.debug("约束系统不可用，跳过约束添加");
            } catch (Exception e) {
                TokenAuthMod.LOGGER.error("为玩家添加约束时出错", e);
            }
            
            // 发送认证挑战给客户端
            boolean challengeSent = nety.ys.server.AuthPacketHandler.sendChallengeToClient(player);
            if (!challengeSent) {
                TokenAuthMod.LOGGER.error("向玩家 {} 发送认证挑战失败，断开连接", player.getName().getString());
                // 断开连接
                player.networkHandler.disconnect(net.minecraft.text.Text.literal("认证系统错误，请稍后再试"));
            } else {
                TokenAuthMod.LOGGER.info("已向玩家 {} 发送认证挑战，等待响应", player.getName().getString());
                // 给予玩家一定时间完成认证，否则断开连接
                scheduleAuthenticationTimeout(player, server);
            }
        }
    }
    
    /**
     * 安排认证超时检查
     *
     * @param player 玩家实体
     * @param server 服务器实例
     */
    private static void scheduleAuthenticationTimeout(ServerPlayerEntity player, MinecraftServer server) {
        // 获取服务器配置
        nety.ys.config.ModConfig.ServerConfig config = nety.ys.TokenAuthMod.getInstance().getConfigManager().getServerConfig();
        
        TokenAuthMod.LOGGER.info("安排玩家 {} 的认证超时检查，超时时间: {} 毫秒",
            player.getName().getString(), config.responseTimeout);
        
        // 使用调度器而不是阻塞服务器主线程
        java.util.concurrent.ScheduledExecutorService scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
        scheduler.schedule(() -> {
            // 在服务器主线程中执行检查
            server.execute(() -> {
                try {
                    // 检查玩家是否已通过认证
                    if (!AuthSessionManager.isPlayerAuthenticated(player.getUuid().toString()) && player.networkHandler != null) {
                        TokenAuthMod.LOGGER.warn("玩家 {} 认证超时，断开连接", player.getName().getString());
                        player.networkHandler.disconnect(net.minecraft.text.Text.literal("认证超时，请使用支持令牌认证的客户端"));
                    } else {
                        TokenAuthMod.LOGGER.info("玩家 {} 已通过认证或已断开连接，取消超时检查", player.getName().getString());
                    }
                } catch (Exception e) {
                    TokenAuthMod.LOGGER.error("检查认证超时时出错", e);
                }
            });
            
            // 关闭调度器
            scheduler.shutdown();
        }, config.responseTimeout, java.util.concurrent.TimeUnit.MILLISECONDS);
    }
    
    /**
     * 玩家断开连接事件处理
     * 
     * @param handler 网络处理器
     * @param server 服务器实例
     */
    public static void onPlayerDisconnect(ServerPlayNetworkHandler handler, MinecraftServer server) {
        ServerPlayerEntity player = handler.player;
        
        TokenAuthMod.LOGGER.info("玩家 {} 已断开连接", player.getName().getString());
        
        // 清理玩家的认证状态
        AuthSessionManager.removePlayerAuthentication(player.getUuid().toString());
    }
}