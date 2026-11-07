package dev.silent.silentui.util;

import com.destroystokyo.paper.profile.PlayerProfile;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/** Completed player profiles (with skin textures), looked up once per name and cached. */
public final class ProfileCache {

    private static final long TTL_MS = 30 * 60 * 1000L;

    private final Plugin plugin;
    private final Map<String, Entry> cache = new ConcurrentHashMap<>();
    private final Map<String, List<Consumer<PlayerProfile>>> pending = new ConcurrentHashMap<>();

    private record Entry(PlayerProfile profile, long time) {}

    public ProfileCache(Plugin plugin) {
        this.plugin = plugin;
    }

    /** Calls back on the main thread with a profile that has textures (when Mojang knows the name). */
    public void get(String name, Consumer<PlayerProfile> callback) {
        if (name == null || name.isBlank()) return;
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) {
            callback.accept(online.getPlayerProfile());
            return;
        }
        String key = name.toLowerCase(Locale.ROOT);
        Entry e = cache.get(key);
        if (e != null && System.currentTimeMillis() - e.time() < TTL_MS) {
            callback.accept(e.profile());
            return;
        }
        List<Consumer<PlayerProfile>> waiting = pending.computeIfAbsent(key, k -> new ArrayList<>());
        synchronized (waiting) {
            waiting.add(callback);
            if (waiting.size() > 1) return; // lookup already running
        }
        CompletableFuture.supplyAsync(() -> complete(name)).thenAccept(profile ->
                Bukkit.getScheduler().runTask(plugin, () -> {
                    List<Consumer<PlayerProfile>> cbs = pending.remove(key);
                    if (profile == null || cbs == null) return;
                    cache.put(key, new Entry(profile, System.currentTimeMillis()));
                    synchronized (cbs) {
                        for (Consumer<PlayerProfile> cb : cbs) cb.accept(profile);
                    }
                }));
    }

    /** Blocking lookup, for async callers (the web API). */
    public PlayerProfile getBlocking(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online.getPlayerProfile();
        String key = name.toLowerCase(Locale.ROOT);
        Entry e = cache.get(key);
        if (e != null && System.currentTimeMillis() - e.time() < TTL_MS) return e.profile();
        PlayerProfile p = complete(name);
        if (p != null) cache.put(key, new Entry(p, System.currentTimeMillis()));
        return p;
    }

    private static PlayerProfile complete(String name) {
        try {
            PlayerProfile profile = Bukkit.createProfile(name);
            profile.complete(true);
            return profile;
        } catch (RuntimeException ex) {
            return null;
        }
    }
}
