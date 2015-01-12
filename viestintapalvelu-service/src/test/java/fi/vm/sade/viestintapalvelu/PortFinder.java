/**
 * Copyright (c) 2014 The Finnish Board of Education - Opetushallitus
 *
 * This program is free software:  Licensed under the EUPL, Version 1.1 or - as
 * soon as they will be approved by the European Commission - subsequent versions
 * of the EUPL (the "Licence");
 *
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at: http://www.osor.eu/eupl/
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * European Union Public Licence for more details.
 **/
package fi.vm.sade.viestintapalvelu;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;

/**
 * 
 * http://fahdshariff.blogspot.fi/2012/10/java-find-available-port-number.html
 * 
 */
public class PortFinder {
    // the ports below 1024 are system ports
    private static final int MIN_PORT_NUMBER = 10024;

    // the ports above 49151 are dynamic and/or private
    private static final int MAX_PORT_NUMBER = 49151;

    /**
     * Finds a free port between {@link #MIN_PORT_NUMBER} and
     * {@link #MAX_PORT_NUMBER}.
     * 
     * @return a free port
     * @throw RuntimeException if a port could not be found
     */
    public static int findFreePort() {
        for (int i = MIN_PORT_NUMBER; i <= MAX_PORT_NUMBER; i++) {
            if (available(i)) {
                return i;
            }
        }
        throw new RuntimeException("Could not find an available port between " + MIN_PORT_NUMBER + " and "
                + MAX_PORT_NUMBER);
    }

    /**
     * Returns true if the specified port is available on this host.
     * 
     * @param port
     *            the port to check
     * @return true if the port is available, false otherwise
     */
    private static boolean available(final int port) {
        ServerSocket serverSocket = null;
        DatagramSocket dataSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setReuseAddress(true);
            dataSocket = new DatagramSocket(port);
            dataSocket.setReuseAddress(true);
            return true;
        } catch (final IOException e) {
            return false;
        } finally {
            if (dataSocket != null) {
                dataSocket.close();
            }
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (final IOException e) {
                    // can never happen
                }
            }
        }
    }
}
