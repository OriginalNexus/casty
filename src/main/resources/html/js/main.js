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
				var resultDiv = $("<div>", {
					"class": "search-result flex flex-align",
					click: function() {
						searchResultClick(this, "https://www.youtube.com/watch?v=" + result["id"]);
					}
				}).append($("<p>", {
					"class": "thumbnail",
					css: {
						"background-image": "url(\"" + result["thumbnail"] + "\")"
					}
				}), $("<p>", {
					text: result["title"]
				}), $("<div>", {
					"class": "spinner"
				}).append($("<div>", {
					"class": "mdl-spinner mdl-js-spinner mdl-spinner--single-color"
				})));
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


function playPauseClick() {
	$.post("/player/control", { action: "playpause"}, function(data) {
		if (playerState == State.PLAYING) playerState = State.PAUSED;
		else playerState = State.PLAYING;
		$("#play-pause-btn").toggleClass("pause");
	});
}

function nextClick() {
	$.post("/player/control", { action: "next"});
}

function previousClick() {
	$.post("/player/control", { action: "previous"}, function(data) {
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
		var newSongCount = 0;
		if (status) {
			playerState = status.hasOwnProperty("state") ? status.state : State.STOPPED;
			songPercent = status.hasOwnProperty("percent") ? status.percent : -1;
			newSongCount = status.hasOwnProperty("songCount") ? status.songCount : songCount;
		}
		else {
			playerState = State.STOPPED;
			songPercent = -1;
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

		if (status && songLength == -1) {
			songLength = -2;
			$.get("/song/length", null, function(length) {
				if (length > 0) songLength = length;
				else songLength = -1;
			}).fail(function() {
				songLength = -1;
			});
		}

	}).done(function() {
		window.setTimeout(function() { updateStatus(); }, 1500);
	}).fail(function() {
		$("#player-controls button").prop("disabled", true);
		songPercent = -1;
		$("#offline-wrapper").show();
	});
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

	// Input
	$("#search-textfield").keyup(function(event) {
        if(event.keyCode == 13) $("#search-btn").click();
    });

	// Playlist
	var playlist = $("#playlist");
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