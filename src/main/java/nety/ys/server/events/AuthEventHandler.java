package nety.ys.server.events;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import nety.ys.TokenAuthMod;
import nety.ys.server.AuthSessionManager;

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
        
        TokenAuthMod.LOGGER.info("玩家 {} 尝试加入服务器", player.getName().getString());
        
        // 检查玩家是否已通过认证
        if (AuthSessionManager.isPlayerAuthenticated(player.getUuid().toString())) {
            TokenAuthMod.LOGGER.info("玩家 {} 已通过认证，允许加入", player.getName().getString());
        } else {
            TokenAuthMod.LOGGER.warn("玩家 {} 未通过认证，但已加入服务器", player.getName().getString());
            // 这里可以添加额外的处理逻辑，例如踢出未认证的玩家
        }
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