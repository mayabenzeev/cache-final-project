package com.tdp.telco.service;

import java.util.LinkedHashMap;
import java.util.Map;

import com.tdp.telco.entity.Account;

/**
 * Converts an Account to and from Redis hash fields. Provided code.
 *
 * A Redis hash is a flat map of field -> string value:
 * <pre>
 *   telco:account:acc-1001
 *     name        "Noa Cohen"
 *     address     "12 Herzl St, Tel Aviv"
 *     planId      "unlimited"
 *     dataUsedMb  "4200"
 *     dataLimitMb "102400"
 * </pre>
 */
public final class AccountFields {

	static final String DATA_USED_MB = "dataUsedMb";

	private AccountFields() {
	}

	static Map<String, String> toHash(Account account) {
		Map<String, String> fields = new LinkedHashMap<>();
		fields.put("id", account.id());
		fields.put("name", account.name());
		fields.put("address", account.address());
		fields.put("planId", account.planId());
		fields.put(DATA_USED_MB, Long.toString(account.dataUsedMb()));
		fields.put("dataLimitMb", Long.toString(account.dataLimitMb()));
		return fields;
	}

	static Account fromHash(Map<Object, Object> fields) {
		return new Account((String) fields.get("id"), (String) fields.get("name"), (String) fields.get("address"),
				(String) fields.get("planId"), Long.parseLong((String) fields.get(DATA_USED_MB)),
				Long.parseLong((String) fields.get("dataLimitMb")));
	}

}
