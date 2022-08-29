package de.vsy.chat.server.server;

import java.net.Socket;

public
interface ClientServer {

    void serveClient (Socket clientConnection);

    boolean isOperable ();
}
