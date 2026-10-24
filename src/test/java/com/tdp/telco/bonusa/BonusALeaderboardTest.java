package com.tdp.telco.bonusa;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.tdp.telco.WorkshopTest;
import com.tdp.telco.dto.PlanRankResponse;
import com.tdp.telco.service.LeaderboardService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.DataType;

import static org.assertj.core.api.Assertions.assertThat;

/** Bonus A - a live leaderboard with a sorted set in LeaderboardService. */
class BonusALeaderboardTest extends WorkshopTest {

	@Autowired
	LeaderboardService leaderboard;

	@Test
	void signupsAreKeptInASortedSet() {
		leaderboard.recordSignup("cust-1", "unlimited");

		assertThat(redis.type(LeaderboardService.LEADERBOARD_KEY))
			.as("Expected a sorted set (ZSET) under %s. Use opsForZSet().incrementScore(key, planId, 1)",
					LeaderboardService.LEADERBOARD_KEY)
			.isEqualTo(DataType.ZSET);
	}

	@Test
	void topPlansAreRankedBySignupsHighestFirst() {
		signUp("unlimited", 3);
		signUp("basic", 5);
		signUp("family-4", 1);

		List<PlanRankResponse> top = leaderboard.topPlans(10);

		assertThat(top)
			.as("Use opsForZSet().reverseRangeWithScores(key, 0, limit - 1): highest score first")
			.containsExactly(new PlanRankResponse("basic", 5), new PlanRankResponse("unlimited", 3),
					new PlanRankResponse("family-4", 1));
	}

	@Test
	void topPlansRespectsTheLimit() {
		signUp("unlimited", 3);
		signUp("basic", 5);
		signUp("family-4", 1);

		assertThat(leaderboard.topPlans(2))
			.as("Redis ranges are inclusive: for the top 2, ask for indexes 0..1")
			.containsExactly(new PlanRankResponse("basic", 5), new PlanRankResponse("unlimited", 3));
	}

	private void signUp(String planId, int times) {
		for (int i = 0; i < times; i++) {
			leaderboard.recordSignup("cust-" + planId + "-" + i, planId);
		}
	}

}
