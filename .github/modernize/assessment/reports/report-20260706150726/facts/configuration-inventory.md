# Configuration Inventory - Airsonic Advanced

## 1. Configuration Sources

| File / Location | Purpose | Format |
|---|---|---|
| `airsonic-main/src/main/resources/application.properties` | Main Spring Boot configuration (MVC, logging, actuator, Liquibase) | Java Properties |
| `airsonic-main/src/test/resources/application.properties` | Test-specific configuration (in-memory DB, reduced logging) | Java Properties |
| `airsonic-main/src/test/resources/logback.xml` | Test logging configuration | XML (Logback) |
| `airsonic-main/src/main/resources/META-INF/spring.factories` | Spring Boot auto-configuration listeners | Java Properties |
| `airsonic-main/src/main/resources/liquibase/db-changelog.xml` | Database migration master changelog | XML (Liquibase) |
| `${airsonic.home}/airsonic.properties` | Runtime persistent user settings (generated at runtime) | Java Properties |
| `pom.xml` (root) | Parent POM, dependency management, Maven profiles | XML (Maven POM) |
| `airsonic-main/pom.xml` | Main module build configuration, dependencies | XML (Maven POM) |
| `install/docker/Dockerfile` | Docker image build definition | Dockerfile |
| `install/docker/run.sh` | Docker container entrypoint/run script | Shell |
| `install/docker/entry.sh` | Docker UID/GID user-switching entrypoint | Shell |
| `install/docker/pom.xml` | Docker build orchestration via fabric8 plugin | XML (Maven POM) |
| `integration-test/pom.xml` | Integration test module (Docker-based testing) | XML (Maven POM) |
| `GlobalSecurityConfig.java` | Spring Security programmatic configuration | Java (Spring) |
| `CacheConfiguration.java` | Ehcache/JSR-107 programmatic configuration | Java (Spring) |
| `WebsocketConfiguration.java` | WebSocket/STOMP broker configuration | Java (Spring) |
| `ServletConfiguration.java` | Servlet filters, view resolvers, CXF servlet | Java (Spring) |
| `SonosConfiguration.java` | Sonos SOAP endpoint (CXF/JAX-WS) | Java (Spring) |
| `CustomPropertySourceConfigurer.java` | Custom property source priority/migration logic | Java (Spring) |

---

## 2. Application Properties

### 2.1 Spring MVC & Web

| Property | Value | Notes |
|---|---|---|
| `spring.mvc.view.prefix` | `/WEB-INF/jsp/` | JSP view resolution |
| `spring.mvc.view.suffix` | `.jsp` | JSP view resolution |
| `spring.messages.basename` | `org.airsonic.player.i18n.ResourceBundle` | i18n message source |
| `spring.mvc.pathmatch.use-suffix-pattern` | `true` | Legacy URL pattern matching |
| `spring.mvc.hiddenmethod.filter.enabled` | `true` | HTTP method override via `_method` param |
| `server.error.includeStacktrace` | `ALWAYS` | Stack traces in error responses |
| `server.servlet.context-path` | `/` (default) | Configurable via `-Dserver.servlet.context-path` |
| `server.servlet.session.timeout` | `30m` (default) | Configurable via `airsonic.properties` |

### 2.2 Logging

| Property | Value | Notes |
|---|---|---|
| `logging.level.root` | `WARN` | Root logger level |
| `logging.level.org.airsonic` | `INFO` | Application logger level |
| `logging.level.liquibase` | `INFO` | Migration logger level |
| `logging.pattern.console` | Custom colorized pattern | Timestamp, level, logger, message |
| `logging.pattern.file` | Plain pattern | Same fields without ANSI colors |
| `logging.file.name` | `${airsonic.home}/airsonic.log` (default) | Auto-set if not provided |

### 2.3 Spring Boot Actuator

| Property | Value | Notes |
|---|---|---|
| `management.endpoints.web.exposure.include` | `metrics,health,caches,customscheduledtasks,pathwatcher` | Exposed management endpoints |
| `management.endpoint.health.show-details` | `when-authorized` | Health detail visibility |
| `management.endpoint.health.show-components` | `when-authorized` | Health component visibility |
| `management.health.ldap.enabled` | `false` | LDAP health check disabled |

### 2.4 Database (Spring DataSource / Liquibase)

