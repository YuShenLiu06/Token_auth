package nety.ys.util;

import java.security.SecureRandom;

/**
 * 加密工具类
 * 提供各种加密相关的实用方法
 * 
 * @author nety.ys
 */
public class CryptoUtil {
    
    /**
     * 安全随机数生成器
     */
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    
    /**
     * 生成随机字节数组
     * 
     * @param length 字节数组长度
     * @return 随机字节数组
     */
    public static byte[] generateRandomBytes(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("长度必须大于0");
        }
        
        byte[] bytes = new byte[length];
        SECURE_RANDOM.nextBytes(bytes);
        return bytes;
    }
    
    /**
     * 生成随机字符串
     * 
     * @param length 字符串长度
     * @return 随机字符串
     */
    public static String generateRandomString(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("长度必须大于0");
        }
        
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(length);
        
        for (int i = 0; i < length; i++) {
            sb.append(characters.charAt(SECURE_RANDOM.nextInt(characters.length())));
        }
        
        return sb.toString();
    }
    
    /**
     * 生成随机十六进制字符串
     * 
     * @param length 字符串长度
     * @return 随机十六进制字符串
     */
    public static String generateRandomHexString(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("长度必须大于0");
        }
        
        String characters = "0123456789abcdef";
        StringBuilder sb = new StringBuilder(length);
        
        for (int i = 0; i < length; i++) {
            sb.append(characters.charAt(SECURE_RANDOM.nextInt(characters.length())));
        }
        
        return sb.toString();
    }
    
    /**
     * 常数时间比较，防止时序攻击
     * 
     * @param a 第一个字节数组
     * @param b 第二个字节数组
     * @return 如果两个数组相等则返回true
     */
    public static boolean constantTimeEquals(byte[] a, byte[] b) {
        if (a.length != b.length) {
            return false;
        }
        
        int result = 0;
        for (int i = 0; i < a.length; i++) {
            result |= a[i] ^ b[i];
        }
        
        return result == 0;
    }
    
    /**
     * 将字节数组转换为十六进制字符串
     * 
     * @param bytes 字节数组
     * @return 十六进制字符串
     */
    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        
        return sb.toString();
    }
    
    /**
     * 将十六进制字符串转换为字节数组
     * 
     * @param hex 十六进制字符串
     * @return 字节数组
     */
    public static byte[] hexToBytes(String hex) {
        if (hex == null || hex.length() % 2 != 0) {
            return null;
        }
        
        byte[] bytes = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length(); i += 2) {
            bytes[i / 2] = (byte) Integer.parseInt(hex.substring(i, i + 2), 16);
        }
        
        return bytes;
    }
}