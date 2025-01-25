package com.xdq.mianjingtong.config;


import com.xdq.mianjingtong.properties.RedisProperties;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RedissonConfig {

    /**
     * addNodeAddress集群使用
     * setAddress单机使用
     * @param redisProperties
     * @return
     */
    @Bean
    public RedissonClient redissonClient(RedisProperties redisProperties) {
        log.info("RedissonClient init: {}", redisProperties);
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + redisProperties.getHost() + ":" + redisProperties.getPort())
                .setDatabase(redisProperties.getDatabase())
                .setPassword(redisProperties.getPassword());
        return Redisson.create(config);
    }

}
