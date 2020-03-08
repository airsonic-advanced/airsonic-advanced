
// assume websockets are loaded

var songPlayingTimerId = null;

var javaJukeboxPlayerModel = {
  currentStreamUrl : null,
  playing : false,
  songDuration : null,
  songPosition : 0
};

function refreshView() {
  if (javaJukeboxPlayerModel.playing == true) {
    if (songPlayingTimerId == null) {
      songPlayingTimerId = setInterval(songPlayingTimer, 1000);
    }
    document.getElementById('startIcon').style.display = 'none';
    document.getElementById('pauseIcon').style.display = 'block';
  } else {
    if (songPlayingTimerId != null) {
        clearInterval(songPlayingTimerId);
        songPlayingTimerId = null;
    }
    document.getElementById('pauseIcon').style.display = 'none';
    document.getElementById('startIcon').style.display = 'block';
  }
  if (javaJukeboxPlayerModel.songDuration == null) {
    $("#playingDurationDisplay").html("-:--");
  } else {
    $("#playingDurationDisplay").html(songTimeAsString(javaJukeboxPlayerModel.songDuration));
  }
  $("#playingPositionDisplay").html(songTimeAsString(javaJukeboxPlayerModel.songPosition));
  $("#javaJukeboxSongPositionSlider").slider("value",javaJukeboxPlayerModel.songPosition);
}

function onJavaJukeboxStart() {
  StompClient.send("/app/playqueues/" + playerId + "/start", "");
}

function javaJukeboxStartCallback() {
  javaJukeboxPlayerModel.playing = true;
  refreshView();
}

function onJavaJukeboxStop() {
  StompClient.send("/app/playqueues/" + playerId + "/stop", "");
}

function javaJukeboxStopCallback() {
  javaJukeboxPlayerModel.playing = false;
  refreshView();
}

function onJavaJukeboxVolumeChanged() {
    var value = $("#javaJukeboxVolumeSlider").slider("value");
    var gain = value / 100;
    StompClient.send("/app/playqueues/" + playerId + "/jukebox/gain", gain);
}

function javaJukeboxGainCallback(gain) {
    $("#javaJukeboxVolumeSlider").slider("option", "value", Math.floor(gain * 100));
}

function onJavaJukeboxPositionChanged() {
    var pos = $("#javaJukeboxSongPositionSlider").slider("value");
    StompClient.send("/app/playqueues/" + playerId + "/jukebox/position", pos);
}

function javaJukeboxPositionCallback(pos) {
    javaJukeboxPlayerModel.songPosition = pos || 0;
    refreshView();
}

function updateJavaJukeboxPlayerControlBar(song, pos) {
    if (song != null) {
        var playingStream = song.streamUrl;
        if (playingStream != javaJukeboxPlayerModel.currentStreamUrl) {
            javaJukeboxPlayerModel.currentStreamUrl = playingStream;
            newSongPlaying(song, pos);
        }
    }
}

function songTimeAsString(timeInSeconds) {
    var minutes = Math.floor(timeInSeconds / 60);
    var seconds = timeInSeconds - minutes * 60;

    return minutes + ":" + ("00" + seconds).slice(-2);
}

function newSongPlaying(song, pos) {
    javaJukeboxPlayerModel.songDuration = song.duration;
    $("#javaJukeboxSongPositionSlider").slider({max: javaJukeboxPlayerModel.songDuration, value: 0, animate: "fast", range: "min"});
    javaJukeboxPlayerModel.playing = true;
    javaJukeboxPlayerModel.songPosition = pos || 0;
    refreshView();
}

function songPlayingTimer() {
    javaJukeboxPlayerModel.songPosition += 1;
    refreshView();
}

function initJavaJukeboxPlayerControlBar() {
    $("#javaJukeboxSongPositionSlider").slider({max: 100, value: 0, animate: "fast", range: "min"});
    $("#javaJukeboxSongPositionSlider").slider("value",0);
    $("#javaJukeboxSongPositionSlider").on("slidestop", onJavaJukeboxPositionChanged);

    $("#javaJukeboxVolumeSlider").slider({max: 100, value: 50, animate: "fast", range: "min"});
    $("#javaJukeboxVolumeSlider").on("slidestop", onJavaJukeboxVolumeChanged);

    refreshView();
}
