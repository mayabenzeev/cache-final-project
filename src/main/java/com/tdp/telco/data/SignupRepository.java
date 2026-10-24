package com.tdp.telco.data;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import org.springframework.stereotype.Repository;

/**
 * Plan sign-ups (Bonus A). Provided code - don't change it.
 */
@Repository
public class SignupRepository {

	public record Signup(String customerId, String planId) {
	}

	private final SlowDatabase db;
	private final List<Signup> signups = new CopyOnWriteArrayList<>();

	public SignupRepository(SlowDatabase db) {
		this.db = db;
	}

	public void save(String customerId, String planId) {
		signups.add(new Signup(customerId, planId));
	}

	/** The expensive "SELECT plan_id, COUNT(*) ... GROUP BY plan_id" query. Slow on purpose. */
	public Map<String, Long> countByPlan() {
		db.slowRead();
		return signups.stream().collect(Collectors.groupingBy(Signup::planId, Collectors.counting()));
	}

	public void reset() {
		signups.clear();
	}

}
