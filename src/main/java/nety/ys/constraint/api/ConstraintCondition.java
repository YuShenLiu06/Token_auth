package nety.ys.constraint.api;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

/**
 * 约束条件接口
 * 定义了判断是否应该对玩家应用约束的条件
 * 
 * @author nety.ys
 */
public interface ConstraintCondition {
    
    /**
     * 检查是否应该约束玩家
     * 
     * @param player 玩家实体
     * @param constraintType 约束类型
     * @param context 约束上下文
     * @return 如果应该约束玩家则返回true
     */
    boolean shouldConstrain(ServerPlayerEntity player, ConstraintType constraintType, ConstraintContext context);
    
    /**
     * 获取约束条件标识符
     * 
     * @return 约束条件标识符
     */
    Identifier getIdentifier();
    
    /**
     * 获取约束条件优先级
     * 
     * @return 优先级数值，数值越大优先级越高
     */
    int getPriority();
    
    /**
     * 获取约束条件描述
     * 
     * @return 约束条件的描述
     */
    String getDescription();
}