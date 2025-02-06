package com.xdq.mianjingtong.blackfilter;

import cn.hutool.bloomfilter.BitMapBloomFilter;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import io.netty.util.internal.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.yaml.snakeyaml.Yaml;

import java.awt.image.ImageFilter;
import java.util.List;
import java.util.Map;

/**
 * @author xdq
 * @date 2025/2/7 1:26
 * @description BlackIpUtils
 * 使用布隆过滤器对黑名单进行过滤
 */
@Slf4j
public class BlackIpUtils {

    private static BitMapBloomFilter blomFilter;

    // 判断ip是否在黑名单
    public static boolean isBlackIp(String ip) {
        return blomFilter.contains(ip);
    }

    /**
     * 重建 ip 黑名单
     * @param configInfo
     */
    public static void rebuildBloomFilter(String configInfo) {
        if (StrUtil.isBlank(configInfo)) {
            configInfo = "";
        }
        // 解析yaml文件
        Yaml yaml = new Yaml();
        Map map = yaml.loadAs(configInfo, Map.class);
        //获取ip黑名单
        List<String> blackIpList = (List<String>) map.get("blackIpList");

        //加锁避免并发的冲突，保证串行执行
        synchronized (BlackIpUtils.class) {
            if (CollUtil.isNotEmpty(blackIpList)) {
                // 注意构造参数的设置
                BitMapBloomFilter bitMapBloomFilter = new BitMapBloomFilter(958506);
                for (String blackIp : blackIpList) {
                    bitMapBloomFilter.add(blackIp);
                }
                blomFilter = bitMapBloomFilter;
            } else {
                blomFilter = new BitMapBloomFilter(100);
            }
        }

    }

}
