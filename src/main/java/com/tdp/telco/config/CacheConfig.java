package com.tdp.telco.config;

import tools.jackson.databind.json.JsonMapper;

import com.tdp.telco.entity.Plan;

import org.springframework.boot.cache.autoconfigure.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

/**
 * Spring's cache abstraction (@Cacheable, @CacheEvict, @CachePut) backed by Redis.
 *
 * @EnableCaching turns the annotations on: Spring wraps your @Service in a proxy that
 * checks the cache before calling your method. Without it the annotations do nothing.
 */
@Configuration
@EnableCaching
public class CacheConfig {

	@Bean
	RedisCacheManagerBuilderCustomizer plansCacheConfig(JsonMapper jsonMapper, RedisConnectionFactory connectionFactory) {
		RedisCacheConfiguration plans = RedisCacheConfiguration.defaultCacheConfig()
			// Store plans as readable JSON instead of Java-serialized bytes
			.serializeValuesWith(SerializationPair.fromSerializer(new JacksonJsonRedisSerializer<>(jsonMapper, Plan.class)));
			// TODO (Exercise 1, step 3b): entries in the "plans" cache never expire right now.
			// @Cacheable has no TTL attribute - the TTL is configured per cache, here.
			// Add .entryTtl(...) to the chain above (use the same TTL as your manual cache).

		return builder -> builder
			// Spring Data Redis 4 sends cache writes in the background by default ("fire and forget").
			// Immediate writes make a put/evict finish before your method returns, so what you
			// see in redis-cli right after a request is exactly what the annotation did.
			.cacheWriter(RedisCacheWriter.create(connectionFactory, writer -> writer.immediateWrites()))
			.withCacheConfiguration("plans", plans);
	}

}
