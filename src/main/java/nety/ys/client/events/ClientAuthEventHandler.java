package nety.ys.client.events;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import nety.ys.TokenAuthMod;
import nety.ys.client.ClientTokenManager;

/**
 * 客户端认证事件处理器
 * 处理客户端的各种认证相关事件
 *
 * @author nety.ys
 */
public class ClientAuthEventHandler {
    
    /**
     * 玩家加入服务器事件处理
     *
     * @param handler 网络处理器
     * @param sender 数据包发送器
     * @param client 客户端实例
     */
    public static void onJoinServer(ClientPlayNetworkHandler handler, MinecraftClient client) {
        TokenAuthMod.LOGGER.info("客户端加入服务器");
        
        // 重新初始化客户端令牌管理器
        ClientTokenManager.reinitialize();
        
        // 这里可以添加客户端加入服务器后的处理逻辑
        // 例如检查服务器是否支持令牌认证
    }
    
    /**
     * 玩家断开连接事件处理
     *
     * @param handler 网络处理器
     * @param client 客户端实例
     */
    public static void onDisconnect(ClientPlayNetworkHandler handler, MinecraftClient client) {
        TokenAuthMod.LOGGER.info("客户端断开连接");
        
        // 这里可以添加客户端断开连接后的处理逻辑
        // 例如清理客户端认证状态
    }
}