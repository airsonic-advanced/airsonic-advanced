/*
 * This file is part of Airsonic.
 *
 *  Airsonic is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Airsonic is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Airsonic.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  Copyright 2015 (C) Sindre Mehus
 */

package org.airsonic.player.service.sonos;

import org.airsonic.player.service.sonos.SonosSoapFault.TokenRefreshRequired;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.binding.soap.interceptor.AbstractSoapInterceptor;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

/**
 * Intercepts all SonosSoapFault exceptions and builds a SOAP Fault.
 *
 * @author Sindre Mehus
 * @version $Id$
 */
@Component
public class SonosFaultInterceptor extends AbstractSoapInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(SonosFaultInterceptor.class);

    private static Marshaller marshaller = createMarshaller();

    private static Marshaller createMarshaller() {
        try {
            return JAXBContext.newInstance("com.sonos.services._1").createMarshaller();
        } catch (JAXBException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Constructor, setting the phase to Marshal. This happens before the default Fault Interceptor
     */
    public SonosFaultInterceptor() {
        super(Phase.MARSHAL);
    }

    /*
     * Only handles instances of SonosSoapFault, all other exceptions fall through to the default Fault Interceptor
     */
    @Override
    public void handleMessage(SoapMessage message) throws Fault {
        Fault fault = (Fault) message.getContent(Exception.class);
        LOG.warn("Error with Soap message", fault);

        if (fault.getCause() instanceof SonosSoapFault) {
            SonosSoapFault cause = (SonosSoapFault) fault.getCause();
            fault.setFaultCode(new QName(cause.getFaultCode()));
            fault.setMessage(cause.getFaultCode());

            Document document = DOMUtils.createDocument();
            Element details = document.createElement("detail");
            fault.setDetail(details);

            if (cause instanceof TokenRefreshRequired) {
                try {
                    marshaller.marshal(((TokenRefreshRequired) cause).getRefreshTokens(), details);
                } catch (JAXBException e) {
                    LOG.warn("Could not marshal Sonos refresh tokens", e);
                }
            } else {
                details.appendChild(document.createElement("ExceptionInfo"));

                Element sonosError = document.createElement("SonosError");
                sonosError.setTextContent(String.valueOf(cause.getSonosError()));
                details.appendChild(sonosError);
            }
        }
    }
}
