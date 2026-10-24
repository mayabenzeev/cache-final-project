package com.tdp.telco.ex3;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.tdp.telco.WorkshopTest;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * Exercise 3 - not a pass/fail test, an experiment. It sends the same 200 parallel usage updates
 * to both caches and prints what each one ends up believing.
 * Run it: ./mvnw test -Dtest=Ex3CompareStringVsHashTest
 */
class Ex3CompareStringVsHashTest extends WorkshopTest {

	static final int UPDATES = 200;
	static final int MB_PER_UPDATE = 10;

	@Autowired
	StringAccountCache stringCache;

	@Autowired
	HashAccountCache hashCache;

	@Test
	void compare() throws InterruptedException {
		long expected = accountRepository.findById("acc-1001").orElseThrow().dataUsedMb() + UPDATES * MB_PER_UPDATE;

		long stringResult = hammer(stringCache);
		accountRepository.reset();
		long hashResult = hammer(hashCache);

		System.out.printf("""

				  ============ %d parallel usage updates of %dMB ============
				  Database (source of truth) : %d MB
				  String cache (JSON, GET+SET): %d MB   -> %d updates lost
				  Hash cache   (HINCRBY)      : %d MB   -> %d updates lost
				  ===========================================================
				%n""", UPDATES, MB_PER_UPDATE, expected, stringResult, (expected - stringResult) / MB_PER_UPDATE,
				hashResult, (expected - hashResult) / MB_PER_UPDATE);
	}

	private long hammer(AccountCache cache) throws InterruptedException {
		cache.getAccount("acc-1001");
		try (ExecutorService pool = Executors.newFixedThreadPool(16)) {
			for (int i = 0; i < UPDATES; i++) {
				pool.submit(() -> cache.addUsage("acc-1001", MB_PER_UPDATE));
			}
			pool.shutdown();
			pool.awaitTermination(60, TimeUnit.SECONDS);
		}
		return cache.getDataUsedMb("acc-1001");
	}

}
