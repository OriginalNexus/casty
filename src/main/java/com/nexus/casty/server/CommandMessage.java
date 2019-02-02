package com.nexus.casty.server;

import com.google.gson.JsonObject;

class CommandMessage {
    String scope;
    String action;
    JsonObject params;
}
