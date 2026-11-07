package dev.silent.silentui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.silent.silentui.hud.HudLayout;
import dev.silent.silentui.model.UiDefinition;
import dev.silent.silentui.model.UiRegistry;
import dev.silent.silentui.pack.GlyphRegistry;
import dev.silent.silentui.pack.PackBuilder;
import dev.silent.silentui.render.AnimationPlayer;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Builds the pack from the bundled example UIs and checks it against the vanilla font rules. */
class PackBuilderTest {

    private static UiRegistry registry;
    private static PackBuilder.Result result;
    private static final Map<String, byte[]> files = new HashMap<>();

    @BeforeAll
    static void build() throws Exception {
        Path dir = Files.createTempDirectory("silentui-test");
        for (String name : List.of("example_menu.json", "example_shop.json", "example_hud.json", "example_board.json",
                "example_chest.json", "example_hologram.json", "example_path.json")) {
            try (InputStream in = PackBuilderTest.class.getResourceAsStream("/uis/" + name)) {
                assertNotNull(in, "missing example " + name);
                Files.copy(in, dir.resolve(name));
            }
        }
        registry = new UiRegistry(dir, Logger.getLogger("test"));
        registry.load();
        result = new PackBuilder(Logger.getLogger("test"), 75, "WHITE").build(registry.all());
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(result.zip()))) {
            ZipEntry e;
            while ((e = zip.getNextEntry()) != null) files.put(e.getName(), zip.readAllBytes());
        }
    }

    @Test
    void examplesLoad() {
        assertEquals(7, registry.all().size());
        UiDefinition chest = registry.get("example_chest");
        assertTrue(chest.isChest());
        assertEquals(176, chest.canvas.width);
        assertEquals(14 + 4 * 18 + 114, chest.canvas.height);
        assertEquals(8, chest.slots.size());
        assertEquals(4, chest.slot(22).frames.size());
        UiDefinition path = registry.get("example_path");
        assertEquals(90, path.settings.tilt, 1e-9);
        assertTrue(path.elements.stream().allMatch(e -> e.tintSprite));
        assertEquals(7, path.animations.get(0).tracks.size());
        assertEquals("center", registry.get("example_hologram").settings.billboard);
        UiDefinition shop = registry.get("example_shop");
        assertEquals("blocks", shop.tabs.get("shop"));
        assertEquals("shop", shop.commands.get(0).name);
        assertEquals("choice", shop.commands.get(0).args.get(0).type);
        assertTrue(shop.elements.stream().anyMatch(e -> "shop/tools".equals(e.tabFor)));
        assertTrue(registry.get("example_menu").isScreen());
        assertTrue(registry.get("example_hud").isHud());
        assertTrue(registry.get("example_board").isWorld());
        assertEquals("menu", registry.get("example_menu").settings.command);
    }

    @Test
    void packHasBasics() {
        assertTrue(files.containsKey("pack.mcmeta"));
        assertTrue(files.containsKey("assets/silentui/font/ui.json"));
        assertTrue(files.containsKey("assets/silentui/font/hud.json"));
        assertTrue(files.containsKey("assets/silentui/font/space.json"));
        assertTrue(files.containsKey("assets/silentui/font/fx.json"));
        assertTrue(files.containsKey("assets/minecraft/textures/gui/sprites/boss_bar/white_background.png"));
        assertEquals(40, result.sha1Hex().length());
    }

    /** Vanilla rejects a bitmap provider whose ascent is larger than its height, or whose texture is missing. */
    @Test
    void everyBitmapProviderIsValid() throws Exception {
        int providers = 0;
        for (Map.Entry<String, byte[]> f : files.entrySet()) {
            if (!f.getKey().startsWith("assets/silentui/font/")) continue;
            JsonObject root = JsonParser.parseString(new String(f.getValue(), StandardCharsets.UTF_8)).getAsJsonObject();
            for (JsonElement el : root.getAsJsonArray("providers")) {
                JsonObject p = el.getAsJsonObject();
                if (!"bitmap".equals(p.get("type").getAsString())) continue;
                providers++;
                int ascent = p.get("ascent").getAsInt(), height = p.get("height").getAsInt();
                assertTrue(ascent <= height, f.getKey() + ": ascent " + ascent + " > height " + height);
                String file = p.get("file").getAsString();
                if (file.startsWith("minecraft:")) continue;
                String path = "assets/" + file.replace(":", "/textures/");
                assertTrue(files.containsKey(path), "missing texture " + path);
                BufferedImage img = ImageIO.read(new ByteArrayInputStream(files.get(path)));
                JsonArray chars = p.getAsJsonArray("chars");
                assertEquals(0, img.getHeight() % chars.size(), path + " height not divisible by rows");
            }
        }
        assertTrue(providers > 20, "expected many glyph providers, got " + providers);
    }

    @Test
    void glyphsExistForEveryElement() {
        GlyphRegistry g = result.glyphs();
        assertNotNull(g.pixel());
        assertNotNull(g.sprite("example_menu", "panel"));
        assertNotNull(g.sprite("example_menu", "tile_hover"));
        assertNotNull(g.sprite("example_board", "btn"));
        UiDefinition hud = registry.get("example_hud");
        for (UiDefinition.ElementDef e : hud.elements) {
            int y = HudLayout.round(e.y), w = HudLayout.round(e.w), h = HudLayout.round(e.h);
            switch (e.type) {
                case "image" -> assertNotNull(g.hud(GlyphRegistry.spriteKey(hud.id, e.sprite), w, h, y), "hud glyph " + e.id);
                case "text" -> assertNotNull(g.hudTextFont(HudLayout.labelY(y, h)), "hud text font " + e.id);
                case "progress" -> {
                    assertNotNull(g.hud("#pixel", w, h, y), "progress track " + e.id);
                    for (int p = 1; p <= w; p <<= 1) assertNotNull(g.hud("#pixel", p, h, y), "progress piece " + p);
                }
                default -> { }
            }
        }
    }

    @Test
    void shiftAddsUp() {
        Map<Character, Integer> adv = new HashMap<>();
        for (int i = 0; i <= 11; i++) {
            adv.put(GlyphRegistry.negSpace(i), -(1 << i));
            adv.put(GlyphRegistry.posSpace(i), 1 << i);
        }
        for (int px : new int[]{-1000, -137, -1, 0, 1, 5, 64, 240, 777}) {
            int sum = 0;
            for (char c : GlyphRegistry.shift(px).toCharArray()) sum += adv.get(c);
            assertEquals(px, sum, "shift " + px);
        }
    }

    @Test
    void easingEndpoints() {
        for (String e : List.of("linear", "ease-in", "ease-out", "ease-in-out", "back", "bounce")) {
            assertEquals(0, AnimationPlayer.ease(e, 0), 1e-6, e);
            assertEquals(1, AnimationPlayer.ease(e, 1), 1e-6, e);
        }
        UiDefinition.KeyDef a = new UiDefinition.KeyDef(), b = new UiDefinition.KeyDef();
        a.t = 0; a.v = 10; b.t = 10; b.v = 20;
        assertEquals(15, AnimationPlayer.sample(List.of(a, b), 5), 1e-9);
        assertEquals(20, AnimationPlayer.sample(List.of(a, b), 99), 1e-9);
    }
}
