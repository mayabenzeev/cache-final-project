package com.tdp.telco.ex1.controller;

import com.tdp.telco.common.entity.Plan;
import com.tdp.telco.ex1.service.AnnotatedPlanService;
import com.tdp.telco.ex1.service.ManualPlanService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ex1")
class PlanCatalogController {

	private final ManualPlanService manual;
	private final AnnotatedPlanService annotated;

	PlanCatalogController(ManualPlanService manual, AnnotatedPlanService annotated) {
		this.manual = manual;
		this.annotated = annotated;
	}

	@GetMapping("/plans/{id}")
	Plan getPlan(@PathVariable String id) {
		return manual.getPlan(id);
	}

	@GetMapping("/annotated/plans/{id}")
	Plan getPlanAnnotated(@PathVariable String id) {
		return annotated.getPlan(id);
	}

}
