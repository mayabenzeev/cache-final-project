package com.tdp.telco.ex2;

import java.math.BigDecimal;

import com.tdp.telco.data.NotFoundException;
import com.tdp.telco.data.Plan;
import com.tdp.telco.data.PlanRepository;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
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

	// Runs the method, then DEL plans::{id}. Evicts AFTER the database write by default
	// (beforeInvocation = false), which is the safe order.
	@CacheEvict(cacheNames = "plans", key = "#id")
	public Plan updatePriceAndEvict(String id, BigDecimal newPrice) {
		return repository.updatePrice(id, newPrice);
	}

	// Always runs the method, then SETs the RETURNED value into plans::{id}.
	// That's why the method must return the updated Plan, not void.
	@CachePut(cacheNames = "plans", key = "#id")
	public Plan updatePriceWriteThrough(String id, BigDecimal newPrice) {
		return repository.updatePrice(id, newPrice);
	}

}
