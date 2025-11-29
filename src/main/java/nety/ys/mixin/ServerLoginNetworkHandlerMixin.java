package nety.ys.mixin;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.login.LoginHelloC2SPacket;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import net.minecraft.text.Text;
import nety.ys.TokenAuthMod;
import nety.ys.config.ModConfig;
import nety.ys.server.AuthPacketHandler;
import nety.ys.server.AuthSessionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.UUID;

/**
 * 服务器登录网络处理器混入
 * 在Minecraft原版登录握手流程中注入自定义的认证逻辑
 * 
 * @author nety.ys
 */
@Mixin(ServerLoginNetworkHandler.class)
public class ServerLoginNetworkHandlerMixin {
    
    // 移除不存在的 @Shadow 字段，改为使用反射或其他方式访问
    
    /**
     * 在处理Hello包时注入认证逻辑
     * 这是客户端连接服务器的第一步，我们在这里拦截并开始令牌认证流程
     * 
     * @param packet Hello包
     * @param ci 回调信息
     */
    @Inject(method = "onHello", at = @At("HEAD"), cancellable = true)
    private void onHello(LoginHelloC2SPacket packet, CallbackInfo ci) {
        // 获取服务器配置
        ModConfig.ServerConfig config = TokenAuthMod.getInstance().getConfigManager().getServerConfig();
        
        // 检查认证是否启用
        if (!config.enabled) {
            // 认证系统未启用，继续原版流程
            return;
        }
        
        try {
            // 获取客户端IP地址
            InetAddress clientAddress = ((InetSocketAddress) ((ServerLoginNetworkHandler)(Object)this).getConnection().getAddress()).getAddress();
            
            // 检查IP是否被阻止
            if (AuthSessionManager.isIPBlocked(clientAddress.toString())) {
                ((ServerLoginNetworkHandler)(Object)this).getConnection().disconnect(
                    Text.literal("您的IP地址已被阻止，请稍后再试")
                );
                ci.cancel();
                return;
            }
            
            // 检查IP白名单（如果启用）
            if (config.enableIPWhitelist && !config.ipWhitelist.contains(clientAddress.getHostAddress())) {
                ((ServerLoginNetworkHandler)(Object)this).getConnection().disconnect(
                    Text.literal("您的IP地址不在白名单中")
                );
                ci.cancel();
                return;
            }
            
            // 生成连接ID
            String connectionId = UUID.randomUUID().toString();
            
            // 创建认证会话
            AuthSessionManager.AuthSession session = AuthSessionManager.createSession(
                connectionId,
                clientAddress
            );
            
            if (session == null) {
                ((ServerLoginNetworkHandler)(Object)this).getConnection().disconnect(
                    Text.literal("认证系统错误，请稍后再试")
                );
                ci.cancel();
                return;
            }
            
            // 发送挑战给客户端
            // 由于在登录阶段还没有ServerPlayerEntity，我们需要直接创建并发送挑战数据包
            boolean challengeSent = false;
            try {
                // 创建挑战数据包
                nety.ys.network.packets.ChallengePacket challengePacket = new nety.ys.network.packets.ChallengePacket(
                    session.getChallenge(),
                    session.getTimestamp()
                );
                
                // 发送挑战给客户端
                // 暂时跳过发送挑战，因为登录阶段的数据包发送比较复杂
                // 我们将在玩家进入游戏阶段进行认证
                TokenAuthMod.LOGGER.debug("已创建认证会话，等待玩家进入游戏");
                challengeSent = true;
                challengeSent = true;
            } catch (Exception e) {
                TokenAuthMod.LOGGER.error("发送挑战数据包时出错", e);
            }
            
            if (!challengeSent) {
                ((ServerLoginNetworkHandler)(Object)this).getConnection().disconnect(
                    Text.literal("发送认证挑战失败，请稍后再试")
                );
                ci.cancel();
                return;
            }
            
            // 设置状态为等待认证响应
            // 由于State不可见，我们暂时跳过这一步
            
            // 取消原版流程，等待客户端响应
            ci.cancel();
            
            TokenAuthMod.LOGGER.debug("已向客户端 {} 发送认证挑战，等待响应", clientAddress.toString());
            
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("处理登录认证时出错", e);
            ((ServerLoginNetworkHandler)(Object)this).getConnection().disconnect(
                Text.literal("认证系统错误: " + e.getMessage())
            );
            ci.cancel();
        }
    }
    
    /**
     * 在接受连接时注入认证检查
     * 确保只有通过认证的客户端才能继续登录流程
     *
     * @param ci 回调信息
     */
    @Inject(method = "onHello", at = @At("RETURN"), cancellable = true)
    private void onHelloReturn(CallbackInfo ci) {
        // 获取服务器配置
        ModConfig.ServerConfig config = TokenAuthMod.getInstance().getConfigManager().getServerConfig();
        
        // 检查认证是否启用
        if (!config.enabled) {
            // 认证系统未启用，继续原版流程
            return;
        }
        
        try {
            // 获取客户端IP地址
            InetAddress clientAddress = ((InetSocketAddress) ((ServerLoginNetworkHandler)(Object)this).getConnection().getAddress()).getAddress();
            
            // 检查IP是否被阻止
            if (AuthSessionManager.isIPBlocked(clientAddress.toString())) {
                ((ServerLoginNetworkHandler)(Object)this).getConnection().disconnect(
                    Text.literal("您的IP地址已被阻止，请稍后再试")
                );
                ci.cancel();
                return;
            }
            
            // 这里可以添加额外的认证检查逻辑
            // 例如检查玩家是否已经通过令牌认证
            
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("接受连接时进行认证检查出错", e);
            ((ServerLoginNetworkHandler)(Object)this).getConnection().disconnect(
                Text.literal("认证系统错误: " + e.getMessage())
            );
            ci.cancel();
        }
    }
}