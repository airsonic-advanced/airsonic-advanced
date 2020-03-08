<!--
# README.md
# airsonic-advanced/airsonic-advanced
-->
Airsonic-Advanced
=================
[![Build Status](https://travis-ci.org/airsonic-advanced/airsonic-advanced.svg?branch=master)](https://travis-ci.org/airsonic-advanced/airsonic-advanced)
[![Language grade: JavaScript](https://img.shields.io/lgtm/grade/javascript/g/airsonic/airsonic.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/airsonic/airsonic/context:javascript)
[![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/airsonic/airsonic.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/airsonic/airsonic/context:java)

What is Airsonic-Advanced?
--------------------------
Airsonic-Advanced is a more modern implementation of the Airsonic fork with several key performance and feature enhancements. It adds and supersedes several features in Airsonic.

What is Airsonic?
-----------------

Airsonic is a free, web-based media streamer, providing ubiquitous access to your music. Use it to share your music with friends, or to listen to your own music while at work. You can stream to multiple players simultaneously, for instance to one player in your kitchen and another in your living room.

Airsonic is designed to handle very large music collections (hundreds of gigabytes). Although optimized for MP3 streaming, it works for any audio or video format that can stream over HTTP, for instance AAC and OGG. By using transcoder plug-ins, Airsonic supports on-the-fly conversion and streaming of virtually any audio format, including WMA, FLAC, APE, Musepack, WavPack and Shorten.

If you have constrained bandwidth, you may set an upper limit for the bitrate of the music streams. Airsonic will then automatically resample the music to a suitable bitrate.

In addition to being a streaming media server, Airsonic works very well as a local jukebox. The intuitive web interface, as well as search and index facilities, are optimized for efficient browsing through large media libraries. Airsonic also comes with an integrated Podcast receiver, with many of the same features as you find in iTunes.

Written in Java, Airsonic runs on most platforms, including Windows, Mac, Linux and Unix variants.

![Screenshot](contrib/assets/screenshot.png)

Feature Enhancements:
---------------------
The following is an incomplete list of features that are enhanced from Airsonic:
- More modern base frameworks and libraries
  - Spring Boot 2.x (instead of 1.x), Spring Framework 5.x (instead of 4.x). Plus all the additional dependency upgrades due to the base libaries being upgraded (including EhCache, upgraded SQL connectors etc.)
- Security
  - A completely revamped credential system that actually stores credentials securely instead of openly. Includes encryption for credentials that need to be retrievable later (such as for third-party locations) and backwards compatibility. Also includes modern password hashing algorithms such as bcrypt, Argon for password storage.
- More compliant with web specs and utilizes frameworks to apply them instead of custom home-implemented solutions
  - RFC 7233 for Range headers
  - Send correct ETags and Last-Modified headers to aid in client-side caching
- Performance enhancements
  - A more efficient and compliant streaming engine, utilizing piping and threading
  - Removal of pessimistic locking throughout the software in favor of more modern concurrency techniques
  - Aggressively uses multi-threading and parallelization for most operations, including but not limited to:
    - Massively parallelized engine for media scanning (media scanning is done much much faster)
    - Other various use cases utilizing async or parallel options via fork-join pools
  - Use of websockets to communicate with web-clients instead of polling
    - Much lighter on resource utilization as well as more dynamic
    - Does not have to keep running the same command client side again every 10 seconds to check statuses
    - Server pushes status updates when they're needed when something has changed
    - Web clients can update UIs immediately (live views)
    - Removal of DWR (10 year old technology used as an interface between the web-client and the server)
    - Provides status indicator whether client is connected to server
- Bugfixes:
  - Several race condition fixes
  - Consistency checks and refactors
- Miscellaneous
  - Uses JSR 310 (Java time) instead of older Java packages for time/duration tracking
  - Uses Java's NIO for handling files instead of the older IO packages
  - More precise song duration calculation
  - Ability to use Repeat-One in play queues in web-clients
- Testing
  - Various fixes to make it compatible with multiple external DBs
  - Automated tests are performed against external DBs
    - Postgres
    - MySQL
    - MariaDB
  - Uses failsafe for integration testing instead of cucumber
- Build and deployment
  - An updated Docker image with OpenJDK 11 base layer.
  - A more advanced build pipeline including automatic releases and deploys at merge
    - Allows people to grab the newest build without compiling from source as soon as features/enhancements are merged, instead of waiting for the next stable build (which may be months away)

The complete list of PRs that were used to enhance Airsonic can be seen on the PRs page. At some point an automatic changelog generator will be added to keep track.

Airsonic-Advanced will occasionally backport features introduced in the base Airsonic fork, but is generally much more modern and bleeding edge than Airsonic.

History
-----

The original [Subsonic](http://www.subsonic.org/) is developed by [Sindre Mehus](mailto:sindre@activeobjects.no). Subsonic was open source through version 6.0-beta1, and closed-source from then onwards.

Libresonic was created and maintained by [Eugene E. Kashpureff Jr](mailto:eugene@kashpureff.org). It originated as an unofficial("Kang") of Subsonic which did not contain the Licensing code checks present in the official builds. With the announcement of Subsonic's closed-source future, a decision was made to make a full fork and rebrand to Libresonic.

Around July 2017, it was discovered that Eugene had different intentions/goals
for the project than some contributors had.  Although the developers were
hesitant to create a fork as it would fracture/confuse the community even
further, it was deemed necessary in order to preserve a community-focused fork.
To reiterate this more clearly:

Airsonic's goal is to provide a full-featured, stable, self-hosted media server
based on the Subsonic codebase that is free, open source, and community driven.

Around November 2019, Airsonic-Advanced was forked off the base Airsonic fork due to differences in pace and review of development. Several key features of the framework were outdated, and attempts to upgrade them occasionally took upto a year. Airsonic-Advanced tries a modern implementation and bleeding edge approach to development, and is thus usually ahead of the base fork in dependencies and features.

Pull Requests are always welcome. Keep in mind that we strive to balance
stability with new features. As such, all Pull Requests are reviewed before
being merged to ensure we continue to meet our goals.

License
-------

Airsonic-Advanced and Airsonic are free software and licensed under the [GNU General Public License version 3](http://www.gnu.org/copyleft/gpl.html). The code in this repository (and associated binaries) are free of any "license key" or other restrictions. If you wish to thank the maintainer of this repository, please consider a donation to the [Electronic Frontier Foundation](https://supporters.eff.org/donate).

The [Subsonic source code](https://github.com/airsonic/subsonic-svn) was released under the GPLv3 through version 6.0-beta1. Beginning with 6.0-beta2, source is no longer provided. Binaries of Subsonic are only available under a commercial license. There is a [Subsonic Premium](http://www.subsonic.org/pages/premium.jsp) service which adds functionality not available in Airsonic. Subsonic also offers RPM, Deb, Exe, and other pre-built packages that Airsonic [currently does not](https://github.com/airsonic/airsonic/issues/65).

The cover zooming feature is provided by [jquery.fancyzoom](https://github.com/keegnotrub/jquery.fancyzoom),
released under [MIT License](http://www.opensource.org/licenses/mit-license.php).

The icons are from the amazing [feather](https://feathericons.com/) project,
and are licensed under [MIT license](https://github.com/feathericons/feather/blob/master/LICENSE).

Usage
-----
Airsonic-Advanced can be downloaded from
[GitHub](https://github.com/airsonic-advanced/airsonic-advanced/releases).

Docker releases are at [DockerHub](https://hub.docker.com/r/airsonicadvanced/airsonic-advanced).

Please note that for Docker images, the volume mounting points have changed and are different from Airsonic. Airsonic mount points are at /airsonic/* inside the container. Airsonic-Advanced tries to use the same volume locations as the default war image at /var/* in order to remain consistent if people want to switch between the containers and non-containers.
  - `Music:/airsonic/music` -> `Music:/var/music`
  - `Podcasts:/airsonic/podcast` -> `Podcasts:/var/podcast`
  - `Playlists:/airsonic/playlists` -> `Playlists:/var/playlists`
  - `/airsonic/data` -> `/var/airsonic`

Airsonic can be downloaded from
[GitHub](https://github.com/airsonic/airsonic/releases).

Please use the [Airsonic documentation](https://airsonic.github.io/docs/) for instructions on running Airsonic.


Community
---------
Airsonic itself has several places outside of github for community discussion, questions, etc:

- [#airsonic:matrix.org on Matrix](https://matrix.to/#/#airsonic:matrix.org)
- [#airsonic on IRC](http://webchat.freenode.net?channels=%23airsonic)
- [airsonic subreddit](https://www.reddit.com/r/airsonic)

*Note that the Matrix room and IRC channel are bridged together.*
