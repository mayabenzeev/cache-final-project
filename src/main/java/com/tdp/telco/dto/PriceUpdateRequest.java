package com.tdp.telco.dto;

import java.math.BigDecimal;

public record PriceUpdateRequest(BigDecimal monthlyPrice) {
}
