/*
 This file is part of Airsonic.

 Airsonic is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Airsonic is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.

 Copyright 2016 (C) Airsonic Authors
 Based upon Subsonic, Copyright 2009 (C) Sindre Mehus
 */
package org.airsonic.player.controller;

import com.google.common.net.MediaType;

import org.airsonic.player.controller.SubsonicRESTController.APIException;
import org.airsonic.player.util.StringUtil;
import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.persistence.jaxb.JAXBContext;
import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.subsonic.restapi.Error;
import org.subsonic.restapi.ObjectFactory;
import org.subsonic.restapi.Response;
import org.subsonic.restapi.ResponseStatus;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.time.Instant;
import java.util.GregorianCalendar;
import java.util.Map.Entry;

import static org.airsonic.player.util.XMLUtil.createSAXBuilder;
import static org.springframework.web.bind.ServletRequestUtils.getStringParameter;

/**
 * @author Sindre Mehus
 * @version $Id$
 */
public class JAXBWriter {

    private static final Logger LOG = LoggerFactory.getLogger(JAXBWriter.class);

    private final javax.xml.bind.JAXBContext jaxbContext;
    private final DatatypeFactory datatypeFactory;
    private static final String restProtocolVersion = parseRESTProtocolVersion();

    public JAXBWriter() {
        try {
            jaxbContext = JAXBContext.newInstance(Response.class);
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }

    private Marshaller createXmlMarshaller() {
        Marshaller marshaller = null;
        try {
            marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_ENCODING, StringUtil.ENCODING_UTF8);
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            return marshaller;
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    private Marshaller createJsonMarshaller() {
        try {
            Marshaller marshaller;
            marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_ENCODING, StringUtil.ENCODING_UTF8);
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.setProperty(MarshallerProperties.MEDIA_TYPE, "application/json");
            marshaller.setProperty(MarshallerProperties.JSON_INCLUDE_ROOT, true);
            return marshaller;
        } catch (JAXBException e) {
            throw new RuntimeException(e);
        }
    }

    private static String parseRESTProtocolVersion() {
        try (InputStream in = StringUtil.class.getResourceAsStream("/subsonic-rest-api.xsd")) {
            Document document = createSAXBuilder().build(in);
            Attribute version = document.getRootElement().getAttribute("version");
            return version.getValue();
        } catch (Exception x) {
            throw new RuntimeException(x);
        }
    }

    public static String getRestProtocolVersion() {
        return restProtocolVersion;
    }

    public Response createResponse(boolean ok) {
        Response response = new ObjectFactory().createResponse();
        response.setStatus(ok ? ResponseStatus.OK : ResponseStatus.FAILED);
        response.setVersion(restProtocolVersion);
        return response;
    }

    public void writeResponse(HttpServletRequest request, HttpServletResponse httpResponse, Response jaxbResponse) {
        Entry<String, String> serializedResp = serializeForType(request, jaxbResponse);

        httpResponse.setCharacterEncoding(StringUtil.ENCODING_UTF8);
        httpResponse.setContentType(serializedResp.getKey());

        try {
            httpResponse.getWriter().append(serializedResp.getValue());
        } catch (IOException x) {
            LOG.error("Failed to marshal JAXB", x);
            throw new RuntimeException(x);
        }
    }

    public void writeErrorResponse(HttpServletRequest request, HttpServletResponse response,
            SubsonicRESTController.ErrorCode code, String message) {
        Response res = createErrorResponse(code, message);
        writeResponse(request, response, res);
    }

    public Response createErrorResponse(APIException e) {
        return createErrorResponse(e.getError(), e.getMessage());
    }

    public Response createErrorResponse(SubsonicRESTController.ErrorCode code, String message) {
        Response res = createResponse(false);
        Error error = new Error();
        res.setError(error);
        error.setCode(code.getCode());
        error.setMessage(message);
        return res;
    }

    public Entry<String, String> serializeForType(HttpServletRequest request, Response resp) {
        String format = getStringParameter(request, "f", "xml");
        String jsonpCallback = request.getParameter("callback");
        boolean json = "json".equals(format);
        boolean jsonp = "jsonp".equals(format) && jsonpCallback != null;
        Marshaller marshaller;
        MediaType type;

        if (json) {
            marshaller = createJsonMarshaller();
            type = MediaType.JSON_UTF_8;
        } else if (jsonp) {
            marshaller = createJsonMarshaller();
            type = MediaType.JAVASCRIPT_UTF_8;
        } else {
            marshaller = createXmlMarshaller();
            type = MediaType.XML_UTF_8;
        }

        StringWriter writer = new StringWriter();
        try {
            if (jsonp) {
                writer.append(jsonpCallback).append('(');
            }
            marshaller.marshal(new ObjectFactory().createSubsonicResponse(resp), writer);
            if (jsonp) {
                writer.append(");");
            }
        } catch (JAXBException x) {
            LOG.error("Failed to marshal JAXB", x);
            throw new RuntimeException(x);
        }

        return Pair.of(type.toString(), writer.toString());
    }

    public XMLGregorianCalendar convertDate(Instant date) {
        if (date == null) {
            return null;
        }

        GregorianCalendar c = new GregorianCalendar();
        c.setTimeInMillis(date.toEpochMilli());
        return datatypeFactory.newXMLGregorianCalendar(c).normalize();
    }
}
