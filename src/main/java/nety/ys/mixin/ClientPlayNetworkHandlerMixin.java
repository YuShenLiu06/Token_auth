package nety.ys.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import nety.ys.TokenAuthMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 客户端游戏网络处理器混入
 * 用于处理客户端游戏相关事件
 * 
 * @author nety.ys
 */
@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {
    
    /**
     * 在玩家加入游戏时处理事件
     * 
     * @param ci 回调信息
     */
    @Inject(method = "onGameJoin", at = @At("HEAD"))
    private void onGameJoin(CallbackInfo ci) {
        TokenAuthMod.LOGGER.info("客户端加入游戏世界");
        
        // 这里可以添加客户端加入游戏后的处理逻辑
        // 例如验证服务器是否支持令牌认证
    }
    
    /**
     * 在玩家被踢出时处理事件
     * 
     * @param reason 踢出原因
     * @param ci 回调信息
     */
    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void onDisconnect(net.minecraft.text.Text reason, CallbackInfo ci) {
        TokenAuthMod.LOGGER.info("客户端断开连接，原因: {}", reason.getString());
        
        // 这里可以添加客户端断开连接后的处理逻辑
        // 例如清理客户端认证状态
    }
}