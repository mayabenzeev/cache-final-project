package com.tdp.telco.ex2;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.tdp.telco.WorkshopTest;

import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** Exercise 2, step 3 - @CacheEvict and @CachePut in AnnotatedPlanPriceService. */
class Ex2Step3AnnotationsTest extends WorkshopTest {

	static final BigDecimal NEW_PRICE = new BigDecimal("79.99");

	@Autowired
	AnnotatedPlanPriceService service;

	@Test
	void cacheEvictMakesTheNewPriceVisible() {
		service.getPlan("unlimited-premium");

		service.updatePriceAndEvict("unlimited-premium", NEW_PRICE);

		assertThat(redis.hasKey("plans::unlimited-premium"))
			.as("plans::unlimited-premium should be deleted by the update. "
					+ "Add @CacheEvict(cacheNames = \"plans\", key = \"#id\") to updatePriceAndEvict")
			.isFalse();
		assertThat(service.getPlan("unlimited-premium").monthlyPrice()).isEqualByComparingTo(NEW_PRICE);
	}

	@Test
	void cachePutMakesTheNewPriceVisibleWithoutAMiss() {
		service.getPlan("family-4");
		int readsBefore = planRepository.readCount();

		service.updatePriceWriteThrough("family-4", NEW_PRICE);

		assertThat(service.getPlan("family-4").monthlyPrice())
			.as("The cache still returns the old price. "
					+ "Add @CachePut(cacheNames = \"plans\", key = \"#id\") to updatePriceWriteThrough")
			.isEqualByComparingTo(NEW_PRICE);
		assertThat(planRepository.readCount())
			.as("@CachePut should write the returned plan into the cache, so this read is a HIT")
			.isEqualTo(readsBefore);
	}

}
