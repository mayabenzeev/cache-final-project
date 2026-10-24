package com.tdp.telco.ex1;

import org.junit.jupiter.api.Test;

import com.tdp.telco.WorkshopTest;
import com.tdp.telco.entity.Plan;
import com.tdp.telco.service.AnnotatedPlanService;

import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** Exercise 1, step 3 - the same thing with @Cacheable (AnnotatedPlanService + CacheConfig). */
class Ex1Step3CacheableTest extends WorkshopTest {

	@Autowired
	AnnotatedPlanService service;

	@Test
	void secondReadIsServedFromTheCache() {
		Plan first = service.getPlan("unlimited");
		Plan second = service.getPlan("unlimited");

		assertThat(second).isEqualTo(first);
		assertThat(planRepository.readCount())
			.as("The slow database was read %d times for 2 identical requests. "
					+ "Did you put @Cacheable(cacheNames = \"plans\", key = \"#id\") on AnnotatedPlanService.getPlan?",
					planRepository.readCount())
			.isEqualTo(1);
	}

	@Test
	void springStoresItUnderCacheNameColonColonKey() {
		service.getPlan("unlimited");

		assertThat(redis.hasKey("plans::unlimited"))
			.as("Expected Spring to create the key \"plans::unlimited\" (<cacheName>::<key>). "
					+ "Use cacheNames = \"plans\" and key = \"#id\"")
			.isTrue();
	}

	@Test
	void springSetsTheTtlFromCacheConfig() {
		service.getPlan("unlimited");

		long ttl = ttlSeconds("plans::unlimited");
		assertThat(ttl)
			.as("TTL of plans::unlimited is %d (-1 = never expires). @Cacheable has no TTL attribute: "
					+ "set .entryTtl(...) on the \"plans\" cache in CacheConfig", ttl)
			.isPositive();
	}

}
