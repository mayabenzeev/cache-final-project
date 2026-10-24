package com.tdp.telco.ex3;

import com.tdp.telco.data.Account;

/**
 * Two implementations of the same account cache, so you can compare them:
 * {@link StringAccountCache} (whole account as one JSON string) and
 * {@link HashAccountCache} (one Redis hash field per account field).
 */
public interface AccountCache {

	Account getAccount(String id);

	long getDataUsedMb(String id);

	void addUsage(String id, long mb);

}
