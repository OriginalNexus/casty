package com.nexus.casty.player;

import java.util.ArrayList;
import java.util.LinkedList;

public class Playlist {

	private final LinkedList<PlaylistItem> playlist = new LinkedList<>();
	private final PlaylistStatus status = new PlaylistStatus();
	private int nextIndex = -1;

	public synchronized boolean addItem(PlaylistItem item) {
		if (item == null) return false;
		playlist.add(item);
		if (status.index < 0 && (nextIndex < 0 || nextIndex >= playlist.size())) nextIndex = playlist.size() - 1;
		status.version++;
		return true;
	}

	synchronized PlaylistItem next() {
		status.index = getNextIndex();
		nextIndex = -1;
		if (status.index < 0) return null;
		return playlist.get(status.index);
	}

	synchronized PlaylistItem previous() {
		int index = getPreviousIndex();
		if (index < 0) return null;
		status.index = index;
		nextIndex = -1;
		return playlist.get(status.index);
	}

	public synchronized boolean removeItem(int index) {
		if (index < 0 || index >= playlist.size()) return false;
		playlist.remove(index);
		if (index == status.index) {
			nextIndex = status.index;
			status.index = -1;
		}
		else if (index < status.index){
			status.index--;
		}
		status.version++;
		return true;
	}

	public synchronized PlaylistItem[] toArray() {
		return playlist.toArray(new PlaylistItem[0]);
	}

	public synchronized void setRepeat(boolean repeat) {
		status.repeat = repeat;
	}

	PlaylistStatus getStatus() {
		return status;
	}

	synchronized PlaylistItem setIndex(int index) {
		if (index < 0 || index >= playlist.size()) return null;
		status.index = index;
		nextIndex = -1;
		return playlist.get(index);
	}

	synchronized void load(ArrayList<PlaylistItem> array) {
		playlist.clear();
		playlist.addAll(array);
		status.index = -1;
		nextIndex = playlist.isEmpty() ? -1 : 0;
		status.version++;
	}

	synchronized void reset() {
		playlist.clear();
		status.index = -1;
		nextIndex = -1;
		status.repeat = false;
		status.version = 0;
	}

	private synchronized int getNextIndex() {
		if (playlist.size() == 0) return -1;
		if (nextIndex >= 0) {
			if (nextIndex >= playlist.size() && status.repeat) return 0;
			else if (nextIndex >= playlist.size()) return -1;
			else return nextIndex;
		}
		return status.index >= 0 ? (status.index + 1 >= playlist.size() ? (status.repeat ? 0 : -1) : (status.index + 1)) : -1;
	}

	private int getPreviousIndex() {
		if (playlist.size() == 0) return -1;
		if (status.index > 0 && status.index < playlist.size()) return status.index - 1;
		if (status.index == 0 && status.repeat) return playlist.size() - 1;
		if (status.index == 0) return -1;
		if (status.index < 0) {
			if (nextIndex >= playlist.size()) return playlist.size() - 1;
			if (nextIndex > 0) return nextIndex - 1;
			if (nextIndex == 0 && status.repeat) return playlist.size() - 1;
		}
		return -1;
	}

}
