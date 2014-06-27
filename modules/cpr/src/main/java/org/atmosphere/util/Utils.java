/*
 * Copyright 2014 Jeanfrancois Arcand
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.atmosphere.util;

import org.atmosphere.cpr.AtmosphereRequest;
import org.atmosphere.cpr.AtmosphereResource;
import org.atmosphere.cpr.AtmosphereResourceImpl;
import org.atmosphere.cpr.FrameworkConfig;
import org.atmosphere.cpr.HeaderConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;

import static org.atmosphere.cpr.ApplicationConfig.SUSPENDED_ATMOSPHERE_RESOURCE_UUID;
import static org.atmosphere.cpr.HeaderConfig.WEBSOCKET_UPGRADE;

/**
 * Utils class.
 *
 * @author Jeanfrancois Arcand
 */
public final class Utils {

    /**
     * The logger.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    public final static boolean webSocketEnabled(HttpServletRequest request) {

        if (closeMessage(request) || !webSocketQueryStringPresentOrNull(request)) return false;

        boolean allowWebSocketWithoutHeaders = request.getHeader(HeaderConfig.X_ATMO_WEBSOCKET_PROXY) != null ? true : false;
        if (allowWebSocketWithoutHeaders) return true;

        boolean webSocketEnabled = false;
        Enumeration<String> connection = request.getHeaders("Connection");
        if (connection == null || !connection.hasMoreElements()) {
            connection = request.getHeaders("connection");
        }

        if (connection != null && connection.hasMoreElements()) {
            String[] e = connection.nextElement().toString().split(",");
            for (String upgrade : e) {
                if (upgrade.trim().equalsIgnoreCase(WEBSOCKET_UPGRADE)) {
                    webSocketEnabled = true;
                    break;
                }
            }
        }
        return webSocketEnabled;
    }

    public final static boolean firefoxWebSocketEnabled(HttpServletRequest request) {
        return webSocketEnabled(request)
                && request.getHeader(HeaderConfig.X_ATMO_PROTOCOL) != null
                && request.getHeader(HeaderConfig.X_ATMO_PROTOCOL).equals("true")
                && request.getHeader("User-Agent") != null
                && request.getHeader("User-Agent").toLowerCase().indexOf("firefox") != -1;
    }

    public final static boolean twoConnectionsTransport(AtmosphereResource.TRANSPORT t) {
        switch (t) {
            case JSONP:
            case LONG_POLLING:
            case STREAMING:
            case SSE:
            case POLLING:
            case HTMLFILE:
                return true;
            default:
                return false;
        }
    }

    public final static boolean webSocketQueryStringPresentOrNull(HttpServletRequest request) {
        String transport = request.getHeader(HeaderConfig.X_ATMOSPHERE_TRANSPORT);
        if (transport == null) {
            // ignore so other framework client can work
            return true;
        } else {
            return transport.equalsIgnoreCase(HeaderConfig.WEBSOCKET_TRANSPORT);
        }
    }

    public final static boolean resumableTransport(AtmosphereResource.TRANSPORT t) {
        switch (t) {
            case JSONP:
            case LONG_POLLING:
                return true;
            default:
                return false;
        }
    }

    public final static boolean pollableTransport(AtmosphereResource.TRANSPORT t) {
        switch (t) {
            case POLLING:
            case UNDEFINED:
            case CLOSE:
            case AJAX:
                return true;
            default:
                return false;
        }
    }

    public final static boolean pushMessage(AtmosphereResource.TRANSPORT t) {
        switch (t) {
            case POLLING:
            case UNDEFINED:
            case AJAX:
                return true;
            default:
                return false;
        }
    }

    public final static boolean atmosphereProtocol(AtmosphereRequest r) {
        String p = r.getHeader(HeaderConfig.X_ATMO_PROTOCOL);
        return (p != null && Boolean.valueOf(p));
    }

    public final static boolean webSocketMessage(AtmosphereResource r) {
        AtmosphereRequest request = AtmosphereResourceImpl.class.cast(r).getRequest(false);
        return request.getAttribute(FrameworkConfig.WEBSOCKET_MESSAGE) != null;
    }

    public static boolean properProtocol(HttpServletRequest request) {
        Enumeration<String> connection = request.getHeaders("Connection");
        if (connection == null || !connection.hasMoreElements()) {
            connection = request.getHeaders("connection");
        }

        boolean isOK = false;
        boolean isWebSocket = (request.getHeader("sec-websocket-version") != null || request.getHeader("Sec-WebSocket-Draft") != null);
        if (connection != null && connection.hasMoreElements()) {
            String[] e = connection.nextElement().toString().split(",");
            for (String upgrade : e) {
                if (upgrade.trim().equalsIgnoreCase("upgrade")) {
                    isOK = true;
                }
            }
        }
        return isWebSocket ? isOK : true;
    }

    public static final AtmosphereResource websocketResource(AtmosphereResource r) {
        String parentUUID = (String) AtmosphereResourceImpl.class.cast(r).getRequest(false).getAttribute(SUSPENDED_ATMOSPHERE_RESOURCE_UUID);
        if (parentUUID != null) {
            r = r.getAtmosphereConfig().resourcesFactory().find(parentUUID);
        }
        return r;
    }

    public static final boolean closeMessage(HttpServletRequest request) {
        String s = request.getHeader(HeaderConfig.X_ATMOSPHERE_TRANSPORT);
        return s != null && s.equalsIgnoreCase(HeaderConfig.DISCONNECT_TRANSPORT_MESSAGE);
    }

    /**
     * <p>
     * Manages the invocation of the given method on the specified 'proxied' instance. Logs any invocation failure.
     * </p>
     *
     * @param proxiedInstance the instance
     * @param m the method to invoke that belongs to the instance
     * @param o the optional parameter
     * @return the result of the invocation
     */
    public static Object invoke(final Object proxiedInstance, Method m, Object o) {
        if (m != null) {
            try {
                return m.invoke(proxiedInstance, o == null ? new Object[]{} : new Object[]{o});
            } catch (IllegalAccessException e) {
                LOGGER.debug("", e);
            } catch (InvocationTargetException e) {
                LOGGER.debug("", e);
            }
        }
        LOGGER.trace("No Method Mapped for {}", o);
        return null;
    }
}
