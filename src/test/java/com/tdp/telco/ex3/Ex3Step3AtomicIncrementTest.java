package com.tdp.telco.ex3;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.tdp.telco.WorkshopTest;
import com.tdp.telco.common.entity.Account;
import com.tdp.telco.ex3.service.HashAccountCache;

import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

/** Exercise 3, step 3 - update one field atomically with HINCRBY in HashAccountCache.addUsage(). */
class Ex3Step3AtomicIncrementTest extends WorkshopTest {

	@Autowired
	HashAccountCache cache;

	@Test
	void usageUpdateIsVisibleInTheCache() {
		cache.getAccount("acc-1001"); // cached with dataUsedMb = 4200

		cache.addUsage("acc-1001", 250);

		assertThat(cache.getDataUsedMb("acc-1001"))
			.as("The cached data usage didn't change. Update the cached field with opsForHash().increment(...)")
			.isEqualTo(4450);
		assertThat(accountRepository.readCount())
			.as("The update shouldn't force a reload from the slow database")
			.isEqualTo(1);
	}

	@Test
	void concurrentUsageUpdatesAreNotLost() throws InterruptedException {
		cache.getAccount("acc-1001");

		try (ExecutorService pool = Executors.newFixedThreadPool(16)) {
			for (int i = 0; i < 100; i++) {
				pool.submit(() -> cache.addUsage("acc-1001", 10));
			}
			pool.shutdown();
			assertThat(pool.awaitTermination(30, TimeUnit.SECONDS)).isTrue();
		}

		assertThat(cache.getDataUsedMb("acc-1001"))
			.as("100 parallel updates of 10MB should add exactly 1000MB to the cached value")
			.isEqualTo(4200 + 1000);
	}

	@Test
	void usageForAnAccountThatIsNotCachedDoesNotCreateAHalfEmptyHash() {
		cache.addUsage("acc-1002", 250); // acc-1002 was never read, so it's not cached

		Account account = cache.getAccount("acc-1002");

		assertThat(account.name())
			.as("getAccount returned an account with no name: addUsage created a hash containing only dataUsedMb. "
					+ "Only increment when the key exists (redis.hasKey(key))")
			.isEqualTo("Daniel Levi");
		assertThat(account.dataUsedMb()).isEqualTo(18_750 + 250);
	}

}
