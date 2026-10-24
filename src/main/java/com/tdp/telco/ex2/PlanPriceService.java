package com.tdp.telco.ex2;

import java.math.BigDecimal;
import java.time.Duration;

import tools.jackson.databind.json.JsonMapper;

import com.tdp.telco.data.NotFoundException;
import com.tdp.telco.data.Plan;
import com.tdp.telco.data.PlanRepository;

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
		// BUG: the database is updated, but the cached copy still has the old price
		// for up to 10 minutes.
		// TODO (Exercise 2, step 1): after updating the source of truth, delete the cached copy
		// so the next read is a MISS and reloads the new price. Use redis.delete(key).
		// Think: does it matter whether you delete BEFORE or AFTER the database update?
		return repository.updatePrice(id, newPrice);
	}

	/** Strategy B: write-through - the write path also refreshes the cache. */
	public Plan updatePriceWriteThrough(String id, BigDecimal newPrice) {
		// TODO (Exercise 2, step 2): after updating the source of truth, write the UPDATED plan
		// into the cache (as JSON) so the next read is still a HIT with the new price.
		// Watch out: a SET without a TTL removes the key's existing TTL.
		return repository.updatePrice(id, newPrice);
	}

	String planKey(String id) {
		return "telco:plan:" + id;
	}

}
