package com.tdp.telco.bonusb.service;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Bonus B - login sessions. Lots of them, each only useful for a while.
 */
@Component
public class SessionStore {

	static final Duration SESSION_TTL = Duration.ofMinutes(30);

	private final StringRedisTemplate redis;

	public SessionStore(StringRedisTemplate redis) {
		this.redis = redis;
	}

	public void saveSession(String sessionId, String payload) {
		// TODO (Bonus B, step 2): sessions are stored forever. Give them SESSION_TTL.
		// Besides cleaning up on time, how does a TTL change what the eviction policy may delete?
		redis.opsForValue().set(key(sessionId), payload);
	}

	public String getSession(String sessionId) {
		return redis.opsForValue().get(key(sessionId));
	}

	public static String key(String sessionId) {
		return "telco:session:" + sessionId;
	}

}
