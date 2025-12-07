package nety.ys.server.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
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
import nety.ys.util.EmailNotifier;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 令牌管理命令
 * 提供重载配置、生成密钥和查看状态等功能
 * 
 * @author nety.ys
 */
public class TokenCommand {
    
    /**
     * 注册令牌管理命令
     * 
     * @param dispatcher 命令分发器
     */
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // 主命令：/token
        dispatcher.register(CommandManager.literal("token")
            .requires(source -> source.hasPermissionLevel(3)) // 需要OP 3级权限
            .then(CommandManager.literal("reload")
                .executes(TokenCommand::reloadConfig))
            .then(CommandManager.literal("generate-key")
                .executes(TokenCommand::generateNewKey))
            .then(CommandManager.literal("status")
                .executes(TokenCommand::showStatus))
            .then(CommandManager.literal("block-ip")
                .then(CommandManager.argument("ip", com.mojang.brigadier.arguments.StringArgumentType.string())
                    .executes(context -> blockIP(context, com.mojang.brigadier.arguments.StringArgumentType.getString(context, "ip"), 30))
                    .then(CommandManager.argument("minutes", IntegerArgumentType.integer(1, 1440))
                        .executes(context -> blockIP(context, 
                            com.mojang.brigadier.arguments.StringArgumentType.getString(context, "ip"),
                            IntegerArgumentType.getInteger(context, "minutes"))))))
            .then(CommandManager.literal("unblock-ip")
                .then(CommandManager.argument("ip", com.mojang.brigadier.arguments.StringArgumentType.string())
                    .executes(context -> unblockIP(context,
                        com.mojang.brigadier.arguments.StringArgumentType.getString(context, "ip")))))
            .then(CommandManager.literal("list-blocked-ips")
                .executes(TokenCommand::listBlockedIPs))
            .then(CommandManager.literal("list-authenticated")
                .executes(TokenCommand::listAuthenticatedPlayers))
            .then(CommandManager.literal("remove-auth")
                .then(CommandManager.argument("player", com.mojang.brigadier.arguments.StringArgumentType.string())
                    .executes(context -> removeAuthentication(context,
                        com.mojang.brigadier.arguments.StringArgumentType.getString(context, "player")))))
            .then(CommandManager.literal("enable-email")
                .executes(TokenCommand::enableEmailAlerts))
            .then(CommandManager.literal("disable-email")
                .executes(TokenCommand::disableEmailAlerts))
            .then(CommandManager.literal("test-email")
                .executes(TokenCommand::testEmailAlert))
        );
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
            
            // 认证系统状态
            status.append(Text.literal("§a认证系统: " + (config.enabled ? "§2启用" : "§c禁用") + "\n"));
            
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
    
    /**
     * 启用邮件警报命令处理
     *
     * @param context 命令上下文
     * @return 命令执行结果
     */
    private static int enableEmailAlerts(CommandContext<ServerCommandSource> context) {
        try {
            SimpleConfigManager configManager = (SimpleConfigManager) TokenAuthMod.getInstance().getConfigManager();
            ModConfig.ServerConfig config = configManager.getServerConfig();
            config.enableEmailAlerts = true;
            
            context.getSource().sendFeedback(
                Text.literal("§a邮件警报已启用，认证失败和超时将发送邮件通知"), true);
            return 1;
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("启用邮件警报时出错", e);
            context.getSource().sendError(Text.literal("§c启用邮件警报失败: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * 禁用邮件警报命令处理
     *
     * @param context 命令上下文
     * @return 命令执行结果
     */
    private static int disableEmailAlerts(CommandContext<ServerCommandSource> context) {
        try {
            SimpleConfigManager configManager = (SimpleConfigManager) TokenAuthMod.getInstance().getConfigManager();
            ModConfig.ServerConfig config = configManager.getServerConfig();
            config.enableEmailAlerts = false;
            
            context.getSource().sendFeedback(
                Text.literal("§a邮件警报已禁用，认证失败和超时将不再发送邮件通知"), true);
            return 1;
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("禁用邮件警报时出错", e);
            context.getSource().sendError(Text.literal("§c禁用邮件警报失败: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * 测试邮件警报命令处理
     *
     * @param context 命令上下文
     * @return 命令执行结果
     */
    private static int testEmailAlert(CommandContext<ServerCommandSource> context) {
        try {
            SimpleConfigManager configManager = (SimpleConfigManager) TokenAuthMod.getInstance().getConfigManager();
            ModConfig.ServerConfig config = configManager.getServerConfig();
            
            // 检查邮件配置是否有效
            if (!config.enableEmailAlerts) {
                context.getSource().sendError(Text.literal("§c邮件警报功能未启用，请先使用 /token enable-email 启用"));
                return 0;
            }
            
            // 检查邮件配置是否完整
            if (config.smtpHost.isEmpty() || config.smtpUsername.isEmpty() ||
                config.emailFromAddress.isEmpty() || config.emailToAddress.isEmpty()) {
                context.getSource().sendError(Text.literal("§c邮件配置不完整，请检查配置文件"));
                return 0;
            }
            
            context.getSource().sendFeedback(
                Text.literal("§6正在发送测试邮件..."), false);
            
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
                    EmailNotifier.sendIntrusionAlert(
                        config.serverName,
                        "测试玩家",
                        java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss")),
                        "127.0.0.1",
                        "测试位置",
                        "邮件功能测试",
                        emailConfig
                    ).thenAccept(success -> {
                        if (success) {
                            context.getSource().sendFeedback(
                                Text.literal("§a测试邮件发送成功！"), true);
                        } else {
                            context.getSource().sendError(
                                Text.literal("§c测试邮件发送失败，请检查配置和日志"));
                        }
                    });
                } catch (Exception e) {
                    TokenAuthMod.LOGGER.error("发送测试邮件时出错", e);
                    context.getSource().sendError(Text.literal("§c发送测试邮件失败: " + e.getMessage()));
                }
            });
            
            return 1;
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("测试邮件警报时出错", e);
            context.getSource().sendError(Text.literal("§c测试邮件警报失败: " + e.getMessage()));
            return 0;
        }
    }
}