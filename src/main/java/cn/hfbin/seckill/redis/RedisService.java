package cn.hfbin.seckill.redis;

import cn.hfbin.seckill.bo.GoodsBo;
import cn.hfbin.seckill.util.JsonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSON;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class RedisService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 获取当个对象
     */
    public <T> T get(KeyPrefix prefix, String key, Class c) {
        //生成真正的key
        String realKey = prefix.getPrefix() + key;
        return (T) redisTemplate.opsForValue().get(realKey);
    }

    public void expire(KeyPrefix prefix, String key, int exTime) {
        String realKey = prefix.getPrefix() + key;
        redisTemplate.expire(realKey, exTime, TimeUnit.SECONDS);
    }

    /**
     * 设置对象
     */
    public <T> Boolean set(KeyPrefix prefix, String key, T value, int exTime) {
        String realKey = prefix.getPrefix() + key;
        try {
            if (exTime > 0) {
                redisTemplate.opsForValue().set(realKey, value, exTime, TimeUnit.SECONDS);
            } else {
                redisTemplate.opsForValue().set(realKey, value);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public Boolean del(KeyPrefix prefix, String key) {
        String realKey = prefix.getPrefix() + key;
        return redisTemplate.delete(realKey);
    }

    /**
     * 判断key是否存在
     */
    public <T> Boolean exists(KeyPrefix prefix, String key) {
        String realKey = prefix.getPrefix() + key;
        return redisTemplate.hasKey(realKey);
    }

    /**
     * 增加值
     */
    public <T> T incr(KeyPrefix prefix, String key) {
        String realKey = prefix.getPrefix() + key;
        return (T) redisTemplate.opsForValue().increment(realKey);
    }

    /*
   ·  * 减少值
     */
    public <T> Long decr(KeyPrefix prefix, String key) {
        String realKey = prefix.getPrefix() + key;

        return redisTemplate.opsForValue().decrement(realKey);
    }

    /**
     * bean 转 String
     *
     * @param value
     * @param <T>
     * @return
     */
    public static <T> String beanToString(T value) {
        if (value == null) {
            return null;
        }
        Class<?> clazz = value.getClass();
        if (clazz == int.class || clazz == Integer.class) {
            return "" + value;
        } else if (clazz == String.class) {
            return (String) value;
        } else if (clazz == long.class || clazz == Long.class) {
            return "" + value;
        } else {
            return JSON.toJSONString(value);
        }
    }


    /**
     * string转bean
     *
     * @param str
     * @param clazz
     * @param <T>
     * @return
     */
    public static <T> T stringToBean(String str, Class<T> clazz) {
        if (str == null || str.length() <= 0 || clazz == null) {
            return null;
        }
        if (clazz == int.class || clazz == Integer.class) {
            return (T) Integer.valueOf(str);
        } else if (clazz == String.class) {
            return (T) str;
        } else if (clazz == long.class || clazz == Long.class) {
            return (T) Long.valueOf(str);
        } else {
            return JSON.toJavaObject(JSON.parseObject(str), clazz);
        }
    }

    public <T, M> T executeLuaScript(AccessKey prefix, List<String> keys, String luaScript, M[] argvs, Class<T> resultType) {
        List<String> keyList = keys.stream().map(k -> prefix.getPrefix() + k).collect(Collectors.toList());
        DefaultRedisScript<T> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(luaScript);
        redisScript.setResultType(resultType);

        return redisTemplate.execute(redisScript, keyList, argvs);
    }



    public boolean getLock(BasePrefix prefix, String key, String value, long time, TimeUnit timeUnit) {
        String realKey = prefix.getPrefix() + key;
        return redisTemplate.opsForValue().setIfAbsent(realKey, value, time, timeUnit);
    }

    public void deleteLock(OrderPrefix prefix, String key) {
        String realKey = prefix.getPrefix() + key;
        redisTemplate.delete(realKey);
    }
}
