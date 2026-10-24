package com.tdp.telco.ex3;

import java.time.Duration;
import java.util.Map;

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

	private final StringRedisTemplate redis;
	private final AccountRepository repository;

	public HashAccountCache(StringRedisTemplate redis, AccountRepository repository) {
		this.redis = redis;
		this.repository = repository;
	}

	@Override
	public Account getAccount(String id) {
		// HGETALL returns an empty map (not null) when the key doesn't exist
		Map<Object, Object> fields = redis.opsForHash().entries(key(id));
		if (!fields.isEmpty()) {
			return AccountFields.fromHash(fields);
		}

		Account account = repository.findById(id).orElseThrow(() -> new NotFoundException("Account", id));
		redis.opsForHash().putAll(key(id), AccountFields.toHash(account)); // HSET with all fields
		redis.expire(key(id), ACCOUNT_TTL); // the TTL belongs to the whole hash, not to a field
		return account;
	}

	@Override
	public long getDataUsedMb(String id) {
		// HGET reads one field: no need to load or parse the rest of the account
		Object used = redis.opsForHash().get(key(id), AccountFields.DATA_USED_MB);
		if (used != null) {
			return Long.parseLong((String) used);
		}
		return getAccount(id).dataUsedMb();
	}

	@Override
	public void addUsage(String id, long mb) {
		repository.addUsage(id, mb);

		// Only touch the cache if the account is cached. HINCRBY on a missing key would create
		// a half-empty hash containing only dataUsedMb, which getAccount() would then return.
		// If it's not cached, the next read loads the fresh value from the database anyway.
		// (A key expiring between hasKey and increment is still possible; a Lua script
		// would close that gap. Good enough for this workshop.)
		if (Boolean.TRUE.equals(redis.hasKey(key(id)))) {
			// HINCRBY is atomic inside Redis: no read-modify-write, no lost updates
			redis.opsForHash().increment(key(id), AccountFields.DATA_USED_MB, mb);
		}
	}

	String key(String id) {
		return "telco:account:" + id;
	}

}
