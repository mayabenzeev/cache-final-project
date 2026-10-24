package com.tdp.telco.bonusb;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Bonus B - login sessions. Lots of them, each only useful for a while.
 */
@Component
public class SessionStore {

	// A session is worthless after the user's idle timeout. The TTL cleans it up on time AND
	// marks it as evictable under volatile-* eviction policies.
	static final Duration SESSION_TTL = Duration.ofMinutes(30);

	private final StringRedisTemplate redis;

	public SessionStore(StringRedisTemplate redis) {
		this.redis = redis;
	}

	public void saveSession(String sessionId, String payload) {
		redis.opsForValue().set(key(sessionId), payload, SESSION_TTL);
	}

	public String getSession(String sessionId) {
		return redis.opsForValue().get(key(sessionId));
	}

	static String key(String sessionId) {
		return "telco:session:" + sessionId;
	}

}
