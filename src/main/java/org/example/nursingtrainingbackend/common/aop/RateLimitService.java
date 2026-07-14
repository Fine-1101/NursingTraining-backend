package org.example.nursingtrainingbackend.common.aop;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class RateLimitService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 尝试获取令牌
     * @param key 限流键
     * @param count 限流次数
     * @param time 限流时间（秒）
     * @return 是否获取成功
     */
    public boolean tryAcquire(String key, int count, int time) {
        // Lua脚本实现令牌桶算法
        String luaScript =
                "local key = KEYS[1] " +
                        "local limit = tonumber(ARGV[1]) " +
                        "local window = tonumber(ARGV[2]) " +
//                        "local current_time = redis.call('TIME') " +
//                        "local current_timestamp = tonumber(current_time[1]) " +
                        "local requests = redis.call('GET', key) " +
                        "if requests == false then " +
                        "  redis.call('SET', key, 1) " +
                        "  redis.call('EXPIRE', key, window) " +
                        "  return 1 " +
                        "else " +
                        "  requests = tonumber(requests) " +
                        "  if requests < limit then " +
                        "    redis.call('INCR', key) " +
                        "    return 1 " +
                        "  else " +
                        "    return 0 " +
                        "  end " +
                        "end";

        RedisScript<Long> script = RedisScript.of(luaScript, Long.class);
        Long result = redisTemplate.execute(script, Collections.singletonList(key), String.valueOf(count), String.valueOf(time));

        return result != null && result == 1L;
    }

    /**
     * 获取当前请求数
     */
    public Long getCurrentCount(String key) {
        String countStr = redisTemplate.opsForValue().get(key);
        return countStr != null ? Long.valueOf(countStr) : 0L;
    }

    /**
     * 重置限流计数
     */
    public void reset(String key) {
        redisTemplate.delete(key);
    }
}
