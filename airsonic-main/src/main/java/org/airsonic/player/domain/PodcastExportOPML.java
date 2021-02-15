package org.airsonic.player.domain;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "opml")
public class PodcastExportOPML {
    private String version = "2.0";
    private Head head = new Head();
    private Body body = new Body();

    @XmlAttribute
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @XmlElement
    public Head getHead() {
        return head;
    }

    public void setHead(Head head) {
        this.head = head;
    }

    @XmlElement
    public Body getBody() {
        return body;
    }

    public void setBody(Body body) {
        this.body = body;
    }

    public static class Head {
        private String title = "Airsonic Exported OPML";
        private String dateCreated = OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.RFC_1123_DATE_TIME);

        @XmlElement
        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        @XmlElement
        public String getDateCreated() {
            return dateCreated;
        }

        public void setDateCreated(String dateCreated) {
            this.dateCreated = dateCreated;
        }
    }

    public static class Body {
        public Body() {
            Outline o = new Outline();
            o.setType(null);
            o.setTitle("Airsonic Exported OPML");
            o.setText("Airsonic Exported OPML");
            outline.add(o);
        }

        private List<Outline> outline = new ArrayList<>();

        @XmlElement
        public List<Outline> getOutline() {
            return outline;
        }

        public void setOutline(List<Outline> outline) {
            this.outline = outline;
        }
    }

    public static class Outline {
        private String type = "rss";
        private String xmlUrl;
        private String title;
        private String text;
        private List<Outline> outline = new ArrayList<>();

        @XmlAttribute
        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        @XmlAttribute
        public String getXmlUrl() {
            return xmlUrl;
        }

        public void setXmlUrl(String xmlUrl) {
            this.xmlUrl = xmlUrl;
        }

        @XmlAttribute
        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        @XmlAttribute
        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        @XmlElement
        public List<Outline> getOutline() {
            return outline;
        }

        public void setOutline(List<Outline> outline) {
            this.outline = outline;
        }

    }

}
