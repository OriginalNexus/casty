(() => {

// Global variables
var ws;
var songLength = -1;
var songPercent = -1;
var progressBarValue = 0;
var progressBar;
var lastUpdateTime;
var State = {
	'STOPPED': 0,
	'PLAYING': 1,
	'PAUSED': 2
}
var playerState = State.STOPPED;
var playlistIndex = -1;

function createWebSocket() {
    $('#offline-wrapper').hide();

    // WebSocket
    ws = new WebSocket('ws://' + location.host + '/ws/');

    ws.onopen = function() {
        console.log('WebSocket opened!');
    };

    ws.onmessage = function (evt) {
        updateStatus(JSON.parse(evt.data));
    };

    ws.onclose = function() {
        console.log('WebSocket closed!');
        updateStatus();
    };

    ws.onerror = function(err) {
        console.log('WebSocket error!');
        console.log(err);
    };
}

function sendCommand(scope, action, params = {}) {
    ws.send(JSON.stringify({
        scope: scope,
        action: action,
        params: params
    }));
}

function searchResultClick(target, url) {
    sendCommand('player', 'playurl', { url: url });
}

function search() {
	var query = $('#search-textfield').val();

	if (query === '')
	    return;

	if (query.startsWith('http://') || query.startsWith('https://')) {
	    sendCommand('player', 'playurl', { url: query });
		return;
	}

	var overlay = $('#content-overlay');
	var resultsWrapper = $('#results-wrapper');
	var results = $('#results');

	overlay.fadeIn();

	$.getJSON('results?q=' + encodeURI(query), null, function(data) {
		resultsWrapper.hide();

		if (results) {
			results.empty();
			$.each(data, function(i, result) {
				var resultDiv =
				$('<li>', {
					'class': 'list-group-item list-group-item-action d-flex align-items-center px-0',
					click: function() {
						searchResultClick(this, 'https://www.youtube.com/watch?v=' + result['id']);
					}
				}).append(
					$('<div>', {
						'class': 'thumbnail flex-shrink-0 ml-3',
						css: {
							'background-image': 'url("' + result['thumbnail'] + '")'
						}
					}),
					$('<div>', {
						'class': 'd-flex flex-column flex-grow-1 ml-3',
						css: {
						    'min-width': '0'
						}
					}).append(
						$('<span>', {
							'class': 'text-truncate',
							text: result['title']
						}),
						$('<span>', {
							'class' : 'text-truncate',
							text: result['author'] ? result['author'] : 'Unknown artist'
						})
					),
					$('<div>', {
						'class': 'flex-shrink-0 mr-1'
					}).append(
						$('<button>', {
							'class': 'btn btn-flat btn-icon',
							click: function(event) {
								playlistAdd(result['title'], 'https://www.youtube.com/watch?v=' + result['id']);
								event.stopPropagation();
							}
						}).append(
						    $('<i>', {
						        'class': 'material-icons',
						        text: 'playlist_add'
						    })
						)
					)
				);
				results.append(resultDiv);
			});
			resultsWrapper.fadeIn();
		}
	})
	.always(function() {
		overlay.hide();
	});
}


function playlistAdd(title, url) {
    sendCommand('playlist', 'add', { title: title, url: url });
}

function playlistRemove(index) {
    sendCommand('playlist', 'remove', { index: index });

    $('#playlist').children().eq(index).remove();
}

function playlistPlay(index) {
    sendCommand('player', 'playlist', { index: index });
}

function playlistSelectCurrent() {
	var playlist = $('#playlist');

	playlist.children('.active').removeClass('active');
    if (playlistIndex >= 0)
        playlist.children().eq(playlistIndex).addClass('active');
}

function playlistRepeatClick() {
	var btn = $('#playlist-repeat-btn');

	btn.toggleClass('colored');
	sendCommand('playlist', 'repeat', { value: btn.hasClass('colored') });
}

function playlistCacheClick() {
    sendCommand('playlist', 'cache');
}


function playPauseClick() {
    sendCommand('player', 'playpause');

    if (playerState == State.PLAYING)
        playerState = State.PAUSED;
    else
        playerState = State.PLAYING;

    $('#play-pause-btn').toggleClass('pause');
}

function nextClick() {
    sendCommand('player', 'next');
}

function previousClick() {
    sendCommand('player', 'previous');
}

function setProgressBar(percent) {
    progressBar.children().css('width', percent * 100 + '%');
}

function updateProgressBar() {
	if (songPercent < 0) {
		progressBar.fadeOut();
	}
	else {
		if (playerState == State.PLAYING && songLength > 0)
			songPercent += (Date.now() - lastUpdateTime) / songLength;

		if (songPercent > 1)
		    songPercent = 1;

        progressBar.fadeIn();

        if (!progressBar.hasClass('is-dragged'))
            setProgressBar(songPercent);

        if (songLength > 0) {
            var totalMin = Math.floor(songLength / 60000);
            var totalSec = Math.floor(songLength / 1000) % 60;
            var currentMin = Math.floor(songLength * songPercent / 60000);
            var currentSec = Math.floor(songLength * songPercent / 1000) % 60;
            var totalText =  totalMin + ':' + (totalSec < 10 ? '0' : '') + totalSec;
            var currentText = currentMin + ':' + (currentSec < 10 ? '0' : '') + currentSec;
            var timeText = currentText + ' / ' + totalText;
            $('#player .time').text(timeText);
        }
        else {
            $('#player .time').text('');
        }
	}
	lastUpdateTime = Date.now();
	requestAnimationFrame(updateProgressBar);
}

function sendPosition() {
    sendCommand('player', 'position', { percent: songPercent });
}

function downloadClick() {
	var url = $(this).data('target');

	if (url != '')
	    document.location.href = url;
}

function saveClick() {
    var btn = $('.save-btn');
    var dlBtn = $('.download-btn');

    if (btn.hasClass('colored')) {
        if ($('.download-btn').prop('disabled')) {
            return;
        }
        sendCommand('player', 'uncache');
        dlBtn.prop('disabled', true);
    }
    else {
        sendCommand('player', 'cache');
    }

    btn.toggleClass('colored');
}

function volumeClick() {
	var volWrap = $('#volume-wrapper');

	if (!volWrap.hasClass('volume-opened')) {
		volWrap.fadeIn();
		volWrap.addClass('volume-opened')
		return;
	}

	var volSlider = $('#volume-slider');

	if (volSlider.val() == 0)
	    volSlider.val(100);
	else
	    volSlider.val(0);

	volumeChanged();
}

function volumeChanged(send = true) {
	var volume = $('#volume-slider').val();
	var icon = $('#volume-btn i');

	if (volume == 0)
	    icon.text('volume_mute');
	else if (volume < 50)
	    icon.text('volume_down');
	else
	    icon.text('volume_up');

	if (send)
	    sendCommand('player', 'volume', { level: volume });
}

function updateStatus(statusMsg) {
    if (ws == null) {
        createWebSocket()
        return;
    }

    if (ws.readyState == 1) {
        if (statusMsg.scope == 'player') {
            var status = statusMsg.data;

            if (status.hasOwnProperty('state')) {
                playerState = status.state;

                var btn = $('#play-pause-btn');
                if (playerState == State.PLAYING)
                    btn.addClass('pause');
                else
                    btn.removeClass('pause');

                if (playerState == State.STOPPED) {
                    $('#player-controls button').prop('disabled', true);
                    $('.save-btn').prop('disabled', true);
                }
                else {
                    $('#player-controls button').prop('disabled', false);
                    $('.save-btn').prop('disabled', false);
                }
            }

            if (status.hasOwnProperty('percent')) {
                songPercent = status.percent;
                lastUpdateTime = Date.now();
            }

            if (status.hasOwnProperty('songLength'))
                songLength = status.songLength;

            if (status.hasOwnProperty('volume')) {
                var volSlider = $('#volume-slider');

                if (!volSlider.hasClass('is-dragged')) {
                    volSlider.val(status.volume);
                    volumeChanged(false);
                }
            }
        }
        else if (statusMsg.scope == 'playlist') {
            var status = statusMsg.data;

            if (status.hasOwnProperty('items'))
                updatePlaylistItems(status.items);

            if (status.hasOwnProperty('index')) {
                playlistIndex = status.index;
                playlistSelectCurrent();
            }

            if (status.hasOwnProperty('repeat')) {
                var repeatBtn = $('#playlist-repeat-btn');

                if (status.repeat != repeatBtn.hasClass('colored'))
                    repeatBtn.toggleClass('colored');
            }
        }
        else if (statusMsg.scope == 'song') {
            var info = statusMsg.data;

            $('#player .title').text(info.title);
            $('#player .source').text(info.sourceName).attr('href', info.source);

            var thumb = $('#player .thumbnail').css('background-image', 'url("' + info.thumbnail + '")');

            if (info.thumbnailFull)
                thumb.attr('href', info.thumbnailFull);
            else
                thumb.removeAttr('href');

            var downloadBtn = $('.download-btn');
            var saveBtn = $('.save-btn');

            if (info.download) {
                downloadBtn.prop('disabled', false);
                downloadBtn.data('target', info.download);
                saveBtn.addClass('colored');
            } else {
                downloadBtn.prop('disabled', true);
                saveBtn.removeClass('colored');
            }

        }
    }
    else {
        $('#player-controls button, .save-btn, .download-btn').prop('disabled', true);
        songPercent = -1;
        $('#offline-wrapper').show();
        ws = null;
    }
}

function updatePlaylistItems(items) {
	var playlist = $('#playlist');

    playlist.empty();
    $.each(items, function(i, item) {
        playlist.append(
            $('<li>', {
                'class': 'list-group-item list-group-item-action d-flex align-items-center py-0 pr-1',
                click: function(event) {
                    playlistPlay(i);
                }
            }).append(
                $('<span>', {
                    'class': 'flex-grow-1 ellipsis',
                    text: item['title']
                }),
                $('<button>', {
                    'class': 'btn btn-flat btn-icon flex-noshrink',
                    click: function(event) {
                        event.stopPropagation();
                        playlistRemove(i);
                    },

                }).append(
                    $('<i>', {
                        'class': 'material-icons',
                        text: 'clear'
                    })
                )

            )
        );
    });
}


// Main function
$(document).ready(function() {

	// Click events
	$('#connection-retry-btn').click(createWebSocket);
	$('#play-pause-btn').click(playPauseClick);
	$('#next-btn').click(nextClick);
	$('#previous-btn').click(previousClick);
	$('.download-btn').click(downloadClick);
	$('#playlist-repeat-btn').click(playlistRepeatClick);
	$('#playlist-cache-btn').click(playlistCacheClick);
	$('#volume-btn').click(volumeClick);
	$('.save-btn').click(saveClick);

	// Input
	$('#search-textfield').keyup(function(event) {
        if(event.keyCode == 13)
            search();
    });

	// Playlist
	var playlist = $('#playlist-card');
	var playlistWrapper = $('#playlist-wrapper');
	playlist.on('animationend webkitAnimationEnd', function(event) {
		if (!playlistWrapper.hasClass('playlist-opened')) {
			playlist.hide();
		}
	});
	$('#playlist-toggle-btn').click(function() {
		playlistWrapper.toggleClass('playlist-opened');
		if (playlistWrapper.hasClass('playlist-opened'))
		    playlist.show();
	});
	$(document).mouseup(function (e) {
		// Hide playlist if user clicks elsewhere
		var container = $('#bottom-wrapper');
		if (!container.is(e.target) && container.has(e.target).length === 0) {
			playlistWrapper.removeClass('playlist-opened');
		}
	});

	// Progress bar
	progressBar = $('#progress-bar');
	function progressBarEvent(event) {
		if (event.type == 'mousedown' || event.type == 'touchstart') {
			progressBar.addClass('is-dragged');
			$(document).on('mousemove mouseup', progressBarEvent);
			event.preventDefault();
		}
		if (progressBar.hasClass('is-dragged')) {
       		var newPercent, x;
       		if (event.type.substr(0, 5) == 'touch')
       		    x = event.originalEvent.changedTouches[0].clientX;
       		else
       		    x = event.pageX;

       		newPercent = x / progressBar.width();
       		newPercent = newPercent >= 0 ? (newPercent <= 1 ? newPercent : 1) : 0;
        	setProgressBar(newPercent);

        	if (event.type == 'mouseup' || event.type == 'touchend') {
        		songPercent = newPercent;
        		progressBar.removeClass('is-dragged');
        		$(document).off('mousemove mouseup', progressBarEvent);
        		sendPosition();
        	}
        	event.preventDefault();
        }
	}
	progressBar.on('mousedown touchstart touchmove touchend', progressBarEvent);

	// Volume
	$(document).mouseup(function (e) {
		// Hide volume if user clicks elsewhere
		var volWrap = $('#volume-wrapper');
		var container = $('#volume-wrapper, #volume-btn');
		if (!container.is(e.target) && container.has(e.target).length === 0) {
			volWrap.removeClass('volume-opened');
			volWrap.hide();
		}
    });

    var volSlider = $('#volume-slider');
	volSlider.on('change', function() {
	    volumeChanged();
	});
	volSlider.on('mouseup mousedown touchstart touchend', function(event) {
		if (event.type == 'mousedown' || event.type == 'touchstart')
		    volSlider.addClass('is-dragged');
        else if (volSlider.hasClass('is-dragged'))
            volSlider.removeClass('is-dragged');
	});

	// Hotkeys
	var keyDown = {}; // Used to keep track of keys held down
	$(document).keydown(function(e) {
		if (keyDown[e.which])
		    return;
		keyDown[e.which] = true;

		var tag = e.target.tagName.toLowerCase();
		var enterCode = 32, zeroCode = 48, leftCode = 37, rightCode = 39;

		if (tag != 'input' && tag != 'textarea') {
			if (!$('#play-pause-btn').prop('disabled')) {
				if (e.which === enterCode) {
					$('#play-pause-btn').click();
					event.preventDefault();
				}
				else if (e.which >= zeroCode && e.which <= zeroCode + 9) {
					songPercent = (e.which - zeroCode) / 10.0;
					sendPosition();
				}
				else if (e.which === leftCode || e.which === rightCode) {
					var delta = 0.05;

					if (songLength > 0)
					    delta = 5000.0 / songLength;
					if (e.which === leftCode)
					    delta *= -1;
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

    createWebSocket();

	// Update progress bar
	lastUpdateTime = Date.now();
	requestAnimationFrame(updateProgressBar);

});

})();