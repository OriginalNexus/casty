package com.nexus.casty.player;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.nexus.casty.Utils;

import com.google.gson.annotations.SerializedName;
import com.nexus.casty.cache.CacheFile;
import com.nexus.casty.cache.CacheManager;
import com.nexus.casty.song.*;
import com.originalnexus.ytd.*;
import org.jetbrains.annotations.NotNull;
import uk.co.caprica.vlcj.binding.LibVlcFactory;
import uk.co.caprica.vlcj.binding.internal.libvlc_state_t;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;
import uk.co.caprica.vlcj.player.MediaPlayerFactory;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;


public class CastyPlayer {

	enum PlayerState {
		@SerializedName("0")
		STOPPED,
		@SerializedName("1")
		PLAYING,
		@SerializedName("2")
		PAUSED
	}

	private static final String YOUTUBE_PREFIX = "yt";
	private static final int DEFAULT_VOLUME = 80;

	private static final CastyPlayer ourInstance = new CastyPlayer();

	private final CacheManager cache;

	private final MediaPlayerFactory playerFactory;
	private MediaPlayer player = null;

	private Song currentSong;
	private long songCount = 0;
	private int volume = DEFAULT_VOLUME;

	private final Playlist playlist = new Playlist();

	private CastyPlayer() {
		try {
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			File cacheDir = new File("cache/");
			File dataFile = new File(cacheDir, "cache-data.json");
			cache = new CacheManager(cacheDir, new String[]{ YOUTUBE_PREFIX }, dataFile, gson, SongData.class);
		} catch (IOException e) {
			throw new RuntimeException("Could not instantiate player cache", e);
		}

		// Create a player factory from a synchronized LibVlc instance
		playerFactory = new MediaPlayerFactory(LibVlcFactory.factory().synchronise().create());
	}

	public static CastyPlayer getInstance() {
		return ourInstance;
	}

	public boolean playUrl(String url) {
		Song song = getSongFromUrl(url);
		if (song == null || !playSong(song)) {
			System.err.println("Failed to play URL: " + url);
			return false;
		}
		return true;
	}

	private Song getSongFromUrl(String url) {
		if (url == null) return null;
		Song song = getYouTubeSong(url);
		if (song == null) song = getDirectSong(url);
		return song;
	}

	private Song getYouTubeSong(String url) {
		YTUrl ytUrl = YTUrl.fromUrl(url);
		if (ytUrl == null) return null;

		YTExtractor ytExtractor = new YTExtractor(ytUrl);
		YTFormat ytFormat = null;

		String id = ytUrl.getId();
		Song song;
		boolean isNew = false;

		// Check cache first
		CacheFile f = cache.getCacheFile(YOUTUBE_PREFIX, id);
		if (f == null) return null;
		f.getLocks().read();
		try {
			if (!f.exists() || f.getData() == null) {
				f.getLocks().upgrade();
				try {
					if (f.exists()) Files.delete(f.toPath());

					ArrayList<YTFormat> formats = ytExtractor.getFormats();
					String[] audioItags = {"251", "171", "140", "250", "249", "22", "43", "36", "17"};

					for (String audioItag : audioItags) {
						for (YTFormat format : formats) {
							if (format.getItag().equals(audioItag)) {
								ytFormat = format;
								break;
							}
						}
						if (ytFormat != null) break;
					}

					if (ytFormat != null) {
						if (ytFormat.isEncrypted()) {
							YTPlayer ytPlayer = ytExtractor.getPlayer();
							ytPlayer.decryptFormat(ytFormat);
						}

						// Download file
						Utils.urlToFile(ytFormat.getURL(), f);
						isNew = true;

						// Set song data
						SongData data = new SongData();
						data.source = url;
						data.sourceName = "YouTube";
						try {
							data.title = ytExtractor.getTitle();
						} catch (IOException e) {
							System.err.println("Could not extract title");
							e.printStackTrace();
						}

						data.container = YTFormats.ItagsMap.get(ytFormat.getItag()).Container.toLowerCase();
						if (ytFormat.getItag().equals("140")) data.specialDemux = true;
						data.filename = ((data.title != null) ? data.title : YOUTUBE_PREFIX + id) + "." + data.container;

						data.thumbnail = ytExtractor.getThumbnailUrl();
						data.thumbnailFull = ytExtractor.getFullThumbnailUrl();
						data.download = "download/" + YOUTUBE_PREFIX + "/" + id;
						f.setData(data);
					}
					else {
						System.err.println("No appropriate format found for url");
					}
				} catch (Exception e) {
					// Try to delete the file since it could be corrupt
					try {
						if (f.exists()) Files.delete(f.toPath());
					} catch (Exception ignored) {}
					throw e;
				} finally {
					f.getLocks().downgrade();
				}
			}

			if (f.exists() && f.isFile()) {
				f.updateAccessTime();
				if (isNew) {
					cache.saveData();
					cache.requestCacheClean();
				}

				song = new Song();
				song.file = f;
				song.data = f.getData();
				song.streamUrl = song.file.getPath();
				song.ytExtractor = ytExtractor;
				return song;
			}

		} catch (Exception e) {
			System.err.println("Failed to parse and download YouTube url");
			e.printStackTrace();
			f.getLocks().readRelease();
		}

		return null;
	}

