// Global variables
var windowFocused = true;
var songCount = 0;
var songLength = -1;
var songPercent = -1;
var lasSongPercent = -1;
var progressBarValue = 0;
var State = {
	"STOPPED": 0,
	"PLAYING": 1,
	"PAUSED": 2
}
var playerState = State.STOPPED;
var playlistVersion = 0;
var playlistIndex = -1;
var playlistRepeat = false;


function searchResultClick(target, url) {
	if ($("#results .mdl-spinner.is-active").length > 0) return;
	var spinner = $(target).find(".mdl-spinner");
	spinner.addClass("is-active");

	$.post("/player/control", { action: "playurl", url: url})
	.always(function(data) {
		spinner.removeClass("is-active");
	});
}

function searchBtnClick() {
	var query = $("#search-textfield").val();
	if (query === "") return;

	if (query.startsWith("http://") || query.startsWith("https://")) {
		$.post("/player/control", { action: "playurl", url: query});
		return;
	}

	var overlay = $("#content-overlay")
	var spinner = overlay.find(".mdl-spinner");
	var resultsWrapper = $("#results-wrapper");
	var results = $("#results");

	if (spinner.hasClass("is-active")) return;
	spinner.addClass("is-active");

	overlay.fadeIn();

	$.getJSON("/results?q=" + encodeURI(query), null, function(data) {
		resultsWrapper.hide();
		if (results) {
			results.empty();
			$.each(data, function(i, result) {
				var resultDiv =
				$("<li>", {
					"class": "mdl-list__item  mdl-list__item--two-line",
					click: function() {
						searchResultClick(this, "https://www.youtube.com/watch?v=" + result["id"]);
					}
				}).append(
					$("<span>", {
						"class": "thumbnail",
						css: {
							"background-image": "url(\"" + result["thumbnail"] + "\")"
						}
					}),
					$("<span>", {
						"class": "mdl-list__item-primary-content"
					}).append(
						$("<span>", {
							"class": "title ellipsis",
							text: result["title"]
						}),
						$("<span>", {
							"class": "spinner"
						}).append(
							$("<div>", {
								"class": "mdl-spinner mdl-js-spinner mdl-spinner--single-color"
							})
						),
						$("<span>", {
							text: result["author"] ? result["author"] : "Unknown artist",
							"class" : "mdl-list__item-sub-title ellipsis"
						})
					),
					$("<span>", {
						"class": "mdl-list__item-secondary-content"
					}).append(
						$("<button>", {
							"class": "mdl-list__item-secondary-action mdl-button mdl-js-button mdl-button--icon mdl-js-ripple-effect",
							click: function(event) {
								event.stopPropagation();
								playlistAdd(result["title"], "https://www.youtube.com/watch?v=" + result["id"]);
							}
						}).append(
							$("<i>", {
								text: "playlist_add",
								"class": "material-icons"
							})
						)
					)
				);
				componentHandler.upgradeElement(resultDiv.find(".mdl-spinner")[0]);
				results.append(resultDiv);
			});
			resultsWrapper.fadeIn();
		}
	})
	.always(function() {
		overlay.hide();
		spinner.removeClass("is-active");
	});
}


function playlistAdd(title, url) {
	$.post("/playlist/add", { title: title, url: url });

}

function playlistRemove(index) {
	$.post("/playlist/remove", { index: index }, function() {
		var playlist = $("#playlist");
		playlist.children().eq(index).remove();
		if (playlist.children().length === 0) playlistEmpty();
	});
}

function playlistPlay(index) {
	$.post("/player/control", { action: "playlist", index: index });

}

function playlistEmpty() {
	// Note that this function only displays the "Empty" text and does not remove songs from server
	$("#playlist").empty().append($("<span>", { "class": "empty" }));
}

function playlistSelectCurrent() {
	var playlist = $("#playlist");
	playlist.children(".active").removeClass("active");
    if (playlistIndex >= 0) playlist.children().eq(playlistIndex).addClass("active");
}

function playlistRepeatClick() {
	var btn = $("#playlist-repeat-btn");
	btn.toggleClass("mdl-button--colored");
	$.post("/playlist/control", { repeat: btn.hasClass("mdl-button--colored") });
}

function playlistCacheClick() {
	$.post("/playlist/cache");

}


