package com.nexus.casty.cache;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.ZoneId;

public class CacheFile extends File {

	static final String PREFIX_KEY_SEPARATOR = "-";

	private final CacheReadWriteLock lock = new CacheReadWriteLock();
	private final Object accessTimeLock = new Object();
	private Instant lastAccessTime = null;
	private Object data = null;

	CacheFile(File parent, String prefix, String hash) {
		super(parent, prefix + PREFIX_KEY_SEPARATOR + hash);
	}

	public void updateAccessTime() {
		synchronized (accessTimeLock) {
			try {
				Instant now = Instant.now().atZone(ZoneId.systemDefault()).toInstant();
				Files.setAttribute(this.toPath(), "lastAccessTime", FileTime.from(now));
				lastAccessTime = now;
			} catch (IOException e) {
				System.err.println("Could not update lastAccessTime for " + this.getName());
				e.printStackTrace();
			}
		}
	}

	Instant getLastAccessTime() {
		synchronized (accessTimeLock) {
			try {
				if (lastAccessTime == null)
					lastAccessTime = ((FileTime) Files.getAttribute(this.toPath(), "lastAccessTime")).toInstant();
				return lastAccessTime;
			} catch (IOException e) {
				System.err.println("Could not get lastAccessTime for " + this.getName());
				e.printStackTrace();
				return Instant.EPOCH;
			}
		}
	}

	public CacheReadWriteLock getLocks() {
		return lock;
	}

	@SuppressWarnings("unchecked")
	public <T> @Nullable T getData() {
		return (T) data;
	}

	public void setData(Object data) {
		this.data = data;
	}

}

