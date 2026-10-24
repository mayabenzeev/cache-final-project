package com.tdp.telco.common.entity;

import java.math.BigDecimal;

/**
 * A mobile plan in the catalogue.
 */
public record Plan(String id, String name, BigDecimal monthlyPrice, int dataGb, String network) {

	public Plan withMonthlyPrice(BigDecimal newPrice) {
		return new Plan(id, name, newPrice, dataGb, network);
	}

}
