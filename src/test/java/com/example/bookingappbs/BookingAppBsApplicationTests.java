package com.example.bookingappbs;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = BookingAppBsApplication.class)
@ImportAutoConfiguration(exclude = {RedisAutoConfiguration.class})
@ActiveProfiles("test")
class BookingAppBsApplicationTests {
    @MockBean
    private RedisConnectionFactory redisConnectionFactory;
    @MockBean
    private RedisTemplate<String, Object> redisTemplate;
    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @Test
    void contextLoads() {
        assertTrue(true, "Spring context was loaded successfully");
    }
}
