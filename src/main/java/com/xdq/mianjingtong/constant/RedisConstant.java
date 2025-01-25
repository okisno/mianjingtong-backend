package com.xdq.mianjingtong.constant;

/**
 * @author xdq
 * @date 2025/1/25 20:52
 * @description RedisConstant
 * Redis常量
 */
public interface RedisConstant {

    /**
     * 用户签到记录的 Redis key 的前缀
     */
    String USER_SIGN_IN_REDIS_KEY_PREFIX = "user:signins";

    /**
     * 获取用户签到记录的Redis key
     * @param year 年份
     * @param userId 用户id
     * @return 拼接好的 Redis key
     * java支持的默认方法，相对简单的方法可以在接口中使用默认方法default实现，省去再编写接口的实现类
     * 但由于我们需要在其他类中使用，直接编写静态方法即可
     */
    static String getUserSignInRedisKey(int year, long userId) {
        return String.format("%s:%s:%s", USER_SIGN_IN_REDIS_KEY_PREFIX, year, userId);
    }

}