| Property | Default | Notes |
|---|---|---|
| `spring.datasource.url` | `jdbc:hsqldb:file:${airsonic.home}/db/airsonic;...` | HSQLDB file-based default; supports PostgreSQL, MySQL, MariaDB |
| `spring.datasource.username` | `sa` | Default HSQLDB username |
| `spring.datasource.password` | (empty) | Default HSQLDB password |
| `spring.datasource.driver-class-name` | (auto-detected) | Explicit driver override |
| `spring.datasource.jndi-name` | (none) | JNDI lookup, overrides URL-based config |
| `spring.liquibase.change-log` | `classpath:liquibase/db-changelog.xml` | Migration changelog |
| `spring.liquibase.rollback-file` | `${airsonic.home}/rollback.sql` | Auto-generated rollback |
| `spring.liquibase.parameters.mysqlVarcharLimit` | `384` | MySQL/MariaDB VARCHAR limit |
| `spring.liquibase.parameters.defaultMusicFolder` | OS-dependent | Default music directory path |
| `spring.liquibase.parameters.defaultPodcastFolder` | OS-dependent | Default podcast directory path |

### 2.5 Task Scheduling

| Property | Value | Notes |
|---|---|---|
| `spring.task.scheduling.pool.size` | `10` | Scheduled task thread pool size |

---

## 3. Spring Profiles

### Compile-time / Build Profiles (Maven)

| Profile ID | Activation | Purpose |
|---|---|---|
| `tomcat-embed` | `activeByDefault=true` | Default build with embedded Tomcat (scope: `provided`) |
| `ide-tomcat-embed` | Manual | Changes Tomcat scope to `compile` for IDE debugging |
| `docker` | Manual (`-Pdocker`) | Includes `install/docker` module for image build |
| `integration-test` | Manual (`-Pintegration-test`) | Includes Docker + `integration-test` module |

### Runtime Profiles / Modes

The application does not use Spring `@Profile` annotations or `application-{profile}.properties` files. Instead, it uses:

1. **Development Mode**: Activated via JVM property `-Dairsonic.development`. Disables Tomcat resource caching, enables JSP recompilation.
2. **Property Source Priority** (searched in order):
   - Spring CLI args (`--var=foo`)
   - JVM system properties (`-Dvar=foo`)
   - Environment variables
   - `application.properties` (classpath)
   - `airsonic.properties` (external runtime file at `${airsonic.home}`)
   - Default constants (computed in `CustomPropertySourceConfigurer`)

---

## 4. Security Configuration

### 4.1 Authentication

| Mechanism | Configuration | Notes |
|---|---|---|
| Form Login | `/login` page, `j_username`/`j_password` params | Default success: `/index` |
| HTTP Basic | Enabled on web security chain | Used by REST API clients |
| JWT Token | `JWTRequestParameterProcessingFilter` + `JWTAuthenticationProvider` | Key auto-generated and stored in `airsonic.properties` |
| LDAP | Optional, toggled via `LdapEnabled` setting | URL, manager DN/password, search filter configurable |
| Remember Me | Cookie-based, optional fixed key (`RememberMeKey`) | Ephemeral key if not set |
| REST Legacy Auth | `RESTRequestParameterProcessingFilter` | Subsonic-compatible username/password/token params |
| Sonos JWT | Additional JWT verification for `/ws/Sonos` endpoint | Separate verification strategy |

### 4.2 Password Encoders

| Encoder ID | Type | Purpose |
|---|---|---|
| `bcrypt` | One-way | Default non-decodable encoder |
| `argon2` | One-way | Strong hashing option |
| `scrypt` | One-way | Alternative option |
| `pbkdf2` | One-way | Alternative option |
| `encrypted-AES-GCM` | Decodable (symmetric) | Default decodable encoder; key derived from `EncryptionKeyPassword` + `EncryptionKeySalt` |
| `noop` / `hex` | Decodable (open-text) | Legacy/migration encoders |
| `legacynoop` / `legacyhex` | Decodable (legacy) | Marked for upgrade at next breaking version |

### 4.3 Authorization (URL-based)

| URL Pattern | Required Role |
|---|---|
| `/recover*`, `/login`, `/error`, `/style/**`, `/icons/**`, `/script/**` | `permitAll` |
| `/generalSettings*`, `/advancedSettings*`, `/userSettings*`, `/musicFolderSettings*`, `/databaseSettings*`, `/transcodeSettings*` | `ADMIN` |
| `/personalSettings*`, `/playerSettings*`, `/shareSettings*`, `/credentialsSettings*` | `SETTINGS` |
| `/deletePlaylist*`, `/savePlaylist*` | `PLAYLIST` |
| `/download*` | `DOWNLOAD` |
| `/upload*` | `UPLOAD` |
| `/createShare*` | `SHARE` |
| `/changeCoverArt*`, `/editTags*` | `COVERART` |
| `/podcastReceiverAdmin*` | `PODCAST` |
| `/**` (everything else) | `USER` |
| `/ext/stream/**`, `/ext/coverArt*`, `/ext/share/**`, `/ext/hls/**` | `TEMP` or `USER` (stateless JWT) |

