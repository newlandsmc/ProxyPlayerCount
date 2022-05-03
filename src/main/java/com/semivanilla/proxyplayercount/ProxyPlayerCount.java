package com.semivanilla.proxyplayercount;

import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerListPingEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;
import sun.security.jgss.GSSHeader;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ProxyPlayerCount extends JavaPlugin implements Listener {
    private int playerCount = 0;

    private Gson gson = new Gson();

    @Override
    public void onEnable() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        if (!new File(getDataFolder(), "config.yml").exists()) {
            saveDefaultConfig();
        }
        getServer().getPluginManager().registerEvents(this, this);
        List<URL> urls = getConfig().getStringList("urls").stream().map(s -> {
            try {
                return new URL(s);
            } catch (Exception e) {
                return null;
            }
        }).filter(Objects::nonNull).collect(Collectors.toList());
        getServer().getScheduler().scheduleAsyncRepeatingTask(this, () -> {
            int total = 0;
            for (URL url : urls) {
                try {
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("User-Agent", "Mozilla/5.0");
                    InputStream is = connection.getInputStream();
                    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                    StringBuffer response = new StringBuffer();
                    String line;
                    while ((line = rd.readLine()) != null) {
                        response.append(line);
                        response.append('\r');
                    }
                    rd.close();
                    JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                    JsonObject players = json.get("players").getAsJsonObject();
                    total += players.get("online").getAsInt();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            playerCount = total + Bukkit.getOnlinePlayers().size();
        }, getConfig().getLong("interval"), getConfig().getLong("interval"));
    }

    @EventHandler
    public void onPing(PaperServerListPingEvent event) {
        event.setNumPlayers(playerCount);
        while (event.iterator().hasNext()) {
            event.iterator().next();
            event.iterator().remove();
        }
    }
}
