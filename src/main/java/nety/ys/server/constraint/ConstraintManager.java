package nety.ys.server.constraint;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import nety.ys.TokenAuthMod;
import nety.ys.constraint.api.ConstraintAPI;
import nety.ys.constraint.api.ConstraintType;

/**
 * 约束管理器
 * 负责管理玩家的约束状态
 * 
 * @author nety.ys
 */
public class ConstraintManager {
    
    /**
     * 约束来源标识符
     */
    private static final String CONSTRAINT_SOURCE = "token_auth:auth_constraint";
    
    /**
     * 初始化约束系统
     */
    public static void initialize() {
        TokenAuthMod.LOGGER.info("初始化约束系统...");
        
        try {
            // 注册认证约束条件
            ConstraintAPI.addConstraintCondition(new AuthenticationConstraintCondition());
            TokenAuthMod.LOGGER.info("已注册认证约束条件");
            
            // 监听约束事件
            nety.ys.constraint.api.ConstraintEvent.CONSTRAINT_APPLIED.register((player, constraintType, context) -> {
                TokenAuthMod.LOGGER.debug("玩家 {} 被应用约束: {}", player.getName().getString(), constraintType);
            });
            
            nety.ys.constraint.api.ConstraintEvent.CONSTRAINT_REMOVED.register((player, constraintType, context) -> {
                TokenAuthMod.LOGGER.debug("玩家 {} 的约束 {} 已移除", player.getName().getString(), constraintType);
            });
            
            TokenAuthMod.LOGGER.info("约束系统初始化完成");
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("初始化约束系统时出错", e);
        }
    }
    
    /**
     * 为未认证玩家添加约束
     * 
     * @param player 目标玩家
     */
    public static void applyConstraintsToPlayer(ServerPlayerEntity player) {
        if (player == null) {
            TokenAuthMod.LOGGER.warn("尝试为空玩家添加约束");
            return;
        }
        
        TokenAuthMod.LOGGER.info("为未认证玩家 {} 添加约束", player.getName().getString());
        
        try {
            // 添加各种约束类型
            addConstraint(player, ConstraintType.MOVEMENT);
            addConstraint(player, ConstraintType.CHAT);
            addConstraint(player, ConstraintType.COMMAND);
            addConstraint(player, ConstraintType.BLOCK_INTERACTION);
            addConstraint(player, ConstraintType.BLOCK_BREAKING);
            addConstraint(player, ConstraintType.ENTITY_INTERACTION);
            addConstraint(player, ConstraintType.ENTITY_ATTACKING);
            addConstraint(player, ConstraintType.ITEM_USING);
            addConstraint(player, ConstraintType.ITEM_DROPPING);
            addConstraint(player, ConstraintType.ITEM_MOVING);
            
            TokenAuthMod.LOGGER.info("已为玩家 {} 添加所有约束", player.getName().getString());
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("为玩家添加约束时出错", e);
        }
    }
    
    /**
     * 移除玩家的所有约束
     * 
     * @param player 目标玩家
     */
    public static void removeConstraintsFromPlayer(ServerPlayerEntity player) {
        if (player == null) {
            TokenAuthMod.LOGGER.warn("尝试为空玩家移除约束");
            return;
        }
        
        TokenAuthMod.LOGGER.info("为已认证玩家 {} 移除约束", player.getName().getString());
        
        try {
            // 移除各种约束类型
            removeConstraint(player, ConstraintType.MOVEMENT);
            removeConstraint(player, ConstraintType.CHAT);
            removeConstraint(player, ConstraintType.COMMAND);
            removeConstraint(player, ConstraintType.BLOCK_INTERACTION);
            removeConstraint(player, ConstraintType.BLOCK_BREAKING);
            removeConstraint(player, ConstraintType.ENTITY_INTERACTION);
            removeConstraint(player, ConstraintType.ENTITY_ATTACKING);
            removeConstraint(player, ConstraintType.ITEM_USING);
            removeConstraint(player, ConstraintType.ITEM_DROPPING);
            removeConstraint(player, ConstraintType.ITEM_MOVING);
            
            TokenAuthMod.LOGGER.info("已为玩家 {} 移除所有约束", player.getName().getString());
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("为玩家移除约束时出错", e);
        }
    }
    
    /**
     * 添加指定类型的约束
     * 
     * @param player 目标玩家
     * @param constraintType 约束类型
     */
    private static void addConstraint(ServerPlayerEntity player, ConstraintType constraintType) {
        try {
            ConstraintAPI.addConstraint(player, constraintType, CONSTRAINT_SOURCE, new nety.ys.constraint.api.ConstraintContext());
            TokenAuthMod.LOGGER.debug("已为玩家 {} 添加约束: {}", player.getName().getString(), constraintType);
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("添加约束 {} 时出错", constraintType, e);
        }
    }
    
    /**
     * 移除指定类型的约束
     * 
     * @param player 目标玩家
     * @param constraintType 约束类型
     */
    private static void removeConstraint(ServerPlayerEntity player, ConstraintType constraintType) {
        try {
            ConstraintAPI.removeConstraint(player, constraintType, CONSTRAINT_SOURCE);
            TokenAuthMod.LOGGER.debug("已为玩家 {} 移除约束: {}", player.getName().getString(), constraintType);
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("移除约束 {} 时出错", constraintType, e);
        }
    }
    
    /**
     * 检查玩家是否被约束
     * 
     * @param player 目标玩家
     * @return 如果玩家被约束则返回true
     */
    public static boolean isPlayerConstrained(ServerPlayerEntity player) {
        try {
            nety.ys.constraint.api.ConstraintState state = ConstraintAPI.getConstraintState(player);
            return state != null && state.isConstrained();
        } catch (Exception e) {
            TokenAuthMod.LOGGER.error("检查玩家约束状态时出错", e);
            return false;
        }
    }
}