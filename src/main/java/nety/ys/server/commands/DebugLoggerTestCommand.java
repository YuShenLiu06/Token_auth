package nety.ys.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import nety.ys.TokenAuthMod;
import nety.ys.util.DebugLogger;
import nety.ys.util.DebugLoggerTest;
import nety.ys.config.SimpleConfigManager;

/**
 * 调试日志测试命令
 * 用于测试调试模式开关是否正常工作
 * 
 * @author nety.ys
 */
public class DebugLoggerTestCommand {
    
    /**
     * 注册调试日志测试命令
     * 
     * @param dispatcher 命令分发器
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // 调试模式测试命令：/debugtest
        dispatcher.register(CommandManager.literal("debugtest")
            .requires(source -> source.hasPermissionLevel(3)) // 需要OP 3级权限
            .executes(DebugLoggerTestCommand::execute));
        
        // 调试模式状态命令：/debugstatus
        dispatcher.register(CommandManager.literal("debugstatus")
            .requires(source -> source.hasPermissionLevel(3)) // 需要OP 3级权限
            .executes(DebugLoggerTestCommand::showDebugStatus));
    }
    
    /**
     * 执行调试日志测试命令
     * 
     * @param context 命令上下文
     * @return 命令执行结果
     */
    private static int execute(CommandContext<ServerCommandSource> context) {
        try {
            context.getSource().sendFeedback(
                Text.literal("§6开始测试调试日志功能..."), false);
            
            // 测试调试日志功能
            DebugLoggerTest.testDebugLogging();
            
            context.getSource().sendFeedback(
                Text.literal("§a调试日志功能测试完成！请检查服务器日志查看结果"), true);
            return 1;
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("执行调试日志测试时出错", e);
            context.getSource().sendError(Text.literal("§c测试失败: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * 显示调试模式状态
     * 
     * @param context 命令上下文
     * @return 命令执行结果
     */
    private static int showDebugStatus(CommandContext<ServerCommandSource> context) {
        try {
            SimpleConfigManager configManager = (SimpleConfigManager) TokenAuthMod.getInstance().getConfigManager();
            if (configManager != null) {
                boolean debugMode = configManager.getServerConfig().debugMode;
                String status = debugMode ? "§2启用" : "§c禁用";
                
                String message = "§6=== 调试模式状态 ===\n" +
                    "§a调试模式: " + status + "\n" +
                    "§7说明: 调试模式控制详细日志的输出\n" +
                    "§7- 启用时: 显示所有调试信息（[DEBUG]、[VERBOSE]、[AUTH]、[CSV]、[EMAIL]前缀）\n" +
                    "§7- 禁用时: 只显示重要日志，减少日志输出量";
                
                context.getSource().sendFeedback(Text.literal(message), false);
            } else {
                context.getSource().sendError(Text.literal("§c无法获取配置管理器"));
            }
            return 1;
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("显示调试模式状态时出错", e);
            context.getSource().sendError(Text.literal("§c显示状态失败: " + e.getMessage()));
            return 0;
        }
    }
}