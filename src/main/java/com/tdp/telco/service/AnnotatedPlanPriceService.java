package com.tdp.telco.service;

import java.math.BigDecimal;

import com.tdp.telco.entity.Plan;
import com.tdp.telco.exception.NotFoundException;
import com.tdp.telco.repository.PlanRepository;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Exercise 2 - the same two strategies with Spring's cache annotations.
 */
@Service
public class AnnotatedPlanPriceService {

	private final PlanRepository repository;

	public AnnotatedPlanPriceService(PlanRepository repository) {
		this.repository = repository;
	}

	/** Provided: cached read (same cache as Exercise 1). */
	@Cacheable(cacheNames = "plans", key = "#id")
	public Plan getPlan(String id) {
		return repository.findById(id).orElseThrow(() -> new NotFoundException("Plan", id));
	}

	// TODO (Exercise 2, step 3a): invalidate on write with @CacheEvict
	// (same cache name and key as getPlan). Does Spring evict before or after the method runs?
	public Plan updatePriceAndEvict(String id, BigDecimal newPrice) {
		return repository.updatePrice(id, newPrice);
	}

	// TODO (Exercise 2, step 3b): write-through with @CachePut (same cache name and key).
	// What does Spring put in the cache - the arguments, or the return value?
	public Plan updatePriceWriteThrough(String id, BigDecimal newPrice) {
		return repository.updatePrice(id, newPrice);
	}

}