	private Song getDirectSong(String url) {
		Song song = new Song();
		song.streamUrl = url;
		song.file = null;
		song.ytExtractor = null;
		song.data = new SongData();
		song.data.title = url;
		song.data.source = url;
		song.data.sourceName = "Direct URL";
		song.data.download = url;
		return song;
	}

	private synchronized boolean playSong(@NotNull Song song) {
		boolean success = false;
		try {
			if (player != null) {
				player.stop();
				player.setVolume(DEFAULT_VOLUME);
				player.release();
			}
			player = null;
			if (currentSong != null && currentSong.file != null) currentSong.file.getLocks().readRelease();
			String lastStreamUrl = (currentSong != null) ? currentSong.streamUrl : null;
			currentSong = null;

			if (song.streamUrl != null) {
				player = playerFactory.newHeadlessMediaPlayer();
				player.setVolume(volume);
				player.addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
					@Override
					public void finished(MediaPlayer mediaPlayer) {
						next();
					}
				});
				boolean demux = (song.data != null) && song.data.specialDemux;
				success = player.startMedia(song.streamUrl, demux ? "demux=avformat" : "", "http-reconnect");

				if (success) {
					if (lastStreamUrl == null || !lastStreamUrl.equals(song.streamUrl)) songCount++;
					currentSong = song;
				}
			}
		} catch (Exception e) {
			System.err.println("Player internal error");
			e.printStackTrace();
		} finally {
			if (!success && song.file != null) song.file.getLocks().readRelease();
		}

		return success;
	}

	public synchronized boolean playPause() {
		if (player == null) return false;
		if (player.getMediaState() == libvlc_state_t.libvlc_Stopped || player.getMediaState() == libvlc_state_t.libvlc_Paused) {
			player.play();
		}
		else {
			player.pause();
		}
		return true;
	}

	public synchronized boolean next() {
		String url;

		// Check playlist first
		PlaylistItem item = playlist.next();
		if (item != null) {
			url = item.url;
		}
		else {
			if (currentSong == null || currentSong.ytExtractor == null) return false;
			try {
				url = currentSong.ytExtractor.getNextVideoUrl();
			} catch (IOException e) {
				System.err.println("Could extract next url for current song");
				e.printStackTrace();
				return false;
			}
		}
		new Thread(() -> playUrl(url)).start();
		return true;
	}

	public synchronized boolean previous() {
		if (player == null) return false;
		if (getCurrentSongLength() * player.getPosition() > 5000) return setPosition(0);
		PlaylistItem item = playlist.previous();
		if (item != null) {
			new Thread(() -> playUrl(item.url)).start();
			return true;
		}
		else return setPosition(0);
	}

	public synchronized void reset() {
		if (player != null) {
			player.stop();
			player.setVolume(DEFAULT_VOLUME);
			volume = DEFAULT_VOLUME;
			player.release();
		}
		player = null;
		if (currentSong != null && currentSong.file != null) currentSong.file.getLocks().readRelease();
		currentSong = null;
		songCount = 0;
		playlist.reset();
		cache.requestCacheClean();
	}

	public synchronized Status getStatus() {
		Status status = new Status();
		if (player != null) {
			if (player.isPlaying()) {
				status.state = PlayerState.PLAYING;
				status.percent = player.getPosition();
			}
			else if (player.getMediaState() == libvlc_state_t.libvlc_Paused) {
				status.state = PlayerState.PAUSED;
				status.percent = player.getPosition();
			}
			else {
				status.state = PlayerState.STOPPED;
			}
			status.songCount = songCount;
			status.volume = player.getVolume();
		}
		else {
			status.state = PlayerState.STOPPED;
			status.songCount = 0;
			status.volume = -1;
		}
		status.playlist = playlist.getStatus();
		return status;
	}

	public synchronized SongInfo getCurrentSongInfo() {
		if (currentSong == null) return null;
		return currentSong.data;
	}

	public synchronized long getCurrentSongLength() {
		return (player == null) ? -1 : player.getLength();
	}

	public synchronized boolean setPosition(float percent) {
		if (player == null) return false;
		player.setPosition(percent);
		return true;
	}

	public synchronized boolean setVolume(int volume) {
		if (player == null) return false;
		player.setVolume(volume);
		this.volume = volume;
		return true;
	}

	public CacheManager getCache() {
		return cache;
	}

	public Playlist getPlaylist() {
		return playlist;
	}

	public boolean playlistPlayItem(int index) {
		PlaylistItem item = playlist.setIndex(index);
		if (item == null) return false;
		new Thread(() -> playUrl(item.url)).start();
		return true;
	}

	public boolean loadCachePlaylist() {
		ArrayList<SongData> array = cache.getDataArray(SongData::new);
		ArrayList<PlaylistItem> list;
		list = array.stream()
				.map(songData -> {
					PlaylistItem item = new PlaylistItem();
					item.url = songData.source;
					item.title = songData.title;
					return item;
				})
				.sorted(Comparator.comparing(item -> item.title))
				.collect(Collectors.toCollection(ArrayList::new));

		playlist.load(list);
		return true;
	}

}
