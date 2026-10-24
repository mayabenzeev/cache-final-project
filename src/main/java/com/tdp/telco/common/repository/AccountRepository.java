package com.tdp.telco.common.repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.tdp.telco.common.entity.Account;
import com.tdp.telco.common.exception.NotFoundException;

import org.springframework.stereotype.Repository;

/**
 * The customer accounts "database" - the source of truth for accounts.
 * Provided code - don't change it.
 */
@Repository
public class AccountRepository {

	private final SlowDatabase db;
	private final Map<String, Account> accounts = new ConcurrentHashMap<>();
	private final AtomicInteger reads = new AtomicInteger();

	public AccountRepository(SlowDatabase db) {
		this.db = db;
		reset();
	}

	public Optional<Account> findById(String id) {
		db.slowRead();
		reads.incrementAndGet();
		return Optional.ofNullable(accounts.get(id));
	}

	/** Records data usage. The database update itself is atomic. */
	public Account addUsage(String id, long mb) {
		db.slowWrite();
		Account updated = accounts.computeIfPresent(id, (key, a) -> a.withDataUsedMb(a.dataUsedMb() + mb));
		if (updated == null) {
			throw new NotFoundException("Account", id);
		}
		return updated;
	}

	public int readCount() {
		return reads.get();
	}

	/** Restores the original accounts. Used by tests. */
	public void reset() {
		accounts.clear();
		reads.set(0);
		save(new Account("acc-1001", "Noa Cohen", "12 Herzl St, Tel Aviv", "unlimited", 4_200, 102_400));
		save(new Account("acc-1002", "Daniel Levi", "3 Ben Yehuda St, Haifa", "basic", 18_750, 51_200));
		save(new Account("acc-1003", "Maria Garcia", "200 Main St, Dallas, TX", "family-4", 120_000, 409_600));
	}

	private void save(Account account) {
		accounts.put(account.id(), account);
	}

}
