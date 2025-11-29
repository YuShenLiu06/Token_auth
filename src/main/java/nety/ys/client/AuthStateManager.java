package nety.ys.client;

import nety.ys.TokenAuthMod;

/**
 * 客户端认证状态管理器
 * 管理客户端的认证状态和UI显示
 * 
 * @author nety.ys
 */
public class AuthStateManager {
    
    /**
     * 认证状态枚举
     */
    public enum AuthState {
        IDLE("空闲", "等待连接服务器..."),
        CONNECTING("连接中", "正在连接服务器..."),
        CHALLENGE_RECEIVED("收到挑战", "正在处理认证挑战..."),
        PROCESSING("处理中", "生成令牌响应..."),
        WAITING_RESULT("等待结果", "等待服务器验证..."),
        SUCCESS("认证成功", "认证成功，正在进入游戏..."),
        FAILED("认证失败", "认证失败，请检查配置"),
        TIMEOUT("认证超时", "认证超时，请重试"),
        ERROR("错误", "认证过程中发生错误");
        
        private final String title;
        private final String description;
        
        AuthState(String title, String description) {
            this.title = title;
            this.description = description;
        }
        
        public String getTitle() {
            return title;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 当前认证状态
     */
    private static AuthState currentState = AuthState.IDLE;
    
    /**
     * 认证进度 (0-100)
     */
    private static int progress = 0;
    
    /**
     * 状态消息
     */
    private static String statusMessage = "";
    
    /**
     * 认证屏幕实例
     */
    private static AuthScreen authScreen;
    
    /**
     * 设置认证状态
     * 
     * @param newState 新状态
     */
    public static void setState(AuthState newState) {
        TokenAuthMod.LOGGER.info("认证状态变更: {} -> {}", currentState.getTitle(), newState.getTitle());
        currentState = newState;
        
        // 根据状态更新进度
        switch (newState) {
            case IDLE:
                progress = 0;
                break;
            case CONNECTING:
                progress = 10;
                break;
            case CHALLENGE_RECEIVED:
                progress = 30;
                break;
            case PROCESSING:
                progress = 50;
                break;
            case WAITING_RESULT:
                progress = 70;
                break;
            case SUCCESS:
                progress = 100;
                break;
            case FAILED:
            case TIMEOUT:
            case ERROR:
                progress = 0;
                break;
        }
        
        // 更新UI
        updateAuthScreen();
    }
    
    /**
     * 设置状态消息
     * 
     * @param message 状态消息
     */
    public static void setStatusMessage(String message) {
        statusMessage = message;
        TokenAuthMod.LOGGER.info("认证状态消息: {}", message);
        updateAuthScreen();
    }
    
    /**
     * 设置进度
     * 
     * @param newProgress 新进度 (0-100)
     */
    public static void setProgress(int newProgress) {
        progress = Math.max(0, Math.min(100, newProgress));
        updateAuthScreen();
    }
    
    /**
     * 获取当前状态
     * 
     * @return 当前认证状态
     */
    public static AuthState getCurrentState() {
        return currentState;
    }
    
    /**
     * 获取进度
     * 
     * @return 当前进度 (0-100)
     */
    public static int getProgress() {
        return progress;
    }
    
    /**
     * 获取状态消息
     * 
     * @return 状态消息
     */
    public static String getStatusMessage() {
        return statusMessage;
    }
    
    /**
     * 检查是否正在认证
     * 
     * @return 如果正在认证则返回true
     */
    public static boolean isAuthenticating() {
        return currentState == AuthState.CONNECTING || 
               currentState == AuthState.CHALLENGE_RECEIVED || 
               currentState == AuthState.PROCESSING || 
               currentState == AuthState.WAITING_RESULT;
    }
    
    /**
     * 检查认证是否成功
     * 
     * @return 如果认证成功则返回true
     */
    public static boolean isAuthenticated() {
        return currentState == AuthState.SUCCESS;
    }
    
    /**
     * 检查认证是否失败
     * 
     * @return 如果认证失败则返回true
     */
    public static boolean isAuthenticationFailed() {
        return currentState == AuthState.FAILED || 
               currentState == AuthState.TIMEOUT || 
               currentState == AuthState.ERROR;
    }
    
    /**
     * 重置认证状态
     */
    public static void reset() {
        TokenAuthMod.LOGGER.info("重置认证状态");
        currentState = AuthState.IDLE;
        progress = 0;
        statusMessage = "";
        authScreen = null;
    }
    
    /**
     * 设置认证屏幕
     * 
     * @param screen 认证屏幕实例
     */
    public static void setAuthScreen(AuthScreen screen) {
        authScreen = screen;
    }
    
    /**
     * 更新认证屏幕
     */
    private static void updateAuthScreen() {
        if (authScreen != null) {
            // 在客户端主线程中更新UI
            net.minecraft.client.MinecraftClient.getInstance().execute(() -> {
                if (authScreen != null) {
                    authScreen.updateStatus();
                }
            });
        }
    }
    
    /**
     * 处理认证结果
     * 
     * @param success 是否成功
     * @param message 结果消息
     */
    public static void handleAuthResult(boolean success, String message) {
        if (success) {
            setState(AuthState.SUCCESS);
            setStatusMessage(message);
        } else {
            setState(AuthState.FAILED);
            setStatusMessage(message);
        }
        
        // 3秒后重置状态或关闭屏幕
        new java.util.Timer().schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                if (success && authScreen != null) {
                    // 认证成功，关闭认证屏幕
                    net.minecraft.client.MinecraftClient.getInstance().execute(() -> {
                        if (net.minecraft.client.MinecraftClient.getInstance().player != null) {
                            net.minecraft.client.MinecraftClient.getInstance().player.closeScreen();
                        }
                    });
                }
            }
        }, 3000); // 3秒
    }
}