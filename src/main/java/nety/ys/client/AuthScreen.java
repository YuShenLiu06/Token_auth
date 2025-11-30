package nety.ys.client;

import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;

/**
 * 认证屏幕
 * 在登录阶段显示认证状态和进度
 * 
 * @author nety.ys
 */
public class AuthScreen extends Screen {
    
    /**
     * 背景颜色
     */
    private static final int BACKGROUND_COLOR = 0xFF101010;
    
    /**
     * 进度条颜色
     */
    private static final int PROGRESS_COLOR = 0xFF4CAF50;
    private static final int PROGRESS_BG_COLOR = 0xFF333333;
    
    /**
     * 文本颜色
     */
    private static final int TITLE_COLOR = 0xFFFFFFFF;
    private static final int STATUS_COLOR = 0xFFCCCCCC;
    private static final int ERROR_COLOR = 0xFFFF5252;
    
    /**
     * 构造函数
     */
    public AuthScreen() {
        super(Text.literal("Token认证"));
    }
    
    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        // 绘制背景
        DrawableHelper.fill(matrices, 0, 0, this.width, this.height, BACKGROUND_COLOR);
        
        // 计算中心位置
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        
        // 绘制标题
        String title = "Token认证系统";
        int titleWidth = this.textRenderer.getWidth(title);
        this.textRenderer.draw(matrices, title, centerX - titleWidth / 2, centerY - 60, TITLE_COLOR);
        
        // 绘制状态
        AuthStateManager.AuthState state = AuthStateManager.getCurrentState();
        String stateText = state.getTitle();
        int stateWidth = this.textRenderer.getWidth(stateText);
        this.textRenderer.draw(matrices, stateText, centerX - stateWidth / 2, centerY - 30,
            state == AuthStateManager.AuthState.FAILED || state == AuthStateManager.AuthState.ERROR ? ERROR_COLOR : STATUS_COLOR);
        
        // 绘制状态消息
        String message = AuthStateManager.getStatusMessage();
        if (!message.isEmpty()) {
            int messageWidth = this.textRenderer.getWidth(message);
            this.textRenderer.draw(matrices, message, centerX - messageWidth / 2, centerY - 10, STATUS_COLOR);
        }
        
        // 绘制进度条
        int progress = AuthStateManager.getProgress();
        drawProgressBar(matrices, centerX - 100, centerY + 20, 200, 10, progress);
        
        // 绘制进度百分比
        String progressText = progress + "%";
        int progressWidth = this.textRenderer.getWidth(progressText);
        this.textRenderer.draw(matrices, progressText, centerX - progressWidth / 2, centerY + 35, STATUS_COLOR);
        
        nety.ys.TokenAuthMod.LOGGER.debug("认证界面更新 - 状态: {}, 进度: {}%, 消息: {}",
            AuthStateManager.getCurrentState().getTitle(),
            AuthStateManager.getProgress(),
            AuthStateManager.getStatusMessage());
        
        // 绘制提示信息
        if (AuthStateManager.isAuthenticationFailed()) {
            String hint = "请检查客户端配置或联系服务器管理员";
            int hintWidth = this.textRenderer.getWidth(hint);
            this.textRenderer.draw(matrices, hint, centerX - hintWidth / 2, centerY + 60, ERROR_COLOR);
        } else if (AuthStateManager.isAuthenticating()) {
            String hint = "正在与服务器进行安全认证，请稍候...";
            int hintWidth = this.textRenderer.getWidth(hint);
            this.textRenderer.draw(matrices, hint, centerX - hintWidth / 2, centerY + 60, STATUS_COLOR);
        }
    }
    
    /**
     * 绘制进度条
     *
     * @param matrices 矩阵堆栈
     * @param x X坐标
     * @param y Y坐标
     * @param width 宽度
     * @param height 高度
     * @param progress 进度 (0-100)
     */
    private void drawProgressBar(MatrixStack matrices, int x, int y, int width, int height, int progress) {
        // 绘制背景
        DrawableHelper.fill(matrices, x, y, x + width, y + height, PROGRESS_BG_COLOR);
        
        // 绘制进度
        int progressWidth = (width * progress) / 100;
        DrawableHelper.fill(matrices, x, y, x + progressWidth, y + height, PROGRESS_COLOR);
        
        // 绘制边框
        fillHorizontalLine(matrices, x, y, x + width, 0xFF666666);
        fillHorizontalLine(matrices, x, y + height, x + width, 0xFF666666);
        fillVerticalLine(matrices, x, y, y + height, 0xFF666666);
        fillVerticalLine(matrices, x + width, y, y + height, 0xFF666666);
    }
    
    @Override
    public boolean shouldPause() {
        return false; // 不暂停游戏
    }
    
    @Override
    public boolean shouldCloseOnEsc() {
        // 只有在认证失败或错误时才允许ESC关闭
        return AuthStateManager.isAuthenticationFailed();
    }
    
    /**
     * 更新状态（由AuthStateManager调用）
     */
    public void updateStatus() {
        // 屏幕会在render方法中自动更新状态
        // 这个方法主要用于触发重绘
    }
    
    /**
     * 填充水平线
     */
    private void fillHorizontalLine(MatrixStack matrices, int x1, int y, int x2, int color) {
        DrawableHelper.fill(matrices, x1, y, x2, y + 1, color);
    }
    
    /**
     * 填充垂直线
     */
    private void fillVerticalLine(MatrixStack matrices, int x, int y1, int y2, int color) {
        DrawableHelper.fill(matrices, x, y1, x + 1, y2, color);
    }
}