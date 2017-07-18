package com.nexus.casty.cache;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.nexus.casty.Utils;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

public class CacheManager {

	public static final long DEFAULT_CACHE_SIZE = 250 * 1024 * 1024;

	private final Map<String, CacheFile> cacheMap = new HashMap<>();
	private final AtomicLong maxCacheSize = new AtomicLong(-1);
	private final String[] prefixes;
	private final File cacheDir;
	private final File dataFile;
	private final Type dataType;
	private final Gson gson;
	private static final ThreadPoolExecutor cacheCleanExecutor = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1), new ThreadPoolExecutor.DiscardPolicy());

	public CacheManager(File cacheDir, String[] prefixes, File dataFile, Gson gson, Type dataType) throws IOException {
		this.prefixes = prefixes;
		this.dataFile = dataFile;
		this.dataType = dataType;
		this.cacheDir = cacheDir;
		this.gson = gson;

		// Create cache directory
		if (!cacheDir.exists() && !cacheDir.mkdir()) {
			throw new IOException("Could not create cache directory");
		}

		// Add existing files to cache
		File[] files = cacheDir.listFiles();
		if (files != null) {
			for (File f : files) {
				if (!f.isFile()) continue;
				// Check prefix
				int i = f.getName().indexOf(CacheFile.PREFIX_KEY_SEPARATOR);
				if (i < 0 || i == f.getName().length() - 1) continue;

				String prefix = f.getName().substring(0, i);
				if (!isPrefixValid(prefix)) continue;

				String hashedKey = f.getName().substring(i + 1);
				String key = Utils.base36ToString(hashedKey);
				cacheMap.computeIfAbsent(prefix + key, k -> new CacheFile(cacheDir, prefix, hashedKey));
			}
		}

		// Load data cache
		if (dataFile != null && dataFile.exists() && dataFile.isFile()) {
			try (FileReader fileReader = new FileReader(this.dataFile)) {
				JsonReader reader = gson.newJsonReader(fileReader);
				reader.beginObject();
				while (reader.hasNext()) {
					String id = reader.nextName();
					CacheFile f = cacheMap.get(id);
					if (f != null) f.setData(gson.fromJson(reader, dataType));
					else reader.skipValue();
				}
			} catch (Exception e) {
				System.err.println("Could not load cache data");
				e.printStackTrace();
			}
		}
		saveData();

	}

	public @Nullable CacheFile getCacheFile(String prefix, String id) {
		if (!isPrefixValid(prefix)) return null;
		CacheFile file;
		synchronized (this) {
			file = cacheMap.compute(prefix + id, (k, f) -> {
				if (f == null) f = new CacheFile(cacheDir, prefix, Utils.stringToBase36(id));
				return f;
			});
		}
		return file;
	}

	public void requestCacheClean() {
		cacheCleanExecutor.execute(this::cleanCache);
	}

	public void setCacheSize(long maxSize) {
		long previousSize = maxCacheSize.getAndSet(maxSize);
		if (maxSize >= 0 && (previousSize < 0 || previousSize > maxSize)) requestCacheClean();
	}

	public void saveData() {
		if (dataFile == null) return;
		synchronized (this) {
			try (FileWriter fileWriter = new FileWriter(dataFile)) {
				JsonWriter writer = gson.newJsonWriter(fileWriter);
				writer.beginObject();

				for (Map.Entry<String, CacheFile> e : cacheMap.entrySet()) {
					CacheFile f = e.getValue();
					if (f.getLocks().tryRead()) {
						try {
							if (f.exists() && f.isFile()) {
								writer.name(e.getKey());
								gson.toJson(f.getData(), dataType, writer);
							}
						} finally {
							f.getLocks().readRelease();
						}
					}

				}
				writer.endObject();
			} catch (Exception e) {
				System.err.println("Could not save cache data");
				e.printStackTrace();
			}
		}
	}


	private boolean isPrefixValid(String prefix) {
		boolean valid = false;
		for (String p : prefixes) {
			if (prefix.equals(p)) {
				valid = true;
				break;
			}
		}
		return valid;
	}

	private void cleanCache() {
		long maxSize = maxCacheSize.get();
		if (maxSize < 0) return;
		final long[] size = {0};

		// Lock all valid files from cache and add them to a new map
		Map<String, CacheFile> copyMap = new LinkedHashMap<>();
		synchronized (this) {
			for (Map.Entry<String, CacheFile> e : cacheMap.entrySet()) {
				CacheFile f = e.getValue();
				boolean valid = false;
				if (f.getLocks().tryRead()) {
					try {
						if (f.exists() && f.isFile()) {
							if (f.getLocks().tryUpgrade()) {
								try {
									copyMap.put(e.getKey(), e.getValue());
									valid = true;
								} finally {
									if (!valid) f.getLocks().downgrade();
								}
							}
							else size[0] += f.length();
						}
					} finally {
						if (!valid) f.getLocks().readRelease();
					}
				}
			}

			// Sort by lastAccessTime descending and free space as needed
			copyMap.entrySet()
					.stream()
					.sorted((e1, e2) -> e2.getValue().getLastAccessTime().compareTo(e1.getValue().getLastAccessTime()))
					.forEachOrdered(e -> {
						CacheFile f = e.getValue();
						try {
							long length = f.length();
							if ((size[0] += length) > maxSize) {
								Files.delete(f.toPath());
								size[0] -= length;
							}
						} catch (IOException | SecurityException ex) {
							System.err.println("Could not access file " + f.getPath());
							ex.printStackTrace();
						} finally {
							f.getLocks().writeRelease();
						}
					});
		}
	}

}
