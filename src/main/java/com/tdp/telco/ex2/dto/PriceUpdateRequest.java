package com.tdp.telco.ex2.dto;

import java.math.BigDecimal;

public record PriceUpdateRequest(BigDecimal monthlyPrice) {
}
