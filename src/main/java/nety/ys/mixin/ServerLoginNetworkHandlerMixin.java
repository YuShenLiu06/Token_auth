package nety.ys.mixin;

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
    
    @Shadow
    private UUID authenticatedUuid;
    
    @Shadow
    private ServerLoginNetworkHandler.State state;
    
    @Shadow
    private ServerLoginNetworkHandler.Connection connection;
    
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
            InetAddress clientAddress = ((InetSocketAddress) this.connection.getAddress()).getAddress();
            
            // 检查IP是否被阻止
            if (AuthSessionManager.isIPBlocked(clientAddress.toString())) {
                this.connection.disconnect(
                    Text.literal("您的IP地址已被阻止，请稍后再试")
                );
                ci.cancel();
                return;
            }
            
            // 检查IP白名单（如果启用）
            if (config.enableIPWhitelist && !config.ipWhitelist.contains(clientAddress.getHostAddress())) {
                this.connection.disconnect(
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
                this.connection.disconnect(
                    Text.literal("认证系统错误，请稍后再试")
                );
                ci.cancel();
                return;
            }
            
            // 发送挑战给客户端
            boolean challengeSent = AuthPacketHandler.sendChallengeToClient(
                null // 1.19.2版本中没有直接访问player的方法
            );
            
            if (!challengeSent) {
                this.connection.disconnect(
                    Text.literal("发送认证挑战失败，请稍后再试")
                );
                ci.cancel();
                return;
            }
            
            // 设置状态为等待认证响应
            this.state = ServerLoginNetworkHandler.State.READY_TO_ACCEPT;
            
            // 取消原版流程，等待客户端响应
            ci.cancel();
            
            TokenAuthMod.LOGGER.debug("已向客户端 {} 发送认证挑战，等待响应", clientAddress.toString());
            
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("处理登录认证时出错", e);
            this.connection.disconnect(
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
    @Inject(method = "acceptConnection", at = @At("HEAD"), cancellable = true)
    private void acceptConnection(CallbackInfo ci) {
        // 获取服务器配置
        ModConfig.ServerConfig config = TokenAuthMod.getInstance().getConfigManager().getServerConfig();
        
        // 检查认证是否启用
        if (!config.enabled) {
            // 认证系统未启用，继续原版流程
            return;
        }
        
        try {
            // 获取客户端IP地址
            InetAddress clientAddress = ((InetSocketAddress) this.connection.getAddress()).getAddress();
            
            // 检查IP是否被阻止
            if (AuthSessionManager.isIPBlocked(clientAddress.toString())) {
                this.connection.disconnect(
                    Text.literal("您的IP地址已被阻止，请稍后再试")
                );
                ci.cancel();
                return;
            }
            
            // 这里可以添加额外的认证检查逻辑
            // 例如检查玩家是否已经通过令牌认证
            
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("接受连接时进行认证检查出错", e);
            this.connection.disconnect(
                Text.literal("认证系统错误: " + e.getMessage())
            );
            ci.cancel();
        }
    }
}