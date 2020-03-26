
// assume websockets are loaded

var JavaJukeBox = {
  songPlayingTimerId: null,

  javaJukeboxPlayerModel: {
    currentStreamUrl: null,
    playing: false,
    songDuration: null,
    songPosition: 0
  },

  refreshView() {
    if (this.javaJukeboxPlayerModel.playing == true) {
      if (this.songPlayingTimerId == null) {
        var jb = this;
        this.songPlayingTimerId = setInterval(jb.songPlayingTimer, 1000);
      }
      document.getElementById('startIcon').style.display = 'none';
      document.getElementById('pauseIcon').style.display = 'block';
    } else {
      if (this.songPlayingTimerId != null) {
        this.clearInterval(this.songPlayingTimerId);
        this.songPlayingTimerId = null;
      }
      document.getElementById('pauseIcon').style.display = 'none';
      document.getElementById('startIcon').style.display = 'block';
    }
    if (this.javaJukeboxPlayerModel.songDuration == null) {
      $("#playingDurationDisplay").html("-:--");
    } else {
      $("#playingDurationDisplay").html(this.songTimeAsString(this.javaJukeboxPlayerModel.songDuration));
    }
    $("#playingPositionDisplay").html(this.songTimeAsString(this.javaJukeboxPlayerModel.songPosition));
    $("#javaJukeboxSongPositionSlider").slider("value", this.javaJukeboxPlayerModel.songPosition);
  },

  onJavaJukeboxStart() {
    StompClient.send("/app/playqueues/" + playQueue.playerId + "/start", "");
  },

  javaJukeboxStartCallback() {
    this.javaJukeboxPlayerModel.playing = true;
    this.refreshView();
  }

  onJavaJukeboxStop() {
    StompClient.send("/app/playqueues/" + playQueue.playerId + "/stop", "");
  },

  javaJukeboxStopCallback() {
    this.javaJukeboxPlayerModel.playing = false;
    this.refreshView();
  },

  onJavaJukeboxVolumeChanged() {
    var value = $("#javaJukeboxVolumeSlider").slider("value");
    var gain = value / 100;
    StompClient.send("/app/playqueues/" + playQueue.playerId + "/jukebox/gain", gain);
  },

  javaJukeboxGainCallback(gain) {
    $("#javaJukeboxVolumeSlider").slider("option", "value", Math.floor(gain * 100));
  },

  onJavaJukeboxPositionChanged() {
    var pos = $("#javaJukeboxSongPositionSlider").slider("value");
    StompClient.send("/app/playqueues/" + playQueue.playerId + "/jukebox/position", pos);
  },

  javaJukeboxPositionCallback(pos) {
    this.javaJukeboxPlayerModel.songPosition = pos || 0;
    this.refreshView();
  },

  updateJavaJukeboxPlayerControlBar(song, pos) {
    if (song != null) {
        var playingStream = song.streamUrl;
        if (playingStream != this.javaJukeboxPlayerModel.currentStreamUrl) {
            this.javaJukeboxPlayerModel.currentStreamUrl = playingStream;
            this.newSongPlaying(song, pos);
        }
    }
  },

  songTimeAsString(timeInSeconds) {
    var minutes = Math.floor(timeInSeconds / 60);
    var seconds = timeInSeconds - minutes * 60;

    return minutes + ":" + ("00" + seconds).slice(-2);
  },

  newSongPlaying(song, pos) {
    this.javaJukeboxPlayerModel.songDuration = song.duration;
    $("#javaJukeboxSongPositionSlider").slider({max: this.javaJukeboxPlayerModel.songDuration, value: 0, animate: "fast", range: "min"});
    this.javaJukeboxPlayerModel.playing = true;
    this.javaJukeboxPlayerModel.songPosition = pos || 0;
    this.refreshView();
  },

  songPlayingTimer() {
    this.javaJukeboxPlayerModel.songPosition += 1;
    this.refreshView();
  },

  initJavaJukeboxPlayerControlBar() {
    var jb = this;
    $("#javaJukeboxSongPositionSlider").slider({max: 100, value: 0, animate: "fast", range: "min"});
    $("#javaJukeboxSongPositionSlider").slider("value", 0);
    $("#javaJukeboxSongPositionSlider").on("slidestop", function() { jb.onJavaJukeboxPositionChanged(); });

    $("#javaJukeboxVolumeSlider").slider({max: 100, value: 50, animate: "fast", range: "min"});
    $("#javaJukeboxVolumeSlider").on("slidestop", function() { jb.onJavaJukeboxVolumeChanged(); });

    this.refreshView();
  }
}
