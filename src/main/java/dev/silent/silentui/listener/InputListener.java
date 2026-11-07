package dev.silent.silentui.listener;

import dev.silent.silentui.SilentUI;
import dev.silent.silentui.render.ScreenSession;
import dev.silent.silentui.util.Text;
import io.papermc.paper.event.player.AsyncChatEvent;
import io.papermc.paper.event.player.PrePlayerAttackEntityEvent;
import org.bukkit.Input;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInputEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerResourcePackStatusEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Clicks, keys, chat input and lifecycle events. */
public final class InputListener implements Listener {

    private final SilentUI plugin;
    private final Map<UUID, Input> lastInput = new HashMap<>();

    public InputListener(SilentUI plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------ clicks

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onAttack(PrePlayerAttackEntityEvent e) {
        if (plugin.manager().click(e.getPlayer(), e.getAttacked(), true)) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInteractEntity(PlayerInteractEntityEvent e) {
        if (e.getRightClicked().getScoreboardTags().contains("silentui") && plugin.manager().byHitbox(e.getRightClicked()) == null) {
            e.setCancelled(true); // player models and other UI entities are not interactable
            return;
        }
        if (plugin.manager().byHitbox(e.getRightClicked()) == null) return;
        e.setCancelled(true);
        if (e.getHand() != EquipmentSlot.HAND) return;
        plugin.manager().click(e.getPlayer(), e.getRightClicked(), false);
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
    public void onInteractAt(PlayerInteractAtEntityEvent e) {
        if (plugin.manager().byHitbox(e.getRightClicked()) != null) e.setCancelled(true);
    }

    /** While a screen is open, never let clicks break/place/use blocks or items. */
    @EventHandler(priority = EventPriority.LOWEST)
    public void onInteract(PlayerInteractEvent e) {
        if (plugin.manager().screen(e.getPlayer()) != null) e.setCancelled(true);
    }

    // ------------------------------------------------------------------ keys

    @EventHandler
    public void onInput(PlayerInputEvent e) {
        Player p = e.getPlayer();
        Input now = e.getInput();
        Input before = lastInput.put(p.getUniqueId(), now);
        if (plugin.manager().screen(p) == null) return;
        if (now.isForward() && (before == null || !before.isForward())) plugin.manager().key(p, "W");
        if (now.isLeft() && (before == null || !before.isLeft())) plugin.manager().key(p, "A");
        if (now.isBackward() && (before == null || !before.isBackward())) plugin.manager().key(p, "S");
        if (now.isRight() && (before == null || !before.isRight())) plugin.manager().key(p, "D");
        if (now.isJump() && (before == null || !before.isJump())) plugin.manager().key(p, "JUMP");
        if (now.isSprint() && (before == null || !before.isSprint())) plugin.manager().key(p, "SPRINT");
        if (now.isSneak() && (before == null || !before.isSneak())) plugin.manager().key(p, "SNEAK");
    }

    @EventHandler(ignoreCancelled = true)
    public void onScroll(PlayerItemHeldEvent e) {
        if (plugin.manager().screen(e.getPlayer()) == null) return;
        int diff = (e.getNewSlot() - e.getPreviousSlot() + 9) % 9;
        e.setCancelled(true);
        if (diff == 1) plugin.manager().key(e.getPlayer(), "SCROLL_DOWN");
        else if (diff == 8) plugin.manager().key(e.getPlayer(), "SCROLL_UP");
        else plugin.manager().key(e.getPlayer(), "SLOT_" + (e.getNewSlot() + 1));
    }

    @EventHandler(ignoreCancelled = true)
    public void onDrop(PlayerDropItemEvent e) {
        if (plugin.manager().screen(e.getPlayer()) == null) return;
        e.setCancelled(true);
        plugin.manager().key(e.getPlayer(), "DROP");
    }

    @EventHandler(ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent e) {
        if (plugin.manager().screen(e.getPlayer()) == null) return;
        e.setCancelled(true);
        plugin.manager().key(e.getPlayer(), "SWAP");
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncChatEvent e) {
        String msg = Text.plain(e.message());
        if (plugin.manager().captureChat(e.getPlayer(), msg)) e.setCancelled(true);
    }

    // ------------------------------------------------------------------ lifecycle

    @EventHandler(ignoreCancelled = true)
    public void onDamage(EntityDamageEvent e) {
        // our display/interaction entities can never be hurt
        if (e.getEntity().getScoreboardTags().contains("silentui")) e.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent e) {
        if (plugin.manager().isInternalTeleport(e.getPlayer())) return;
        ScreenSession s = plugin.manager().screen(e.getPlayer());
        if (s != null) plugin.manager().closeScreen(e.getPlayer());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        plugin.manager().closeScreen(e.getPlayer());
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent e) {
        plugin.manager().closeScreen(e.getPlayer());
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        ScreenSession.removeFreeze(plugin, p);
        if (plugin.packSending()) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) plugin.sendPack(p);
            }, plugin.getConfig().getLong("resource-pack.send-delay", 20));
            // HUDs wait for the pack (or 30s), otherwise players see boxes
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline() && plugin.hud().shown(p).isEmpty()) plugin.hud().autoShow(p);
            }, 20L * 30);
        } else {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (p.isOnline()) plugin.hud().autoShow(p);
            }, 20L);
        }
    }

    @EventHandler
    public void onPackStatus(PlayerResourcePackStatusEvent e) {
        if (!plugin.packId().equals(e.getID())) return;
        if (e.getStatus() == PlayerResourcePackStatusEvent.Status.SUCCESSFULLY_LOADED) {
            Player p = e.getPlayer();
            if (plugin.hud().shown(p).isEmpty()) plugin.hud().autoShow(p);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        plugin.manager().closeScreen(p);
        plugin.hud().remove(p);
        lastInput.remove(p.getUniqueId());
        plugin.forgetVars(p);
    }
}
