package nety.ys.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import nety.ys.TokenAuthMod;
import nety.ys.config.ModConfig;
import nety.ys.config.SimpleConfigManager;
import nety.ys.util.CSVLoggingTest;
import nety.ys.util.FailedAuthLogger;

/**
 * 简化的令牌管理命令，用于测试CSV记录功能
 * 
 * @author nety.ys
 */
public class TokenCommandSimple {
    
    /**
     * 注册令牌管理命令
     * 
     * @param dispatcher 命令分发器
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // CSV测试命令：/csvtest
        dispatcher.register(CommandManager.literal("csvtest")
            .requires(source -> source.hasPermissionLevel(3)) // 需要OP 3级权限
            .executes(TokenCommandSimple::testCSVLogging));
        
        // CSV状态命令：/csvstatus
        dispatcher.register(CommandManager.literal("csvstatus")
            .requires(source -> source.hasPermissionLevel(3)) // 需要OP 3级权限
            .executes(TokenCommandSimple::showCSVStatus));
        
        // CSV启用命令：/csvenable
        dispatcher.register(CommandManager.literal("csvenable")
            .requires(source -> source.hasPermissionLevel(3)) // 需要OP 3级权限
            .executes(TokenCommandSimple::enableCSVLogging));
        
        // CSV禁用命令：/csvdisable
        dispatcher.register(CommandManager.literal("csvdisable")
            .requires(source -> source.hasPermissionLevel(3)) // 需要OP 3级权限
            .executes(TokenCommandSimple::disableCSVLogging));
        
        // 超时记录启用命令：/csvtimeout
        dispatcher.register(CommandManager.literal("csvtimeout")
            .requires(source -> source.hasPermissionLevel(3)) // 需要OP 3级权限
            .executes(TokenCommandSimple::toggleTimeoutLogging));
    }
    
    /**
     * 测试CSV记录功能命令处理
     * 
     * @param context 命令上下文
     * @return 命令执行结果
     */
    private static int testCSVLogging(CommandContext<ServerCommandSource> context) {
        try {
            context.getSource().sendFeedback(
                Text.literal("§6正在测试CSV记录功能..."), false);
            
            // 在服务器线程中执行测试
            context.getSource().getServer().execute(() -> {
                boolean testResult = CSVLoggingTest.runAllTests();
                
                if (testResult) {
                    context.getSource().sendFeedback(
                        Text.literal("§aCSV记录功能测试通过！"), true);
                } else {
                    context.getSource().sendError(
                        Text.literal("§cCSV记录功能测试失败，请检查日志获取详细信息"));
                }
            });
            
            return 1;
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("测试CSV记录功能时出错", e);
            context.getSource().sendError(Text.literal("§c测试CSV记录功能失败: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * 显示CSV记录状态命令处理
     *
     * @param context 命令上下文
     * @return 命令执行结果
     */
    private static int showCSVStatus(CommandContext<ServerCommandSource> context) {
        try {
            SimpleConfigManager configManager = (SimpleConfigManager) TokenAuthMod.getInstance().getConfigManager();
            ModConfig.ServerConfig config = configManager.getServerConfig();
            
            String status = "§6=== CSV记录状态 ===\n";
            status += "§aCSV记录: " + (config.enableCSVLogging ? "§2启用" : "§c禁用") + "\n";
            status += "§aCSV文件名: §b" + config.csvFileName + "\n";
            status += "§a包含地理位置: " + (config.includeGeoLocation ? "§2是" : "§c否") + "\n";
            
            if (FailedAuthLogger.csvFileExists()) {
                status += "§aCSV文件状态: §2存在\n";
                status += "§aCSV文件路径: §b" + FailedAuthLogger.getCSVFilePathString();
            } else {
                status += "§aCSV文件状态: §c不存在";
            }
            
            context.getSource().sendFeedback(Text.literal(status), false);
            return 1;
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("显示CSV状态时出错", e);
            context.getSource().sendError(Text.literal("§c显示CSV状态失败: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * 启用CSV记录命令处理
     *
     * @param context 命令上下文
     * @return 命令执行结果
     */
    private static int enableCSVLogging(CommandContext<ServerCommandSource> context) {
        try {
            SimpleConfigManager configManager = (SimpleConfigManager) TokenAuthMod.getInstance().getConfigManager();
            ModConfig.ServerConfig config = configManager.getServerConfig();
            config.enableCSVLogging = true;
            
            context.getSource().sendFeedback(
                Text.literal("§aCSV记录已启用，认证失败信息将被记录到CSV文件"), true);
            return 1;
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("启用CSV记录时出错", e);
            context.getSource().sendError(Text.literal("§c启用CSV记录失败: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * 禁用CSV记录命令处理
     *
     * @param context 命令上下文
     * @return 命令执行结果
     */
    private static int disableCSVLogging(CommandContext<ServerCommandSource> context) {
        try {
            SimpleConfigManager configManager = (SimpleConfigManager) TokenAuthMod.getInstance().getConfigManager();
            ModConfig.ServerConfig config = configManager.getServerConfig();
            config.enableCSVLogging = false;
            
            context.getSource().sendFeedback(
                Text.literal("§aCSV记录已禁用，认证失败信息将不再记录到CSV文件"), true);
            return 1;
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("禁用CSV记录时出错", e);
            context.getSource().sendError(Text.literal("§c禁用CSV记录失败: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * 切换超时记录命令处理
     *
     * @param context 命令上下文
     * @return 命令执行结果
     */
    private static int toggleTimeoutLogging(CommandContext<ServerCommandSource> context) {
        try {
            SimpleConfigManager configManager = (SimpleConfigManager) TokenAuthMod.getInstance().getConfigManager();
            ModConfig.ServerConfig config = configManager.getServerConfig();
            config.logTimeoutAttempts = !config.logTimeoutAttempts;
            
            String status = config.logTimeoutAttempts ? "已启用" : "已禁用";
            context.getSource().sendFeedback(
                Text.literal("§a认证超时CSV记录" + status + "，认证超时将" + (config.logTimeoutAttempts ? "会" : "不会") + "被记录到CSV文件"), true);
            return 1;
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("切换超时记录时出错", e);
            context.getSource().sendError(Text.literal("§c切换超时记录失败: " + e.getMessage()));
            return 0;
        }
    }
}