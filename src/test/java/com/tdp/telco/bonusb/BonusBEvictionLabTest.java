package com.tdp.telco.bonusb;

import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

import com.tdp.telco.TestcontainersConfiguration;

import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Bonus B - the eviction lab. Not a pass/fail test, an experiment.
 *
 * It starts a Redis configured by redis/bonus-b.conf, stores 200 feature flags (no TTL) and then
 * floods it with 20,000 sessions, and prints what survived.
 *
 * Change maxmemory-policy in redis/bonus-b.conf and run it again:
 *   ./mvnw test -Dtest=BonusBEvictionLabTest
 */
class BonusBEvictionLabTest {

	static final int FLAGS = 200;
	static final int SESSIONS = 20_000;

	@Test
	void floodTheCache() {
		try (GenericContainer<?> redisContainer = new GenericContainer<>(TestcontainersConfiguration.REDIS_IMAGE)
			.withExposedPorts(6379)
			.withCopyFileToContainer(MountableFile.forHostPath("redis/bonus-b.conf"), "/usr/local/etc/redis/redis.conf")
			.withCommand("redis-server", "/usr/local/etc/redis/redis.conf")) {
			redisContainer.start();
			StringRedisTemplate redis = SmallRedis.templateFor(redisContainer);
			FeatureFlagStore flags = new FeatureFlagStore(redis);
			SessionStore sessions = new SessionStore(redis);

			int refused = 0;
			for (int i = 0; i < FLAGS; i++) {
				flags.setFlag("flag-" + i, true);
			}
			for (int i = 0; i < SESSIONS; i++) {
				try {
					sessions.saveSession("s-" + i, SmallRedis.PAYLOAD);
				}
				catch (RuntimeException e) {
					refused++;
				}
			}

			String policy = redis.getConnectionFactory().getConnection().serverCommands()
				.getConfig("maxmemory-policy").getProperty("maxmemory-policy");
			Properties stats = SmallRedis.info(redis, "stats");
			Properties memory = SmallRedis.info(redis, "memory");

			System.out.printf("""

					  ================= Eviction lab =================
					  maxmemory-policy   : %s
					  maxmemory          : %s
					  Writes attempted   : %d flags (no TTL) + %d sessions
					  Writes refused     : %d
					  Keys evicted       : %s
					  Flags still there  : %d / %d
					  Sessions still there: %d / %d
					  ================================================
					  Try another policy in redis/bonus-b.conf and run again.
					%n""", policy, memory.getProperty("maxmemory_human"), FLAGS, SESSIONS, refused,
					stats.getProperty("evicted_keys"), SmallRedis.countKeys(redis, "telco:flag:*"), FLAGS,
					SmallRedis.countKeys(redis, "telco:session:*"), SESSIONS);
		}
	}

}
