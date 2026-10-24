package com.tdp.telco.repository;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.tdp.telco.entity.Plan;
import com.tdp.telco.exception.NotFoundException;

import org.springframework.stereotype.Repository;

/**
 * The plan catalogue "database" - the source of truth for plans.
 * Reads are slow (about 1 second). Provided code - don't change it.
 */
@Repository
public class PlanRepository {

	private final SlowDatabase db;
	private final Map<String, Plan> plans = new ConcurrentHashMap<>();
	private final AtomicInteger reads = new AtomicInteger();

	public PlanRepository(SlowDatabase db) {
		this.db = db;
		reset();
	}

	public Optional<Plan> findById(String id) {
		db.slowRead();
		reads.incrementAndGet();
		return Optional.ofNullable(plans.get(id));
	}

	public List<Plan> findAll() {
		db.slowRead();
		reads.incrementAndGet();
		return plans.values().stream().sorted(Comparator.comparing(Plan::monthlyPrice)).toList();
	}

	public Plan updatePrice(String id, BigDecimal newPrice) {
		db.slowWrite();
		Plan updated = plans.computeIfPresent(id, (key, plan) -> plan.withMonthlyPrice(newPrice));
		if (updated == null) {
			throw new NotFoundException("Plan", id);
		}
		return updated;
	}

	/** How many times the slow database was read. Tests use this to tell a cache hit from a miss. */
	public int readCount() {
		return reads.get();
	}

	/** Restores the original catalogue. Used by tests. */
	public void reset() {
		plans.clear();
		reads.set(0);
		save(new Plan("prepaid-15", "Prepaid 15GB", new BigDecimal("30.00"), 15, "4G LTE"));
		save(new Plan("basic", "Basic 50GB", new BigDecimal("45.00"), 50, "5G"));
		save(new Plan("unlimited", "Unlimited Starter", new BigDecimal("65.00"), 100, "5G"));
		save(new Plan("unlimited-premium", "Unlimited Premium", new BigDecimal("85.00"), 250, "5G+"));
		save(new Plan("family-4", "Family 4 Lines", new BigDecimal("160.00"), 400, "5G"));
	}

	private void save(Plan plan) {
		plans.put(plan.id(), plan);
	}

}
