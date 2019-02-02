package com.nexus.casty.server;

import com.google.gson.Gson;
import com.nexus.casty.player.*;
import com.nexus.casty.song.SongInfo;
import com.nexus.casty.status.StatusListener;
import org.eclipse.jetty.websocket.api.Session;


public class StatusUpdateHandler implements StatusListener {
    private Gson gson = new Gson();
    private Session session;

    StatusUpdateHandler(Session session) {
        this.session = session;
    }

    @Override
    public void onUpdate(Object status) {
        if (!session.isOpen()) {
            CastyPlayer.getInstance().removeStatusListener(this);
            return;
        }

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