### 4.4 CORS

CORS is globally permissive (`*` origins, `*` methods) for:
- `/rest/**`, `/stream/**`, `/hls**`, `/captions**`
- `/ext/stream/**`, `/ext/hls**`, `/ext/captions**`

### 4.5 CSRF

- Enabled globally with custom `CsrfSecurityRequestMatcher`
- Ignored for `/ws/Sonos/**` (SOAP endpoint)

### 4.6 Session Management

- Stateless (`STATELESS`) for `/ext/**` endpoints (JWT-secured)
- Standard session for web UI with form login
- Logout clears `JSESSIONID` and `XSRF-TOKEN` cookies

---

## 5. Infrastructure Configuration

### 5.1 Database

| Aspect | Details |
|---|---|
| Default Engine | HSQLDB 2.5.0 (embedded file-based) |
| Supported Engines | HSQLDB, PostgreSQL, MySQL, MariaDB |
| Connection Strategy | Spring JDBC `DataSource` or JNDI lookup |
| Migration Tool | Liquibase 4.31.0 |
| Migration History | Versions: legacy, 6.2, 6.3, 6.4, 10.6, 11.0 |
| HSQLDB Auto-Migration | `LegacyHsqlMigrationUtil` upgrades HSQLDB from 1.8 to 2.x at startup (before Spring context) |
| Backup | Configurable interval and retention (`DbBackupUpdateInterval`, `DbBackupRetentionCount`) |
| Multi-DB Support | Liquibase properties differentiate types (`binary_type`, `json_type`, `varchar_type`, `timestamp_type`) |

### 5.2 Caching (Ehcache 3 / JSR-107)

| Cache Name | Key Type | Value Type | TTL | Max Entries |
|---|---|---|---|---|
| `userCache` | `String` | `User` | 2 days | 1000 |
| `userSettingsCache` | `String` | `UserSettings` | 2 days | 1000 |
| `mediaFilePathCache` | `String` | `MediaFile` | 2 hours | 1000 |
| `mediaFileIdCache` | `Integer` | `MediaFile` | 2 hours | 1000 |
| `coverArtCache` | `String` | `CoverArt` | 2 days | 1000 |
| `playlistCache` | `Integer` | `Playlist` | 10 days | 1000 |
| `playlistUsersCache` | `Integer` | `Object` | 10 days | 1000 |

Cache persistence: file-based at `${airsonic.home}/cache/`. Caching mode: AspectJ (supports self-invocation caching).

### 5.3 WebSocket / Messaging

| Aspect | Details |
|---|---|
| Protocol | STOMP over WebSocket with SockJS fallback |
| Endpoint | `/websocket` |
| Broker Prefixes | `/topic`, `/queue` |
| App Prefix | `/app` |
| Heartbeat | 20s send / 20s receive |
| Publish Order | Serial (preserves order, lower throughput) |

### 5.4 Search Engine

| Aspect | Details |
|---|---|
| Engine | Apache Lucene 8.9.0 (embedded) |
| Index Location | `${airsonic.home}/` (implicit from Lucene directory) |
| Scheduled Indexing | Configurable interval (`IndexCreationInterval`) + hour (`IndexCreationHour`) |

### 5.5 External Service Integrations

| Service | Purpose | Configuration Keys |
|---|---|---|
| Last.fm | Artist bios, album notes, cover art, scrobbling | Per-user settings |
| ListenBrainz | Music scrobbling | Per-user settings |
| Sonos | SOAP/WSDL integration via CXF | `SonosEnabled`, `SonosServiceName`, `SonosServiceId`, `SonosCallbackHostAddress`, `SonosLinkMethod` |
| DLNA/UPnP | Media server (Cling 2.0.1) | `DlnaEnabled`, `DlnaServerName`, `DlnaServerId`, `DlnaBaseLANURL`, `UPnpPort` |
| SMTP | Email notifications (password recovery) | `SmtpServer`, `SmtpPort`, `SmtpUser`, `SmtpPassword`, `SmtpEncryption`, `SmtpFrom` |
| reCAPTCHA | Bot prevention on registration | `CaptchaEnabled`, `ReCaptchaSiteKey`, `ReCaptchaSecretKey` |
| NAT-PMP | Port mapping for remote access | `natpmp` library |

