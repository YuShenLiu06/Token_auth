package nety.ys.config;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * 密钥生成工具类
 * 用于生成安全的共享密钥和随机挑战数据
 * 
 * @author nety.ys
 */
public class KeyGenerator {
    
    /**
     * 安全随机数生成器
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    
    /**
     * 默认密钥长度（位）
     */
    public static final int DEFAULT_KEY_LENGTH_BITS = 256;
    
    /**
     * 默认密钥长度（字节）
     */
    public static final int DEFAULT_KEY_LENGTH_BYTES = DEFAULT_KEY_LENGTH_BITS / 8;
    
    /**
     * 生成共享密钥
     * 
     * @return Base64编码的共享密钥字符串
     */
    public static String generateSharedSecret() {
        return generateSharedSecret(DEFAULT_KEY_LENGTH_BYTES);
    }
    
    /**
     * 生成指定长度的共享密钥
     * 
     * @param lengthBytes 密钥长度（字节）
     * @return Base64编码的共享密钥字符串
     */
    public static String generateSharedSecret(int lengthBytes) {
        byte[] key = new byte[lengthBytes];
        SECURE_RANDOM.nextBytes(key);
        return Base64.getEncoder().encodeToString(key);
    }
    
    /**
     * 生成随机挑战数据
     * 
     * @param length 挑战数据长度（字节）
     * @return 随机挑战数据
     */
    public static byte[] generateChallenge(int length) {
        byte[] challenge = new byte[length];
        SECURE_RANDOM.nextBytes(challenge);
        return challenge;
    }
    
    /**
     * 生成默认长度的随机挑战数据
     * 
     * @return 随机挑战数据
     */
    public static byte[] generateChallenge() {
        return generateChallenge(16); // 默认16字节
    }
    
    /**
     * 生成随机字符串
     * 
     * @param length 字符串长度
     * @return 随机字符串
     */
    public static String generateRandomString(int length) {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        
        for (int i = 0; i < length; i++) {
            int index = SECURE_RANDOM.nextInt(characters.length());
            sb.append(characters.charAt(index));
        }
        
        return sb.toString();
    }
    
    /**
     * 验证密钥格式是否有效（Base64编码）
     * 
     * @param secret Base64编码的密钥字符串
     * @return 如果密钥格式有效则返回true
     */
    public static boolean isValidSecret(String secret) {
        if (secret == null || secret.isEmpty()) {
            return false;
        }
        
        try {
            byte[] decoded = Base64.getDecoder().decode(secret);
            return decoded.length > 0;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
    
    /**
     * 主方法，用于生成密钥
     * 可以直接运行此类来生成新的共享密钥
     * 
     * @param args 命令行参数
     */
    public static void main(String[] args) {
        System.out.println("=== Token Auth Mod 密钥生成工具 ===");
        
        // 生成默认长度的共享密钥
        String sharedSecret = generateSharedSecret();
        System.out.println("生成的共享密钥: " + sharedSecret);
        System.out.println("密钥长度: " + DEFAULT_KEY_LENGTH_BITS + " 位 (" + DEFAULT_KEY_LENGTH_BYTES + " 字节)");
        
        // 验证密钥
        boolean isValid = isValidSecret(sharedSecret);
        System.out.println("密钥有效性: " + (isValid ? "有效" : "无效"));
        
        // 解码并显示密钥的字节表示
        if (isValid) {
            byte[] decoded = Base64.getDecoder().decode(sharedSecret);
            System.out.println("解码后的字节长度: " + decoded.length + " 字节");
        }
        
        System.out.println("\n请将此密钥同时配置到服务器和客户端配置文件中");
        System.out.println("服务器配置文件: config/token-auth/token-auth-server.toml");
        System.out.println("客户端配置文件: config/token-auth/token-auth-client.toml");
        
        // 生成示例挑战数据
        byte[] challenge = generateChallenge();
        System.out.println("\n示例挑战数据: " + Base64.getEncoder().encodeToString(challenge));
    }
}