package com.nexus.casty.player;

public class PlayerStatus {
	private CastyPlayer.PlayerState state;
	private Float percent;
	private Integer volume;
	private Long songLength;

	PlayerStatus setState(CastyPlayer.PlayerState state) {
		this.state = state;
		return this;
	}

	PlayerStatus setPercent(Float percent) {
		this.percent = percent;
		return this;
	}

	PlayerStatus setVolume(Integer volume) {
		this.volume = volume;
		return this;
	}

	PlayerStatus setSongLength(Long songLength) {
		this.songLength = songLength;
		return this;
	}
}
