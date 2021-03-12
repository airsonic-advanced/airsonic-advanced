package org.airsonic.player.service.metadata;

import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.Set;

public class Track {
    private final int id;

    private final String type;

    private final String language;

    private final String codec;

    private final static Set<String> STREAMABLE_CODECS = ImmutableSet.of("h264", "aac");

    public Track(int id, String type, String language, String codec) {
        this.id = id;
        this.type = StringUtils.trimToNull(type);
        this.language = StringUtils.trimToNull(language);
        this.codec = StringUtils.trimToNull(codec);
    }

    public int getId() {
        return this.id;
    }

    public String getType() {
        return this.type;
    }

    public String getLanguage() {
        return this.language;
    }

    public String getLanguageName() {
        if (this.language == null)
            return String.valueOf(this.id);
        Locale locale = new Locale(this.language);
        String languageName = StringUtils.trimToNull(locale.getDisplayLanguage(Locale.ENGLISH));
        return (languageName == null) ? this.language : languageName;
    }

    public String getCodec() {
        return this.codec;
    }

    @Override
    public String toString() {
        return this.id + " " + this.type + " " + this.language + " " + this.codec;
    }

    public boolean isAudio() {
        return "audio".equals(this.type);
    }

    public boolean isVideo() {
        return ("video".equals(this.type) && !"mjpeg".equals(this.codec) && !"bmp".equals(this.codec));
    }

    public boolean isSubtitle() {
        return "subtitle".equals(this.type);
    }

    public boolean isStreamable() {
        return STREAMABLE_CODECS.contains(this.codec);
    }
}
