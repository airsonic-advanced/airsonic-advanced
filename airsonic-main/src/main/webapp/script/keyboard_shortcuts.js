if (top.keyboardShortcutsEnabled) {
  Mousetrap.bind('space', function() { top.playQueue.onToggleStartStop(); return false; });
  Mousetrap.bind('left',  function() { top.playQueue.onPrevious(); });
  Mousetrap.bind('right', function() { top.playQueue.onNext('OFF'); });
  Mousetrap.bind('*',     function() { top.playQueue.onStarCurrent(); });
  Mousetrap.bind('plus',  function() { top.playQueue.onGainAdd(+5); });
  Mousetrap.bind('-',     function() { top.playQueue.onGainAdd(-5); });
  Mousetrap.bind('q',     function() { top.playQueue.onTogglePlayQueue(!top.playQueue.isVisible); });

  Mousetrap.bind('/',     function() { parent.frames.upper.$("#query").focus(); });
  Mousetrap.bind('m',     function() { parent.frames.upper.toggleLeftFrameVisible(); });

  Mousetrap.bind('g h', function() { parent.frames.main.location.href = "home.view?"; });
  Mousetrap.bind('g p', function() { parent.frames.main.location.href = "playlists.view?"; });
  Mousetrap.bind('g o', function() { parent.frames.main.location.href = "podcastChannels.view?"; });
  Mousetrap.bind('g s', function() { parent.frames.main.location.href = "settings.view?"; });
  Mousetrap.bind('g b', function() { parent.frames.main.location.href = "bookmarks.view?"; });
  Mousetrap.bind('g t', function() { parent.frames.main.location.href = "starred.view?"; });
  Mousetrap.bind('g r', function() { parent.frames.main.location.href = "more.view?"; });
  Mousetrap.bind('g a', function() { parent.frames.main.location.href = "help.view?"; });
  Mousetrap.bind('?',   function() { parent.frames.main.location.href = "more.view#shortcuts"; });
}
