package com.tdp.telco.ex2;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.tdp.telco.WorkshopTest;
import com.tdp.telco.ex2.service.PlanPriceService;

import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** Exercise 2, step 2 - write-through in PlanPriceService.updatePriceWriteThrough(). */
class Ex2Step2WriteThroughTest extends WorkshopTest {

	static final BigDecimal NEW_PRICE = new BigDecimal("59.99");

	@Autowired
	PlanPriceService service;

	@Test
	void newPriceIsVisibleRightAfterTheUpdate() {
		service.getPlan("unlimited");

		service.updatePriceWriteThrough("unlimited", NEW_PRICE);

		assertThat(service.getPlan("unlimited").monthlyPrice())
			.as("The cache is lying: it still returns the old price after the update")
			.isEqualByComparingTo(NEW_PRICE);
	}

	@Test
	void readAfterTheUpdateIsStillAHit() {
		service.getPlan("unlimited");
		int readsBefore = planRepository.readCount();

		service.updatePriceWriteThrough("unlimited", NEW_PRICE);
		service.getPlan("unlimited");

		assertThat(planRepository.readCount())
			.as("With write-through the cache already holds the new value, so the next read must not go to the "
					+ "database. Write the updated plan into Redis instead of deleting it")
			.isEqualTo(readsBefore);
	}

	@Test
	void cachedCopyKeepsItsTtl() {
		service.getPlan("unlimited");

		service.updatePriceWriteThrough("unlimited", NEW_PRICE);

		long ttl = ttlSeconds(service.planKey("unlimited"));
		assertThat(ttl)
			.as("TTL is %d after the update. A plain SET removes the existing TTL! Pass PLAN_TTL to set(...) "
					+ "so the safety net stays in place", ttl)
			.isPositive();
	}

}
