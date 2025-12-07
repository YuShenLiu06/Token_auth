package nety.ys;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import nety.ys.client.ClientInitializer;
import nety.ys.client.ClientTokenManager;
import nety.ys.config.ConfigManager;
import nety.ys.config.SimpleConfigManager;
import nety.ys.network.PacketRegistry;
import nety.ys.server.AuthSessionManager;
import nety.ys.server.commands.TokenCommandUnified;
import nety.ys.server.events.AuthEventHandler;
import nety.ys.server.constraint.ConstraintManager;
import nety.ys.util.DebugLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Token Auth Mod 主入口类
 * 负责初始化客户端和服务端的认证系统组件
 * 
 * @author nety.ys
 */
public class TokenAuthMod implements ModInitializer {
    /**
     * 模组日志记录器
     */
    public static final Logger LOGGER = LoggerFactory.getLogger("token-auth");
    
    /**
     * 模组实例
     */
    private static TokenAuthMod INSTANCE;
    
    /**
     * 模组版本号
     */
    private static String MOD_VERSION;
    
    /**
     * 配置管理器
     */
    private SimpleConfigManager configManager;
    
    /**
     * 获取模组实例
     *
     * @return 模组实例
     */
    public static TokenAuthMod getInstance() {
        return INSTANCE;
    }
    
    /**
     * 获取配置管理器
     *
     * @return 配置管理器实例
     */
    public SimpleConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * 模组初始化方法
     * 根据环境类型初始化相应的组件
     */
    @Override
    public void onInitialize() {
        INSTANCE = this;
        
        // 加载版本号
        loadVersion();
        
        LOGGER.info("Token Auth Mod 正在初始化...");
        
        // 初始化配置管理器
        // 使用SimpleConfigManager以支持CSV记录功能
        configManager = new SimpleConfigManager();
        
        // 根据环境初始化
        if (FabricLoader.getInstance().getEnvironmentType() == net.fabricmc.api.EnvType.SERVER) {
            initializeServer();
        } else {
            initializeClient();
        }
        
        LOGGER.info("Token Auth Mod 初始化完成！版本: " + MOD_VERSION);
    }
    
    /**
     * 从版本属性文件加载版本号
     */
    private void loadVersion() {
        try (InputStream stream = getClass().getResourceAsStream("/version.properties")) {
            if (stream != null) {
                Properties props = new Properties();
                props.load(stream);
                MOD_VERSION = props.getProperty("mod.version", "未知版本");
            } else {
                MOD_VERSION = "未知版本";
                LOGGER.warn("无法找到版本属性文件，使用默认版本");
            }
        } catch (IOException e) {
            MOD_VERSION = "未知版本";
            LOGGER.error("读取版本信息时出错", e);
        }
    }
    
    /**
     * 初始化服务端组件
     */
    private void initializeServer() {
        LOGGER.info("正在初始化服务端认证系统...");
        
        // 加载服务器配置
        configManager.loadServerConfig();
        
        // 初始化认证会话管理器
        AuthSessionManager.initialize();
        
        // 注册服务端数据包
        PacketRegistry.registerServerPackets();
        
        // 注册事件处理器
        registerServerEvents();
        
        // 注册管理命令
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            TokenCommandUnified.register(dispatcher);
            // 保留原有的简单命令用于向后兼容
            // TokenCommandSimple.register(dispatcher);
            // nety.ys.server.commands.DebugLoggerTestCommand.register(dispatcher);
        });
        
        // 延迟初始化约束系统，确保ConstraintAPI已经完全初始化
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            initializeConstraintSystem();
        });
        
        LOGGER.info("服务端认证系统初始化完成");
    }
    
    /**
     * 延迟初始化约束系统
     * 确保在服务器启动时ConstraintAPI已经完全初始化
     */
    private void initializeConstraintSystem() {
        try {
            // 检查constraint模组是否可用
            Class.forName("nety.ys.constraint.api.ConstraintAPI");
            
            // 检查ConstraintAPI是否已经初始化
            if (nety.ys.constraint.api.ConstraintAPI.isInitialized()) {
                ConstraintManager.initialize();
                DebugLogger.debug("约束系统初始化成功");
            } else {
                TokenAuthMod.LOGGER.warn("ConstraintAPI尚未初始化，约束功能将不可用");
            }
        } catch (ClassNotFoundException e) {
            TokenAuthMod.LOGGER.warn("约束模组未找到，约束功能将不可用");
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("初始化约束系统时出错", e);
        }
    }
    
    /**
     * 初始化客户端组件
     */
    private void initializeClient() {
        LOGGER.info("正在初始化客户端认证系统...");
        
        // 加载客户端配置
        configManager.loadClientConfig();
        
        // 初始化客户端令牌管理器
        ClientTokenManager.initialize();
        
        // 注册客户端数据包
        PacketRegistry.registerClientPackets();
        
        // 添加关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("客户端关闭钩子触发，清理资源...");
            
            // 重置认证状态
            nety.ys.client.AuthStateManager.reset();
            
            // 注销客户端数据包
            PacketRegistry.unregisterClientPackets();
            
            LOGGER.info("客户端资源清理完成");
        }));
        
        LOGGER.info("客户端认证系统初始化完成");
    }
    
    /**
     * 注册服务端事件处理器
     */
    private void registerServerEvents() {
        // 服务器生命周期事件
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            DebugLogger.debug("服务器启动中，认证系统准备就绪");
            AuthSessionManager.onServerStarting(server);
        });
        
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
            DebugLogger.debug("服务器已停止，清理认证会话");
            AuthSessionManager.onServerStopped();
            
            // 注销数据包处理器
            PacketRegistry.unregisterServerPackets();
            
            // 关闭邮件通知服务
            nety.ys.util.EmailNotifier.shutdown();
            
            // 关闭认证警报服务
            nety.ys.server.AuthAlertService.shutdown();
        });
        
        // 玩家连接事件
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            AuthEventHandler.onPlayerJoin(handler, sender, server);
        });
        
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            AuthEventHandler.onPlayerDisconnect(handler, server);
        });
    }
}