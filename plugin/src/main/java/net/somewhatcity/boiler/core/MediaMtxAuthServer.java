/*
 * Copyright (c) 2025.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package net.somewhatcity.boiler.core;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import io.javalin.Javalin;

import java.util.HashMap;

public class MediaMtxAuthServer {

    private Javalin app;
    private final java.util.concurrent.ConcurrentHashMap<String, String> credentials = new java.util.concurrent.ConcurrentHashMap<>();

    public MediaMtxAuthServer() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(BoilerPlugin.class.getClassLoader());
        try { app = Javalin.create().start(7000); }
        finally { Thread.currentThread().setContextClassLoader(classLoader); }

        app.exception(Exception.class, (e, context) -> {
            e.printStackTrace();
        });

        app.post("/auth", ctx -> {
            JsonObject json;
            try { json = JsonParser.parseString(ctx.body()).getAsJsonObject(); }
            catch (RuntimeException e) { ctx.status(400).result("Invalid JSON"); return; }

            String ip = json.get("ip").getAsString();
            String user = json.get("user").getAsString();
            String password = json.get("password").getAsString();


            if(ip.equals("::1")) {
                ctx.status(200);
            }
            else {
                if(password.equals(credentials.get(user))) {
                    ctx.status(200);
                }
                else {
                    ctx.status(401);
                }
            }
        });

    }

    public void registerUser(String user, String password) {
        credentials.put(user, password);
    }

    public synchronized void stop() {
        credentials.clear();
        Javalin current = app;
        app = null;
        if (current != null) current.stop();
    }
}