---

## 6. Build & Deployment Configuration

### 6.1 Build System

| Aspect | Details |
|---|---|
| Build Tool | Maven (multi-module) |
| Java Version | 21 |
| Spring Boot Version | 3.3.7 |
| Packaging | WAR (executable via Spring Boot plugin, `mainClass=org.airsonic.player.Application`) |
| Module Structure | `subsonic-rest-api`, `airsonic-sonos-api`, `airsonic-main`, `install/docker` (profile), `integration-test` (profile) |

### 6.2 Maven Plugins

| Plugin | Purpose |
|---|---|
| `spring-boot-maven-plugin` | Executable WAR packaging |
| `maven-checkstyle-plugin` | Code style enforcement |
| `maven-pmd-plugin` | JSP code analysis |
| `maven-enforcer-plugin` | Dependency convergence |
| `maven-dependency-plugin` | Dependency analysis |
| `owasp dependency-check-maven` | CVE scanning (currently commented out) |
| `buildnumber-maven-plugin` | Build metadata (Git revision, timestamp) |
| `checksum-maven-plugin` | SHA-256 artifact checksums |
| `aspectj-maven-plugin` | AspectJ compile-time weaving |
| `jetty-jspc-maven-plugin` | JSP pre-compilation |
| `docker-maven-plugin` (fabric8) | Docker image build |

### 6.3 Docker Configuration

| Aspect | Details |
|---|---|
| Base Image | `eclipse-temurin:17.0.5_8-jre-jammy` (NOTE: mismatch with Java 21 in POM) |
| Build Pattern | Multi-stage (builder extracts WAR, runtime is JRE-only) |
| Default Port | `4040` (configurable via `AIRSONIC_PORT`) |
| UPnP Port | `4041` (configurable via `UPNP_PORT`) |
| Default Memory | `-Xmx256m` |
| Volumes | `/var/airsonic`, `/var/music`, `/var/playlists`, `/var/podcasts` |
| Healthcheck | `curl -f http://localhost:$AIRSONIC_PORT$CONTEXT_PATH/rest/ping` (15s interval) |
| User Management | `PUID`/`PGID` env vars with `gosu` for runtime user switching |
| Pre-installed Tools | `ffmpeg`, `x264`, `x265`, `lame`, `xmp` |

### 6.4 Docker Environment Variables

| Variable | Default | Purpose |
|---|---|---|
| `AIRSONIC_PORT` | `4040` | Server listen port |
| `AIRSONIC_DIR` | `/var` | Base directory for data |
| `CONTEXT_PATH` | `/` | Servlet context path |
| `UPNP_PORT` | `4041` | UPnP/DLNA port |
| `PUID` | `0` | Runtime user ID |
| `PGID` | `0` | Runtime group ID |
| `JAVA_OPTS` | (empty) | Additional JVM arguments |

### 6.5 Docker JVM System Properties (set in `run.sh`)

| Property | Value |
|---|---|
| `-Dserver.address` | `0.0.0.0` |
| `-Dserver.port` | `$AIRSONIC_PORT` |
| `-Dserver.servlet.context-path` | `$CONTEXT_PATH` |
| `-Dairsonic.home` | `$AIRSONIC_DIR/airsonic` |
| `-Dairsonic.defaultMusicFolder` | `$AIRSONIC_DIR/music` |
| `-Dairsonic.defaultPodcastFolder` | `$AIRSONIC_DIR/podcasts` |
| `-Dairsonic.defaultPlaylistFolder` | `$AIRSONIC_DIR/playlists` |
| `-DUPnpPort` | `$UPNP_PORT` |
| `-Djava.awt.headless` | `true` |

---

## 7. Startup Dependencies & Sequence

### 7.1 Boot Sequence

```
1. spring.factories listeners registered:
   - CustomPropertySourceConfigurer (property source priority + migration)
   - StartupEventListener (HSQLDB upgrade before context init)

2. ApplicationEnvironmentPreparedEvent:
   - Property migration (legacy -> current keys)
   - External airsonic.properties loaded into Environment
   - Default constants computed (DB URL, log file, etc.)

3. ApplicationContextInitializedEvent:
   - LegacyHsqlMigrationUtil: upgrades embedded HSQLDB if needed

4. Spring Context Refresh:
   - DataSource created (HSQLDB/PostgreSQL/MySQL/MariaDB)
   - Liquibase migrations executed (db-changelog.xml)
   - Security config: JWT key generated if missing
   - Security config: Encryption keys generated if missing
   - Ehcache manager initialized (disk persistence at ${airsonic.home}/cache)
   - CXF/Sonos SOAP endpoint published at /ws/Sonos
   - WebSocket STOMP broker started
   - Task scheduler initialized (pool size 10)
   - Lucene search index loaded
   - Media scan scheduling configured

5. ApplicationReady:
   - Embedded Tomcat listening on configured port
   - DLNA/UPnP service started if enabled
   - Sonos registration if enabled
```

