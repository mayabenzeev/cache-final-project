package com.tdp.telco.data;

/**
 * A customer account. Most fields rarely change; dataUsedMb changes all the time.
 */
public record Account(String id, String name, String address, String planId, long dataUsedMb, long dataLimitMb) {

	public Account withDataUsedMb(long newDataUsedMb) {
		return new Account(id, name, address, planId, newDataUsedMb, dataLimitMb);
	}

}
