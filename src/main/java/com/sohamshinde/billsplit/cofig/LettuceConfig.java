package com.sohamshinde.billsplit.cofig;

import com.sohamshinde.billsplit.dto.ExpenseDto;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class LettuceConfig {

    @Value("${spring.data.redis.url}")
    private String redisHost;

    @Value("${spring.data.redis.port}")
    private int redisPort;

    @Value("${spring.data.redis.password}")
    private String redisPassword;

    @Bean
    public LettuceConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration redisConfig = new RedisStandaloneConfiguration(redisHost, redisPort);

        // ✅ Only set password if provided
        if (redisPassword != null && !redisPassword.isEmpty()) {
            redisConfig.setPassword(RedisPassword.of(redisPassword));
        }

        return new LettuceConnectionFactory(redisConfig);
    }

    @Bean
    public RedisTemplate<String, ExpenseDto> redisTemplate(LettuceConnectionFactory connectionFactory) {
        RedisTemplate<String, ExpenseDto> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // ✅ Key Serializer
        template.setKeySerializer(new StringRedisSerializer());

        // ✅ Value Serializer (Convert Java Objects to JSON)
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());

        return template;
    }

//    @Bean
//    public RedisClient redisClient() {
//        return RedisClient.create(RedisURI.create("redis://" + redisHost + ":" + redisPort));
//    }
//
//    @Bean
//    public StatefulRedisConnection<String, String> statefulRedisConnection(RedisClient redisClient) {
//        return redisClient.connect();
//    }
//
//    @Bean
//    public RedisCommands<String, String> redisCommands(StatefulRedisConnection<String, String> connection) {
//        return connection.sync();
//    }
}
