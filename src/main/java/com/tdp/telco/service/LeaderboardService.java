package com.tdp.telco.service;

import java.util.Comparator;
import java.util.List;

import com.tdp.telco.dto.PlanRankResponse;
import com.tdp.telco.repository.SignupRepository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Bonus A - a live "most popular plans" leaderboard with a Redis sorted set.
 */
@Service
public class LeaderboardService {

	// One key for the whole leaderboard: member = planId, score = number of sign-ups.
	public static final String LEADERBOARD_KEY = "telco:leaderboard:plan-signups";

	private final StringRedisTemplate redis; // redis.opsForZSet() -> ZINCRBY / ZREVRANGE
	private final SignupRepository signups;

	public LeaderboardService(StringRedisTemplate redis, SignupRepository signups) {
		this.redis = redis;
		this.signups = signups;
	}

	public void recordSignup(String customerId, String planId) {
		signups.save(customerId, planId);
		// TODO (Bonus A, step 1): add 1 to this plan's score in the sorted set:
		// redis.opsForZSet().incrementScore(LEADERBOARD_KEY, planId, 1)   (ZINCRBY)
	}

	public List<PlanRankResponse> topPlans(int limit) {
		// TODO (Bonus A, step 2): read the top `limit` plans, highest score first:
		// redis.opsForZSet().reverseRangeWithScores(LEADERBOARD_KEY, start, end)   (ZREVRANGE ... WITHSCORES)
		// Redis ranges are inclusive. Map each TypedTuple to new PlanRankResponse(value, score).
		return List.of();
	}

	/** Provided: the "before" version - an expensive GROUP BY on every request. */
	public List<PlanRankResponse> topPlansFromDatabase(int limit) {
		return signups.countByPlan()
			.entrySet()
			.stream()
			.map(e -> new PlanRankResponse(e.getKey(), e.getValue()))
			.sorted(Comparator.comparingLong(PlanRankResponse::signups).reversed())
			.limit(limit)
			.toList();
	}

}
