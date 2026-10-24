package com.tdp.telco.ex3.controller;

import com.tdp.telco.common.entity.Account;
import com.tdp.telco.ex3.dto.UsageEventRequest;
import com.tdp.telco.ex3.dto.UsageResponse;
import com.tdp.telco.ex3.service.AccountCache;
import com.tdp.telco.ex3.service.HashAccountCache;
import com.tdp.telco.ex3.service.StringAccountCache;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Exercise 3.
 * ?model=hash   (default) HashAccountCache
 * ?model=string StringAccountCache
 */
@RestController
@RequestMapping("/ex3/accounts")
class AccountController {

	private final HashAccountCache hash;
	private final StringAccountCache string;

	AccountController(HashAccountCache hash, StringAccountCache string) {
		this.hash = hash;
		this.string = string;
	}

	@GetMapping("/{id}")
	Account getAccount(@PathVariable String id, @RequestParam(defaultValue = "hash") String model) {
		return cache(model).getAccount(id);
	}

	@GetMapping("/{id}/usage")
	UsageResponse getUsage(@PathVariable String id, @RequestParam(defaultValue = "hash") String model) {
		return new UsageResponse(cache(model).getDataUsedMb(id));
	}

	@PostMapping("/{id}/usage")
	UsageResponse addUsage(@PathVariable String id, @RequestBody UsageEventRequest event,
			@RequestParam(defaultValue = "hash") String model) {
		cache(model).addUsage(id, event.mb());
		return new UsageResponse(cache(model).getDataUsedMb(id));
	}

	private AccountCache cache(String model) {
		return "string".equalsIgnoreCase(model) ? string : hash;
	}

}