function playPauseClick() {
	$.post("/player/control", { action: "playpause" }, function(data) {
		if (playerState == State.PLAYING) playerState = State.PAUSED;
		else playerState = State.PLAYING;
		$("#play-pause-btn").toggleClass("pause");
	});
}

function nextClick() {
	$.post("/player/control", { action: "next" });

}

function previousClick() {
	$.post("/player/control", { action: "previous" }, function(data) {
		songPercent = 0;
	});
}

function updateProgressBar(lastTime) {
	var time = Date.now();
	var progressBar = $("#progress-bar");

	if (!windowFocused);
	else if (songPercent < 0) {
		progressBar.fadeOut();
		songPercent = -1;
		lasSongPercent = -1;
	}
	else {
		if (lastTime && playerState == State.PLAYING && songLength > 0) {
			songPercent += (time - lastTime) * 1.0 / songLength;
		}
		if (songPercent > 1) songPercent = 1;

		if (lasSongPercent != songPercent) {
			lasSongPercent = songPercent;

			progressBar.fadeIn();
			if (!progressBar.hasClass("is-dragged")) {
				progressBar[0].MaterialProgress.setProgress(songPercent * 100);
			}
			if (songLength > 0) {
				var totalMin = Math.floor(songLength / 60000);
				var totalSec = Math.floor(songLength / 1000) % 60;
				var currentMin = Math.floor(songLength * songPercent / 60000);
				var currentSec = Math.floor(songLength * songPercent / 1000) % 60;
				var totalText =  totalMin + ":" + (totalSec < 10 ? "0" : "") + totalSec;
				var currentText = currentMin + ":" + (currentSec < 10 ? "0" : "") + currentSec;
				var timeText = currentText + " / " + totalText;
				$("#player .time").text(timeText);
			}
			else $("#player .time").text("");
		}
	}

	if (lastTime) window.setTimeout(function() { updateProgressBar(time); }, 250);
}

function sendPosition() {
	$.post("/player/control", { action: "position", percent: songPercent });
	updateProgressBar();
}

function downloadClick() {
	var url = $(this).data("target");
	if (url != "") document.location.href = url;
}

function volumeClick() {
	var volWrap = $("#volume-wrapper");
	if (!volWrap.hasClass("volume-opened")) {
		volWrap.fadeIn();
		volWrap.addClass("volume-opened")
		return;
	}
	var volSlider = $("#volume-slider");
	if (volSlider.val() == 0) volSlider[0].MaterialSlider.change(100);
	else volSlider[0].MaterialSlider.change(0);
	volumeChanged();
}

function volumeChanged(send = true) {
	var volume = $("#volume-slider").val();
	var icon = $("#volume-btn .material-icons");
	if (volume == 0) icon.text("volume_mute");
	else if (volume < 50) icon.text("volume_down");
	else icon.text("volume_up");
	if (send) $.post("/player/control", { action: "volume", level: volume });
}

function updateInfo() {
	$.getJSON("/song/info", null, function(info) {
		var title = "", source = "", sourceName = "", thumbnail = "img/generic-song.png", thumbnailFull = "", download = "";
		if (info) {
			title = info.hasOwnProperty("title") ? info.title : "Unknown title";
			source = info.hasOwnProperty("source") ? info.source : "#";
			sourceName = info.hasOwnProperty("sourceName") ? info.sourceName : "Unknown source";
			thumbnail = info.hasOwnProperty("thumbnail") ? info.thumbnail : "img/generic-song.png";
			thumbnailFull = info.hasOwnProperty("thumbnailFull") ? info.thumbnailFull : "";
			download = info.hasOwnProperty("download") ? info.download : "";
		}

		$("#player .title").text(title);
		$("#player .source").text(sourceName).attr("href", source);

		var thumb = $("#player .thumbnail").css("background-image", "url('" + thumbnail + "')");
		if (thumbnailFull != "") thumb.attr("href", thumbnailFull);
		else thumb.removeAttr("href");

		var downloadBtn = $(".download-btn");
		if (download === "") {
			downloadBtn.prop("disabled", true);
			downloadBtn.data("target", "");
		}
		else {
			downloadBtn.data("target", download);
			downloadBtn.prop("disabled", false);
		}
	});
}

