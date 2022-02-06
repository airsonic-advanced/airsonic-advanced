<!--
# README.md
# airsonic-advanced/airsonic-advanced
-->
Airsonic-Advanced
=================
![](https://github.com/airsonic-advanced/airsonic-advanced/workflows/Edge%20Deploy%20CI%20(Maven)/badge.svg)
![](https://github.com/airsonic-advanced/airsonic-advanced/workflows/Stable%20Deploy%20CI%20(Maven)/badge.svg)
[![Language grade: JavaScript](https://img.shields.io/lgtm/grade/javascript/g/airsonic-advanced/airsonic-advanced.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/airsonic-advanced/airsonic-advanced/context:javascript)
[![Language grade: Java](https://img.shields.io/lgtm/grade/java/g/airsonic-advanced/airsonic-advanced.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/airsonic-advanced/airsonic-advanced/context:java)

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
- Compliance: More compliant with web specs and utilizes frameworks to apply them instead of custom home-implemented solutions
  - RFC 7233 for Range headers
  - Send correct ETags and Last-Modified headers to aid in client-side caching
- Performance enhancements
  - A more efficient and compliant streaming engine, utilizing piping and threading
  - Removal of pessimistic locking throughout the software in favor of more modern concurrency techniques
  - Upgraded internal database that uses connection pooling and uses MVCC control mode for dealing with concurrent updates
    - Massive throughput boost (100K media library scan times reduced from ~40 min to ~3 mins)
  - Much faster UI rendering for browsers, especially for massive playlists
  - Aggressively uses multi-threading and parallelization for most operations, including but not limited to:
    - Massively parallelized engine for media scanning (media scanning is done much much faster, ~8x)
    - Other various use cases utilizing async or parallel options via fork-join pools
  - Use of websockets to communicate with web-clients instead of polling
    - Much lighter on resource utilization as well as more dynamic
    - Does not have to keep running the same command client side again every 10 seconds to check statuses
    - Server pushes status updates when they're needed when something has changed
    - Web clients can update UIs immediately (live views)
    - Removal of DWR (10 year old technology used as an interface between the web-client and the server)
    - Provides status indicator whether client is connected to server
- UI:
    - HTML5 compliant
      - Redesigned layout: Uses iframes instead of framesets and frames
    - Utilize a dedicated library (DataTables) to render tables
      - Deferred rendering and data manipulation outside the DOM allows much faster rendering (~10x-800x!)
        - The bigger the table, the more performance benefits it sees
        - Play queue that took about 800s to render in the browser, can now render in < 1s
      - Allow optional paging and accessible searching within tables
    - Customize generated cover art thumbnail quality
    - Ability to show and display more track fields in playlists, playqueue and file browse mode
    - Option to show header row for track fields
    - Sort tracks in browse mode (such as show most recently scanned files etc.)
    - Star and unstar from playqueue and media browser in bulk
    - Status charts/graphs are rendered on client-side using JS library, instead of on the server
- Bugfixes:
  - Several race condition fixes
  - Consistency checks and refactors
  - Documentation fixes
- Miscellaneous
  - Works with JDK17
  - Uses JSR 310 (Java time) instead of older Java packages for time/duration tracking
  - Uses Java's NIO for handling files instead of the older IO packages
  - More precise song duration calculation
  - Ability to pass properties via environment or system variables. You can but do not need to modify `airsonic.properties` to change preferences
  - Ability to use custom URLs to scrobble on ListenBrainz servers
  - Ability to use Repeat-One in play queues in web-clients
  - Sonos support: [read documentation](https://github.com/airsonic-advanced/airsonic-advanced/blob/master/SONOS.md)
  - Chromecast support: [read details](https://github.com/airsonic-advanced/airsonic-advanced/blob/master/CHROMECAST.md)
  - Ability to upload to specified folders (via `UploadsFolder` property/parameter)
  - Ability to upload multiple files simultaneously
  - Ability to upload and extract more archive formats:
    - rar
    - 7z
    - tar
  - Ability to export Podcasts to OPML
  - Ability to import playlists with relative file paths (resolved relative to Playlists folder)
  - Support direct binary internet radio stream urls
  - Catalog multiple genres
  - Ability to specify custom log file location (via `logging.file.name` property/parameter)
  - Auto Bookmarks and Bookmark management
  - Ability to backup internal database, manually and on a schedule, including specifying how many backups to keep
  - Export and import data across installations and databases
- Testing
  - Various fixes to make it compatible with multiple external DBs
  - Automated tests are performed against external DBs
    - Postgres
    - MySQL
    - MariaDB
  - Uses failsafe for integration testing instead of cucumber
- Build and deployment
  - An updated Docker image with JRE 14 base layer.
    - Add support for XMP to support playing MOD files out of the box
  - Multiplatform builds, including for ARM v7 and ARM64
  - A more advanced build pipeline including automatic releases and deploys at merge
    - Allows people to grab the newest build without compiling from source as soon as features/enhancements are merged, instead of waiting for the next stable build (which may be months away)
  - Available on GHCR as well as Docker Hub
- Stepbacks
  - The Java Jukebox has been removed, due to the third-party library not being kept up to date with modern JVMs. See [PR #636](https://github.com/airsonic-advanced/airsonic-advanced/pull/636).

The complete list of PRs that were used to enhance Airsonic can be seen on the PRs page. At some point an automatic changelog generator will be added to keep track.

Airsonic-Advanced will occasionally backport features introduced in the base Airsonic fork, but is generally much more modern and bleeding edge than Airsonic.

Usage
-----
Airsonic-Advanced v10.6.x series (and its snapshots) are intercompatible with vanilla Airsonic 10.6.x series. This may not necessarily be the case with 11.x versions.

Also note that Airsonic-Advanced 11.x (and its snapshots) are *breaking* (non-backwards-compatible) version changes. You will not be able to revert back to 10.6.x after upgrading (the system _does_ create a backup of the DB in case such revert is necessary, but it must be manually restored).

Airsonic-Advanced snapshots are generally pretty stable and recommended for use over the stable releases (which may be extremely outdated).

### Stand-alone binaries
Airsonic-Advanced can be downloaded from
[GitHub](https://github.com/airsonic-advanced/airsonic-advanced/releases).

The release signature may be verified using the [public key](https://github.com/airsonic-advanced/airsonic-advanced/blob/master/releases_public_key.asc).

You need a _minimum_ Java Runtime Environment (JRE) of 1.8 for 10.6.x series, and 11 for 11.x onwards (including snapshots).
- For 10.6.x releases -> Java 8
- For 11.x releases and onwards -> Java 11

Airsonic-Advanced is run similarly to (and in lieu of) vanilla Airsonic.

Read the [compatibility notes](#compatibility-notes).

### Docker
Docker releases are at [DockerHub](https://hub.docker.com/r/airsonicadvanced/airsonic-advanced) and [GHCR](https://ghcr.io/airsonic-advanced/airsonic-advanced). Docker releases are recently multiplatform, which means ARMv7 and ARM64 are also released to Dockerhub. However, automated testing for those archs is not currently done in the CI/CD pipeline (only Linux platform is tested).

Please note that for Docker images, the volume mounting points have changed and are different from Airsonic. Airsonic mount points are at `/airsonic/*` inside the container. Airsonic-Advanced tries to use the same volume locations as the default war image at `/var/*` in order to remain consistent if people want to switch between the containers and non-containers.
  - `Music:/airsonic/music` -> `Music:/var/music`
  - `Podcasts:/airsonic/podcast` -> `Podcasts:/var/podcast`
  - `Playlists:/airsonic/playlists` -> `Playlists:/var/playlists`
  - `/airsonic/data` -> `/var/airsonic`

Also note that the Docker image will by default run as user root (0), group root (0), and so any files created in the external volume will be owned as such. You may change the user running the internal process in one of two ways:
  - Specifying `--user` when invoking the `docker run` command, and providing it with one or both in the format `uid:gid`
  - Specifying the `PUID` or `PGID` environment variables to the container image when invoking the `docker run` command (`-e PUID=uid -e PGID=gid`)

Vanilla Airsonic can be downloaded from
[GitHub](https://github.com/airsonic/airsonic/releases).

Please use the [Airsonic documentation](https://airsonic.github.io/docs/) for instructions on running Airsonic. For the most part (currently) Airsonic-Advanced shares similar running instructions unless stated otherwise. Notable exceptions are available as comments or resolutions in the Issues page (please search).

### Building/Compiling
You may compile the code yourself by using maven. One of the repositories does not have https, so you may need to allow that for maven. A custom `settings.xml` has been put in `.mvn` folder for this purpose. A sample invocation would be (in the root):
```
mvn clean compile package verify
```
The main binary would be in `airsonic-main/target`

Compatibility Notes:
------
The following properties are new in Airsonic-Advanced:
  - `MediaScannerParallelism`: (default: number of available processors + 1) The parallelism to use when scanning media
  - `ClearFullScanSettingAfterScan`: (default: false) Whether to clear FullScan setting after the next SUCCESSFUL scan (useful for doing full scan once and then reverting to default scan)

The following property names are different between Airsonic and Airsonic-Advanced:
  - `UPNP_PORT` -> `UPnpPort`
  - `server.context-path` -> `server.servlet.context-path` (Airsonic will use the latter from 11.0 onwards)
  - `IgnoreFileTimestamps` -> `FullScan`

Note that Airsonic-Advanced communicates with its Web UI via websockets. If you're behind a proxy, you need to enable websockets and allow UPGRADE http requests through the proxy. A sample configuration is posted here: [nginx sample](https://github.com/airsonic-advanced/airsonic-advanced/issues/145).

Additionally, if placed behind a proxy, the Airsonic server needs to forward headers, for which the following property is necessary (either in `/path/to/airsonic-data/airsonic.properties` or as a jvm argument):
  - After and including *Edge Release 11.0.0-SNAPSHOT.20210117214044*: `server.forward-headers-strategy=native`
  - Prior to *Edge Release 11.0.0-SNAPSHOT.20210117214044*: `server.use-forward-headers=true`

### 11.x series
Certain property names have been changed from 10.6 to recent snapshots of 11.0 and will be _automigrated_. When modifying properties, use the modern name.
  - `DatabaseConfigEmbedDriver` -> `spring.datasource.driver-class-name`
  - `DatabaseConfigEmbedUrl` -> `spring.datasource.url`
  - `DatabaseConfigEmbedUsername` -> `spring.datasource.username`
  - `DatabaseConfigEmbedPassword` -> `spring.datasource.password`
  - `DatabaseConfigJNDIName` -> `spring.datasource.jndi-name`
  - `DatabaseMysqlMaxlength` -> `spring.liquibase.parameters.mysqlVarcharLimit`
  - `DatabaseUsertableQuote` -> `spring.liquibase.parameters.userTableQuote`

The following property names have been changed from 10.6 to recent snapshots of 11.0 and will NOT be _automigrated_. Make sure you switch the property names if you use them.
  - `server.use-forward-headers=true` -> `server.forward-headers-strategy=native` (due to [Spring Boot 2.2 deprecation](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-2.2-Release-Notes#deprecations-in-spring-boot-22), verified in [comment](https://github.com/airsonic-advanced/airsonic-advanced/issues/359#issuecomment-772724722), changed enacted in [#428](https://github.com/airsonic-advanced/airsonic-advanced/pull/428) and released in [Edge Release 11.0.0-SNAPSHOT.20210117214044](https://github.com/airsonic-advanced/airsonic-advanced/releases/tag/11.0.0-SNAPSHOT.20210117214044))

Other properties are obsolete and have been removed:
  - `DatabaseConfigType`
  - `DatabaseUsertableQuote` (now self-managed)

First migration to 11.x will create a backup DB next to the DB folder. It will be marked as `db.backup.<timestamp>`. Use this folder as the DB if a revert to an older major version is needed (11.0 -> 10.6.0).

History
-----

The original [Subsonic](http://www.subsonic.org/) is developed by [Sindre Mehus](mailto:sindre@activeobjects.no). Subsonic was open source through version 6.0-beta1, and closed-source from then onwards.

Libresonic was created and maintained by [Eugene E. Kashpureff Jr](mailto:eugene@kashpureff.org). It originated as an unofficial ("Kang") of Subsonic which did not contain the Licensing code checks present in the official builds. With the announcement of Subsonic's closed-source future, a decision was made to make a full fork and rebrand to Libresonic.

Around July 2017, it was discovered that Eugene had different intentions/goals
for the project than some contributors had.  Although the developers were
hesitant to create a fork as it would fracture/confuse the community even
further, it was deemed necessary in order to preserve a community-focused fork.
To reiterate this more clearly:

Airsonic's goal is to provide a full-featured, stable, self-hosted media server
based on the Subsonic codebase that is free, open source, and community driven.

Around November 2019, Airsonic-Advanced was forked off the base Airsonic fork due to differences in pace and review of development. Several key features of the framework were outdated, and attempts to upgrade them occasionally took upto a year. Airsonic-Advanced tries a modern implementation and bleeding edge approach to development, and is thus usually ahead of the base fork in dependencies and features.

Pull Requests are always welcome. All Pull Requests are reviewed before being merged to ensure we continue to meet our goals.

License
-------

Airsonic-Advanced and Airsonic are free software and licensed under the [GNU General Public License version 3](http://www.gnu.org/copyleft/gpl.html). The code in this repository (and associated binaries) are free of any "license key" or other restrictions. If you wish to thank the maintainer of this repository, please consider a donation to the [Electronic Frontier Foundation](https://supporters.eff.org/donate).

The [Subsonic source code](https://github.com/airsonic/subsonic-svn) was released under the GPLv3 through version 6.0-beta1. Beginning with 6.0-beta2, source is no longer provided. Binaries of Subsonic are only available under a commercial license. There is a [Subsonic Premium](http://www.subsonic.org/pages/premium.jsp) service which adds functionality not available in Airsonic. Subsonic also offers RPM, Deb, Exe, and other pre-built packages that Airsonic [currently does not](https://github.com/airsonic/airsonic/issues/65).

The cover zooming feature is provided by [jquery.fancyzoom](https://github.com/keegnotrub/jquery.fancyzoom),
released under [MIT License](http://www.opensource.org/licenses/mit-license.php).

The icons are from the amazing [feather](https://feathericons.com/) project,
and are licensed under [MIT license](https://github.com/feathericons/feather/blob/master/LICENSE).

Community
---------
Bugs/feature requests/discussions pertaining to Airsonic-Advanced may be raised as issues within GitHub on the Airsonic-Advanced project page.

Vanilla Airsonic itself has several places outside of GitHub for community discussion, questions, etc:

- [#airsonic:matrix.org on Matrix](https://matrix.to/#/#airsonic:matrix.org)
- [#airsonic on IRC](http://webchat.freenode.net?channels=%23airsonic)
- [airsonic subreddit](https://www.reddit.com/r/airsonic)

*Note that the Matrix room and IRC channel are bridged together.*
