package dev.silent.silentui.render;

import dev.silent.silentui.SilentUI;
import dev.silent.silentui.hud.GlyphComposer;
import dev.silent.silentui.hud.HudLayout;
import dev.silent.silentui.model.UiDefinition;
import dev.silent.silentui.model.UiDefinition.ElementDef;
import dev.silent.silentui.model.UiDefinition.SlotDef;
import dev.silent.silentui.model.UiRegistry;
import dev.silent.silentui.pack.GlyphRegistry;
import dev.silent.silentui.script.UiInstance;
import dev.silent.silentui.util.Text;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

/**
 * A chest GUI: a real chest inventory whose title is a line of glyphs that paints a custom background over the
 * vanilla chest texture (items still render on top). Slots hold configured items; clicks on slots, or on the
 * elements drawn over them, run the UI's scripts. Players can never take items out.
 */
public final class ChestSession implements UiInstance, InventoryHolder {

    private final SilentUI plugin;
    private final UiDefinition def;
    private final Player player;
    private final Map<String, ElementState> states = new LinkedHashMap<>();
    private final Map<String, TreeSet<Integer>> yValues;
    /** Runtime slot definitions (scripts can change them). */
    private final Map<Integer, SlotDef> slots = new HashMap<>();
    private final Map<Integer, ItemStack> shown = new HashMap<>();
    private Inventory inventory;
    private boolean active;
    private boolean titleDirty;
    private boolean reopening;
    private long ticks;
    private int lastTitleTick = -100;

    public ChestSession(SilentUI plugin, UiDefinition def, Player player) {
        this.plugin = plugin;
        this.def = def;
        this.player = player;
        for (ElementDef e : def.elements) states.put(e.id, new ElementState(e));
        for (SlotDef s : def.slots) slots.put(s.slot, copy(s));
        this.yValues = HudLayout.yValues(def);
    }

    private static SlotDef copy(SlotDef s) {
        return UiRegistry.GSON.fromJson(UiRegistry.GSON.toJson(s), SlotDef.class);
    }

    public Player player() {
        return player;
    }

    public int size() {
        return def.settings.rows * 9;
    }

    // ------------------------------------------------------------------ opening

    public void open() {
        active = true;
        applyDefaultTabs();
        build();
        plugin.scripts().fire(this, player, "on_open");
        if (titleDirty) reopen();
    }

    private void build() {
        Component title = title();
        Inventory inv = Bukkit.createInventory(this, size(), title);
        shown.clear();
        inventory = inv;
        fillSlots(true);
        reopening = true;
        try {
            player.openInventory(inv);
        } finally {
            reopening = false;
        }
        titleDirty = false;
        lastTitleTick = (int) ticks;
    }

    /** The title redraws the whole background, so changes to elements reopen the inventory (contents are kept). */
    private void reopen() {
        if (!active) return;
        build();
    }

    private Component title() {
        Map<String, String> vars = plugin.vars(player);
        Component glyphs = GlyphComposer.compose(def, states.values(), player, vars, plugin.glyphs().guiSet(),
                8, -def.settings.guiTop, yValues);
        // shift to the chest's left edge first: the title starts at x=8
        Component plain = def.settings.title.isEmpty() ? Component.empty()
                : Text.mm(Text.apply(def.settings.title, player, vars)).colorIfAbsent(TextColor.color(0x404040));
        return Component.text().append(glyphs).append(plain).build();
    }

    // ------------------------------------------------------------------ slots

    private void fillSlots(boolean force) {
        Map<Integer, ItemStack> wanted = new HashMap<>();
        // tooltips for elements drawn over empty slots
        for (int i = 0; i < size(); i++) {
            ElementState s = elementAt(i);
            if (s != null && !s.def.tooltip.isEmpty() && !slots.containsKey(i)) wanted.put(i, tooltipItem(s.def.tooltip));
        }
        for (SlotDef s : slots.values()) {
            if (s.slot >= size()) continue;
            wanted.put(s.slot, build(s));
        }
        for (int i = 0; i < size(); i++) {
            ItemStack w = wanted.get(i);
            ItemStack old = shown.get(i);
            if (!force && java.util.Objects.equals(w, old)) continue;
            inventory.setItem(i, w);
            if (w == null) shown.remove(i);
            else shown.put(i, w);
        }
    }

