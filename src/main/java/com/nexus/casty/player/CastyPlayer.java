package com.nexus.casty.player;

import com.nexus.casty.Utils;
import com.nexus.casty.server.CastyServer;

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

	private static final int DEFAULT_VOLUME = 80;

	private static final CastyPlayer ourInstance = new CastyPlayer();
	private final CacheManager cache = new CacheManager();

	private final MediaPlayer player;
	private final Playlist playlist = new Playlist();
	private Song currentSong;

	private CastyPlayer() {
		// Create and setup audio player
		AudioMediaPlayerComponent playerComponent = new AudioMediaPlayerComponent();
		player = playerComponent.getMediaPlayer();
		player.setVolume(DEFAULT_VOLUME);
		player.addMediaPlayerEventListener(new MediaPlayerEventAdapter() {
			@Override
			public void finished(MediaPlayer mediaPlayer) {
				new Timer().schedule(new TimerTask() {
					@Override
					public void run() {
						next();
					}
				}, 500);
			}

			@Override
			public void mediaChanged(MediaPlayer mediaPlayer, libvlc_media_t media, String mrl) {
				CastyServer.getInstance().updateStatus(getSongInfo());
				CastyServer.getInstance().updateStatus(new PlayerStatus().setPercent(-1f));
			}

			@Override
			public void playing(MediaPlayer mediaPlayer) {
				CastyServer.getInstance().updateStatus(new PlayerStatus().setState(PlayerState.PLAYING).setPercent(player.getPosition()));
			}

			@Override
			public void paused(MediaPlayer mediaPlayer) {
				CastyServer.getInstance().updateStatus(new PlayerStatus().setState(PlayerState.PAUSED));
			}

			@Override
			public void stopped(MediaPlayer mediaPlayer) {
				CastyServer.getInstance().updateStatus(new PlayerStatus().setState(PlayerState.STOPPED).setPercent(-1f));
			}

			@Override
			public void lengthChanged(MediaPlayer mediaPlayer, long newLength) {
				CastyServer.getInstance().updateStatus(new PlayerStatus().setSongLength(newLength).setPercent(player.getPosition()));
			}
		});

		CastyServer.getInstance().registerStatusSupplier(this::getStatus);
		CastyServer.getInstance().registerStatusSupplier(this::getSongInfo);
	}

	public static CastyPlayer getInstance() {
		return ourInstance;
	}


	private PlayerStatus getStatus() {
		PlayerStatus status = new PlayerStatus();
		if (player.isPlaying())
			status.setState(PlayerState.PLAYING);
		else if (player.getMediaState() == libvlc_state_t.libvlc_Paused)
			status.setState(PlayerState.PAUSED);
		else
			status.setState(PlayerState.STOPPED);

		status.setPercent(player.getPosition()).setVolume(player.getVolume()).setSongLength(player.getLength());

		return status;
	}

	private SongInfo getSongInfo() {
		if (currentSong != null)
			return new SongInfo(currentSong.data);
		return new SongInfo();
	}

	public void playUrl(String url) {
		Song song = getSongFromUrl(url);
		if (song == null || !playSong(song)) {
			System.err.println("Failed to play URL: " + url);
		}
	}

	private Song getSongFromUrl(String url) {
		YTUrl ytUrl = YTUrl.fromUrl(url);
		if (ytUrl == null)
			return null;

		YTExtractor ytExtractor = new YTExtractor(ytUrl);
		String id = ytExtractor.getYTUrl().getId();
		Song song = new Song();

		// Check cache first
		SongData songData = cache.getData(id);
		if (songData == null) {
			try {
				ArrayList<YTFormat> formats = ytExtractor.getFormats();
				List<String> audioItags = Arrays.asList("251", "171", "140", "250", "249", "22", "43", "36", "17");

				YTFormat ytFormat = formats.stream()
						.filter(f -> audioItags.contains(f.getItag()))
						.min(Comparator.comparingInt(o -> audioItags.indexOf(o.getItag())))
						.orElseThrow(() -> new Exception("No appropriate format found for url"));

				if (ytFormat.isEncrypted()) {
					YTPlayer ytPlayer = ytExtractor.getPlayer();
					ytPlayer.decryptFormat(ytFormat);
				}

				// Set song data
				songData = new SongData();
				songData.source = url;
				songData.sourceName = "YouTube";

				songData.title = ytExtractor.getTitle();

				songData.container = YTFormats.ItagsMap.get(ytFormat.getItag()).Container.toLowerCase();

				if (ytFormat.getItag().equals("140"))
					songData.specialDemux = true;

				songData.filename = ((!songData.title.isEmpty()) ? songData.title : id) + "." + songData.container;

				songData.thumbnail = ytExtractor.getThumbnailUrl();
				songData.thumbnailFull = ytExtractor.getFullThumbnailUrl();

				song.streamUrl = ytFormat.getURL();

			} catch (Exception e) {
				System.err.println("Failed to parse and download YouTube url");
				e.printStackTrace();
				return null;
			}
		} else {
			song.streamUrl = cache.getFile(id).getPath();
		}

		song.cacheId = id;
		song.data = songData;

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
			if (currentSong == null)
				return;

			try {
				url = new YTExtractor(new YTUrl(currentSong.cacheId, "https:")).getNextUrl();
			} catch (IOException e) {
				System.err.println("Could not extract next song");
				e.printStackTrace();
				return;
			}
			playlist.resetIndex();
		}
		new Thread(() -> playUrl(url)).start();
	}

	public synchronized void previous() {
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
		CastyServer.getInstance().updateStatus(new PlayerStatus().setPercent(percent));
	}

	public synchronized void setVolume(int volume) {
		player.setVolume(volume);
		CastyServer.getInstance().updateStatus(new PlayerStatus().setVolume(volume));
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
		File file = cache.getFile(id);

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
			cache.putData(id, data);
			CastyServer.getInstance().updateStatus(getSongInfo());

		}).start();
	}

	public synchronized void uncacheCurrentSong() {
		if (currentSong == null)
			return;

		cache.removeData(currentSong.cacheId);
		currentSong.data.download = "";
		CastyServer.getInstance().updateStatus(getSongInfo());
	}

	public void loadCachePlaylist() {
		List<SongData> array = cache.getDataArray();
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
