package com.tdp.telco.controller;

import com.tdp.telco.dto.PriceUpdateRequest;
import com.tdp.telco.entity.Plan;
import com.tdp.telco.service.AnnotatedPlanPriceService;
import com.tdp.telco.service.PlanPriceService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * PUT .../price?strategy=evict          (default) invalidate on write
 * PUT .../price?strategy=write-through  update the cache on write
 */
@RestController
@RequestMapping("/ex2")
class PlanPriceController {

	private final PlanPriceService manual;
	private final AnnotatedPlanPriceService annotated;

	PlanPriceController(PlanPriceService manual, AnnotatedPlanPriceService annotated) {
		this.manual = manual;
		this.annotated = annotated;
	}

	@GetMapping("/plans/{id}")
	Plan getPlan(@PathVariable String id) {
		return manual.getPlan(id);
	}

	@PutMapping("/plans/{id}/price")
	Plan updatePrice(@PathVariable String id, @RequestBody PriceUpdateRequest update,
			@RequestParam(defaultValue = "evict") String strategy) {
		return isWriteThrough(strategy) ? manual.updatePriceWriteThrough(id, update.monthlyPrice())
				: manual.updatePriceAndEvict(id, update.monthlyPrice());
	}

	@GetMapping("/annotated/plans/{id}")
	Plan getPlanAnnotated(@PathVariable String id) {
		return annotated.getPlan(id);
	}

	@PutMapping("/annotated/plans/{id}/price")
	Plan updatePriceAnnotated(@PathVariable String id, @RequestBody PriceUpdateRequest update,
			@RequestParam(defaultValue = "evict") String strategy) {
		return isWriteThrough(strategy) ? annotated.updatePriceWriteThrough(id, update.monthlyPrice())
				: annotated.updatePriceAndEvict(id, update.monthlyPrice());
	}

	private static boolean isWriteThrough(String strategy) {
		return "write-through".equalsIgnoreCase(strategy);
	}

}
