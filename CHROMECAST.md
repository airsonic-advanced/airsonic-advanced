Several issues may dog Chromecast usage.

This is intended as an informational reference.

- The Presentation API which contains Chromecast [isn't available over HTTP anymore](https://chromium-review.googlesource.com/c/chromium/src/+/1306336) [(source)](https://stackoverflow.com/questions/56568652/window-chrome-cast-not-available-on-android-chrome-browser)
  - As such if you go to an Airsonic website via an http connection, you may not see the Chromecast button appear.

- Chromecast does not accept self-signed certificates (for https) for media playback
  - Thus, if your Airsonic installation is HTTPS (and you're accessing the page via HTTPS to be able to cast as above), but are using self-signed certificates to get the HTTPS, Chromecast may not play your media.
  - Note that Chromecast can play HTTP media just fine

- Both the above issues lead to a unique problem:
   - If you play over http, you don't get the Cast button, even if Chromecast can play your media
   - If you play over https with self-signed certs, you can get the Cast button, but Chromecast can no longer play your media
   - Thus, ideally you should be using https with certs from a trusted authority.
