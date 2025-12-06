package nety.ys.server.constraint;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import nety.ys.TokenAuthMod;
import nety.ys.server.AuthSessionManager;
import nety.ys.util.DebugLogger;

/**
 * 认证约束条件
 * 用于约束未认证玩家的行为
 * 
 * @author nety.ys
 */
public class AuthenticationConstraintCondition implements nety.ys.constraint.api.ConstraintCondition {
    
    /**
     * 约束条件标识符
     */
    private static final Identifier IDENTIFIER = new Identifier("token_auth", "authentication_check");
    
    /**
     * 约束条件优先级
     */
    private static final int PRIORITY = 10; // 高优先级，确保认证约束优先执行
    
    /**
     * 检查是否应该约束玩家
     * 
     * @param player 玩家实体
     * @param constraintType 约束类型
     * @param context 约束上下文
     * @return 如果玩家未认证则返回true（应该约束）
     */
    @Override
    public boolean shouldConstrain(ServerPlayerEntity player, nety.ys.constraint.api.ConstraintType constraintType, nety.ys.constraint.api.ConstraintContext context) {
        // 检查玩家是否已认证
        boolean isAuthenticated = AuthSessionManager.isPlayerAuthenticated(player.getUuid().toString());
        
        // 如果玩家未认证，则应该被约束
        boolean shouldConstrain = !isAuthenticated;
        
        if (shouldConstrain) {
            DebugLogger.debug("玩家 {} 未认证，应用约束类型: {}", player.getName().getString(), constraintType);
        } else {
            DebugLogger.debug("玩家 {} 已认证，不应用约束", player.getName().getString());
        }
        
        return shouldConstrain;
    }
    
    /**
     * 获取约束条件标识符
     * 
     * @return 约束条件标识符
     */
    @Override
    public Identifier getIdentifier() {
        return IDENTIFIER;
    }
    
    /**
     * 获取约束条件优先级
     * 
     * @return 优先级数值，数值越大优先级越高
     */
    @Override
    public int getPriority() {
        return PRIORITY;
    }
    
    /**
     * 获取约束条件描述
     *
     * @return 约束条件的描述
     */
    @Override
    public String getDescription() {
        return "Token Auth认证约束条件：检查玩家是否已完成令牌认证";
    }
}