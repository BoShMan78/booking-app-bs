package com.example.bookingappbs.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisService {
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    @Autowired
    private ObjectMapper objectMapper;

    public <T> void save(String key, T value) {
        try {
            String json = objectMapper.writeValueAsString(value);
            System.out.println("Saving to Redis: " + key + " -> " + value);
            redisTemplate.opsForValue().set(key, json);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error saving to Redis: " + e);
        }
    }

    public <T> T find(String key, Class<T> clazz) {
        String json = (String) redisTemplate.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        System.out.println("Retrieved from Redis: " + key + " -> " + json);
        try {
            return objectMapper.readValue(json, clazz);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error deserializing from Redis: " + e);
        }


    }

    public <T> List<T> findAll(String key, Class<T> clazz) {
        String json = (String) redisTemplate.opsForValue().get(key);
        if (json == null) {
            return Collections.emptyList();
        }
        System.out.println("Retrieved from Redis: " + key + " -> " + json);
        try {
            return objectMapper.readValue(json, objectMapper.getTypeFactory().constructCollectionType(List.class, clazz));
        } catch (JsonProcessingException e) {
            System.out.println("Error deserializing value from Redis: " + e);
            return Collections.emptyList();
        }
    }

    public void delete(String key) {
        redisTemplate.delete(key);
    }

    public void deletePattern(String pattern) {
        Set<String> keys = redisTemplate.keys(pattern);
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }
}
