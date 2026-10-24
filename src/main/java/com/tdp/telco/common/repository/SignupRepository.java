package com.tdp.telco.common.repository;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import com.tdp.telco.common.entity.Signup;

import org.springframework.stereotype.Repository;

/**
 * Plan sign-ups (Bonus A). Provided code - don't change it.
 */
@Repository
public class SignupRepository {

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
