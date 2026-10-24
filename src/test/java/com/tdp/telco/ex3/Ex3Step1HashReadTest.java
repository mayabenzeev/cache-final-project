package com.tdp.telco.ex3;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.tdp.telco.WorkshopTest;
import com.tdp.telco.data.Account;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.DataType;

import static org.assertj.core.api.Assertions.assertThat;

/** Exercise 3, step 1 - cache the account as a hash in HashAccountCache.getAccount(). */
class Ex3Step1HashReadTest extends WorkshopTest {

	@Autowired
	HashAccountCache cache;

	@Test
	void secondReadIsServedFromTheCache() {
		Account first = cache.getAccount("acc-1001");
		Account second = cache.getAccount("acc-1001");

		assertThat(second).isEqualTo(first);
		assertThat(accountRepository.readCount())
			.as("The slow database was read %d times for 2 identical requests. "
					+ "Check the cache first with opsForHash().entries(key)", accountRepository.readCount())
			.isEqualTo(1);
	}

	@Test
	void accountIsStoredAsAHashWithOneFieldPerProperty() {
		cache.getAccount("acc-1001");

		String key = cache.key("acc-1001");
		assertThat(redis.type(key))
			.as("Expected a HASH under \"%s\". On a MISS, use opsForHash().putAll(key, AccountFields.toHash(account))",
					key)
			.isEqualTo(DataType.HASH);
		Map<Object, Object> fields = redis.opsForHash().entries(key);
		assertThat(fields).containsEntry("name", "Noa Cohen").containsEntry("dataUsedMb", "4200");
	}

	@Test
	void cachedAccountHasATtl() {
		cache.getAccount("acc-1001");

		long ttl = ttlSeconds(cache.key("acc-1001"));
		assertThat(ttl)
			.as("TTL is %d. putAll(...) has no TTL argument: call redis.expire(key, ACCOUNT_TTL) after it", ttl)
			.isPositive();
	}

}
