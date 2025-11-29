package nety.ys.mixin;

import net.minecraft.client.network.ClientLoginNetworkHandler;
import net.minecraft.network.packet.s2c.login.LoginSuccessS2CPacket;
import nety.ys.TokenAuthMod;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 客户端登录网络处理器混入
 * 用于处理客户端登录相关事件
 * 
 * @author nety.ys
 */
@Mixin(ClientLoginNetworkHandler.class)
public class ClientLoginNetworkHandlerMixin {
    
    /**
     * 在登录成功时处理事件
     * 
     * @param packet 登录成功数据包
     * @param ci 回调信息
     */
    @Inject(method = "onLoginSuccess", at = @At("HEAD"))
    private void onLoginSuccess(LoginSuccessS2CPacket packet, CallbackInfo ci) {
        TokenAuthMod.LOGGER.info("客户端登录成功，玩家UUID: {}", packet.uuid());
        
        // 这里可以添加客户端登录成功后的处理逻辑
        // 例如初始化客户端特定的认证状态
    }
}