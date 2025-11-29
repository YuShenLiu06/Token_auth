package nety.ys.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import nety.ys.TokenAuthMod;
import nety.ys.client.events.ClientAuthEventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 客户端初始化类
 * 负责初始化客户端特有的认证系统组件
 * 
 * @author nety.ys
 */
public class ClientInitializer implements ClientModInitializer {
    
    /**
     * 客户端日志记录器
     */
    private static final Logger LOGGER = LoggerFactory.getLogger("token-auth-client");
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("Token Auth Mod 客户端正在初始化...");
        
        // 注册客户端事件处理器
        registerClientEvents();
        
        LOGGER.info("Token Auth Mod 客户端初始化完成！");
    }
    
    /**
     * 注册客户端事件处理器
     */
    private void registerClientEvents() {
        // 玩家加入服务器事件
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            ClientAuthEventHandler.onJoinServer(handler, client);
        });
        
        // 玩家断开连接事件
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            ClientAuthEventHandler.onDisconnect(handler, client);
        });
    }
}