function updateStatus() {
	if (!windowFocused) {
		$(window).one("focus", function(event) { updateStatus(); })
		return;
	}

	$("#offline-wrapper").hide();

	$.getJSON("/player/status", null, function(status) {
		var btn = $("#play-pause-btn");
		var newSongCount = 0, volume = -1;
		if (status) {
			playerState = status.hasOwnProperty("state") ? status.state : State.STOPPED;
			songPercent = status.hasOwnProperty("percent") ? status.percent : -1;
			newSongCount = status.hasOwnProperty("song") ? status.song : songCount;
			volume = status.hasOwnProperty("volume") ? status.volume : -1;
		}
		else {
			playerState = State.STOPPED;
			songPercent = -1;
			playlistIndex = -1;
		}

		var newPlaylistVersion = 0;
		if (status && status.hasOwnProperty("playlist")) {
			newPlaylistVersion = status.playlist.hasOwnProperty("version") ? status.playlist.version : 0;
			playlistIndex = status.playlist.hasOwnProperty("index") ? status.playlist.index : -1;
			playlistRepeat = status.playlist.hasOwnProperty("repeat") ? status.playlist.repeat : false;
		}
		else {
			playlistIndex = -1;
			playlistRepeat = false;
		}


		if (playerState == State.PLAYING) {
			if (!btn.hasClass("pause")) btn.addClass("pause");
		}
		else {
			btn.removeClass("pause");
		}
		if (playerState == State.STOPPED) $("#player-controls button").prop("disabled", true);
		else $("#player-controls button").prop("disabled", false);

		if (songCount != newSongCount) {
			updateInfo();
			songLength = -1;
			songCount = newSongCount;
		}

		if (playerState != State.STOPPED && songLength == -1) {
			songLength = -2;
			$.get("/song/length", null, function(length) {
				if (length > 0) songLength = length;
				else songLength = -1;
			}).fail(function() {
				songLength = -1;
			});
		}

		updatePlaylist(newPlaylistVersion);

		if (volume >= 0) {
			$("#volume-btn").prop("disabled", false);
			var volSlider = $("#volume-slider");
			if (!volSlider.hasClass("is-dragged")) volSlider[0].MaterialSlider.change(volume);
			volumeChanged(false);
		}
		else {
			$("#volume-btn").prop("disabled", true);
			$("#volume-wrapper").removeClass("volume-opened");
			$("#volume-wrapper").hide();
		}

	}).done(function() {
		window.setTimeout(function() { updateStatus(); }, 1500);
	}).fail(function() {
		$("#player-controls button").prop("disabled", true);
		songPercent = -1;
		$("#offline-wrapper").show();
	});
}

function updatePlaylist(newVersion) {
	var playlist = $("#playlist");

	if (playlistVersion != newVersion) {
		$.getJSON("/playlist/list", null, function(list) {
			playlist.empty();
			$.each(list, function(i, item) {
				playlist.append(
					$("<li>", {
						"class": "mdl-list__item",
						click: function(event) {
							playlistPlay(i);
						}
					}).append(
						$("<span>", {
							"class": "mdl-list__item-primary-content"
						}).append(
							$("<span>",{
								"class": "ellipsis",
								text: item["title"]
							})
						),
						$("<span>", {
							"class": "mdl-list__item-secondary-content"
						}).append(
							$("<button>", {
								"class": "mdl-list__item-secondary-action mdl-button mdl-js-button mdl-button--icon mdl-js-ripple-effect flex-noshrink",
								click: function(event) {
									event.stopPropagation();
									playlistRemove(i);
								}
							}).append(
								$("<i>", {
									text: "clear",
									"class": "material-icons"
								})
							)
						)
					)
				);
			});

			if (playlist.children().length === 0) playlistEmpty();
			playlistSelectCurrent();

		}).fail(function() {
			playlistVersion = 0;
		});
		playlistVersion = newVersion;

	}
	else {
		playlistSelectCurrent();
	}

	var repeatBtn = $("#playlist-repeat-btn");
	if (playlistRepeat != repeatBtn.hasClass("mdl-button--colored")) repeatBtn.toggleClass("mdl-button--colored");
}


