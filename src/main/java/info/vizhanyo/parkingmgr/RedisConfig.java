package info.vizhanyo.parkingmgr;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import redis.clients.jedis.JedisPool;

@Configuration
public class RedisConfig {

    @Bean(destroyMethod = "close")
    public JedisPool jedisPool(@Value("${redis.url}") String url) {
        return new JedisPool(URI.create(url));
    }
}
