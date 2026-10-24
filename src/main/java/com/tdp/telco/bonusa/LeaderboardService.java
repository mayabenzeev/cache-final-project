package com.tdp.telco.bonusa;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.tdp.telco.data.SignupRepository;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;

/**
 * Bonus A - a live "most popular plans" leaderboard with a Redis sorted set.
 */
@Service
public class LeaderboardService {

	public record PlanRank(String planId, long signups) {
	}

	// One key for the whole leaderboard: member = planId, score = number of sign-ups.
	// No TTL: this isn't a copy that goes stale, it's kept up to date on every sign-up.
	// If Redis loses it, rebuild it once from SignupRepository.countByPlan().
	static final String LEADERBOARD_KEY = "telco:leaderboard:plan-signups";

	private final StringRedisTemplate redis;
	private final SignupRepository signups;

	public LeaderboardService(StringRedisTemplate redis, SignupRepository signups) {
		this.redis = redis;
		this.signups = signups;
	}

	public void recordSignup(String customerId, String planId) {
		signups.save(customerId, planId);
		// ZINCRBY: atomic, O(log N), and the set stays sorted
		redis.opsForZSet().incrementScore(LEADERBOARD_KEY, planId, 1);
	}

	public List<PlanRank> topPlans(int limit) {
		// ZREVRANGE ... WITHSCORES: highest score first; ranges are inclusive, so 0..limit-1
		Set<TypedTuple<String>> top = redis.opsForZSet().reverseRangeWithScores(LEADERBOARD_KEY, 0, limit - 1);
		if (top == null) {
			return List.of();
		}
		return top.stream().map(t -> new PlanRank(t.getValue(), t.getScore().longValue())).toList();
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
