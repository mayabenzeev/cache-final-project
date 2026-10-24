package com.tdp.telco;

import org.junit.jupiter.api.Test;

import org.springframework.data.redis.core.RedisCallback;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Run this BEFORE the workshop: ./mvnw test -Dtest=SetupCheckTest
 * It downloads everything the workshop needs and proves Docker + Redis + Java all work.
 */
class SetupCheckTest extends WorkshopTest {

	@Test
	void redisIsReachable() {
		String pong = redis.execute((RedisCallback<String>) connection -> connection.ping());
		assertThat(pong).isEqualTo("PONG");

		System.out.println("""

				  ✅  Setup OK: Java %s, Spring Boot, Docker and Redis all work.
				      You're ready for the workshop. See you there!
				""".formatted(Runtime.version().feature()));
	}

}
