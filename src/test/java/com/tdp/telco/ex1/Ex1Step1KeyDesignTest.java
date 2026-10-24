package com.tdp.telco.ex1;

import org.junit.jupiter.api.Test;

import com.tdp.telco.WorkshopTest;

import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** Exercise 1, step 1 - design the cache key in ManualPlanService.planKey(). */
class Ex1Step1KeyDesignTest extends WorkshopTest {

	@Autowired
	ManualPlanService service;

	@Test
	void keyIsNamespacedAndEndsWithTheId() {
		String key = service.planKey("basic");

		assertThat(key)
			.as("planKey(\"basic\") returned \"%s\". Use lowercase segments separated by ':' "
					+ "that end with the id, like <namespace>:<entity>:<id>", key)
			.matches("[a-z0-9-]+(:[a-z0-9-]+)+")
			.endsWith(":basic");
	}

	@Test
	void keySaysWhatKindOfThingItIs() {
		String key = service.planKey("basic");

		assertThat(key)
			.as("planKey(\"basic\") returned \"%s\". Someone running KEYS * should see that this is a PLAN, "
					+ "and it must not collide with an account that has the same id", key)
			.contains("plan");
	}

	@Test
	void differentPlansGetDifferentKeys() {
		assertThat(service.planKey("basic")).isNotEqualTo(service.planKey("unlimited"));
	}

}
