
// assume jquery, websockets, playqueue are loaded

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
        this.songPlayingTimerId = setInterval(function() { jb.songPlayingTimer(); }, 1000);
      }
    } else {
      if (this.songPlayingTimerId != null) {
        clearInterval(this.songPlayingTimerId);
        this.songPlayingTimerId = null;
      }
    }
    if (this.javaJukeboxPlayerModel.songDuration == null) {
      $("#playingDurationDisplay").html("-:--");
    } else {
      $("#playingDurationDisplay").html(this.songTimeAsString(this.javaJukeboxPlayerModel.songDuration));
    }
    $("#playingPositionDisplay").html(this.songTimeAsString(this.javaJukeboxPlayerModel.songPosition));
    $("#javaJukeboxSongPositionSlider").slider("value", this.javaJukeboxPlayerModel.songPosition);
  },

  javaJukeboxStartCallback() {
    this.javaJukeboxPlayerModel.playing = true;
    this.refreshView();
  },

  javaJukeboxStopCallback() {
    this.javaJukeboxPlayerModel.playing = false;
    this.refreshView();
  },

  onJavaJukeboxPositionChanged() {
    var pos = $("#javaJukeboxSongPositionSlider").slider("value");
    StompClient.send("/app/playqueues/" + playQueue.player.id + "/jukebox/position", pos);
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
    $("#javaJukeboxSongPositionSlider").on("slidestop", function() { jb.onJavaJukeboxPositionChanged(); });

    this.refreshView();
  },

  reset() {
    this.javaJukeboxPlayerModel.currentStreamUrl = null;
    this.javaJukeboxPlayerModel.playing = false;
    this.javaJukeboxPlayerModel.songDuration = null;
    this.javaJukeboxPlayerModel.songPosition = 0;

    this.refreshView();
  }
}
