package com.nexus.casty.player;

import com.nexus.casty.status.StatusListenersCollection;

import java.util.LinkedList;
import java.util.List;

public class Playlist {

	private final LinkedList<PlaylistItem> playlist = new LinkedList<>();
	private int nextIndex = -1;
	private int index = -1;
	private boolean repeat = false;

	StatusListenersCollection<PlaylistStatus> playlistListeners = new StatusListenersCollection<>() {
		@Override
		protected PlaylistStatus getOnAddListenerStatus() {
			return new PlaylistStatus().setIndex(index).setRepeat(repeat).setItems(getItems());
		}
	};

	public synchronized void addItem(PlaylistItem item) {
		if (item == null) return;
		playlist.add(item);
		if (index < 0 && (nextIndex < 0 || nextIndex >= playlist.size())) nextIndex = playlist.size() - 1;

		playlistListeners.updateStatus(new PlaylistStatus().setItems(getItems()));
	}

	synchronized PlaylistItem next() {
		int index = getNextIndex();
		if (index < 0) return null;
		this.index = index;
		nextIndex = -1;
		playlistListeners.updateStatus(new PlaylistStatus().setIndex(index));
		return playlist.get(index);
	}

	synchronized PlaylistItem previous() {
		int index = getPreviousIndex();
		if (index < 0) return null;
		this.index = index;
		nextIndex = -1;
		playlistListeners.updateStatus(new PlaylistStatus().setIndex(index));
		return playlist.get(index);
	}

	public synchronized void removeItem(int index) {
		if (index < 0 || index >= playlist.size()) return;
		playlist.remove(index);
		if (index == this.index) {
			nextIndex = this.index;
			this.index = -1;
		}
		else if (index < this.index){
			this.index--;
		}
		playlistListeners.updateStatus(new PlaylistStatus().setIndex(this.index).setItems(getItems()));
	}

	private synchronized PlaylistItem[] getItems() {
		return playlist.toArray(new PlaylistItem[0]);
	}

	public synchronized void setRepeat(boolean repeat) {
		this.repeat = repeat;
		playlistListeners.updateStatus(new PlaylistStatus().setRepeat(repeat));
	}

	synchronized PlaylistItem setIndex(int index) {
		if (index < 0 || index >= playlist.size()) return null;
		this.index = index;
		nextIndex = -1;
		playlistListeners.updateStatus(new PlaylistStatus().setIndex(index));
		return playlist.get(index);
	}

	synchronized void load(List<PlaylistItem> array) {
		playlist.clear();
		playlist.addAll(array);
		this.index = -1;
		nextIndex = playlist.isEmpty() ? -1 : 0;
		playlistListeners.updateStatus(new PlaylistStatus().setIndex(index).setItems(getItems()));
	}

	synchronized void reset() {
		playlist.clear();
		index = -1;
		nextIndex = -1;
		repeat = false;
	}

	private synchronized int getNextIndex() {
		if (playlist.size() == 0) return -1;
		if (nextIndex >= 0) {
			if (nextIndex >= playlist.size() && repeat) return 0;
			else if (nextIndex >= playlist.size()) return -1;
			else return nextIndex;
		}
		return index >= 0 ? (index + 1 >= playlist.size() ? (repeat ? 0 : -1) : (index + 1)) : -1;
	}

	private int getPreviousIndex() {
		if (playlist.size() == 0) return -1;
		if (index > 0 && index < playlist.size()) return index - 1;
		if (index == 0 && repeat) return playlist.size() - 1;
		if (index == 0) return -1;
		if (index < 0) {
			if (nextIndex >= playlist.size()) return playlist.size() - 1;
			if (nextIndex > 0) return nextIndex - 1;
			if (nextIndex == 0 && repeat) return playlist.size() - 1;
		}
		return -1;
	}
}
