package nety.ys.network.packets;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.ServerPlayPacketListener;
// import net.minecraft.network.packet.CustomPayload; // 1.19.2版本中没有这个类
import net.minecraft.network.packet.c2s.play.CustomPayloadC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import nety.ys.TokenAuthMod;

import java.util.Arrays;

/**
 * 令牌响应数据包
 * 客户端向服务器发送对挑战的响应令牌
 * 
 * @author nety.ys
 */
public class TokenResponsePacket {
    
    /**
     * 数据包标识符
     */
    public static final Identifier ID = new Identifier("tokenauth", "token_response");
    
    /**
     * 令牌响应数据
     */
    private final byte[] tokenResponse;
    
    /**
     * 原始挑战数据的时间戳
     */
    private final long challengeTimestamp;
    
    /**
     * 构造函数
     * 
     * @param tokenResponse 令牌响应数据
     * @param challengeTimestamp 原始挑战数据的时间戳
     */
    public TokenResponsePacket(byte[] tokenResponse, long challengeTimestamp) {
        this.tokenResponse = tokenResponse;
        this.challengeTimestamp = challengeTimestamp;
    }
    
    /**
     * 构造函数
     * 
     * @param tokenResponse 令牌响应数据
     */
    public TokenResponsePacket(byte[] tokenResponse) {
        this(tokenResponse, System.currentTimeMillis());
    }
    
    /**
     * 从PacketByteBuf读取TokenResponsePacket
     * 
     * @param buf 数据缓冲区
     * @return TokenResponsePacket实例
     */
    public static TokenResponsePacket fromBytes(PacketByteBuf buf) {
        byte[] tokenResponse = buf.readByteArray();
        long challengeTimestamp = buf.readLong();
        return new TokenResponsePacket(tokenResponse, challengeTimestamp);
    }
    
    /**
     * 将TokenResponsePacket写入PacketByteBuf
     * 
     * @param packet 数据包
     * @return PacketByteBuf实例
     */
    public static PacketByteBuf toBytes(TokenResponsePacket packet) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeByteArray(packet.tokenResponse);
        buf.writeLong(packet.challengeTimestamp);
        return buf;
    }
    
    /**
     * 创建Minecraft数据包
     * 
     * @return Minecraft数据包
     */
    public CustomPayloadC2SPacket toPacket() {
        return new CustomPayloadC2SPacket(ID, toBytes(this));
    }
    
    /**
     * 发送到服务器
     */
    public void send() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getNetworkHandler() != null) {
            client.getNetworkHandler().sendPacket(toPacket());
        }
    }
    
    /**
     * 获取令牌响应数据
     * 
     * @return 令牌响应数据
     */
    public byte[] getTokenResponse() {
        return tokenResponse;
    }
    
    /**
     * 获取原始挑战数据的时间戳
     * 
     * @return 原始挑战数据的时间戳
     */
    public long getChallengeTimestamp() {
        return challengeTimestamp;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        
        TokenResponsePacket that = (TokenResponsePacket) obj;
        return challengeTimestamp == that.challengeTimestamp && Arrays.equals(tokenResponse, that.tokenResponse);
    }
    
    @Override
    public int hashCode() {
        int result = Long.hashCode(challengeTimestamp);
        result = 31 * result + Arrays.hashCode(tokenResponse);
        return result;
    }
    
    @Override
    public String toString() {
        return "TokenResponsePacket{" +
                "tokenResponse=" + Arrays.toString(tokenResponse) +
                ", challengeTimestamp=" + challengeTimestamp +
                '}';
    }
    
    /**
     * 服务端数据包处理器
     */
    public static class ServerHandler implements ServerPlayNetworking.PlayChannelHandler {
        @Override
        public void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
            TokenResponsePacket packet = fromBytes(buf);
            
            // 在服务器主线程处理
            server.execute(() -> {
                try {
                    // 处理客户端令牌响应
                    nety.ys.server.AuthPacketHandler.handleTokenResponse(packet, player, responseSender);
                } catch (Exception e) {
                    TokenAuthMod.LOGGER.error("处理客户端令牌响应时出错", e);
                }
            });
        }
    }
    
    /**
     * 客户端数据包处理器
     * 这个类实际上不会被使用，因为我们在PacketRegistry中直接注册了处理器
     */
    public static class ClientHandler {
        /**
         * 处理服务器响应
         *
         * @param packet 令牌响应数据包
         */
        public static void handle(TokenResponsePacket packet) {
            // 这个方法实际上不会被调用，因为TokenResponsePacket是客户端发送给服务器的
            // 实际处理逻辑在ServerHandler中
        }
    }
}