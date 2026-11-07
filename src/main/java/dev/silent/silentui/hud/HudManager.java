package dev.silent.silentui.hud;

import dev.silent.silentui.SilentUI;
import dev.silent.silentui.model.UiDefinition;
import net.kyori.adventure.bossbar.BossBar;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Owns every player's HUD bossbar. All HUDs a player has are merged into ONE bossbar whose title is only re-sent
 * when the rendered component actually changes.
 */
public final class HudManager {

    private final SilentUI plugin;
    private final Map<UUID, PlayerHud> players = new HashMap<>();

    private static final class PlayerHud {
        final Map<String, HudInstance> huds = new LinkedHashMap<>();
        BossBar bar;
        Component last;
        boolean forceRender = true;
    }

    public HudManager(SilentUI plugin) {
        this.plugin = plugin;
    }

    public boolean show(Player p, String id) {
        UiDefinition def = plugin.registry().get(id);
        if (def == null || !def.isHud()) return false;
        PlayerHud ph = players.computeIfAbsent(p.getUniqueId(), k -> new PlayerHud());
        HudInstance old = ph.huds.remove(def.id);
        if (old != null) old.deactivate();
        HudInstance inst = new HudInstance(plugin, def, p);
        ph.huds.put(def.id, inst);
        ph.forceRender = true;
        plugin.scripts().fire(inst, p, "on_open");
        return true;
    }

    public void hide(Player p, String id) {
        PlayerHud ph = players.get(p.getUniqueId());
        if (ph == null) return;
        HudInstance inst = ph.huds.remove(id);
        if (inst != null) {
            plugin.scripts().fire(inst, p, "on_close");
            inst.deactivate();
        }
        ph.forceRender = true;
        if (ph.huds.isEmpty()) remove(p);
    }

    public void toggle(Player p, String id) {
        if (isShown(p, id)) hide(p, id);
        else show(p, id);
    }

    public boolean isShown(Player p, String id) {
        PlayerHud ph = players.get(p.getUniqueId());
        return ph != null && ph.huds.containsKey(id);
    }

    public HudInstance instance(Player p, String id) {
        PlayerHud ph = players.get(p.getUniqueId());
        return ph == null ? null : ph.huds.get(id);
    }

    public List<String> shown(Player p) {
        PlayerHud ph = players.get(p.getUniqueId());
        return ph == null ? List.of() : new ArrayList<>(ph.huds.keySet());
    }

    public void remove(Player p) {
        PlayerHud ph = players.remove(p.getUniqueId());
        if (ph == null) return;
        for (HudInstance i : ph.huds.values()) i.deactivate();
        if (ph.bar != null) p.hideBossBar(ph.bar);
    }

    public void clear() {
        for (UUID id : new ArrayList<>(players.keySet())) {
            Player p = plugin.getServer().getPlayer(id);
            if (p != null) remove(p);
        }
        players.clear();
    }

    public void autoShow(Player p) {
        for (UiDefinition d : plugin.registry().all()) {
            if (d.isHud() && d.settings.autoShow) {
                String perm = d.settings.permission;
                if (perm == null || perm.isEmpty() || p.hasPermission(perm)) show(p, d.id);
            }
        }
    }

    public void tick(long serverTick) {
        boolean periodic = serverTick % Math.max(1, plugin.settings().hudRefreshTicks) == 0;
        for (Map.Entry<UUID, PlayerHud> en : new ArrayList<>(players.entrySet())) {
            Player p = plugin.getServer().getPlayer(en.getKey());
            PlayerHud ph = en.getValue();
            if (p == null) {
                players.remove(en.getKey());
                continue;
            }
            boolean changed = ph.forceRender || periodic;
            for (HudInstance h : new ArrayList<>(ph.huds.values())) changed |= h.tick();
            if (!changed) continue;
            ph.forceRender = false;
            TextComponent.Builder b = Component.text();
            for (HudInstance h : ph.huds.values()) b.append(h.render());
            Component title = b.build();
            if (title.equals(ph.last)) continue; // nothing new -> no packet
            ph.last = title;
            if (ph.bar == null) {
                ph.bar = BossBar.bossBar(title, 0f, plugin.hudBarColor(), BossBar.Overlay.PROGRESS);
                p.showBossBar(ph.bar);
            } else {
                ph.bar.name(title);
            }
        }
    }
}
