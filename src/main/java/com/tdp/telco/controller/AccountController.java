package com.tdp.telco.controller;

import java.util.Map;

import com.tdp.telco.entity.Account;
import com.tdp.telco.service.AccountCache;
import com.tdp.telco.service.HashAccountCache;
import com.tdp.telco.service.StringAccountCache;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * ?model=hash   (default) HashAccountCache
 * ?model=string StringAccountCache
 */
@RestController
@RequestMapping("/ex3/accounts")
class AccountController {

	record UsageEvent(long mb) {
	}

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
	Map<String, Long> getUsage(@PathVariable String id, @RequestParam(defaultValue = "hash") String model) {
		return Map.of("dataUsedMb", cache(model).getDataUsedMb(id));
	}

	@PostMapping("/{id}/usage")
	Map<String, Long> addUsage(@PathVariable String id, @RequestBody UsageEvent event,
			@RequestParam(defaultValue = "hash") String model) {
		cache(model).addUsage(id, event.mb());
		return Map.of("dataUsedMb", cache(model).getDataUsedMb(id));
	}

	private AccountCache cache(String model) {
		return "string".equalsIgnoreCase(model) ? string : hash;
	}

}
