package com.tdp.telco.ex3;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import com.tdp.telco.WorkshopTest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;

import static org.assertj.core.api.Assertions.assertThat;

/** Exercise 3, step 2 - read one field with HGET in HashAccountCache.getDataUsedMb(). */
class Ex3Step2SingleFieldReadTest extends WorkshopTest {

	@Autowired
	HashAccountCache cache;

	@Test
	void dataUsageIsReadWithASingleHget() {
		cache.getAccount("acc-1001"); // cache it
		resetRedisCommandStats();

		long used = cache.getDataUsedMb("acc-1001");

		assertThat(used).isEqualTo(4200);
		Properties stats = redisCommandStats();
		assertThat(stats.getProperty("cmdstat_hget"))
			.as("To read one number, ask Redis for one field: opsForHash().get(key, \"dataUsedMb\") (HGET)")
			.isNotNull();
		assertThat(stats.getProperty("cmdstat_hgetall"))
			.as("getDataUsedMb() loaded the WHOLE account (HGETALL). Read only the dataUsedMb field")
			.isNull();
	}

	private void resetRedisCommandStats() {
		redis.execute((RedisCallback<Void>) connection -> {
			connection.serverCommands().resetConfigStats();
			return null;
		});
	}

	private Properties redisCommandStats() {
		return redis.execute((RedisCallback<Properties>) connection -> connection.serverCommands().info("commandstats"));
	}

}
