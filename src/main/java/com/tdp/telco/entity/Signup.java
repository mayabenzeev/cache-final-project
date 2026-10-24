package com.tdp.telco.entity;

/**
 * A customer signing up for a plan.
 */
public record Signup(String customerId, String planId) {
}