    private ItemStack tooltipItem(String tooltip) {
        ItemStack it = new ItemStack(Material.PAPER);
        String[] lines = tooltip.split("\\|");
        it.editMeta(meta -> {
            meta.setItemModel(NamespacedKey.fromString(GlyphRegistry.BLANK_MODEL));
            meta.displayName(Text.mm("<!italic>" + Text.apply(lines[0], player, plugin.vars(player))));
            List<Component> lore = new ArrayList<>();
            for (int i = 1; i < lines.length; i++) lore.add(Text.mm("<!italic><gray>" + Text.apply(lines[i], player, plugin.vars(player))));
            if (!lore.isEmpty()) meta.lore(lore);
        });
        return it;
    }

    private ItemStack build(SlotDef s) {
        Map<String, String> vars = plugin.vars(player);
        String id = s.item;
        if (!s.frames.isEmpty()) id = s.frames.get((int) ((ticks / s.frameTicks) % s.frames.size()));
        id = Text.apply(id, null, vars).trim();
        ItemStack it = s.blank ? new ItemStack(Material.PAPER) : ElementView.resolveItem(id, player);
        if (it.getType().isAir()) return null;
        it.setAmount(Math.max(1, Math.min(it.getMaxStackSize(), s.amount)));
        it.editMeta(meta -> {
            if (!s.name.isEmpty()) meta.displayName(Text.mm("<!italic>" + Text.apply(s.name, player, vars)));
            if (!s.lore.isEmpty()) {
                List<Component> lore = new ArrayList<>();
                for (String line : s.lore.split("\\||\\n")) lore.add(Text.mm("<!italic>" + Text.apply(line, player, vars)).colorIfAbsent(TextColor.color(0xAAAAAA)));
                meta.lore(lore);
            }
            if (s.glint) meta.setEnchantmentGlintOverride(true);
            if (s.blank) meta.setItemModel(NamespacedKey.fromString(GlyphRegistry.BLANK_MODEL));
            else if (!s.itemModel.isEmpty()) {
                NamespacedKey k = NamespacedKey.fromString(s.itemModel.toLowerCase(Locale.ROOT));
                if (k != null) meta.setItemModel(k);
            }
            if (s.hideTooltip) meta.setHideTooltip(true);
        });
        if (it.getType() == Material.PLAYER_HEAD) {
            String owner = Text.apply(s.headOwner.isEmpty() ? "{player}" : s.headOwner, player, vars);
            if (owner.equalsIgnoreCase(player.getName())) {
                it.editMeta(SkullMeta.class, m -> m.setOwningPlayer(player));
            } else {
                int slot = s.slot;
                plugin.profiles().get(owner, profile -> {
                    if (!active) return;
                    ItemStack cur = inventory.getItem(slot);
                    if (cur == null || cur.getType() != Material.PLAYER_HEAD) return;
                    cur.editMeta(SkullMeta.class, m -> m.setPlayerProfile(profile));
                    inventory.setItem(slot, cur);
                    shown.put(slot, cur);
                });
            }
        }
        return it;
    }

    /** Top-most interactive element drawn over the centre of a slot. */
    private ElementState elementAt(int slot) {
        int col = slot % 9, row = slot / 9;
        double cx = 8 + col * 18 + 8, cy = def.settings.guiTop + 18 + row * 18 + 8;
        List<ElementState> list = new ArrayList<>(states.values());
        for (int i = list.size() - 1; i >= 0; i--) {
            ElementState s = list.get(i);
            if (s.interactive() && s.contains(cx, cy)) return s;
        }
        return null;
    }

    // ------------------------------------------------------------------ events

    public void click(int slot, ClickType type) {
        if (!active || slot < 0 || slot >= size()) return;
        String button = type.isRightClick() ? "right" : "left";
        boolean shift = type.isShiftClick();
        Map<String, String> extra = Map.of("slot", String.valueOf(slot), "button", button, "shift", String.valueOf(shift));
        plugin.scripts().fire(this, player, "on_slot_click", n -> slotMatches(n.prop("slot", ""), slot)
                && clickMatches(n.prop("button", "any"), button, shift), extra);
        ElementState s = elementAt(slot);
        if (s != null) {
            plugin.scripts().fire(this, player, "on_click", n -> {
                String el = n.prop("element", "");
                return (el.isEmpty() || el.equals(s.def.id)) && clickMatches(n.prop("button", "any"), button, shift);
            }, Map.of("element", s.def.id, "button", button, "slot", String.valueOf(slot)));
            clickedTabButton(s);
            if ("input".equals(s.type())) focusInput(player, s.def.id);
        }
    }

