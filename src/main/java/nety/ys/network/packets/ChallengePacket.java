package nety.ys.network.packets;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ClientPlayPacketListener;
// import net.minecraft.network.packet.CustomPayload; // 1.19.2版本中没有这个类
import net.minecraft.network.packet.s2c.play.CustomPayloadS2CPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import nety.ys.TokenAuthMod;

import java.util.Arrays;

/**
 * 挑战数据包
 * 服务器向客户端发送随机挑战数据，用于令牌认证
 * 
 * @author nety.ys
 */
public class ChallengePacket {
    
    /**
     * 数据包标识符
     */
    public static final Identifier ID = new Identifier("tokenauth", "challenge");
    
    /**
     * 挑战数据
     */
    private final byte[] challenge;
    
    /**
     * 时间戳
     */
    private final long timestamp;
    
    /**
     * 构造函数
     * 
     * @param challenge 挑战数据
     * @param timestamp 时间戳
     */
    public ChallengePacket(byte[] challenge, long timestamp) {
        this.challenge = challenge;
        this.timestamp = timestamp;
    }
    
    /**
     * 构造函数，使用当前时间戳
     * 
     * @param challenge 挑战数据
     */
    public ChallengePacket(byte[] challenge) {
        this(challenge, System.currentTimeMillis());
    }
    
    /**
     * 从PacketByteBuf读取ChallengePacket
     * 
     * @param buf 数据缓冲区
     * @return ChallengePacket实例
     */
    public static ChallengePacket fromBytes(PacketByteBuf buf) {
        byte[] challenge = buf.readByteArray();
        long timestamp = buf.readLong();
        return new ChallengePacket(challenge, timestamp);
    }
    
    /**
     * 将ChallengePacket写入PacketByteBuf
     * 
     * @param packet 数据包
     * @return PacketByteBuf实例
     */
    public static PacketByteBuf toBytes(ChallengePacket packet) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByteArray(packet.challenge);
        buf.writeLong(packet.timestamp);
        return buf;
    }
    
    /**
     * 创建Minecraft数据包
     * 
     * @return Minecraft数据包
     */
    public CustomPayloadS2CPacket toPacket() {
        return new CustomPayloadS2CPacket(ID, toBytes(this));
    }
    
    /**
     * 发送给指定玩家
     * 
     * @param player 目标玩家
     */
    public void send(ServerPlayerEntity player) {
        if (player.networkHandler != null) {
            ServerPlayNetworking.send(player, ID, toBytes(this));
        }
    }
    
    /**
     * 获取挑战数据
     * 
     * @return 挑战数据
     */
    public byte[] getChallenge() {
        return challenge;
    }
    
    /**
     * 获取时间戳
     * 
     * @return 时间戳
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * 检查挑战是否过期
     * 
     * @param timeWindow 时间窗口（毫秒）
     * @return 如果挑战过期则返回true
     */
    public boolean isExpired(long timeWindow) {
        long currentTime = System.currentTimeMillis();
        return Math.abs(currentTime - timestamp) > timeWindow;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        ChallengePacket that = (ChallengePacket) obj;
        return timestamp == that.timestamp && Arrays.equals(challenge, that.challenge);
    }
    
    @Override
    public int hashCode() {
        int result = Long.hashCode(timestamp);
        result = 31 * result + Arrays.hashCode(challenge);
        return result;
    }
    
    @Override
    public String toString() {
        return "ChallengePacket{" +
                "challenge=" + Arrays.toString(challenge) +
                ", timestamp=" + timestamp +
                '}';
    }
    
    /**
     * 客户端数据包处理器
     * 这个类实际上不会被使用，因为我们在PacketRegistry中直接注册了处理器
     */
    public static class ClientHandler {
        /**
         * 处理服务器挑战
         *
         * @param packet 挑战数据包
         */
        public static void handle(ChallengePacket packet) {
            // 在客户端主线程处理
            MinecraftClient.getInstance().execute(() -> {
                try {
                    // 处理服务器挑战
                    nety.ys.client.ClientPacketHandler.handleServerChallenge(packet);
                } catch (Exception e) {
                    TokenAuthMod.LOGGER.error("处理服务器挑战时出错", e);
                }
            });
        }
    }
    
    /**
     * 服务端数据包处理器
     */
    public static class ServerHandler {
        /**
         * 处理客户端响应
         * 
         * @param packet 挑战数据包
         * @param player 玩家实体
         * @param responseSender 响应发送器
         */
        public static void handle(ChallengePacket packet, ServerPlayerEntity player, PacketSender responseSender) {
            // 这个方法实际上不会被调用，因为ChallengePacket是服务器发送给客户端的
            // 实际处理逻辑在ClientHandler中
        }
    }
}