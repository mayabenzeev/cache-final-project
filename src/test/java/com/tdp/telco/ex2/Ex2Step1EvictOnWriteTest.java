package com.tdp.telco.ex2;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.tdp.telco.WorkshopTest;

import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** Exercise 2, step 1 - invalidate on write in PlanPriceService.updatePriceAndEvict(). */
class Ex2Step1EvictOnWriteTest extends WorkshopTest {

	static final BigDecimal NEW_PRICE = new BigDecimal("39.99");

	@Autowired
	PlanPriceService service;

	@Test
	void newPriceIsVisibleRightAfterTheUpdate() {
		service.getPlan("basic"); // the plan page was viewed, so it's cached

		service.updatePriceAndEvict("basic", NEW_PRICE);

		assertThat(service.getPlan("basic").monthlyPrice())
			.as("The cache is lying: it still returns the old price after the update")
			.isEqualByComparingTo(NEW_PRICE);
	}

	@Test
	void updateDeletesTheCachedCopy() {
		service.getPlan("basic");

		service.updatePriceAndEvict("basic", NEW_PRICE);

		assertThat(redis.hasKey(service.planKey("basic")))
			.as("After the update the stale key should be gone, so the next read is a MISS that reloads it. "
					+ "Use redis.delete(key)")
			.isFalse();
	}

}
