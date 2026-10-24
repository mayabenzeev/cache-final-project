package com.tdp.telco.ex1;

import com.tdp.telco.data.Plan;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ex1")
class Ex1PlanController {

	private final ManualPlanService manual;
	private final AnnotatedPlanService annotated;

	Ex1PlanController(ManualPlanService manual, AnnotatedPlanService annotated) {
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
