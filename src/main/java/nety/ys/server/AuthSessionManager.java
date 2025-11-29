package nety.ys.server;

import nety.ys.TokenAuthMod;
import nety.ys.config.ModConfig;
import nety.ys.crypto.DynamicTokenGenerator;

import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 认证会话管理器
 * 负责管理服务器端的认证会话、IP阻止和玩家认证状态
 * 
 * @author nety.ys
 */
public class AuthSessionManager {
    
    /**
     * 活跃的认证会话
     */
    private static final Map<String, AuthSession> activeSessions = new ConcurrentHashMap<>();
    
    /**
     * 已认证的玩家
     */
    private static final Set<String> authenticatedPlayers = ConcurrentHashMap.newKeySet();
    
    /**
     * IP失败尝试次数
     */
    private static final Map<String, Integer> failedAttempts = new ConcurrentHashMap<>();
    
    /**
     * 被阻止的IP地址
     */
    private static final Map<String, Long> blockedIPs = new ConcurrentHashMap<>();
    
    /**
     * 定时任务执行器
     */
    private static ScheduledExecutorService scheduler;
    
    /**
     * 动态令牌生成器
     */
    private static DynamicTokenGenerator tokenGenerator;
    
    /**
     * 服务器实例
     */
    private static net.minecraft.server.MinecraftServer server;
    
    /**
     * 初始化认证会话管理器
     */
    public static void initialize() {
        TokenAuthMod.LOGGER.info("初始化认证会话管理器...");
        
        // 获取服务器配置
        ModConfig.ServerConfig config = TokenAuthMod.getInstance().getConfigManager().getServerConfig();
        
        // 初始化令牌生成器
        if (config.isSharedSecretConfigured()) {
            tokenGenerator = new DynamicTokenGenerator(config.getSharedSecretBytes());
        } else {
            TokenAuthMod.LOGGER.warn("共享密钥未配置，令牌生成器未初始化");
        }
        
        // 初始化定时任务执行器
        scheduler = Executors.newScheduledThreadPool(1);
        
        // 启动会话清理任务
        scheduler.scheduleAtFixedRate(AuthSessionManager::cleanupExpiredSessions, 
                                    1, 1, TimeUnit.MINUTES);
        
        // 启动IP阻止清理任务
        scheduler.scheduleAtFixedRate(AuthSessionManager::cleanupExpiredIPBlocks, 
                                    1, 1, TimeUnit.MINUTES);
        
        TokenAuthMod.LOGGER.info("认证会话管理器初始化完成");
    }
    
    /**
     * 服务器启动事件处理
     * 
     * @param server 服务器实例
     */
    public static void onServerStarting(net.minecraft.server.MinecraftServer server) {
        AuthSessionManager.server = server;
        TokenAuthMod.LOGGER.info("服务器启动，认证系统准备就绪");
    }
    