### 7.2 Critical Startup Dependencies

| Dependency | Required At | Failure Impact |
|---|---|---|
| `${airsonic.home}` directory (writable) | Earliest (property loading) | Fatal - application cannot start |
| Database connectivity | Context refresh | Fatal - Liquibase + DAO init fails |
| Liquibase migrations | Context refresh | Fatal - schema inconsistency |
| `airsonic.properties` file | Environment preparation | Created if missing, but directory must exist |
| Transcode directory | Post-startup | Non-fatal, transcoding unavailable |
| FFmpeg binary | Runtime | Non-fatal, media conversion unavailable |

### 7.3 JVM System Properties (All Recognized)

| Property | Purpose | Default |
|---|---|---|
| `airsonic.home` | Application home directory | `/var/airsonic` (Linux) or `c:/airsonic` (Windows) |
| `libresonic.home` | Legacy home directory (fallback) | - |
| `airsonic.development` | Development mode toggle | (not set = production) |
| `airsonic.defaultMusicFolder` | Default music folder path | OS-dependent |
| `airsonic.defaultPodcastFolder` | Default podcast folder path | OS-dependent |
| `airsonic.defaultPlaylistFolder` | Default playlist folder path | OS-dependent |
| `UPnpPort` | UPnP/DLNA port | `4041` |
| `server.port` | HTTP listen port | `8080` (Spring default) |
| `server.address` | Bind address | `localhost` (Spring default) |
| `server.servlet.context-path` | Context path | `/` |

---

## 8. Feature Flags / Toggles

The application uses **settings-based feature toggling** rather than compile-time feature flags. All toggles are stored in `airsonic.properties` at runtime:

| Feature | Setting Key | Default | Scope |
|---|---|---|---|
| DLNA/UPnP Server | `DlnaEnabled` | `false` | Global |
| Sonos Integration | `SonosEnabled` | `false` | Global |
| LDAP Authentication | `LdapEnabled` | `false` | Global |
| Getting Started Wizard | `GettingStartedEnabled` | `true` | Global |
| CAPTCHA on Registration | `CaptchaEnabled` | `false` | Global |
| Fast Cache (scan optimization) | `FastCacheEnabled` | `false` | Global |
| Full Scan (ignore timestamps) | `FullScan` | `false` | Global |
| Ignore Symlinks | `IgnoreSymLinks` | `false` | Global |
| Sort Albums by Year | `SortAlbumsByYear` | `true` | Global |
| Organize by Folder Structure | `OrganizeByFolderStructure` | `true` | Global |
| Prefer Non-Decodable Passwords | `PreferNonDecodablePasswords` | `true` | Global |
| Development Mode | `-Dairsonic.development` | (not set) | JVM |

---

## 9. Configuration Anti-Patterns & Observations

| Observation | Details |
|---|---|
| No Spring Profiles | Runtime environment differentiation relies entirely on external property file + JVM args rather than `@Profile` |
| Dual Config System | Properties are read from both Spring `Environment` AND a custom `ConfigurationPropertiesService` (Apache Commons Configuration2) |
| Property Migration Complexity | Complex chain-based property key migration in `CustomPropertySourceConfigurer` for backward compatibility |
| No `@ConfigurationProperties` classes | All settings access goes through `SettingsService` getter/setter methods with manual key definitions |
| Docker Java Version Mismatch | Dockerfile uses `eclipse-temurin:17` but POM declares `java.version=21` |
| Deprecated Security APIs | Uses `WebSecurityConfigurerAdapter` (removed in Spring Security 6.x) |
| CORS Wide Open | REST/streaming endpoints allow all origins with all methods |
| Hardcoded Defaults | Default reCAPTCHA keys are Google's test keys (public) |
| No Health Check for External Services | LDAP, SMTP, Last.fm health not exposed via actuator |
| Auto-excluded configurations | `JmxAutoConfiguration`, `MultipartAutoConfiguration` explicitly excluded |
