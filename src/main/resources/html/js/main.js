// Global variables
var windowFocused = true;
var length = 0;

// URL input
function inputChanged() {
	$("#input-textfield-group").removeClass("is-invalid");
}
function playBtnClick() {
	var url = $("#input-textfield").val();
	if (url === "") {
		$("#input-textfield-group").addClass("is-invalid");
		return;
	}
	playUrl(url);
}

function playUrl(url) {
	$.post("/player/control", { action: "playurl", url: url}, function(data) { updateInfo(); });
}

function searchResultClick(target, url) {
	playUrl(url);
}

function searchBtnClick() {
	var query = $("#input-textfield").val();
	if (query === "") {
		$("#input-textfield-group").addClass("is-invalid");
		return;
	}
	$("#results-wrapper").fadeOut();
	$.getJSON("/player/results?q=" + encodeURI(query), null, function(results) {
		if (results) {
			var resultsDiv = $("#results");
			resultsDiv.empty();
			$.each(results, function(id, title) {
				resultsDiv.append("<div class='search-result' onClick='searchResultClick(this, \"https://www.youtube.com/watch?v=" + id  + "\");'>" +
						"<p class='thumbnail' style='background-image: url(\"http://img.youtube.com/vi/" + id + "/mqdefault.jpg\");'></p>" +
						"<p>" + title + "</p>" +
					"</div>");
			});
			$("#results-wrapper").fadeIn();
		}
	});
}

// Player
function playPauseClick() {
	$.post("/player/control", { action: "playpause"}, function(data) {  $("#play-pause-btn").toggleClass("pause"); });
}
function setProgressBar(percent) {
	if (percent < 0) percent = 0;
	if (percent > 1) percent = 1;
	if (!$("#progress-bar").hasClass("is-dragged")) {
		document.getElementById("progress-bar").MaterialSlider.change(percent * 100);
	}
	if (length > 0) $("#player .time").text(Math.floor(length * percent / 60000) + ":"
			+ (Math.floor(length * percent / 1000) % 60 < 10 ? "0" : "") + Math.floor(length * percent / 1000) % 60 + "/"
			+ Math.floor(length / 60000) + ":"
			+ (Math.floor(length / 1000) % 60 < 10 ? "0" : "") + Math.floor(length / 1000) % 60);
	else $("#player .time").text("");
}

// Server update
function updateInfo() {
	$.getJSON("/player/info", null, function(info) {
		var title = "Unknown title";
		var source = "#";
		var sourceName = "Unknown source";
		var thumbnail = "default-thumb.png";
		var thumbnailFull = thumbnail;
		if (!info) {
			title = "";
			source = "";
			sourceName = "";
			thumbnail = "";
			thumbnailFull = "";
			length = 0;
		}
		else {
			if (info.hasOwnProperty("title")) {
				title = info.title;
			}
			if (info.hasOwnProperty("source")) {
				source = info.source;
			}
			if (info.hasOwnProperty("sourceName")) {
				sourceName = info.sourceName;
			}
			if (info.hasOwnProperty("thumbnail")) {
				thumbnail = info.thumbnail;
			}
			if (info.hasOwnProperty("thumbnailFull")) {
            	thumbnailFull = info.thumbnailFull;
            }
            if (info.hasOwnProperty("length")) {
            	length = info.length;
            }
        }
		$("#player .title").text(title);
		$("#player .source").text(sourceName);
		$("#player .source").attr("href", source);
		$("#player .thumbnail").css("background-image", "url('" + thumbnail + "')");
		$("#player .thumbnail").attr("href", thumbnailFull);
	});
}
function updateStatus() {
	if (!windowFocused) {
		$(window).one("focus", function(event) { updateStatus(); })
		return;
	}

	$("#offline-message").hide();

	$.getJSON("/player/status", null, function(status) {
		var btn = $("#play-pause-btn");
		var state;
		if (status.hasOwnProperty("state")) state = status.state;

		if (state === "PLAYING") {
			if (!btn.hasClass("pause")) btn.addClass("pause");
		}
		else {
			btn.removeClass("pause");
		}

		if (status.hasOwnProperty("percent") && status.percent >= 0) {
			setProgressBar(status.percent);
			$("#player .progress-wrapper").show();
		}
		else {
			$("#player .progress-wrapper").hide();
		}

		$("#player-controls button").prop("disabled", false);

	}).done(function() {
		window.setTimeout(function() { updateStatus(); }, 1000);
	}).fail(function() {
		$("#player-controls button").prop("disabled", true);
		$("#player .progress-wrapper").hide();
		$("#offline-message").show();
	});

	updateInfo();
}


$(document).ready(function() {
	// Window focus
	$(window).focus(function() { windowFocused = true; });
	$(window).blur(function() { windowFocused = false; });

	// Input
	$("#input-textfield").keyup(function(event){
        if(event.keyCode == 13){
            $("#input-textfield-group + button").click();
        }
    });

	// Playlist
	var playlist = $("#playlist");
	playlist.on("animationend webkitAnimationEnd", function(event) {
		if (!playlist.hasClass("playlist-opened")) {
			playlist.hide();
		}
	});
	$("#playlist-toggle-btn").click(function() {
		playlist.toggleClass("playlist-opened");
		if (playlist.hasClass("playlist-opened")) {
			playlist.show();
		}
	});
	$(document).mouseup(function (e) {
		// Hide playlist if user clicks elsewhere
		var container = $("#player-container");
		if (!container.is(e.target) && container.has(e.target).length === 0) {
			playlist.removeClass("playlist-opened");
		}
	});

	// Progress bar
	var progressBar = $("#progress-bar");
	progressBar.hover(function() { progressBar.focus(); }, function() { progressBar.blur(); });
	progressBar.on("mousedown touchstart", function(event) {
		$(this).addClass("is-dragged");
	});
	progressBar.on("mouseup touchend", function(event) {
		if ($(this).hasClass("is-dragged")) {
			$(this).removeClass("is-dragged");
			$.post("/player/control", { action: "position", percent: progressBar.val() / 100.0 });
		}
	});

	// Hotkeys
	$(document).on('keypress', function(e) {
		var tag = e.target.tagName.toLowerCase();
		if ( e.which === 32 && tag != 'input' && tag != 'textarea' && !$("#play-pause-btn").prop("disabled")) {
			$("#play-pause-btn").click();
		}
	});

	// Update
	updateStatus();

});