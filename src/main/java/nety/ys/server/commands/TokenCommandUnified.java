package nety.ys.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import nety.ys.TokenAuthMod;
import nety.ys.config.KeyGenerator;
import nety.ys.config.ModConfig;
import nety.ys.config.SimpleConfigManager;
import nety.ys.server.AuthSessionManager;
import nety.ys.util.EmailAlertTest;
import nety.ys.util.EmailNotifier;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 统一的令牌管理命令类
 * 提供统一的命令格式：/token config <xxx> true/false/<值>, /token, /token debug <命令>
 * 
 * @author nety.ys
 */
public class TokenCommandUnified {
    
    /**
     * 注册令牌管理命令
     *
     * @param dispatcher 命令分发器
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("token")
            .requires(source -> source.hasPermissionLevel(3)) // 需要OP 3级权限
            .executes(TokenCommandUnified::showCommandHelp)
            // 配置子命令：/token config <xxx> <value>
            .then(CommandManager.literal("config")
                .then(CommandManager.literal("enabled")
                    .then(CommandManager.argument("value", BoolArgumentType.bool())
                        .executes(context -> setConfigValue(context, "enabled", BoolArgumentType.getBool(context, "value")))))
                .then(CommandManager.literal("maxAttemptsPerIP")
                    .then(CommandManager.argument("value", IntegerArgumentType.integer(1, 20))
                        .executes(context -> setConfigValue(context, "maxAttemptsPerIP", IntegerArgumentType.getInteger(context, "value")))))
                .then(CommandManager.literal("blockDurationMinutes")
                    .then(CommandManager.argument("value", IntegerArgumentType.integer(1, 1440))
                        .executes(context -> setConfigValue(context, "blockDurationMinutes", IntegerArgumentType.getInteger(context, "value")))))
                .then(CommandManager.literal("enableAuthLogging")
                    .then(CommandManager.argument("value", BoolArgumentType.bool())
                        .executes(context -> setConfigValue(context, "enableAuthLogging", BoolArgumentType.getBool(context, "value")))))
                .then(CommandManager.literal("enableCSVLogging")
                    .then(CommandManager.argument("value", BoolArgumentType.bool())
                        .executes(context -> setConfigValue(context, "enableCSVLogging", BoolArgumentType.getBool(context, "value")))))
                .then(CommandManager.literal("includeGeoLocation")
                    .then(CommandManager.argument("value", BoolArgumentType.bool())
                        .executes(context -> setConfigValue(context, "includeGeoLocation", BoolArgumentType.getBool(context, "value")))))
                .then(CommandManager.literal("logTimeoutAttempts")
                    .then(CommandManager.argument("value", BoolArgumentType.bool())
                        .executes(context -> setConfigValue(context, "logTimeoutAttempts", BoolArgumentType.getBool(context, "value")))))
                .then(CommandManager.literal("enableEmailAlerts")
                    .then(CommandManager.argument("value", BoolArgumentType.bool())
                        .executes(context -> setConfigValue(context, "enableEmailAlerts", BoolArgumentType.getBool(context, "value")))))
                .then(CommandManager.literal("smtpHost")
                    .then(CommandManager.argument("value", StringArgumentType.string())
                        .executes(context -> setConfigValue(context, "smtpHost", StringArgumentType.getString(context, "value")))))
                .then(CommandManager.literal("smtpPort")
                    .then(CommandManager.argument("value", StringArgumentType.string())
                        .executes(context -> setConfigValue(context, "smtpPort", StringArgumentType.getString(context, "value")))))
                .then(CommandManager.literal("enableSSL")
                    .then(CommandManager.argument("value", BoolArgumentType.bool())
                        .executes(context -> setConfigValue(context, "enableSSL", BoolArgumentType.getBool(context, "value")))))
                .then(CommandManager.literal("smtpUsername")
                    .then(CommandManager.argument("value", StringArgumentType.string())
                        .executes(context -> setConfigValue(context, "smtpUsername", StringArgumentType.getString(context, "value")))))
                .then(CommandManager.literal("smtpPassword")
                    .then(CommandManager.argument("value", StringArgumentType.string())
                        .executes(context -> setConfigValue(context, "smtpPassword", StringArgumentType.getString(context, "value")))))
                .then(CommandManager.literal("emailFromAddress")
                    .then(CommandManager.argument("value", StringArgumentType.string())
                        .executes(context -> setConfigValue(context, "emailFromAddress", StringArgumentType.getString(context, "value")))))
                .then(CommandManager.literal("emailToAddress")
                    .then(CommandManager.argument("value", StringArgumentType.string())
                        .executes(context -> setConfigValue(context, "emailToAddress", StringArgumentType.getString(context, "value")))))
                .then(CommandManager.literal("serverName")
                    .then(CommandManager.argument("value", StringArgumentType.string())
                        .executes(context -> setConfigValue(context, "serverName", StringArgumentType.getString(context, "value"))))))
            // 调试子命令：/token debug <command>
            .then(CommandManager.literal("debug")
                .then(CommandManager.literal("email")
                    .executes(TokenCommandUnified::debugEmailTest))
                .then(CommandManager.literal("csv")
                    .executes(TokenCommandUnified::debugCSVTest))
                .then(CommandManager.literal("auth")
                    .executes(TokenCommandUnified::debugAuthTest)))
            // 其他子命令
            .then(CommandManager.literal("reload")
                .executes(TokenCommandUnified::reloadConfig))
            .then(CommandManager.literal("generate-key")
                .executes(TokenCommandUnified::generateNewKey))
            .then(CommandManager.literal("status")
                .executes(TokenCommandUnified::showStatus))
            .then(CommandManager.literal("block-ip")
                .then(CommandManager.argument("ip", StringArgumentType.string())
                    .executes(context -> blockIP(context, StringArgumentType.getString(context, "ip"), 30))
                    .then(CommandManager.argument("minutes", IntegerArgumentType.integer(1, 1440))
                        .executes(context -> blockIP(context,
                            StringArgumentType.getString(context, "ip"),
                            IntegerArgumentType.getInteger(context, "minutes"))))))
            .then(CommandManager.literal("unblock-ip")
                .then(CommandManager.argument("ip", StringArgumentType.string())
                    .executes(context -> unblockIP(context, StringArgumentType.getString(context, "ip")))))
            .then(CommandManager.literal("list-blocked-ips")
                .executes(TokenCommandUnified::listBlockedIPs))
            .then(CommandManager.literal("list-authenticated")
                .executes(TokenCommandUnified::listAuthenticatedPlayers))
            .then(CommandManager.literal("remove-auth")
                .then(CommandManager.argument("player", StringArgumentType.string())
                    .executes(context -> removeAuthentication(context, StringArgumentType.getString(context, "player")))))
        );
    }
    
    /**
     * 显示命令帮助
     * 
     * @param context 命令上下文
     * @return 命令执行结果
     */
    private static int showCommandHelp(CommandContext<ServerCommandSource> context) {
        MutableText help = Text.literal("§6=== Token Auth Mod 命令帮助 ===\n\n");
        
        help.append(Text.literal("§a基本命令:\n"));
        help.append(Text.literal("§e/token §7- 显示此帮助信息\n"));
        help.append(Text.literal("§e/token reload §7- 重新加载配置\n"));
        help.append(Text.literal("§e/token generate-key §7- 生成新的共享密钥\n"));
        help.append(Text.literal("§e/token status §7- 显示系统状态\n\n"));
        
        help.append(Text.literal("§a配置命令:\n"));
        help.append(Text.literal("§e/token config <配置项> <值> §7- 设置配置项\n"));
        help.append(Text.literal("§7可用配置项: enabled, maxAttemptsPerIP, blockDurationMinutes, enableAuthLogging, enableCSVLogging, includeGeoLocation, logTimeoutAttempts, enableEmailAlerts, smtpHost, smtpPort, enableSSL, smtpUsername, smtpPassword, emailFromAddress, emailToAddress, serverName\n\n"));
        
        help.append(Text.literal("§a调试命令:\n"));
        help.append(Text.literal("§e/token debug email §7- 测试邮件发送功能\n"));
        help.append(Text.literal("§e/token debug csv §7- 测试CSV记录功能\n"));
        help.append(Text.literal("§e/token debug auth §7- 测试认证系统\n\n"));
        
        help.append(Text.literal("§aIP管理命令:\n"));
        help.append(Text.literal("§e/token block-ip <IP> [分钟] §7- 阻止IP地址\n"));
        help.append(Text.literal("§e/token unblock-ip <IP> §7- 解除阻止IP地址\n"));
        help.append(Text.literal("§e/token list-blocked-ips §7- 列出被阻止的IP\n\n"));
        
        help.append(Text.literal("§a玩家管理命令:\n"));
        help.append(Text.literal("§e/token list-authenticated §7- 列出已认证的玩家\n"));
        help.append(Text.literal("§e/token remove-auth <玩家> §7- 移除玩家认证状态\n"));
        
        context.getSource().sendFeedback(help, false);
        return 1;
    }
    
