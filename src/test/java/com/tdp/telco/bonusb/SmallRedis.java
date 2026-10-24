package com.tdp.telco.bonusb;

import java.util.Properties;

import org.testcontainers.containers.GenericContainer;

import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

/** Helpers for the Bonus B tests, which each start their own tiny Redis. */
final class SmallRedis {

	static final String PAYLOAD = "x".repeat(512);

	private SmallRedis() {
	}

	static StringRedisTemplate templateFor(GenericContainer<?> container) {
		LettuceConnectionFactory factory = new LettuceConnectionFactory(container.getHost(),
				container.getMappedPort(6379));
		factory.afterPropertiesSet();
		factory.start();
		return new StringRedisTemplate(factory);
	}

	static Properties info(StringRedisTemplate redis, String section) {
		return redis.execute((RedisCallback<Properties>) connection -> connection.serverCommands().info(section));
	}

	static long countKeys(StringRedisTemplate redis, String pattern) {
		return redis.keys(pattern).size();
	}

}
