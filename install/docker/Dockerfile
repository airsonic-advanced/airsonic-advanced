FROM adoptopenjdk:14-jre-hotspot

LABEL description="Airsonic-Advanced is a free, web-based media streamer, providing ubiquitous access to your music." \
      url="https://github.com/airsonic-advanced/airsonic-advanced"

ENV AIRSONIC_PORT=4040 AIRSONIC_DIR=/var CONTEXT_PATH=/ UPNP_PORT=4041

WORKDIR $AIRSONIC_DIR

ADD https://github.com/krallin/tini/releases/download/v0.19.0/tini /tini
RUN chmod +x /tini
RUN apt install -y software-properties-common
RUN apt-add-repository ppa:jonathonf/ffmpeg-4
RUN apt-get update
RUN apt-get install -y ffmpeg
RUN apt-get install -y x264
RUN apt-get install -y x265
RUN ffmpeg -version
RUN apt-get install -y lame
RUN apt-get install -y bash
RUN apt-get install -y ttf-dejavu
# RUN apt-get install -y tini

COPY run.sh /usr/local/bin/run.sh

RUN chmod +x /usr/local/bin/run.sh

COPY target/dependency/airsonic-main.war airsonic.war

EXPOSE $AIRSONIC_PORT

# Default DLNA/UPnP ports
EXPOSE $UPNP_PORT
EXPOSE 1900/udp

VOLUME $AIRSONIC_DIR/airsonic $AIRSONIC_DIR/music $AIRSONIC_DIR/playlists $AIRSONIC_DIR/podcasts

HEALTHCHECK --interval=15s --timeout=3s CMD wget -q http://localhost:"$AIRSONIC_PORT""$CONTEXT_PATH"rest/ping -O /dev/null || exit 1

ENTRYPOINT ["/tini", "--", "run.sh"]