    /**
     * 设置配置值
     * 
     * @param context 命令上下文
     * @param configKey 配置键
     * @param value 配置值
     * @return 命令执行结果
     */
    private static int setConfigValue(CommandContext<ServerCommandSource> context, String configKey, Object value) {
        try {
            SimpleConfigManager configManager = (SimpleConfigManager) TokenAuthMod.getInstance().getConfigManager();
            ModConfig.ServerConfig config = configManager.getServerConfig();
            
            // 添加空值检查，防止在客户端环境中出现NullPointerException
            if (config == null) {
                context.getSource().sendError(Text.literal("§c服务器配置未加载，无法设置配置项"));
                return 0;
            }
            
            switch (configKey) {
                case "enabled":
                    config.enabled = (Boolean) value;
                    context.getSource().sendFeedback(Text.literal("§a认证系统已" + ((Boolean) value ? "启用" : "禁用")), true);
                    break;
                case "maxAttemptsPerIP":
                    config.maxAttemptsPerIP = (Integer) value;
                    context.getSource().sendFeedback(Text.literal("§a最大尝试次数/IP已设置为: §b" + value), true);
                    break;
                case "blockDurationMinutes":
                    config.blockDurationMinutes = (Integer) value;
                    context.getSource().sendFeedback(Text.literal("§a阻止持续时间已设置为: §b" + value + " 分钟"), true);
                    break;
                case "enableAuthLogging":
                    config.enableAuthLogging = (Boolean) value;
                    context.getSource().sendFeedback(Text.literal("§a认证日志已" + ((Boolean) value ? "启用" : "禁用")), true);
                    break;
                case "enableCSVLogging":
                    config.enableCSVLogging = (Boolean) value;
                    context.getSource().sendFeedback(Text.literal("§aCSV记录已" + ((Boolean) value ? "启用" : "禁用")), true);
                    break;
                case "includeGeoLocation":
                    config.includeGeoLocation = (Boolean) value;
                    context.getSource().sendFeedback(Text.literal("§a地理位置记录已" + ((Boolean) value ? "启用" : "禁用")), true);
                    break;
                case "logTimeoutAttempts":
                    config.logTimeoutAttempts = (Boolean) value;
                    context.getSource().sendFeedback(Text.literal("§a超时尝试记录已" + ((Boolean) value ? "启用" : "禁用")), true);
                    break;
                case "enableEmailAlerts":
                    config.enableEmailAlerts = (Boolean) value;
                    context.getSource().sendFeedback(Text.literal("§a邮件警报已" + ((Boolean) value ? "启用" : "禁用")), true);
                    break;
                case "smtpHost":
                    config.smtpHost = (String) value;
                    context.getSource().sendFeedback(Text.literal("§aSMTP服务器已设置为: §b" + value), true);
                    break;
                case "smtpPort":
                    config.smtpPort = (String) value;
                    context.getSource().sendFeedback(Text.literal("§aSMTP端口已设置为: §b" + value), true);
                    break;
                case "enableSSL":
                    config.enableSSL = (Boolean) value;
                    context.getSource().sendFeedback(Text.literal("§aSSL连接已" + ((Boolean) value ? "启用" : "禁用")), true);
                    break;
                case "smtpUsername":
                    config.smtpUsername = (String) value;
                    context.getSource().sendFeedback(Text.literal("§aSMTP用户名已设置为: §b" + value), true);
                    break;
                case "smtpPassword":
                    config.smtpPassword = (String) value;
                    context.getSource().sendFeedback(Text.literal("§aSMTP密码已更新"), true);
                    break;
                case "emailFromAddress":
                    config.emailFromAddress = (String) value;
                    context.getSource().sendFeedback(Text.literal("§a发件人邮箱已设置为: §b" + value), true);
                    break;
                case "emailToAddress":
                    config.emailToAddress = (String) value;
                    context.getSource().sendFeedback(Text.literal("§a收件人邮箱已设置为: §b" + value), true);
                    break;
                case "serverName":
                    config.serverName = (String) value;
                    context.getSource().sendFeedback(Text.literal("§a服务器名称已设置为: §b" + value), true);
                    break;
                default:
                    context.getSource().sendError(Text.literal("§c未知的配置项: " + configKey));
                    return 0;
            }
            
            return 1;
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("设置配置值时出错", e);
            context.getSource().sendError(Text.literal("§c设置配置失败: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * 调试邮件测试命令处理
     * 
     * @param context 命令上下文
     * @return 命令执行结果
     */
    private static int debugEmailTest(CommandContext<ServerCommandSource> context) {
        try {
            SimpleConfigManager configManager = (SimpleConfigManager) TokenAuthMod.getInstance().getConfigManager();
            ModConfig.ServerConfig config = configManager.getServerConfig();
            
            context.getSource().sendFeedback(Text.literal("§6开始测试邮件发送功能..."), false);
            
            // 在服务器线程中执行测试
            context.getSource().getServer().execute(() -> {
                try {
                    // 创建邮件配置
                    EmailNotifier.EmailConfig emailConfig = new EmailNotifier.EmailConfig(
                        config.smtpHost,
                        config.smtpPort,
                        config.smtpUsername,
                        config.smtpPassword,
                        config.emailFromAddress,
                        config.emailToAddress,
                        config.enableSSL
                    );
                    
                    // 发送测试邮件
                    EmailAlertTest.testEmailSending(
                        config.smtpHost,
                        config.smtpPort,
                        config.smtpUsername,
                        config.smtpPassword,
                        config.emailFromAddress,
                        config.emailToAddress,
                        config.serverName,
                        config.enableSSL
                    ).thenAccept(success -> {
                        if (success) {
                            context.getSource().sendFeedback(
                                Text.literal("§a邮件发送测试成功！请检查邮箱"), true);
                        } else {
                            context.getSource().sendError(
                                Text.literal("§c邮件发送测试失败，请检查配置和日志"));
                        }
                    });
                } catch (Exception e) {
                    TokenAuthMod.LOGGER.error("测试邮件发送时出错", e);
                    context.getSource().sendError(Text.literal("§c测试邮件发送失败: " + e.getMessage()));
                }
            });
            
            return 1;
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("调试邮件测试时出错", e);
            context.getSource().sendError(Text.literal("§c调试邮件测试失败: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * 调试CSV测试命令处理
     * 
     * @param context 命令上下文
     * @return 命令执行结果
     */
    private static int debugCSVTest(CommandContext<ServerCommandSource> context) {
        try {
            context.getSource().sendFeedback(Text.literal("§6开始测试CSV记录功能..."), false);
            
            // 在服务器线程中执行测试
            context.getSource().getServer().execute(() -> {
                try {
                    nety.ys.util.CSVLoggingTest.runAllTests();
                    context.getSource().sendFeedback(
                        Text.literal("§aCSV记录功能测试完成！请检查日志获取详细信息"), true);
                } catch (Exception e) {
                    TokenAuthMod.LOGGER.error("测试CSV记录时出错", e);
                    context.getSource().sendError(Text.literal("§c测试CSV记录失败: " + e.getMessage()));
                }
            });
            
            return 1;
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("调试CSV测试时出错", e);
            context.getSource().sendError(Text.literal("§c调试CSV测试失败: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * 调试认证测试命令处理
     * 
     * @param context 命令上下文
     * @return 命令执行结果
     */
    private static int debugAuthTest(CommandContext<ServerCommandSource> context) {
        try {
            context.getSource().sendFeedback(Text.literal("§6开始测试认证系统..."), false);
            
            // 在服务器线程中执行测试
            context.getSource().getServer().execute(() -> {
                try {
                    nety.ys.util.DebugLoggerTest.testDebugLogging();
                    context.getSource().sendFeedback(
                        Text.literal("§a认证系统测试完成！请检查日志获取详细信息"), true);
                } catch (Exception e) {
                    TokenAuthMod.LOGGER.error("测试认证系统时出错", e);
                    context.getSource().sendError(Text.literal("§c测试认证系统失败: " + e.getMessage()));
                }
            });
            
            return 1;
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("调试认证测试时出错", e);
            context.getSource().sendError(Text.literal("§c调试认证测试失败: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * 重载配置命令处理
     * 
     * @param context 命令上下文
     * @return 命令执行结果
     */
    private static int reloadConfig(CommandContext<ServerCommandSource> context) {
        try {
            TokenAuthMod.getInstance().getConfigManager().reloadServerConfig();
            context.getSource().sendFeedback(Text.literal("§a配置已重新加载"), true);
            return 1;
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("重载配置时出错", e);
            context.getSource().sendError(Text.literal("§c重载配置失败: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * 生成新密钥命令处理
     * 
     * @param context 命令上下文
     * @return 命令执行结果
     */
    private static int generateNewKey(CommandContext<ServerCommandSource> context) {
        try {
            String newKey = KeyGenerator.generateSharedSecret();
            
            // 创建可点击的文本组件
            MutableText keyText = Text.literal("§e" + newKey);
            keyText.setStyle(keyText.getStyle()
                .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, newKey))
                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal("点击复制密钥")))
                .withFormatting(Formatting.UNDERLINE));
            
            MutableText message = Text.literal("§a生成的共享密钥: ");
            message.append(keyText);
            message.append(Text.literal("\n§6请将此密钥同时配置到服务器和客户端配置文件中"));
            
            context.getSource().sendFeedback(message, true);
            return 1;
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("生成密钥时出错", e);
            context.getSource().sendError(Text.literal("§c生成密钥失败: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * 显示状态命令处理
     * 
     * @param context 命令上下文
     * @return 命令执行结果
     */
    private static int showStatus(CommandContext<ServerCommandSource> context) {
        try {
            ModConfig.ServerConfig config = TokenAuthMod.getInstance().getConfigManager().getServerConfig();
            
            MutableText status = Text.literal("§6=== Token Auth Mod 状态 ===\n");
            
            // 添加空值检查，防止在客户端环境中出现NullPointerException
            String authStatus = "§c未知";
            if (config == null) {
                authStatus = "§c配置未加载";
            } else {
                authStatus = config.enabled ? "§2启用" : "§c禁用";
            }
            
            // 认证系统状态
            status.append(Text.literal("§a认证系统: " + authStatus + "\n"));
            
            // 共享密钥状态
            status.append(Text.literal("§a共享密钥: " + (config.isSharedSecretConfigured() ? "§2已配置" : "§c未配置") + "\n"));
            
            // 会话统计
            status.append(Text.literal("§a活跃会话数: §b" + AuthSessionManager.getActiveSessionCount() + "\n"));
            status.append(Text.literal("§a已认证玩家数: §b" + AuthSessionManager.getAuthenticatedPlayerCount() + "\n"));
            status.append(Text.literal("§a被阻止IP数: §b" + AuthSessionManager.getBlockedIPCount() + "\n"));
            
            // 安全设置
            status.append(Text.literal("§a最大尝试次数/IP: §b" + config.maxAttemptsPerIP + "\n"));
            status.append(Text.literal("§a阻止持续时间: §b" + config.blockDurationMinutes + " 分钟\n"));
            
            // 日志设置
            status.append(Text.literal("§a认证日志: " + (config.enableAuthLogging ? "§2启用" : "§c禁用") + "\n"));
            
            // CSV记录设置
            status.append(Text.literal("§aCSV记录: " + (config.enableCSVLogging ? "§2启用" : "§c禁用") + "\n"));
            
            // 邮件警报设置
            status.append(Text.literal("§a邮件警报: " + (config.enableEmailAlerts ? "§2启用" : "§c禁用") + "\n"));
            status.append(Text.literal("§aSMTP服务器: §b" + (config.smtpHost.isEmpty() ? "未配置" : config.smtpHost) + "\n"));
            status.append(Text.literal("§a发件人: §b" + (config.emailFromAddress.isEmpty() ? "未配置" : config.emailFromAddress) + "\n"));
            status.append(Text.literal("§a收件人: §b" + (config.emailToAddress.isEmpty() ? "未配置" : config.emailToAddress)));
            
            context.getSource().sendFeedback(status, false);
            return 1;
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("显示状态时出错", e);
            context.getSource().sendError(Text.literal("§c显示状态失败: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * 阻止IP命令处理
     * 
     * @param context 命令上下文
     * @param ipAddress IP地址
     * @param minutes 阻止持续时间（分钟）
     * @return 命令执行结果
     */
    private static int blockIP(CommandContext<ServerCommandSource> context, String ipAddress, int minutes) {
        try {
            AuthSessionManager.blockIPAddress(ipAddress, minutes);
            context.getSource().sendFeedback(
                Text.literal("§aIP地址 §e" + ipAddress + " §a已被阻止 §b" + minutes + " §a分钟"), true);
            return 1;
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("阻止IP时出错", e);
            context.getSource().sendError(Text.literal("§c阻止IP失败: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * 解除阻止IP命令处理
     * 
     * @param context 命令上下文
     * @param ipAddress IP地址
     * @return 命令执行结果
     */
    private static int unblockIP(CommandContext<ServerCommandSource> context, String ipAddress) {
        try {
            AuthSessionManager.unblockIPAddress(ipAddress);
            context.getSource().sendFeedback(
                Text.literal("§aIP地址 §e" + ipAddress + " §a的阻止已解除"), true);
            return 1;
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("解除阻止IP时出错", e);
            context.getSource().sendError(Text.literal("§c解除阻止IP失败: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * 列出被阻止的IP命令处理
     * 
     * @param context 命令上下文
     * @return 命令执行结果
     */
    private static int listBlockedIPs(CommandContext<ServerCommandSource> context) {
        try {
            java.util.Set<String> blockedIPs = AuthSessionManager.getBlockedIPs();
            
            if (blockedIPs.isEmpty()) {
                context.getSource().sendFeedback(
                    Text.literal("§6当前没有被阻止的IP地址"), false);
            } else {
                MutableText message = Text.literal("§6被阻止的IP地址:\n");
                for (String ipAddress : blockedIPs) {
                    message.append(Text.literal("§c- §e" + ipAddress + "\n"));
                }
                context.getSource().sendFeedback(message, false);
            }
            
            return 1;
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("列出被阻止的IP时出错", e);
            context.getSource().sendError(Text.literal("§c列出被阻止的IP失败: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * 列出已认证玩家命令处理
     * 
     * @param context 命令上下文
     * @return 命令执行结果
     */
    private static int listAuthenticatedPlayers(CommandContext<ServerCommandSource> context) {
        try {
            Collection<ServerPlayerEntity> players = context.getSource().getServer().getPlayerManager().getPlayerList();
            
            List<String> authenticatedPlayers = players.stream()
                .filter(player -> AuthSessionManager.isPlayerAuthenticated(player.getUuid().toString()))
                .map(player -> player.getName().getString())
                .collect(Collectors.toList());
            
            if (authenticatedPlayers.isEmpty()) {
                context.getSource().sendFeedback(Text.literal("§6当前没有已认证的玩家"), false);
            } else {
                MutableText message = Text.literal("§6已认证的玩家:\n");
                for (String playerName : authenticatedPlayers) {
                    message.append(Text.literal("§a- §b" + playerName + "\n"));
                }
                context.getSource().sendFeedback(message, false);
            }
            
            return 1;
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("列出已认证玩家时出错", e);
            context.getSource().sendError(Text.literal("§c列出已认证玩家失败: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * 移除玩家认证命令处理
     * 
     * @param context 命令上下文
     * @param playerName 玩家名称
     * @return 命令执行结果
     */
    private static int removeAuthentication(CommandContext<ServerCommandSource> context, String playerName) {
        try {
            ServerPlayerEntity player = context.getSource().getServer().getPlayerManager().getPlayer(playerName);
            
            if (player == null) {
                context.getSource().sendError(Text.literal("§c玩家 " + playerName + " 不在线"));
                return 0;
            }
            
            AuthSessionManager.removePlayerAuthentication(player.getUuid().toString());
            context.getSource().sendFeedback(
                Text.literal("§a已移除玩家 §b" + playerName + " §a的认证状态"), true);
            return 1;
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("移除玩家认证时出错", e);
            context.getSource().sendError(Text.literal("§c移除玩家认证失败: " + e.getMessage()));
            return 0;
        }
    }
}