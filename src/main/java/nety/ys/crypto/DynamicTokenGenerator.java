package nety.ys.crypto;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeUnit;

/**
 * 动态令牌生成器
 * 使用HMAC-SHA256算法生成基于时间窗口的动态令牌
 * 
 * @author nety.ys
 */
public class DynamicTokenGenerator {
    
    /**
     * HMAC算法名称
     */
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    
    /**
     * 默认时间窗口（秒）
     */
    private static final int DEFAULT_TIME_WINDOW_SECONDS = 30;
    
    /**
     * 共享密钥
     */
    private final byte[] sharedSecret;
    
    /**
     * 时间窗口大小（毫秒）
     */
    private final long timeWindowMillis;
    
    /**
     * HMAC实例
     */
    private final Mac hmac;
    
    /**
     * 构造函数，使用默认时间窗口
     * 
     * @param sharedSecret 共享密钥
     * @throws IllegalArgumentException 如果共享密钥为空或无效
     */
    public DynamicTokenGenerator(byte[] sharedSecret) {
        this(sharedSecret, TimeUnit.SECONDS.toMillis(DEFAULT_TIME_WINDOW_SECONDS));
    }
    
    /**
     * 构造函数
     * 
     * @param sharedSecret 共享密钥
     * @param timeWindowMillis 时间窗口大小（毫秒）
     * @throws IllegalArgumentException 如果共享密钥为空或无效
     */
    public DynamicTokenGenerator(byte[] sharedSecret, long timeWindowMillis) {
        if (sharedSecret == null || sharedSecret.length == 0) {
            throw new IllegalArgumentException("共享密钥不能为空");
        }
        
        this.sharedSecret = sharedSecret.clone();
        this.timeWindowMillis = timeWindowMillis;
        
        try {
            // 初始化HMAC实例
            this.hmac = Mac.getInstance(HMAC_ALGORITHM);
            SecretKeySpec keySpec = new SecretKeySpec(this.sharedSecret, HMAC_ALGORITHM);
            this.hmac.init(keySpec);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("不支持的HMAC算法: " + HMAC_ALGORITHM, e);
        } catch (InvalidKeyException e) {
            throw new RuntimeException("无效的密钥", e);
        }
    }
    
    /**
     * 生成动态令牌
     * 
     * @param challenge 挑战数据
     * @param timestamp 时间戳
     * @return 动态令牌
     */
    public byte[] generateToken(byte[] challenge, long timestamp) {
        if (challenge == null) {
            throw new IllegalArgumentException("挑战数据不能为空");
        }
        
        try {
            // 创建数据缓冲区：挑战数据 + 时间戳
            ByteBuffer buffer = ByteBuffer.allocate(challenge.length + 8);
            buffer.put(challenge);
            buffer.putLong(timestamp);
            
            // 重置HMAC实例状态
            hmac.reset();
            
            // 计算HMAC值
            return hmac.doFinal(buffer.array());
        } catch (Exception e) {
            throw new RuntimeException("令牌生成失败", e);
        }
    }
    
    /**
     * 验证令牌
     * 
     * @param challenge 挑战数据
     * @param timestamp 时间戳
     * @param token 要验证的令牌
     * @return 如果令牌有效则返回true
     */
    public boolean verifyToken(byte[] challenge, long timestamp, byte[] token) {
        if (token == null || token.length == 0) {
            return false;
        }
        
        try {
            // 生成预期的令牌
            byte[] expectedToken = generateToken(challenge, timestamp);
            
            // 比较令牌
            return constantTimeEquals(expectedToken, token);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 验证令牌（带时间容差）
     * 
     * @param challenge 挑战数据
     * @param timestamp 时间戳
     * @param token 要验证的令牌
     * @param timeToleranceMillis 时间容差（毫秒）
     * @return 如果令牌有效则返回true
     */
    public boolean verifyTokenWithTolerance(byte[] challenge, long timestamp, byte[] token, long timeToleranceMillis) {
        if (token == null || token.length == 0) {
            return false;
        }
        
        try {
            // 尝试验证当前时间戳
            if (verifyToken(challenge, timestamp, token)) {
                return true;
            }
            
            // 在时间容差范围内尝试验证
            long toleranceSteps = timeToleranceMillis / 500; // 每步0.5秒，增加精度
            for (long i = 1; i <= toleranceSteps; i++) {
                // 验证前i*0.5秒
                if (verifyToken(challenge, timestamp - i * 500, token)) {
                    return true;
                }
                // 验证后i*0.5秒
                if (verifyToken(challenge, timestamp + i * 500, token)) {
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * 常数时间比较，防止时序攻击
     * 
     * @param a 第一个字节数组
     * @param b 第二个字节数组
     * @return 如果两个数组相等则返回true
     */
    private boolean constantTimeEquals(byte[] a, byte[] b) {
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
     * 获取时间窗口大小（毫秒）
     * 
     * @return 时间窗口大小（毫秒）
     */
    public long getTimeWindowMillis() {
        return timeWindowMillis;
    }
    
    /**
     * 获取共享密钥的副本
     * 
     * @return 共享密钥的副本
     */
    public byte[] getSharedSecret() {
        return sharedSecret.clone();
    }
}