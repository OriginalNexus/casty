package com.nexus.casty.cache;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.nexus.casty.Utils;
import com.nexus.casty.song.SongData;

import java.io.*;
import java.util.*;

public class CacheManager {

	private final Map<String, SongData> cacheMap = new HashMap<>();
	private final File cacheDir = new File("cache/");
	private final File cacheFile = new File(cacheDir, "cache-data.json");;
	private final Gson gson;

	public CacheManager() {
		this.gson = new GsonBuilder().setPrettyPrinting().create();

		// Create cache directory
		if (!cacheDir.exists() && !cacheDir.mkdir()) {
			throw new RuntimeException("Could not create cache directory");
		}

		// Load data cache
		if (cacheFile.exists() && cacheFile.isFile()) {
			try (FileReader fileReader = new FileReader(this.cacheFile)) {
				Map<String, SongData> map = gson.fromJson(fileReader, new TypeToken<Map<String, SongData>>(){}.getType());
				for (Map.Entry<String, SongData> entry : map.entrySet()) {
					File f = getFile(entry.getKey());
					if (f.exists()) {
						cacheMap.putIfAbsent(entry.getKey(), entry.getValue());
					}
				}
			} catch (Exception e) {
				System.err.println("Could not load cache data");
				e.printStackTrace();
			}
		}
		save();

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

	public synchronized SongData getData(String id) {
		return cacheMap.get(id);
	}

	public File getFile(String id) {
		return new File(cacheDir, Utils.stringToBase36(id));
	}

	public synchronized void putData(String id, SongData data) {
		if (data != null) {
			cacheMap.putIfAbsent(id, data);
			save();
		}
	}

	public synchronized void removeData(String id) {
		cacheMap.remove(id);
		save();
	}

	private synchronized void save() {
		try (FileWriter fileWriter = new FileWriter(cacheFile)) {
			gson.toJson(cacheMap, fileWriter);
		} catch (Exception e) {
			System.err.println("Could not save cache data");
			e.printStackTrace();
		}
	}

	public synchronized List<SongData> getDataArray() {
		return new ArrayList<>(cacheMap.values());
	}

}
