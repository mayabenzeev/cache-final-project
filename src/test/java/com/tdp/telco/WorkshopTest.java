package com.tdp.telco;

import java.util.Set;

import org.junit.jupiter.api.BeforeEach;

import com.tdp.telco.common.repository.AccountRepository;
import com.tdp.telco.common.repository.PlanRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Base class for the exercise tests: a real Redis, a fast "slow database", and a clean slate
 * before every test.
 */
@SpringBootTest(properties = { "telco.slow-db.read-delay=20ms", "telco.slow-db.write-delay=2ms" })
@Import(TestcontainersConfiguration.class)
public abstract class WorkshopTest {

	@Autowired
	protected StringRedisTemplate redis;

	@Autowired
	protected PlanRepository planRepository;

	@Autowired
	protected AccountRepository accountRepository;

	@BeforeEach
	void cleanSlate() {
		Set<String> keys = redis.keys("*");
		if (!keys.isEmpty()) {
			redis.delete(keys);
		}
		planRepository.reset();
		accountRepository.reset();
	}

	/** Redis TTL in seconds: -1 = key exists with no TTL, -2 = key doesn't exist. */
	protected long ttlSeconds(String key) {
		Long ttl = redis.getExpire(key);
		return ttl == null ? -2 : ttl;
	}

}
