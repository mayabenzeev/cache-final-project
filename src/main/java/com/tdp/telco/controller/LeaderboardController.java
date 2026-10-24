package com.tdp.telco.controller;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import com.tdp.telco.dto.PlanRankResponse;
import com.tdp.telco.dto.SignupRequest;
import com.tdp.telco.service.LeaderboardService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/bonus-a")
class LeaderboardController {

	// Some plans are more popular than others, so the simulated ranking is interesting
	private static final String[] WEIGHTED_PLANS = { "unlimited", "unlimited", "unlimited", "unlimited", "basic",
			"basic", "basic", "unlimited-premium", "unlimited-premium", "family-4", "prepaid-15" };

	private final LeaderboardService leaderboard;

	LeaderboardController(LeaderboardService leaderboard) {
		this.leaderboard = leaderboard;
	}

	@PostMapping("/signups")
	void signUp(@RequestBody SignupRequest request) {
		leaderboard.recordSignup(request.customerId(), request.planId());
	}

	/** Generates random sign-ups so the leaderboard has something to show. */
	@PostMapping("/signups/simulate")
	Map<String, Integer> simulate(@RequestParam(defaultValue = "500") int count) {
		ThreadLocalRandom random = ThreadLocalRandom.current();
		for (int i = 0; i < count; i++) {
			String plan = WEIGHTED_PLANS[random.nextInt(WEIGHTED_PLANS.length)];
			leaderboard.recordSignup("cust-" + random.nextInt(100_000), plan);
		}
		return Map.of("simulated", count);
	}

	@GetMapping("/plans/top")
	List<PlanRankResponse> top(@RequestParam(defaultValue = "10") int limit) {
		return leaderboard.topPlans(limit);
	}

	@GetMapping("/plans/top-from-db")
	List<PlanRankResponse> topFromDatabase(@RequestParam(defaultValue = "10") int limit) {
		return leaderboard.topPlansFromDatabase(limit);
	}

}
