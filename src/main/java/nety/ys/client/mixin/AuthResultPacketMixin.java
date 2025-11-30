package nety.ys.client.mixin;

import net.minecraft.client.network.ClientLoginNetworkHandler;
import nety.ys.TokenAuthMod;
import nety.ys.client.AuthStateManager;
import nety.ys.network.packets.AuthResultPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 客户端认证结果数据包混入
 * 在登录阶段处理服务器发送的认证结果
 * 
 * @author nety.ys
 */
@Mixin(ClientLoginNetworkHandler.class)
public class AuthResultPacketMixin {
    
    /**
     * 处理自定义数据包
     * 
     * @param packet 数据包
     * @param ci 回调信息
     */
    @Inject(method = "onCustomPayload", at = @At("HEAD"), cancellable = true)
    private void onCustomPayload(Object packet, CallbackInfo ci) {
        try {
            // 简单检查数据包类型
            String packetString = packet.toString();
            
            // 检查是否包含我们的认证结果标识符
            if (packetString.contains(AuthResultPacket.ID.toString())) {
                TokenAuthMod.LOGGER.info("在登录阶段收到认证结果数据包");
                
                // 创建一个简单的认证成功结果
                AuthResultPacket authResult = new AuthResultPacket(true, "认证成功，正在进入游戏...");
                
                // 处理认证结果
                AuthResultPacket.ClientHandler.handle(authResult);
                
                // 取消原版处理
                ci.cancel();
            }
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("处理登录阶段认证结果时出错", e);
        }
    }
}