    /**
     * 服务器停止事件处理
     */
    public static void onServerStopped() {
        TokenAuthMod.LOGGER.info("服务器已停止，清理认证会话");
        
        // 清理所有会话
        activeSessions.clear();
        authenticatedPlayers.clear();
        failedAttempts.clear();
        blockedIPs.clear();
        
        // 关闭定时任务执行器
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
    
    /**
     * 创建认证会话
     * 
     * @param connectionId 连接ID
     * @param address 客户端IP地址
     * @return 认证会话，如果创建失败则返回null
     */
    public static AuthSession createSession(String connectionId, InetAddress address) {
        // 检查IP是否被阻止
        if (isIPBlocked(address.toString())) {
            TokenAuthMod.LOGGER.warn("IP地址 {} 被阻止，拒绝创建会话", address.toString());
            return null;
        }
        
        // 检查令牌生成器是否已初始化
        if (tokenGenerator == null) {
            TokenAuthMod.LOGGER.error("令牌生成器未初始化，无法创建会话");
            return null;
        }
        
        // 生成挑战数据
        byte[] challenge = nety.ys.config.KeyGenerator.generateChallenge(
            TokenAuthMod.getInstance().getConfigManager().getServerConfig().challengeSize
        );
        
        // 创建会话
        long timestamp = System.currentTimeMillis();
        AuthSession session = new AuthSession(connectionId, challenge, timestamp, address);
        
        // 存储会话
        activeSessions.put(connectionId, session);
        
        // 设置会话超时
        long timeout = TokenAuthMod.getInstance().getConfigManager().getServerConfig().responseTimeout;
        TokenAuthMod.LOGGER.info("设置会话 {} 超时时间: {} 毫秒", connectionId, timeout);
        scheduler.schedule(() -> {
            AuthSession expiredSession = activeSessions.remove(connectionId);
            if (expiredSession != null) {
                TokenAuthMod.LOGGER.info("会话 {} 已超时，创建时间: {}, 当前时间: {}",
                    connectionId, expiredSession.getTimestamp(), System.currentTimeMillis());
            }
        }, timeout, TimeUnit.MILLISECONDS);
        
        return session;
    }
    
    /**
     * 验证令牌响应
     * 
     * @param connectionId 连接ID
     * @param tokenResponse 令牌响应
     * @param challengeTimestamp 挑战时间戳
     * @param address 客户端IP地址
     * @return 如果验证成功则返回true
     */
    public static boolean verifyTokenResponse(String connectionId, byte[] tokenResponse, 
                                          long challengeTimestamp, InetAddress address) {
        // 检查令牌生成器是否已初始化
        if (tokenGenerator == null) {
            TokenAuthMod.LOGGER.error("令牌生成器未初始化，无法验证令牌");
            return false;
        }
        
        // 获取会话 - 尝试多种方式查找会话
        AuthSession session = activeSessions.get(connectionId);
        if (session == null) {
            TokenAuthMod.LOGGER.warn("未找到连接ID {} 的会话，尝试通过其他方式查找", connectionId);
            
            // 如果直接查找失败，打印所有活跃会话用于调试
            TokenAuthMod.LOGGER.info("当前活跃会话数量: {}", activeSessions.size());
            for (Map.Entry<String, AuthSession> entry : activeSessions.entrySet()) {
                AuthSession s = entry.getValue();
                TokenAuthMod.LOGGER.info("会话 - ID: {}, IP: {}, 时间戳: {}",
                    entry.getKey(), s.getAddress().toString(), s.getTimestamp());
            }
            
            return false;
        }
        
        // 验证时间戳
        if (session.getTimestamp() != challengeTimestamp) {
            TokenAuthMod.LOGGER.warn("时间戳不匹配，会话时间戳: {}，挑战时间戳: {}", 
                                  session.getTimestamp(), challengeTimestamp);
            return false;
        }
        
        // 验证令牌
        ModConfig.ServerConfig config = TokenAuthMod.getInstance().getConfigManager().getServerConfig();
        boolean isValid = tokenGenerator.verifyTokenWithTolerance(
            session.getChallenge(), 
            challengeTimestamp, 
            tokenResponse,
            config.timeWindow
        );
        
        if (isValid) {
            // 移除会话
            activeSessions.remove(connectionId);
            
            // 重置失败尝试次数
            failedAttempts.remove(address.toString());
            
            TokenAuthMod.LOGGER.debug("令牌验证成功，连接ID: {}", connectionId);
        } else {
            TokenAuthMod.LOGGER.warn("令牌验证失败，连接ID: {}", connectionId);
        }
        
        return isValid;
    }
    
    /**
     * 标记玩家为已认证
     * 
     * @param playerId 玩家ID
     */
    public static void markPlayerAsAuthenticated(String playerId) {
        authenticatedPlayers.add(playerId);
    }
    
    /**
     * 检查玩家是否已认证
     * 
     * @param playerId 玩家ID
     * @return 如果玩家已认证则返回true
     */
    public static boolean isPlayerAuthenticated(String playerId) {
        return authenticatedPlayers.contains(playerId);
    }
    
    /**
     * 移除玩家认证状态
     * 
     * @param playerId 玩家ID
     */
    public static void removePlayerAuthentication(String playerId) {
        authenticatedPlayers.remove(playerId);
    }
    
    /**
     * 增加失败尝试次数
     * 
     * @param ipAddress IP地址
     * @return 新的失败尝试次数
     */
    public static int incrementFailedAttempt(String ipAddress) {
        return failedAttempts.merge(ipAddress, 1, Integer::sum);
    }
    
    /**
     * 阻止IP地址
     * 
     * @param ipAddress IP地址
     * @param durationMinutes 阻止持续时间（分钟）
     */
    public static void blockIPAddress(String ipAddress, int durationMinutes) {
        long unblockTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(durationMinutes);
        blockedIPs.put(ipAddress, unblockTime);
        
        // 如果玩家在线，踢出服务器
        if (server != null) {
            server.getPlayerManager().getPlayerList().forEach(player -> {
                if (player.getIp().equals(ipAddress)) {
                    player.networkHandler.disconnect(
                        net.minecraft.text.Text.literal("您的IP地址已被阻止，请稍后再试")
                    );
                }
            });
        }
    }
    
    /**
     * 检查IP是否被阻止
     * 
     * @param ipAddress IP地址
     * @return 如果IP被阻止则返回true
     */
    public static boolean isIPBlocked(String ipAddress) {
        Long unblockTime = blockedIPs.get(ipAddress);
        if (unblockTime == null) {
            return false;
        }
        
        if (System.currentTimeMillis() > unblockTime) {
            blockedIPs.remove(ipAddress);
            return false;
        }
        
        return true;
    }
    
    /**
     * 清理过期的会话
     */
    private static void cleanupExpiredSessions() {
        long currentTime = System.currentTimeMillis();
        long timeout = TokenAuthMod.getInstance().getConfigManager().getServerConfig().responseTimeout;
        
        TokenAuthMod.LOGGER.debug("开始清理过期会话，当前时间: {}, 超时时间: {} 毫秒", currentTime, timeout);
        TokenAuthMod.LOGGER.debug("当前活跃会话数量: {}", activeSessions.size());
        
        Iterator<Map.Entry<String, AuthSession>> iterator = activeSessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, AuthSession> entry = iterator.next();
            AuthSession session = entry.getValue();
            long sessionAge = currentTime - session.getTimestamp();
            
            TokenAuthMod.LOGGER.debug("检查会话: {}, 创建时间: {}, 年龄: {} 毫秒",
                entry.getKey(), session.getTimestamp(), sessionAge);
            
            if (sessionAge > timeout) {
                iterator.remove();
                TokenAuthMod.LOGGER.info("清理过期会话: {}, 年龄: {} 毫秒 (超时: {} 毫秒)",
                    entry.getKey(), sessionAge, timeout);
            }
        }
        
        TokenAuthMod.LOGGER.debug("会话清理完成，剩余活跃会话数量: {}", activeSessions.size());
    }
    
