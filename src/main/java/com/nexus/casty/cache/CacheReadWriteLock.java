package com.nexus.casty.cache;

import java.util.concurrent.Semaphore;

public class CacheReadWriteLock {

	private final static int MAX_PERMITS = 10;
	private final Semaphore semaphore = new Semaphore(10);

	CacheReadWriteLock() {}

	public void read() {
		semaphore.acquireUninterruptibly();
	}

	boolean tryRead() {
		return semaphore.tryAcquire();
	}

	public void readRelease() {
		semaphore.release();
	}


	void writeRelease() {
		semaphore.release(MAX_PERMITS);
	}


	public void upgrade() {
		semaphore.acquireUninterruptibly(MAX_PERMITS - 1);
	}

	boolean tryUpgrade() {
		return semaphore.tryAcquire(MAX_PERMITS - 1);
	}


	public void downgrade() {
		semaphore.release(MAX_PERMITS - 1);
	}
}
