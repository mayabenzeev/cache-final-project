package com.tdp.telco.data;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Pretends to be a slow, overloaded database.
 * Every repository call goes through here so you can *feel* what caching saves.
 *
 * Provided code - you don't need to change anything in the data package.
 */
@Component
public class SlowDatabase {

	private final Duration readDelay;
	private final Duration writeDelay;

	public SlowDatabase(@Value("${telco.slow-db.read-delay:1000ms}") Duration readDelay,
			@Value("${telco.slow-db.write-delay:300ms}") Duration writeDelay) {
		this.readDelay = readDelay;
		this.writeDelay = writeDelay;
	}

	void slowRead() {
		sleep(readDelay);
	}

	void slowWrite() {
		sleep(writeDelay);
	}

	private static void sleep(Duration duration) {
		try {
			Thread.sleep(duration);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

}
