package com.ccc.ccctools.redisUtils;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

@Slf4j
@Component
public class CacheClient {
    public  final StringRedisTemplate stringRedisTemplate;

    public CacheClient(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }
    /**
     * @description: 把对象存储为json 字符串存到redis
     * @return: void
     * @author 陈南田
     * @date: 2/2/2023 10:39 PM
     */
    public void set(String key, Object value, Long time, TimeUnit timeUnit){
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(value),time,timeUnit);

    }

    /**
     * @description: 逻辑过期存储对象
     * @param:
     * @return: void
     * @author 陈南田
     * @date: 2/2/2023 10:43 PM
     */
    public void setWithLogicalExpire(String key, Object value, Long time, TimeUnit timeUnit){
        //设置逻辑过期
        RedisData redisData = new RedisData();
        redisData.setData(value);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(timeUnit.toSeconds(time)));
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(redisData),time,timeUnit);

    }
    /**
     * @description: 解决缓存穿透
     * @param:
     * @return: R
     * @author 陈南田
     * @date: 2/2/2023 10:58 PM
     */

    public <R,ID> R queryWithPassThrough(String keyPrefix,
                                         ID id,
                                         Class<R> type,
                                         Function<ID,R> dbFallBack,
                                         Long time,
                                         TimeUnit timeUnit){
    String key=keyPrefix+id;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.isEmpty(json)){
            return JSONUtil.toBean(json,type);
        }
        if (json != null){
            return null;
        }
        R r = dbFallBack.apply(id);
        if (r == null){
            stringRedisTemplate.opsForValue().set(key,"",CacheConstant.CACHE_NULL_TTL,TimeUnit.SECONDS);
            return null;
        }
        this.set(key,r,time,timeUnit);
        return r;

    }

    public <R> R queryWithLogicalExpire(String prefix,Long id,Class<R> type){
        String key =prefix + id;
        String json = stringRedisTemplate.opsForValue().get(key);
        if (StringUtils.isEmpty(json)){
            return null;
        }
        RedisData r = JSONUtil.toBean(json, RedisData.class);
        return (R) r;

    }


}
