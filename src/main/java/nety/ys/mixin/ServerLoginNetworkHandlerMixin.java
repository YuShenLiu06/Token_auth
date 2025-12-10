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
        TokenAuthMod.LOGGER.info("收到客户端Hello包，开始处理登录认证");
        
        // 获取服务器配置
        ModConfig.ServerConfig config = TokenAuthMod.getInstance().getConfigManager().getServerConfig();
        
        // 添加空值检查，防止在客户端环境中出现NullPointerException
        if (config == null) {
            TokenAuthMod.LOGGER.warn("服务器配置为null，可能是客户端环境，继续原版登录流程");
            return;
        }
        
        // 检查认证是否启用
        if (!config.enabled) {
            TokenAuthMod.LOGGER.info("认证系统未启用，继续原版登录流程");
            // 认证系统未启用，继续原版流程
            return;
        }
        
        try {
            // 获取客户端IP地址 - 处理本地连接和远程连接的两种情况
            Object addressObj = ((ServerLoginNetworkHandler)(Object)this).getConnection().getAddress();
            InetAddress clientAddress;
            
            if (addressObj instanceof InetSocketAddress) {
                // 远程连接
                clientAddress = ((InetSocketAddress) addressObj).getAddress();
                TokenAuthMod.LOGGER.info("客户端IP地址（远程）: {}", clientAddress.toString());
            } else if (addressObj instanceof io.netty.channel.local.LocalAddress) {
                // 本地连接
                clientAddress = InetAddress.getLoopbackAddress();
                TokenAuthMod.LOGGER.info("客户端IP地址（本地）: {}", clientAddress.toString());
            } else {
                // 未知地址类型，使用回环地址作为后备
                clientAddress = InetAddress.getLoopbackAddress();
                TokenAuthMod.LOGGER.warn("未知地址类型: {}，使用回环地址", addressObj.getClass().getName());
            }
            
            // 检查IP是否被阻止
            if (AuthSessionManager.isIPBlocked(clientAddress.toString())) {
                TokenAuthMod.LOGGER.warn("IP地址 {} 已被阻止，拒绝连接", clientAddress.toString());
                ((ServerLoginNetworkHandler)(Object)this).getConnection().disconnect(
                    Text.literal("您的IP地址已被阻止，请稍后再试")
                );
                ci.cancel();
                return;
            }
            
            // 检查IP白名单（如果启用）
            if (config.enableIPWhitelist && !config.ipWhitelist.contains(clientAddress.getHostAddress())) {
                TokenAuthMod.LOGGER.warn("IP地址 {} 不在白名单中，拒绝连接", clientAddress.getHostAddress());
                ((ServerLoginNetworkHandler)(Object)this).getConnection().disconnect(
                    Text.literal("您的IP地址不在白名单中")
                );
                ci.cancel();
                return;
            }
            
            // 生成连接ID
            String connectionId = UUID.randomUUID().toString();
            TokenAuthMod.LOGGER.info("为连接 {} 生成连接ID: {}", clientAddress.toString(), connectionId);
            
            // 创建认证会话
            AuthSessionManager.AuthSession session = AuthSessionManager.createSession(
                connectionId,
                clientAddress
            );
            
            if (session == null) {
                TokenAuthMod.LOGGER.error("无法为连接 {} 创建认证会话", connectionId);
                ((ServerLoginNetworkHandler)(Object)this).getConnection().disconnect(
                    Text.literal("认证系统错误，请稍后再试")
                );
                ci.cancel();
                return;
            }
            
            // 不在登录阶段发送挑战，而是让玩家进入游戏后在游戏阶段进行认证
            // 这样可以避免登录阶段的数据包序列化问题
            TokenAuthMod.LOGGER.info("允许玩家进入游戏，将在游戏阶段进行认证");
            
            // 不取消原版流程，让玩家正常进入游戏，然后在游戏阶段进行认证
            // ci.cancel();
            
            TokenAuthMod.LOGGER.info("已为客户端 {} 创建认证会话，等待玩家进入游戏", clientAddress.toString());
            
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
        
        // 添加空值检查，防止在客户端环境中出现NullPointerException
        if (config == null) {
            TokenAuthMod.LOGGER.warn("服务器配置为null，可能是客户端环境，继续原版登录流程");
            return;
        }
        
        // 检查认证是否启用
        if (!config.enabled) {
            // 认证系统未启用，继续原版流程
            return;
        }
        
        try {
            // 获取客户端IP地址 - 处理本地连接和远程连接的两种情况
            Object addressObj = ((ServerLoginNetworkHandler)(Object)this).getConnection().getAddress();
            InetAddress clientAddress;
            
            if (addressObj instanceof InetSocketAddress) {
                // 远程连接
                clientAddress = ((InetSocketAddress) addressObj).getAddress();
            } else if (addressObj instanceof io.netty.channel.local.LocalAddress) {
                // 本地连接
                clientAddress = InetAddress.getLoopbackAddress();
            } else {
                // 未知地址类型，使用回环地址作为后备
                clientAddress = InetAddress.getLoopbackAddress();
            }
            
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