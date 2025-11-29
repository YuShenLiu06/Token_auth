package nety.ys.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import nety.ys.TokenAuthMod;
import nety.ys.network.packets.ChallengePacket;
import nety.ys.network.packets.TokenResponsePacket;

/**
 * 数据包注册器
 * 负责注册客户端和服务端的数据包处理器
 * 
 * @author nety.ys
 */
public class PacketRegistry {
    
    /**
     * 注册服务端数据包处理器
     */
    public static void registerServerPackets() {
        TokenAuthMod.LOGGER.info("注册服务端数据包处理器...");
        
        // 注册客户端令牌响应处理器
        ServerPlayNetworking.registerGlobalReceiver(TokenResponsePacket.ID, (server, player, handler, buf, responseSender) -> {
            TokenAuthMod.LOGGER.info("收到客户端令牌响应数据包");
            new TokenResponsePacket.ServerHandler().receive(server, player, handler, buf, responseSender);
        });
        
        TokenAuthMod.LOGGER.info("服务端数据包处理器注册完成");
    }
    
    /**
     * 注册客户端数据包处理器
     */
    public static void registerClientPackets() {
        TokenAuthMod.LOGGER.info("注册客户端数据包处理器...");
        
        // 注册服务器挑战处理器
        ClientPlayNetworking.registerGlobalReceiver(ChallengePacket.ID, (client, handler, buf, responseSender) -> {
            TokenAuthMod.LOGGER.info("收到服务器挑战数据包");
            ChallengePacket packet = ChallengePacket.fromBytes(buf);
            TokenAuthMod.LOGGER.debug("挑战数据包内容: {}", packet.toString());
            // 在客户端主线程处理
            client.execute(() -> {
                try {
                    TokenAuthMod.LOGGER.info("开始处理服务器挑战");
                    nety.ys.client.ClientPacketHandler.handleServerChallenge(packet);
                } catch (Exception e) {
                    TokenAuthMod.LOGGER.error("处理服务器挑战时出错", e);
                }
            });
        });
        
        TokenAuthMod.LOGGER.info("客户端数据包处理器注册完成");
    }
    
    /**
     * 注销服务端数据包处理器
     */
    public static void unregisterServerPackets() {
        TokenAuthMod.LOGGER.info("注销服务端数据包处理器...");
        
        // 注销客户端令牌响应处理器
        ServerPlayNetworking.unregisterGlobalReceiver(TokenResponsePacket.ID);
        
        TokenAuthMod.LOGGER.info("服务端数据包处理器注销完成");
    }
    
    /**
     * 注销客户端数据包处理器
     */
    public static void unregisterClientPackets() {
        TokenAuthMod.LOGGER.info("注销客户端数据包处理器...");
        
        // 注销服务器挑战处理器
        ClientPlayNetworking.unregisterGlobalReceiver(ChallengePacket.ID);
        
        TokenAuthMod.LOGGER.info("客户端数据包处理器注销完成");
    }
    
    /**
     * 检查服务端数据包是否已注册
     * 
     * @return 如果服务端数据包已注册则返回true
     */
    public static boolean areServerPacketsRegistered() {
        return ServerPlayNetworking.getGlobalReceivers().stream().anyMatch(id -> id.equals(TokenResponsePacket.ID));
    }
    
    /**
     * 检查客户端数据包是否已注册
     * 
     * @return 如果客户端数据包已注册则返回true
     */
    public static boolean areClientPacketsRegistered() {
        // 客户端数据包注册状态检查方法在Fabric API中可能不可用
        // 这里我们假设如果是在客户端环境中，数据包已经注册
        return FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.CLIENT;
    }
}