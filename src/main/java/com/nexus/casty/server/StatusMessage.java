package com.nexus.casty.server;

import com.google.gson.JsonObject;

class StatusMessage {
    private String scope;
    private JsonObject data;

    StatusMessage(String scope, JsonObject data) {
        this.scope = scope;
        this.data = data;
    }

}
