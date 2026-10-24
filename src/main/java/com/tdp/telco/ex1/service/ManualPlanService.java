package com.tdp.telco.ex1.service;

import java.time.Duration;

import tools.jackson.databind.json.JsonMapper;

import com.tdp.telco.common.entity.Plan;
import com.tdp.telco.common.exception.NotFoundException;
import com.tdp.telco.common.repository.PlanRepository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Exercise 1 - cache-aside written by hand with StringRedisTemplate.
 */
@Service
public class ManualPlanService {

	// Plans change a few times a year and a few minutes of a stale price is acceptable.
	// 10 minutes gives a near-100% hit rate; Exercise 2 handles price changes explicitly,
	// so the TTL is only a safety net.
	static final Duration PLAN_TTL = Duration.ofMinutes(10);

	private final StringRedisTemplate redis;
	private final JsonMapper json;
	private final PlanRepository repository;

	public ManualPlanService(StringRedisTemplate redis, JsonMapper json, PlanRepository repository) {
		this.redis = redis;
		this.json = json;
		this.repository = repository;
	}

	public Plan getPlan(String id) {
		String key = planKey(id);

		// 1. Look in the cache first
		String cached = redis.opsForValue().get(key);
		if (cached != null) {
			return json.readValue(cached, Plan.class); // HIT
		}

		// 2. MISS: go to the source of truth
		Plan plan = repository.findById(id).orElseThrow(() -> new NotFoundException("Plan", id));

		// 3. Populate the cache for the next reader, with a TTL
		redis.opsForValue().set(key, json.writeValueAsString(plan), PLAN_TTL);
		return plan;
	}

	// <app>:<entity>:<id>
	// - "telco" namespaces us if another service shares this Redis
	// - "plan" prevents collisions with other entities (telco:account:acc-1001)
	// - readable in redis-cli, and easy to find with SCAN telco:plan:*
	public String planKey(String id) {
		return "telco:plan:" + id;
	}

}
