package com.nexus.casty.player;

public class PlaylistStatus {
	private Integer index;
	private Boolean repeat;
	private PlaylistItem[] items;

	public PlaylistStatus setIndex(Integer index) {
		this.index = index;
		return this;
	}

	public PlaylistStatus setRepeat(Boolean repeat) {
		this.repeat = repeat;
		return this;
	}

	public PlaylistStatus setItems(PlaylistItem[] items) {
		this.items = items;
		return this;
	}
}
