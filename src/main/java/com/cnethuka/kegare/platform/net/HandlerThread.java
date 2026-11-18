package com.cnethuka.kegare.platform.net;

/** Which thread runs a payload's handler. NETWORK is the connection's own thread, as the packet arrives (DESIGN §3.29). */
public enum HandlerThread {
    MAIN, NETWORK
}
