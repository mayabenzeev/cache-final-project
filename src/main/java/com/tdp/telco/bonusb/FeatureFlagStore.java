package com.tdp.telco.bonusb;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Bonus B - feature flags. Few keys, needed by every request, and they must never disappear.
 * Provided code: deliberately stored WITHOUT a TTL.
 */
@Component
public class FeatureFlagStore {

	private final StringRedisTemplate redis;

	public FeatureFlagStore(StringRedisTemplate redis) {
		this.redis = redis;
	}

	public void setFlag(String name, boolean enabled) {
		redis.opsForValue().set(key(name), Boolean.toString(enabled));
	}

	public String getFlag(String name) {
		return redis.opsForValue().get(key(name));
	}

	static String key(String name) {
		return "telco:flag:" + name;
	}

}
