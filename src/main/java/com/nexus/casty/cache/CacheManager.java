package com.nexus.casty.cache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nexus.casty.Utils;
import com.nexus.casty.song.SongData;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class CacheManager {

	private final Map<String, CacheEntry> cacheMap = new HashMap<>();
	private final File cacheDir;
	private final File dataFile;
	private final Gson gson;

	public CacheManager(File cacheDir, File dataFile) throws IOException {
		this.dataFile = dataFile;
		this.cacheDir = cacheDir;
		this.gson = new GsonBuilder().setPrettyPrinting().create();

		// Create cache directory
		if (!cacheDir.exists() && !cacheDir.mkdir()) {
			throw new IOException("Could not create cache directory");
		}

		// Load data cache
		if (dataFile != null && dataFile.exists() && dataFile.isFile()) {
			try (FileReader fileReader = new FileReader(this.dataFile)) {
				for (CacheEntry entry : gson.fromJson(fileReader, CacheEntry[].class)) {
					File f = getCacheFilePath(entry.id);
					if (f.exists()) {
						cacheMap.putIfAbsent(entry.id, entry);
					}
				}
			} catch (Exception e) {
				System.err.println("Could not load cache data");
				e.printStackTrace();
			}
		}
		saveData();

		// Remove other files from cache dir
		File[] files = cacheDir.listFiles();
		if (files != null) {
			for (File f : files) {
				if (!f.isFile())
					continue;

				String id;
				try {
					id = Utils.base36ToString(f.getName());
				} catch (NumberFormatException e) {
					continue;
				}

				if (!cacheMap.containsKey(id) && !f.delete())
					System.err.println("Could not delete cache file " + f.getPath());
			}
		}
	}

	public synchronized SongData getSongData(String id) {
		CacheEntry entry = cacheMap.get(id);
		return entry != null ? entry.data : null;
	}

	public File getCacheFilePath(String id) {
		return new File(cacheDir, Utils.stringToBase36(id));
	}

	public synchronized void computeSongDataIfAbsent(String id, SongData data) {
		if (data != null) {
			CacheEntry e = new CacheEntry(id);
			e.data = data;

			cacheMap.putIfAbsent(id, e);
			saveData();
		}
	}

	public synchronized void removeSongData(String id) {
		cacheMap.remove(id);
		saveData();
	}

	private synchronized void saveData() {
		try (FileWriter fileWriter = new FileWriter(dataFile)) {
			gson.toJson(cacheMap.values().toArray(), CacheEntry[].class, fileWriter);
		} catch (Exception e) {
			System.err.println("Could not save cache data");
			e.printStackTrace();
		}
	}

	public synchronized List<SongData> getSongDataArray() {
		return cacheMap.values().stream()
			.map(cacheEntry -> new SongData(cacheEntry.data))
			.collect(Collectors.toList());
	}

}
