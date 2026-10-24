package com.tdp.telco.bonusa;

import java.util.Comparator;
import java.util.List;

import com.tdp.telco.data.SignupRepository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Bonus A - a live "most popular plans" leaderboard with a Redis sorted set.
 */
@Service
public class LeaderboardService {

	public record PlanRank(String planId, long signups) {
	}

	// One key for the whole leaderboard: member = planId, score = number of sign-ups.
	static final String LEADERBOARD_KEY = "telco:leaderboard:plan-signups";

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

	public List<PlanRank> topPlans(int limit) {
		// TODO (Bonus A, step 2): read the top `limit` plans, highest score first:
		// redis.opsForZSet().reverseRangeWithScores(LEADERBOARD_KEY, start, end)   (ZREVRANGE ... WITHSCORES)
		// Redis ranges are inclusive. Map each TypedTuple to new PlanRank(value, score).
		return List.of();
	}

	/** Provided: the "before" version - an expensive GROUP BY on every request. */
	public List<PlanRank> topPlansFromDatabase(int limit) {
		return signups.countByPlan()
			.entrySet()
			.stream()
			.map(e -> new PlanRank(e.getKey(), e.getValue()))
			.sorted(Comparator.comparingLong(PlanRank::signups).reversed())
			.limit(limit)
			.toList();
	}

}
