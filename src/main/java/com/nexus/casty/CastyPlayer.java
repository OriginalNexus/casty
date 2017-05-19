package com.nexus.casty;

import com.originalnexus.ytd.YTExtractor;
import com.originalnexus.ytd.YTFormat;
import com.originalnexus.ytd.YTPlayer;
import uk.co.caprica.vlcj.binding.internal.libvlc_state_t;
import uk.co.caprica.vlcj.component.AudioMediaPlayerComponent;
import uk.co.caprica.vlcj.player.MediaPlayer;

import javax.script.ScriptException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class CastyPlayer {
	private static final CastyPlayer ourInstance = new CastyPlayer();
	private final Map<String, Lock> lockMap = new LinkedHashMap<>();

	private Song currentSong;

	static CastyPlayer getInstance() {
		return ourInstance;
	}

	private final MediaPlayer player = new AudioMediaPlayerComponent().getMediaPlayer();

	private CastyPlayer() {
		player.setRepeat(true);
	}

	boolean playUrl(String url) {
		if (url == null) return false;

		boolean specialDemux = false;
		Song song = new Song();
		song.info = new SongInfo();

		// Decode YouTube urls
		Matcher m;
		m = Pattern.compile("(?<protocol>http[s]?:)//(?:www\\.)?youtube\\.com/watch\\?\\S*?&?v=(?<id>[0-9A-Za-z_-]+)").matcher(url);
		if (m.find()) {
			String id = m.group("id");
			String protocol = m.group("protocol");
			YTExtractor ytExtractor = new YTExtractor(id, protocol);

			// Check cache first
			//noinspection ResultOfMethodCallIgnored
			new File("cache").mkdir();
			File streamCacheFile = new File("cache/" + id);

			Lock fileLock = lockMap.computeIfAbsent(streamCacheFile.getPath(), k -> new ReentrantLock());

			fileLock.lock();
			try {
				if (streamCacheFile.exists()) {
					song.streamUrl = streamCacheFile.getPath();
				}
				else {
					try {
						ArrayList<YTFormat> ytFormats = ytExtractor.getFormats();
						String[] audioItags = {"251", "171", "140", "250", "249", "22", "43", "36", "17"};
						String specialDemuxItag = "140";
						YTFormat bestFormat = null;

						for (String audioItag : audioItags) {
							for (YTFormat ytFormat : ytFormats) {
								if (ytFormat.getItag().equals(audioItag)) {
									bestFormat = ytFormat;
									break;
								}
							}
							if (bestFormat != null) break;
						}

						if (bestFormat != null) {
							if (bestFormat.getItag().equals(specialDemuxItag)) {
								specialDemux = true;
							}

							if (bestFormat.isEncrypted()) {
								YTPlayer ytPlayer = ytExtractor.getPlayer();
								ytPlayer.decryptFormat(bestFormat);
							}

							// Download and cache file
							if (!streamCacheFile.createNewFile()) throw new IOException("Failed to create stream cache file");
							Utils.downloadURLToFile(bestFormat.getURL(), streamCacheFile);

							// All good, let's set info
							song.streamUrl = streamCacheFile.getPath();

						} else {
							System.err.println("No appropriate format found for url");
						}

					} catch (IOException | ScriptException e) {
						System.err.println("Failed to extract and download YouTube url");
						e.printStackTrace();
					}
				}
			} finally {
				fileLock.unlock();
			}
			if (song.streamUrl != null) {
				// Set info
				song.info.source = url;
				song.info.sourceName = "YouTube";
				try { song.info.title = ytExtractor.getTitle();
				} catch (IOException ignored) {}
				song.info.thumbnail = "http://img.youtube.com/vi/" + id + "/mqdefault.jpg";
				song.info.thumbnailFull = "http://img.youtube.com/vi/" + id + "/maxresdefault.jpg";
			}

		}
		else {
			// Try to play url directly
			song.streamUrl = url;
			song.info.title = url;
			song.info.source = url;
			song.info.sourceName = "Direct URL";
		}

		boolean success = false;
		if (song.streamUrl != null) {
			synchronized (player) {
				success = player.prepareMedia(song.streamUrl, specialDemux ? "demux=avformat" : "", "http-reconnect");
				if (success) {
					player.stop();
					currentSong = song;
					playPause();
				}
			}
		}
		if (!success) {
			System.err.println("Failed to play URL: " + url);
			return false;
		}

		return true;
	}

	boolean playPause() {
		if (currentSong == null) return false;
		synchronized (player) {
			if (player.getMediaState() == libvlc_state_t.libvlc_Stopped || player.getMediaState() == libvlc_state_t.libvlc_Paused) {
				player.play();
			}
			else {
				player.pause();
			}
		}
		return true;
	}

	PlayerStatus getStatus() {
		PlayerStatus status = new PlayerStatus();
		synchronized (player) {
			if (player.isPlaying()) {
				status.state = "PLAYING";
				status.percent = player.getPosition();
			}
			else if (player.getMediaState() == libvlc_state_t.libvlc_Paused) {
				status.state = "PAUSED";
				status.percent = player.getPosition();
			}
			else {
				status.state = "STOPPED";
			}
		}
		return status;
	}

	SongInfo getCurrentSongInfo() {
		if (currentSong == null) return null;
		synchronized (player) {
			currentSong.info.length = player.getLength();
			return currentSong.info;
		}
	}

	boolean setPosition(float percent) {
		if (currentSong == null) return false;
		synchronized (player) {
			player.setPosition(percent);
		}
		return true;
	}

	private class PlayerStatus {
		String state;
		Float percent;
		PlayerStatus() {}
	}

	private class SongInfo {
		String title;
		String source;
		String sourceName;
		long length;
		String thumbnail;
		String thumbnailFull;
	}

	private class Song {
		SongInfo info;
		String streamUrl;
	}
}