// Main function
$(document).ready(function() {
	// Window focus
	$(window).focus(function() { windowFocused = true; });
	$(window).blur(function() { windowFocused = false; });

	// Click events
	$("#search-btn").click(searchBtnClick);
	$("#connection-retry-btn").click(updateStatus);
	$("#play-pause-btn").click(playPauseClick);
	$("#next-btn").click(nextClick);
	$("#previous-btn").click(previousClick);
	$(".download-btn").click(downloadClick);
	$("#playlist-repeat-btn").click(playlistRepeatClick);
	$("#playlist-cache-btn").click(playlistCacheClick);
	$("#volume-btn").click(volumeClick);

	// Input
	$("#search-textfield").keyup(function(event) {
        if(event.keyCode == 13) $("#search-btn").click();
    });

	// Playlist
	var playlist = $("#playlist-card");
	var playlistWrapper = $("#playlist-wrapper");
	playlist.on("animationend webkitAnimationEnd", function(event) {
		if (!playlistWrapper.hasClass("playlist-opened")) {
			playlist.hide();
		}
	});
	$("#playlist-toggle-btn").click(function() {
		playlistWrapper.toggleClass("playlist-opened");
		if (playlistWrapper.hasClass("playlist-opened")) playlist.show();
	});
	$(document).mouseup(function (e) {
		// Hide playlist if user clicks elsewhere
		var container = $("#bottom-wrapper");
		if (!container.is(e.target) && container.has(e.target).length === 0) {
			playlistWrapper.removeClass("playlist-opened");
		}
	});

	// Progress bar
	var progressBar = $("#progress-bar");
	function progressBarEvent(event) {
		if (event.type == "mousedown" || event.type == "touchstart") {
			progressBar.addClass("is-dragged");
			$(document).on("mousemove mouseup", progressBarEvent);
			event.preventDefault();
		}
		if (progressBar.hasClass("is-dragged")) {
       		var newPercent, x;
       		if (event.type.substr(0, 5) == "touch") x = event.originalEvent.changedTouches[0].clientX;
       		else x = event.pageX;

       		newPercent = (x - progressBar.offset().left) / progressBar.width();
       		newPercent = newPercent >= 0 ? (newPercent <= 1 ? newPercent : 1) : 0;
        	progressBar[0].MaterialProgress.setProgress(newPercent * 100);

        	if (event.type == "mouseup" || event.type == "touchend") {
        		songPercent = newPercent;
        		progressBar.removeClass("is-dragged");
        		$(document).off("mousemove mouseup", progressBarEvent);
        		sendPosition();
        	}
        	event.preventDefault();
        }
	}
	progressBar.on("mousedown touchstart touchmove touchend", progressBarEvent);

	// Volume
	$(document).mouseup(function (e) {
		// Hide volume if user clicks elsewhere
		var volWrap = $("#volume-wrapper");
		var container = $("#volume-wrapper, #volume-btn");
		if (!container.is(e.target) && container.has(e.target).length === 0) {
			volWrap.removeClass("volume-opened");
			volWrap.hide();
		}
    });

    var volSlider = $("#volume-slider");
	volSlider.on("change", function() { volumeChanged(); });
	volSlider.on("mouseup mousedown touchstart touchend", function(event) {
		if (event.type == "mousedown" || event.type == "touchstart") volSlider.addClass("is-dragged");
        else if (volSlider.hasClass("is-dragged")) volSlider.removeClass("is-dragged");
	});

	// Hotkeys
	var keyDown = {}; // Used to keep track of keys held down
	$(document).keydown(function(e) {
		if (keyDown[e.which]) return;
		keyDown[e.which] = true;

		var tag = e.target.tagName.toLowerCase();
		var enterCode = 32, zeroCode = 48, leftCode = 37, rightCode = 39;

		if (tag != "input" && tag != "textarea") {
			if (!$("#play-pause-btn").prop("disabled")) {
				if (e.which === enterCode) {
					$("#play-pause-btn").click();
					event.preventDefault();
				}
				else if (e.which >= zeroCode && e.which <= zeroCode + 9) {
					songPercent = (e.which - zeroCode) / 10.0;
					sendPosition();
				}
				else if (e.which === leftCode || e.which === rightCode) {
					var delta = 0.05;
					if (songLength > 0) delta = 5000.0 / songLength;
					if (e.which === leftCode) delta *= -1;
					songPercent += delta;
					songPercent = (songPercent < 0) ? 0 : ((songPercent > 1) ? 1 : songPercent);
					sendPosition();
				}
			}
		}
	});
	$(document).keyup(function(e) {
		delete keyDown[e.which];
	});

	// Update
	updateStatus();
	updateProgressBar(Date.now());

});