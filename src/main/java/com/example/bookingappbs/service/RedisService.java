package com.example.bookingappbs.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RedisService {
    private static final Logger logger = LogManager.getLogger(RedisService.class);

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public <T> void save(String key, T value) {
        logger.info("Saving to Redis with key: {}", key);
        try {
            String json = objectMapper.writeValueAsString(value);
            redisTemplate.opsForValue().set(key, json);
            logger.debug("Value saved to Redis for key: {}", key);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error saving to Redis: " + e);
        }
    }

    @Transactional(readOnly = true)
    public <T> T find(String key, Class<T> clazz) {
        logger.info("Finding from Redis with key: {}", key);
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            logger.debug("No value found in Redis for key: {}", key);
            return null;
        }
        try {
            T value = objectMapper.readValue(json, clazz);
            logger.debug("Value found in Redis for key {}: {}", key, value);
            return value;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializing from Redis: " + e);
        }
    }

    @Transactional(readOnly = true)
    public <T> List<T> findAll(String key, Class<T> clazz) {
        logger.info("Finding all from Redis with key: {}", key);
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            logger.debug("No list found in Redis for key: {}", key);
            return Collections.emptyList();
        }
        try {
            List<T> values = objectMapper.readValue(json, objectMapper
                    .getTypeFactory()
                    .constructCollectionType(List.class, clazz));
            logger.debug("List found in Redis for key {}, size: {}", key, values.size());
            return values;
        } catch (JsonProcessingException e) {
            logger.error("Error deserializing list from Redis with key {}: {}",
                    key, e.getMessage());
            return Collections.emptyList();
        }
    }

    public void delete(String key) {
        logger.info("Deleting from Redis with key: {}", key);
        redisTemplate.delete(key);
        logger.debug("Key {} deleted from Redis.", key);
    }

    public void deletePattern(String pattern) {
        logger.info("Deleting from Redis with pattern: {}", pattern);
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
            logger.debug("Keys matching pattern {} deleted from Redis.", pattern);
        } else {
            logger.debug("No keys found matching pattern: {}.", pattern);
        }
    }
}
