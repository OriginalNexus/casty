package com.nexus.casty.server;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;
import java.io.IOException;

public class CastyWebSocketHandler extends WebSocketAdapter {
    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        System.out.println("Close: statusCode=" + statusCode + ", reason=" + reason);
    }

    @Override
    public void onWebSocketConnect(Session session) {
        System.out.println("Connect: " + session.getRemoteAddress().getAddress());
        try {
            session.getRemote().sendString("Hello Webbrowser");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        System.out.println("Error: " + cause.getMessage());
    }

    @Override
    public void onWebSocketText(String message) {
        System.out.println("Message: " + message);
    }

}