package com.nexus.casty.server;

import com.google.gson.Gson;
import com.nexus.casty.player.*;
import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.WebSocketAdapter;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class CastyWebSocketHandler extends WebSocketAdapter {

    @Override
    public void onWebSocketClose(int statusCode, String reason) {
        System.out.println("Client close: statusCode=" + statusCode + ", reason=" + reason);
    }

    @Override
    public void onWebSocketConnect(Session session) {
        System.out.println("Client connect: " + session.getRemoteAddress().getAddress());
        StatusUpdateHandler handler = new StatusUpdateHandler(session);
        CastyPlayer.getInstance().addStatusListener(handler);
    }

    @Override
    public void onWebSocketError(Throwable cause) {
        System.out.println("Error: " + cause.getMessage());
        cause.printStackTrace();
    }

    @Override
    public void onWebSocketText(String message) {
        Gson gson = new Gson();
        CommandMessage request = gson.fromJson(message, CommandMessage.class);

        if (request.scope == null || request.action == null)
            return;

        switch (request.scope) {
            case "player":
                switch (request.action) {
                    case "playurl":
                        String url = request.params.get("url").getAsString();
                        new Thread(() -> CastyPlayer.getInstance().playUrl(URLDecoder.decode(url, StandardCharsets.UTF_8))).start();
                        break;
                    case "playpause":
                        CastyPlayer.getInstance().playPause();
                        break;
                    case "next":
                        CastyPlayer.getInstance().next();
                        break;
                    case "previous":
                        CastyPlayer.getInstance().previous();
                        break;
                    case "position":
                        float percent = request.params.get("percent").getAsFloat();
                        CastyPlayer.getInstance().setPosition(percent);
                        break;
                    case "playlist":
                        int index = request.params.get("index").getAsInt();
                        CastyPlayer.getInstance().playlistPlayItem(index);
                        break;
                    case "volume":
                        int level = request.params.get("level").getAsInt();
                        CastyPlayer.getInstance().setVolume(level);
                        break;
                    case "cache":
                        CastyPlayer.getInstance().cacheCurrentSong();
                        break;
                    case "uncache":
                        CastyPlayer.getInstance().uncacheCurrentSong();
                        break;
                    default:
                        return;
                }
                break;

            case "playlist":
                switch (request.action) {
                    case "add":
                        String url = request.params.get("url").getAsString();
                        String title = request.params.get("title").getAsString();
                        if (title == null)
                            title = url;

                        PlaylistItem item = new PlaylistItem();
                        item.url = URLDecoder.decode(url, StandardCharsets.UTF_8);
                        item.title = URLDecoder.decode(title, StandardCharsets.UTF_8);
                        CastyPlayer.getInstance().getPlaylist().addItem(item);
                        break;
                    case "remove":
                        int index = request.params.get("index").getAsInt();
                        CastyPlayer.getInstance().getPlaylist().removeItem(index);
                        break;

                    case "repeat":
                        boolean repeat = request.params.get("value").getAsBoolean();
                        CastyPlayer.getInstance().getPlaylist().setRepeat(repeat);
                        break;
                    case "cache":
                        CastyPlayer.getInstance().loadCachePlaylist();
                        break;
                }
                break;
        }
    }

}