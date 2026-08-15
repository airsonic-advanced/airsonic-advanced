FROM eclipse-temurin:17-jdk AS builder
WORKDIR target

COPY target/dependency/airsonic-main.war airsonic.war

FROM ghcr.io/linuxserver/baseimage-alpine:3.21

LABEL description="Airsonic-Advanced is a free, web-based media streamer, providing ubiquitous access to your music." \
      url="https://github.com/slmingol/airsonic-advanced"

ENV AIRSONIC_ADVANCED_HOME="/app/airsonic" \
    AIRSONIC_ADVANCED_SETTINGS="/config" \
    LANG="C.UTF-8"

RUN apk add --no-cache \
        curl \
        ffmpeg \
        flac \
        fontconfig \
        lame \
        openjdk17-jre \
        ttf-dejavu \
        vorbis-tools \
    && mkdir -p /app/airsonic

COPY --from=builder target/airsonic.war /app/airsonic/airsonic.war
COPY root/ /

EXPOSE 4040

VOLUME /config /music /playlists /podcasts

HEALTHCHECK --interval=15s --timeout=3s CMD curl -sf http://localhost:4040/rest/ping || false
