package nety.ys.network.packets;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import nety.ys.TokenAuthMod;
import nety.ys.client.AuthStateManager;

/**
 * 认证结果数据包
 * 服务器通知客户端认证结果
 * 
 * @author nety.ys
 */
public class AuthResultPacket {
    
    /**
     * 数据包标识符
     */
    public static final Identifier ID = new Identifier("tokenauth", "auth_result");
    
    /**
     * 认证是否成功
     */
    private final boolean success;
    
    /**
     * 结果消息
     */
    private final String message;
    
    /**
     * 构造函数
     * 
     * @param success 是否成功
     * @param message 结果消息
     */
    public AuthResultPacket(boolean success, String message) {
        this.success = success;
        this.message = message;
    }
    
    /**
     * 从PacketByteBuf读取AuthResultPacket
     * 
     * @param buf 数据缓冲区
     * @return AuthResultPacket实例
     */
    public static AuthResultPacket fromBytes(PacketByteBuf buf) {
        boolean success = buf.readBoolean();
        String message = buf.readString();
        return new AuthResultPacket(success, message);
    }
    
    /**
     * 将AuthResultPacket写入PacketByteBuf
     * 
     * @param packet 数据包
     * @return PacketByteBuf实例
     */
    public static PacketByteBuf toBytes(AuthResultPacket packet) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeBoolean(packet.success);
        buf.writeString(packet.message);
        return buf;
    }
    
    /**
     * 创建Minecraft数据包
     * 
     * @return Minecraft数据包
     */
    public net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket toPacket() {
        return new net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket(ID, toBytes(this));
    }
    
    /**
     * 发送给指定玩家
     * 
     * @param player 目标玩家
     */
    public void send(ServerPlayerEntity player) {
        if (player.networkHandler != null) {
            ServerPlayNetworking.send(player, ID, toBytes(this));
            TokenAuthMod.LOGGER.debug("认证结果数据包已发送给玩家 {}", player.getName().getString());
        } else {
            TokenAuthMod.LOGGER.error("玩家网络处理器为空，无法发送认证结果数据包");
        }
    }
    
    /**
     * 获取认证是否成功
     * 
     * @return 如果认证成功则返回true
     */
    public boolean isSuccess() {
        return success;
    }
    
    /**
     * 获取结果消息
     * 
     * @return 结果消息
     */
    public String getMessage() {
        return message;
    }
    
    /**
     * 客户端数据包处理器
     */
    public static class ClientHandler {
        /**
         * 处理服务器认证结果
         *
         * @param packet 认证结果数据包
         */
        public static void handle(AuthResultPacket packet) {
            // 在客户端主线程处理
            MinecraftClient.getInstance().execute(() -> {
                try {
                    TokenAuthMod.LOGGER.info("收到服务器认证结果: 成功={}, 消息={}", 
                        packet.isSuccess(), packet.getMessage());
                    
                    // 更新认证状态
                    AuthStateManager.handleAuthResult(packet.isSuccess(), packet.getMessage());
                } catch (Exception e) {
                    TokenAuthMod.LOGGER.error("处理认证结果时出错", e);
                    AuthStateManager.setState(AuthStateManager.AuthState.ERROR);
                    AuthStateManager.setStatusMessage("处理认证结果时出错: " + e.getMessage());
                }
            });
        }
    }
    
    /**
     * 服务端数据包处理器（不使用，因为这是服务器发送给客户端的）
     */
    public static class ServerHandler {
        /**
         * 处理客户端响应（不使用）
         *
         * @param packet 认证结果数据包
         * @param player 玩家实体
         * @param responseSender 响应发送器
         */
        public static void handle(AuthResultPacket packet, ServerPlayerEntity player, PacketSender responseSender) {
            // 这个方法实际上不会被调用，因为AuthResultPacket是服务器发送给客户端的
            // 实际处理逻辑在ClientHandler中
        }
    }
}