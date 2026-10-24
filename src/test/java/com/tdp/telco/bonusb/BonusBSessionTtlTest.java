package com.tdp.telco.bonusb;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.tdp.telco.TestcontainersConfiguration;
import com.tdp.telco.service.FeatureFlagStore;
import com.tdp.telco.service.SessionStore;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * Bonus B - a full Redis with the volatile-lru policy: only keys WITH a TTL may be evicted.
 */
@Testcontainers
class BonusBSessionTtlTest {

	@Container
	static GenericContainer<?> smallRedis = new GenericContainer<>(TestcontainersConfiguration.REDIS_IMAGE)
		.withExposedPorts(6379)
		.withCommand("redis-server", "--maxmemory", "4mb", "--maxmemory-policy", "volatile-lru", "--save", "",
				"--appendonly", "no");

	static StringRedisTemplate redis;

	SessionStore sessions;

	FeatureFlagStore flags;

	@BeforeEach
	void setUp() {
		if (redis == null) {
			redis = SmallRedis.templateFor(smallRedis);
		}
		redis.execute((RedisCallback<Void>) connection -> {
			connection.serverCommands().flushAll();
			return null;
		});
		sessions = new SessionStore(redis);
		flags = new FeatureFlagStore(redis);
	}

	@Test
	void sessionsExpire() {
		sessions.saveSession("s-1", "user=acc-1001");

		Long ttl = redis.getExpire(SessionStore.key("s-1"));
		assertThat(ttl)
			.as("Session TTL is %d (-1 = never expires). Pass SESSION_TTL to set(...) in SessionStore", ttl)
			.isPositive();
	}

	@Test
	void aFloodOfSessionsEvictsOldSessionsButNeverTheFeatureFlags() {
		for (int i = 0; i < 20; i++) {
			flags.setFlag("flag-" + i, true);
		}

		try {
			for (int i = 0; i < 20_000; i++) {
				sessions.saveSession("s-" + i, SmallRedis.PAYLOAD);
			}
		}
		catch (RuntimeException e) {
			fail("Redis refused a write: '%s'. Under volatile-lru only keys WITH a TTL can be evicted. "
					+ "The sessions have no TTL, so when memory is full nothing can be evicted. "
					+ "Give sessions a TTL in SessionStore.saveSession", rootMessage(e));
		}

		assertThat(SmallRedis.countKeys(redis, "telco:flag:*"))
			.as("Feature flags have no TTL, so volatile-lru must never evict them")
			.isEqualTo(20);
		assertThat(SmallRedis.info(redis, "stats").getProperty("evicted_keys")).isNotEqualTo("0");
	}

	private static String rootMessage(Throwable e) {
		while (e.getCause() != null) {
			e = e.getCause();
		}
		return e.getMessage();
	}

}