    /**
     * 清理过期的IP阻止
     */
    private static void cleanupExpiredIPBlocks() {
        long currentTime = System.currentTimeMillis();
        
        Iterator<Map.Entry<String, Long>> iterator = blockedIPs.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Long> entry = iterator.next();
            Long unblockTime = entry.getValue();
            
            if (currentTime > unblockTime) {
                iterator.remove();
                TokenAuthMod.LOGGER.debug("移除过期的IP阻止: {}", entry.getKey());
            }
        }
    }
    
    /**
     * 获取活跃会话数量
     * 
     * @return 活跃会话数量
     */
    public static int getActiveSessionCount() {
        return activeSessions.size();
    }
    
    /**
     * 获取已认证玩家数量
     * 
     * @return 已认证玩家数量
     */
    public static int getAuthenticatedPlayerCount() {
        return authenticatedPlayers.size();
    }
    
    /**
     * 获取被阻止的IP数量
     *
     * @return 被阻止的IP数量
     */
    public static int getBlockedIPCount() {
        return blockedIPs.size();
    }
    
    /**
     * 解除阻止IP地址
     *
     * @param ipAddress IP地址
     */
    public static void unblockIPAddress(String ipAddress) {
        blockedIPs.remove(ipAddress);
        TokenAuthMod.LOGGER.info("IP地址 {} 的阻止已解除", ipAddress);
    }
    
    /**
     * 获取被阻止的IP地址列表
     *
     * @return 被阻止的IP地址集合
     */
    public static java.util.Set<String> getBlockedIPs() {
        return new java.util.HashSet<>(blockedIPs.keySet());
    }
    
    /**
     * 获取认证会话
     * 
     * @param connectionId 连接ID
     * @return 认证会话，如果不存在则返回null
     */
    public static AuthSession getSession(String connectionId) {
        return activeSessions.get(connectionId);
    }
    
    /**
     * 认证会话类
     */
    public static class AuthSession {
        private final String connectionId;
        private final byte[] challenge;
        private final long timestamp;
        private final InetAddress address;
        
        public AuthSession(String connectionId, byte[] challenge, long timestamp, InetAddress address) {
            this.connectionId = connectionId;
            this.challenge = challenge;
            this.timestamp = timestamp;
            this.address = address;
        }
        
        public String getConnectionId() {
            return connectionId;
        }
        
        public byte[] getChallenge() {
            return challenge;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public InetAddress getAddress() {
            return address;
        }
    }
}