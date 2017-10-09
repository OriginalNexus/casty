package com.nexus.casty.player;

import com.google.gson.annotations.SerializedName;

class Status {
	CastyPlayer.PlayerState state;
	PlaylistStatus playlist;
	@SerializedName("song")
	long songCount;
	Float percent;
	int volume;
}
