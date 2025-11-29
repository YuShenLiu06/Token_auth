package nety.ys.mixin;

import net.minecraft.server.network.ServerPlayerEntity;
import nety.ys.TokenAuthMod;
import nety.ys.server.AuthSessionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 服务器玩家实体混入
 * 用于处理玩家连接和断开事件
 * 
 * @author nety.ys
 */
@Mixin(ServerPlayerEntity.class)
public class ServerPlayerEntityMixin {
    
    /**
     * 在玩家断开连接时清理认证状态
     * 
     * @param ci 回调信息
     */
    @Inject(method = "onDisconnect", at = @At("HEAD"))
    private void onDisconnect(CallbackInfo ci) {
        ServerPlayerEntity player = (ServerPlayerEntity)(Object)this;
        
        // 移除玩家认证状态
        if (player != null && player.getUuid() != null) {
            AuthSessionManager.removePlayerAuthentication(player.getUuid().toString());
            TokenAuthMod.LOGGER.debug("玩家 {} 已断开连接，清理认证状态", player.getName().getString());
        }
    }
}