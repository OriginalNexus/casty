package com.nexus.casty.player;

import com.nexus.casty.Utils;
import com.nexus.casty.status.StatusListener;
import com.nexus.casty.status.StatusListenersCollection;

import com.google.gson.annotations.SerializedName;
import com.nexus.casty.cache.CacheManager;
import com.nexus.casty.song.*;
import com.nexus.ytd.*;
import uk.co.caprica.vlcj.binding.internal.libvlc_media_t;
import uk.co.caprica.vlcj.binding.internal.libvlc_state_t;
import uk.co.caprica.vlcj.component.AudioMediaPlayerComponent;
import uk.co.caprica.vlcj.player.MediaPlayer;
import uk.co.caprica.vlcj.player.MediaPlayerEventAdapter;

import java.io.*;
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

	private final MediaPlayer player;
	private final Playlist playlist;
	private Song currentSong;
	private int volume = DEFAULT_VOLUME;

	private StatusListenersCollection<PlayerStatus> playerListeners = new StatusListenersCollection<>() {
		@Override
		protected PlayerStatus getOnAddListenerStatus() {
			return getFullStatus();
		}
	};

	private StatusListenersCollection<SongInfo> songListeners = new StatusListenersCollection<>() {
		@Override
		protected SongInfo getOnAddListenerStatus() {
			return getSongInfo();
		}
	};


	private CastyPlayer() {
		try {
			File cacheDir = new File("cache/");
			File dataFile = new File(cacheDir, "cache-data.json");
			cache = new CacheManager(cacheDir, dataFile);
		} catch (IOException e) {
			throw new RuntimeException("Could not instantiate player cache", e);
		}

		playlist = new Playlist();

		// Create and setup audio player
		AudioMediaPlayerComponent playerComponent = new AudioMediaPlayerComponent();
		player = playerComponent.getMediaPlayer();
		player.setVolume(DEFAULT_VOLUME);
		player.addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
			@Override
			public void finished(MediaPlayer mediaPlayer) {
				next();
			}

			@Override
			public void mediaChanged(MediaPlayer mediaPlayer, libvlc_media_t media, String mrl) {
				songListeners.updateStatus(getSongInfo());
				playerListeners.updateStatus(new PlayerStatus().setPercent(-1f));
			}

			@Override
			public void playing(MediaPlayer mediaPlayer) {
				playerListeners.updateStatus(new PlayerStatus().setState(PlayerState.PLAYING).setPercent(player.getPosition()));
			}

			@Override
			public void paused(MediaPlayer mediaPlayer) {
				playerListeners.updateStatus(new PlayerStatus().setState(PlayerState.PAUSED));
			}

			@Override
			public void stopped(MediaPlayer mediaPlayer) {
				playerListeners.updateStatus(new PlayerStatus().setState(PlayerState.STOPPED).setPercent(-1f));
			}

			@Override
			public void lengthChanged(MediaPlayer mediaPlayer, long newLength) {
				playerListeners.updateStatus(new PlayerStatus().setSongLength(newLength).setPercent(player.getPosition()));
			}
		});
	}


	public static CastyPlayer getInstance() {
		return ourInstance;
	}

	private PlayerStatus getFullStatus() {
		PlayerStatus status = new PlayerStatus();
		if (player.isPlaying())
			status.setState(PlayerState.PLAYING);
		else if (player.getMediaState() == libvlc_state_t.libvlc_Paused)
			status.setState(PlayerState.PAUSED);
		else
			status.setState(PlayerState.STOPPED);

		status.setPercent(player.getPosition()).setVolume(volume).setSongLength(player.getLength());

		return status;
	}

	private SongInfo getSongInfo() {
		if (currentSong != null)
			return new SongInfo(currentSong.data);
		return new SongInfo();
	}


	public void addStatusListener(StatusListener listener) {
		playerListeners.addListener(listener);
		songListeners.addListener(listener);
		playlist.playlistListeners.addListener(listener);
	}

	public void removeStatusListener(StatusListener listener) {
		playerListeners.removeListener(listener);
		songListeners.removeListener(listener);
		playlist.playlistListeners.removeListener(listener);
	}


	public void playUrl(String url) {
		Song song = getSongFromUrl(url);
		if (song == null || !playSong(song)) {
			System.err.println("Failed to play URL: " + url);
		}
	}


	private Song getSongFromUrl(String url) {
		if (url == null)
			return null;

		return getYouTubeSong(url);
	}

	private Song getYouTubeSong(String url) {
		YTUrl ytUrl = YTUrl.fromUrl(url);
		if (ytUrl == null) return null;

		return getYouTubeSong(new YTExtractor(ytUrl));
	}

	private Song getYouTubeSong(YTExtractor ytExtractor) {
		String id = ytExtractor.getYTUrl().getId();
		String url = ytExtractor.getYTUrl().getUrl();
		Song song = new Song();

		// Check cache first
		SongData songData = cache.getSongData(YOUTUBE_PREFIX + id);
		if (songData == null) {
			try {
				ArrayList<YTFormat> formats = ytExtractor.getFormats();
				String[] audioItags = {"251", "171", "140", "250", "249", "22", "43", "36", "17"};
				YTFormat ytFormat = null;

				for (String audioItag : audioItags) {
					for (YTFormat format : formats) {
						if (format.getItag().equals(audioItag)) {
							ytFormat = format;
							break;
						}
					}
					if (ytFormat != null) break;
				}

				if (ytFormat == null) {
					System.err.println("No appropriate format found for url");
					return null;
				}

				if (ytFormat.isEncrypted()) {
					YTPlayer ytPlayer = ytExtractor.getPlayer();
					ytPlayer.decryptFormat(ytFormat);
				}


				// Set song data
				songData = new SongData();
				songData.source = url;
				songData.sourceName = "YouTube";
				songData.title = "Unknown title";
				try {
					songData.title = ytExtractor.getTitle();
				} catch (IOException e) {
					System.err.println("Could not extract title");
					e.printStackTrace();
				}

				songData.container = YTFormats.ItagsMap.get(ytFormat.getItag()).Container.toLowerCase();
				if (ytFormat.getItag().equals("140"))
					songData.specialDemux = true;
				songData.filename = ((songData.title != null) ? songData.title : YOUTUBE_PREFIX + id) + "." + songData.container;

				songData.thumbnail = ytExtractor.getThumbnailUrl();
				songData.thumbnailFull = ytExtractor.getFullThumbnailUrl();

				song.streamUrl = ytFormat.getURL();

			} catch (Exception e) {
				System.err.println("Failed to parse and download YouTube url");
				e.printStackTrace();
				return null;
			}
		} else {
			song.streamUrl = cache.getCacheFilePath(YOUTUBE_PREFIX + id).getPath();
		}

		song.cacheId = YOUTUBE_PREFIX + id;
		song.data = songData;
		song.ytExtractor = ytExtractor;

		return song;
	}

	private synchronized boolean playSong(Song song) {
		if (song.streamUrl != null) {
			boolean demux = (song.data != null) && song.data.specialDemux;

			System.out.println(song.streamUrl);
			currentSong = song;
			if (player.startMedia(song.streamUrl, demux ? "demux=avformat" : "", "http-reconnect"))
				return true;

			currentSong = null;
		}
		return false;
	}

	public synchronized void playPause() {
		if (player.getMediaState() == libvlc_state_t.libvlc_Stopped || player.getMediaState() == libvlc_state_t.libvlc_Paused)
			player.play();
		else
			player.pause();
	}

	public synchronized void next() {
		String url;

		// Check playlist first
		PlaylistItem item = playlist.next();
		if (item != null) {
			url = item.url;
		}
		else {
			if (currentSong == null || currentSong.ytExtractor == null) return;
			try {
				YTExtractor nextYTExtractor = currentSong.ytExtractor.getNextVideo();
				new Thread(() -> {
					Song song = getYouTubeSong(nextYTExtractor);
					if (song == null) return;
					playSong(song);
				}).start();
				return;
			} catch (IOException e) {
				System.err.println("Could extract next url for current song");
				e.printStackTrace();
				return;
			}
		}

		new Thread(() -> playUrl(url)).start();
	}

	public synchronized void previous() {
		if (player == null) return;
		if (player.getLength() * player.getPosition() > 5000) {
			setPosition(0);
			return;
		}
		PlaylistItem item = playlist.previous();
		if (item != null) {
			new Thread(() -> playUrl(item.url)).start();
		}
		else {
			setPosition(0);
		}
	}

	public synchronized void reset() {
		player.stop();
		player.setVolume(DEFAULT_VOLUME);
		currentSong = null;
		playlist.reset();
	}


	public synchronized void setPosition(float percent) {
		player.setPosition(percent);
		playerListeners.updateStatus(new PlayerStatus().setPercent(percent));
	}

	public synchronized void setVolume(int volume) {
		player.setVolume(volume);
		this.volume = volume;
		playerListeners.updateStatus(new PlayerStatus().setVolume(volume));
	}

	public CacheManager getCache() {
		return cache;
	}

	public Playlist getPlaylist() {
		return playlist;
	}

	public void playlistPlayItem(int index) {
		PlaylistItem item = playlist.setIndex(index);
		if (item == null)
			return;
		new Thread(() -> playUrl(item.url)).start();
	}


	public synchronized void cacheCurrentSong() {
		if (currentSong == null)
			return;

		String streamUrl = currentSong.streamUrl;
		SongData data = currentSong.data;
		String id = currentSong.cacheId;
		File file = cache.getCacheFilePath(id);

		new Thread(() -> {
			if (!streamUrl.equals(file.getPath())) {
				try (FileOutputStream fs = new FileOutputStream(file)){
					Utils.downloadToStream(streamUrl, fs);
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
			}

			data.download = "download/" + id;
			cache.computeSongDataIfAbsent(id, data);
			songListeners.updateStatus(getSongInfo());

		}).start();
	}

	public synchronized void uncacheCurrentSong() {
		if (currentSong == null)
			return;

		cache.removeSongData(currentSong.cacheId);
		currentSong.data.download = "";
	}

	public void loadCachePlaylist() {
		List<SongData> array = cache.getSongDataArray();
		List<PlaylistItem> list;
		list = array.stream()
				.map(songData -> {
					PlaylistItem item = new PlaylistItem();
					item.url = songData.source;
					item.title = songData.title;
					return item;
				})
				.sorted(Comparator.comparing(item -> item.title))
				.collect(Collectors.toList());

		playlist.load(list);
	}

}
