package com.xdq.mianjingtong.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author xdq
 * @date 2025/1/25 18:52
 * @description RedisProperties
 */
@Component
@ConfigurationProperties(prefix = "spring.redis")
@Data
public class RedisProperties {

    private String host;
    private Integer port;
    private Integer database;
    private String password;

}
