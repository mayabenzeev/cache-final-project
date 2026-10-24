package com.tdp.telco.common.config;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Logs how long every request took, so you can see cache hits vs misses in the app console:
 * <pre>
 *   GET /ex1/plans/basic -> 200 in 1004 ms
 *   GET /ex1/plans/basic -> 200 in 3 ms
 * </pre>
 */
@Component
class ResponseTimeFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger("response-time");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		long start = System.nanoTime();
		try {
			chain.doFilter(request, response);
		}
		finally {
			long ms = (System.nanoTime() - start) / 1_000_000;
			log.info("{} {} -> {} in {} ms", request.getMethod(), request.getRequestURI(), response.getStatus(), ms);
		}
	}

}
