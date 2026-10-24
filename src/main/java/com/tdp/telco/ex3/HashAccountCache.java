package com.tdp.telco.ex3;

import java.time.Duration;

import com.tdp.telco.data.Account;
import com.tdp.telco.data.AccountRepository;
import com.tdp.telco.data.NotFoundException;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Exercise 3 - the account cached as a Redis hash: one field per account field.
 */
@Service
public class HashAccountCache implements AccountCache {

	static final Duration ACCOUNT_TTL = Duration.ofMinutes(30);

	private final StringRedisTemplate redis; // redis.opsForHash() -> HSET / HGET / HGETALL / HINCRBY
	private final AccountRepository repository;

	public HashAccountCache(StringRedisTemplate redis, AccountRepository repository) {
		this.redis = redis;
		this.repository = repository;
	}

	@Override
	public Account getAccount(String id) {
		// TODO (Exercise 3, step 1): cache-aside, but with a hash instead of a JSON string
		// 1. Read all fields: redis.opsForHash().entries(key(id))   (HGETALL)
		// It returns an EMPTY map (not null) on a miss.
		// 2. HIT  -> AccountFields.fromHash(fields)
		// 3. MISS -> load from the repository, then
		// redis.opsForHash().putAll(key(id), AccountFields.toHash(account))   (HSET)
		// and give the hash a TTL with redis.expire(key(id), ACCOUNT_TTL)
		return repository.findById(id).orElseThrow(() -> new NotFoundException("Account", id));
	}

	@Override
	public long getDataUsedMb(String id) {
		// TODO (Exercise 3, step 2): read ONLY the dataUsedMb field: redis.opsForHash().get(key, field)   (HGET)
		// The value comes back as a String (or null if the account isn't cached yet - then fall back
		// to getAccount(id)).
		return getAccount(id).dataUsedMb();
	}

	@Override
	public void addUsage(String id, long mb) {
		repository.addUsage(id, mb);

		// TODO (Exercise 3, step 3): update the cached usage in place, atomically, inside Redis:
		// redis.opsForHash().increment(key, AccountFields.DATA_USED_MB, mb)   (HINCRBY)
		// Careful: what happens if you increment an account that ISN'T cached yet?
	}

	String key(String id) {
		return "telco:account:" + id;
	}

}
