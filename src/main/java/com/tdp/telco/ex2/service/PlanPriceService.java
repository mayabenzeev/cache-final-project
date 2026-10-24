package com.tdp.telco.ex2.service;

import java.math.BigDecimal;
import java.time.Duration;

import tools.jackson.databind.json.JsonMapper;

import com.tdp.telco.common.entity.Plan;
import com.tdp.telco.common.exception.NotFoundException;
import com.tdp.telco.common.repository.PlanRepository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Exercise 2 - keeping a hand-written cache consistent when prices change.
 */
@Service
public class PlanPriceService {

	static final Duration PLAN_TTL = Duration.ofMinutes(10);

	private final StringRedisTemplate redis;
	private final JsonMapper json;
	private final PlanRepository repository;

	public PlanPriceService(StringRedisTemplate redis, JsonMapper json, PlanRepository repository) {
		this.redis = redis;
		this.json = json;
		this.repository = repository;
	}

	/** Provided: cache-aside read, same as the Exercise 1 solution. */
	public Plan getPlan(String id) {
		String cached = redis.opsForValue().get(planKey(id));
		if (cached != null) {
			return json.readValue(cached, Plan.class);
		}
		Plan plan = repository.findById(id).orElseThrow(() -> new NotFoundException("Plan", id));
		redis.opsForValue().set(planKey(id), json.writeValueAsString(plan), PLAN_TTL);
		return plan;
	}

	/** Strategy A: invalidate on write (explicit invalidation). */
	public Plan updatePriceAndEvict(String id, BigDecimal newPrice) {
		// Order matters: update the source of truth first, THEN delete the copy.
		// Deleting first leaves a window where a reader misses, reads the OLD price
		// from the database and puts it back in the cache.
		Plan updated = repository.updatePrice(id, newPrice);
		redis.delete(planKey(id));
		return updated;
	}

	/** Strategy B: write-through - the write path also refreshes the cache. */
	public Plan updatePriceWriteThrough(String id, BigDecimal newPrice) {
		Plan updated = repository.updatePrice(id, newPrice);
		// Keep the TTL as a safety net in case some other write path forgets to update the cache
		redis.opsForValue().set(planKey(id), json.writeValueAsString(updated), PLAN_TTL);
		return updated;
	}

	public String planKey(String id) {
		return "telco:plan:" + id;
	}

}
