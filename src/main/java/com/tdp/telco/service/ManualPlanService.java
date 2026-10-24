package com.tdp.telco.service;

import java.time.Duration;

import tools.jackson.databind.json.JsonMapper;

import com.tdp.telco.entity.Plan;
import com.tdp.telco.exception.NotFoundException;
import com.tdp.telco.repository.PlanRepository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Exercise 1 - cache-aside written by hand with StringRedisTemplate.
 */
@Service
public class ManualPlanService {

	// TODO (Exercise 1, step 2): choose a TTL for plans and write down WHY in this comment.
	// Duration.ZERO is only a placeholder - Redis rejects it.
	static final Duration PLAN_TTL = Duration.ZERO;

	private final StringRedisTemplate redis; // redis.opsForValue() -> GET / SET on string values
	private final JsonMapper json; // json.writeValueAsString(plan) / json.readValue(text, Plan.class)
	private final PlanRepository repository; // the slow source of truth (~1s per read)

	public ManualPlanService(StringRedisTemplate redis, JsonMapper json, PlanRepository repository) {
		this.redis = redis;
		this.json = json;
		this.repository = repository;
	}

	public Plan getPlan(String id) {
		// TODO (Exercise 1, step 2): implement cache-aside
		// 1. Look in Redis first: redis.opsForValue().get(planKey(id))
		// 2. HIT  -> turn the JSON back into a Plan and return it
		// 3. MISS -> load from the repository (below), store it as JSON WITH a TTL:
		// redis.opsForValue().set(key, value, PLAN_TTL), then return it
		return repository.findById(id).orElseThrow(() -> new NotFoundException("Plan", id));
	}

	// TODO (Exercise 1, step 1): design the cache key for a plan.
	// Think: what else could live in this Redis? How would you find all plan keys?
	// Write down in a comment why you chose this format.
	public String planKey(String id) {
		return id;
	}

}
