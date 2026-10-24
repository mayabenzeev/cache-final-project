package com.tdp.telco.ex3.service;

import java.time.Duration;

import tools.jackson.databind.json.JsonMapper;

import com.tdp.telco.common.entity.Account;
import com.tdp.telco.common.exception.NotFoundException;
import com.tdp.telco.common.repository.AccountRepository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Exercise 3 - the "before" version, provided and working: the whole account is cached as
 * one JSON string. Read it and spot the problems in addUsage().
 */
@Service
public class StringAccountCache implements AccountCache {

	static final Duration ACCOUNT_TTL = Duration.ofMinutes(30);

	private final StringRedisTemplate redis;
	private final JsonMapper json;
	private final AccountRepository repository;

	public StringAccountCache(StringRedisTemplate redis, JsonMapper json, AccountRepository repository) {
		this.redis = redis;
		this.json = json;
		this.repository = repository;
	}

	@Override
	public Account getAccount(String id) {
		String cached = redis.opsForValue().get(key(id));
		if (cached != null) {
			return json.readValue(cached, Account.class);
		}
		Account account = repository.findById(id).orElseThrow(() -> new NotFoundException("Account", id));
		redis.opsForValue().set(key(id), json.writeValueAsString(account), ACCOUNT_TTL);
		return account;
	}

	@Override
	public long getDataUsedMb(String id) {
		// To read ONE number we transfer and parse the WHOLE account
		return getAccount(id).dataUsedMb();
	}

	@Override
	public void addUsage(String id, long mb) {
		repository.addUsage(id, mb);

		// Read-modify-write: GET the whole JSON, change one field, SET the whole JSON back.
		// Two requests doing this at the same time both read the same old value, and one
		// of the updates is lost.
		Account cached = getAccount(id);
		Account updated = cached.withDataUsedMb(cached.dataUsedMb() + mb);
		redis.opsForValue().set(key(id), json.writeValueAsString(updated), ACCOUNT_TTL);
	}

	public String key(String id) {
		return "telco:account-json:" + id;
	}

}
