#!/bin/bash

set -e

mkdir -p $AIRSONIC_DIR/airsonic/transcode
ln -fs /usr/bin/ffmpeg $AIRSONIC_DIR/airsonic/transcode/ffmpeg
ln -fs /usr/bin/lame $AIRSONIC_DIR/airsonic/transcode/lame

if [[ $# -lt 1 ]] || [[ ! "$1" == "java"* ]]; then

    java_opts_array=()
    while IFS= read -r -d '' item; do
        java_opts_array+=( "$item" )
    done < <([[ $JAVA_OPTS ]] && xargs printf '%s\0' <<<"$JAVA_OPTS")
    exec java -Xmx256m \
     -Dserver.host=0.0.0.0 \
     -Dserver.port=$AIRSONIC_PORT \
     -Dserver.servlet.context-path=$CONTEXT_PATH \
     -Dairsonic.home=$AIRSONIC_DIR/airsonic \
     -Dairsonic.defaultMusicFolder=$AIRSONIC_DIR/music \
     -Dairsonic.defaultPodcastFolder=$AIRSONIC_DIR/podcasts \
     -Dairsonic.defaultPlaylistFolder=$AIRSONIC_DIR/playlists \
     -DUPnpPort=$UPNP_PORT \
     -Djava.awt.headless=true \
     "${java_opts_array[@]}" \
     -jar airsonic.war "$@"
fi

exec "$@"
