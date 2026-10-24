package com.tdp.telco.ex1;

import org.junit.jupiter.api.Test;

import com.tdp.telco.WorkshopTest;
import com.tdp.telco.data.Plan;

import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** Exercise 1, step 2 - cache-aside by hand in ManualPlanService.getPlan(). */
class Ex1Step2CacheAsideTest extends WorkshopTest {

	@Autowired
	ManualPlanService service;

	@Test
	void secondReadIsServedFromTheCache() {
		Plan first = service.getPlan("basic");
		Plan second = service.getPlan("basic");

		assertThat(second).isEqualTo(first);
		assertThat(planRepository.readCount())
			.as("The slow database was read %d times for 2 identical requests. "
					+ "Only a MISS should reach the database: check Redis first with opsForValue().get(key)",
					planRepository.readCount())
			.isEqualTo(1);
	}

	@Test
	void planIsStoredAsJsonUnderYourKey() {
		service.getPlan("basic");

		String key = service.planKey("basic");
		String stored = redis.opsForValue().get(key);
		assertThat(stored)
			.as("Nothing found in Redis under \"%s\". On a MISS, store the plan with opsForValue().set(key, json, ttl)",
					key)
			.isNotNull();
		assertThat(stored).as("Store the plan as JSON: use json.writeValueAsString(plan)")
			.contains("\"name\":\"Basic 50GB\"");
	}

	@Test
	void cachedPlanHasATtl() {
		service.getPlan("basic");

		long ttl = ttlSeconds(service.planKey("basic"));
		assertThat(ttl)
			.as("TTL is %d. -1 means the key never expires, -2 means it doesn't exist. "
					+ "Pass a Duration as the third argument of set(...)", ttl)
			.isPositive();
	}

}
