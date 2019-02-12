package com.nexus.casty.server;

import com.google.gson.Gson;
import com.nexus.casty.player.*;
import com.nexus.casty.song.SongInfo;
import org.eclipse.jetty.websocket.api.Session;


class StatusListener {
    private final Gson gson = new Gson();
    final Session session;

    StatusListener(Session session) {
        this.session = session;
    }

    void onUpdate(Object status) {
        String scope;
        if (status instanceof PlayerStatus)
            scope = "player";
        else if (status instanceof PlaylistStatus)
            scope = "playlist";
        else if (status instanceof SongInfo)
            scope = "song";
        else
            return;

        StatusMessage message = new StatusMessage(scope, gson.toJsonTree(status).getAsJsonObject());
        session.getRemote().sendString(gson.toJson(message), null);
    }
}