    static boolean clickMatches(String want, String button, boolean shift) {
        return switch (want) {
            case "left" -> button.equals("left") && !shift;
            case "right" -> button.equals("right") && !shift;
            case "shift" -> shift;
            default -> true;
        };
    }

    /** "", "13", "10,11,12", "10-16" */
    static boolean slotMatches(String spec, int slot) {
        if (spec == null || spec.isBlank() || spec.equalsIgnoreCase("any")) return true;
        for (String part : spec.split(",")) {
            String p = part.trim();
            int dash = p.indexOf('-');
            try {
                if (dash > 0) {
                    int a = Integer.parseInt(p.substring(0, dash).trim()), b = Integer.parseInt(p.substring(dash + 1).trim());
                    if (slot >= Math.min(a, b) && slot <= Math.max(a, b)) return true;
                } else if (Integer.parseInt(p) == slot) return true;
            } catch (NumberFormatException ignored) {
                // bad spec
            }
        }
        return false;
    }

    public boolean isReopening() {
        return reopening;
    }

    /** The player closed the inventory themselves. */
    public void closed() {
        if (!active) return;
        plugin.scripts().fire(this, player, "on_close");
        active = false;
    }

    public void tick() {
        if (!active) return;
        ticks++;
        for (dev.silent.silentui.model.UiDefinition.NodeDef n : def.script.nodes) {
            if (!"on_timer".equals(n.type)) continue;
            long interval = Math.max(1, Math.round(Text.num(n.prop("seconds", "1"), 1) * 20));
            if (ticks % interval == 0) plugin.scripts().fire(this, player, "on_timer", node -> node == n, null);
        }
        boolean frames = slots.values().stream().anyMatch(s -> !s.frames.isEmpty() && ticks % s.frameTicks == 0);
        if (frames || ticks % 20 == 0) fillSlots(false);
        // title changes are batched (at most twice a second) because they reopen the inventory
        if (titleDirty && ticks - lastTitleTick >= 10) reopen();
        if (ticks % 40 == 0 && hasDynamicTitle()) titleDirty = true;
    }

    private boolean hasDynamicTitle() {
        if (def.settings.title.contains("{") || def.settings.title.contains("%")) return true;
        for (ElementState s : states.values()) if (s.visible && (s.text.contains("{") || s.text.contains("%") || s.value.contains("{") || s.value.contains("%"))) return true;
        return false;
    }

    // ------------------------------------------------------------------ script hooks

    public void setSlot(int slot, String item, String name, String lore, String amount) {
        SlotDef s = slots.computeIfAbsent(slot, k -> {
            SlotDef n = new SlotDef();
            n.slot = k;
            return n;
        });
        if (item != null && !item.isEmpty()) {
            if (item.equalsIgnoreCase("air") || item.equalsIgnoreCase("minecraft:air")) {
                slots.remove(slot);
                fillSlots(false);
                return;
            }
            s.item = item;
            s.blank = false;
            s.frames = new ArrayList<>();
        }
        if (name != null && !name.isEmpty()) s.name = name;
        if (lore != null && !lore.isEmpty()) s.lore = lore;
        if (amount != null && !amount.isEmpty()) s.amount = (int) Math.max(1, Text.num(amount, 1));
        fillSlots(false);
    }

    public void submitInput(Player p, String elementId, String text) {
        ElementState s = states.get(elementId);
        if (s == null) return;
        s.text = text;
        plugin.vars(p).put("input_" + elementId, text);
        titleDirty = true;
        plugin.scripts().fireElement(this, p, "on_text", elementId, Map.of("text", text));
    }

    @Override
    public UiDefinition def() {
        return def;
    }

    @Override
    public ElementState state(String elementId) {
        return states.get(elementId);
    }

    @Override
    public Collection<ElementState> states() {
        return states.values();
    }

    @Override
    public void refresh(String elementId, int interpolationTicks) {
        titleDirty = true;
        fillSlots(false);
    }

    @Override
    public void playAnimation(String idOrName) {
        // chest titles cannot animate smoothly; use slot frames or switch tabs instead
    }

    @Override
    public void stopAnimation(String idOrName) {
    }

    @Override
    public void close(Player p) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (active && player.getOpenInventory().getTopInventory().getHolder() == this) player.closeInventory();
        });
    }

    @Override
    public boolean isActive() {
        return active && player.isOnline();
    }

    @Override
    public void focusInput(Player p, String elementId) {
        plugin.manager().beginInput(p, this, elementId);
    }

    @Override
    public @NotNull Inventory getInventory() {
        return inventory;
    }
}
