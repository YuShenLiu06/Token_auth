package nety.ys.util;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * 网络工具类
 * 提供各种网络相关的实用方法
 * 
 * @author nety.ys
 */
public class NetworkUtil {
    
    /**
     * 获取玩家的IP地址字符串
     * 
     * @param player 服务器玩家实体
     * @return IP地址字符串，如果获取失败则返回"unknown"
     */
    public static String getPlayerIPAddress(ServerPlayerEntity player) {
        try {
            if (player != null && player.networkHandler != null && player.networkHandler.connection != null) {
                return player.networkHandler.connection.getAddress().toString();
            }
        } catch (Exception e) {
            // 忽略异常，返回默认值
        }
        return "unknown";
    }
    
    /**
     * 获取玩家的IP地址（不包含端口）
     * 
     * @param player 服务器玩家实体
     * @return IP地址，如果获取失败则返回null
     */
    public static String getPlayerIPWithoutPort(ServerPlayerEntity player) {
        try {
            if (player != null && player.networkHandler != null && player.networkHandler.connection != null) {
                String address = player.networkHandler.connection.getAddress().toString();
                // 移除端口号
                int colonIndex = address.lastIndexOf(':');
                if (colonIndex > 0) {
                    return address.substring(0, colonIndex);
                }
                return address;
            }
        } catch (Exception e) {
            // 忽略异常，返回默认值
        }
        return null;
    }
    
    /**
     * 向玩家发送消息
     * 
     * @param player 服务器玩家实体
     * @param message 消息内容
     * @param color 颜色代码（可选）
     */
    public static void sendMessageToPlayer(ServerPlayerEntity player, String message, String color) {
        if (player == null || message == null) {
            return;
        }
        
        String formattedMessage = color != null ? color + message : message;
        player.sendMessage(Text.literal(formattedMessage), false);
    }
    
    /**
     * 向玩家发送成功消息
     * 
     * @param player 服务器玩家实体
     * @param message 消息内容
     */
    public static void sendSuccessMessage(ServerPlayerEntity player, String message) {
        sendMessageToPlayer(player, message, "§a");
    }
    
    /**
     * 向玩家发送错误消息
     * 
     * @param player 服务器玩家实体
     * @param message 消息内容
     */
    public static void sendErrorMessage(ServerPlayerEntity player, String message) {
        sendMessageToPlayer(player, message, "§c");
    }
    
    /**
     * 向玩家发送警告消息
     * 
     * @param player 服务器玩家实体
     * @param message 消息内容
     */
    public static void sendWarningMessage(ServerPlayerEntity player, String message) {
        sendMessageToPlayer(player, message, "§e");
    }
    
    /**
     * 向玩家发送信息消息
     * 
     * @param player 服务器玩家实体
     * @param message 消息内容
     */
    public static void sendInfoMessage(ServerPlayerEntity player, String message) {
        sendMessageToPlayer(player, message, "§b");
    }
    
    /**
     * 检查客户端网络处理器是否有效
     * 
     * @param handler 客户端网络处理器
     * @return 如果有效则返回true
     */
    public static boolean isClientNetworkHandlerValid(ClientPlayNetworkHandler handler) {
        return handler != null && handler.getConnection() != null;
    }
    
    /**
     * 格式化IP地址用于显示
     * 
     * @param ipAddress IP地址
     * @return 格式化后的IP地址
     */
    public static String formatIPAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return "unknown";
        }
        
        // 如果IP地址包含端口号，只显示IP部分
        int colonIndex = ipAddress.lastIndexOf(':');
        if (colonIndex > 0) {
            return ipAddress.substring(0, colonIndex);
        }
        
        return ipAddress;
    }
    
    /**
     * 检查IP地址是否为本地地址
     * 
     * @param ipAddress IP地址
     * @return 如果是本地地址则返回true
     */
    public static boolean isLocalAddress(String ipAddress) {
        if (ipAddress == null || ipAddress.isEmpty()) {
            return false;
        }
        
        return ipAddress.startsWith("127.") || 
               ipAddress.startsWith("192.168.") || 
               ipAddress.startsWith("10.") ||
               ipAddress.startsWith("169.254.") ||
               ipAddress.contains("localhost") ||
               ipAddress.equals("::1");
    }
}