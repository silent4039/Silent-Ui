package dev.silent.silentui;

import dev.silent.silentui.assets.AssetManager;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Downloads the real client jar and renders a contact sheet of icons (CI only: SILENTUI_RENDER=1).
 * The sheet is pushed to the ci-output branch so the renderer can be checked by eye.
 */
class IconSheetTest {

    private static final List<String> IDS = List.of(
            "grass_block", "stone", "oak_planks", "crafting_table", "furnace", "oak_log", "glass", "oak_leaves",
            "oak_stairs", "stone_slab", "oak_fence", "cobblestone_wall", "hopper", "anvil", "chest", "torch",
            "diamond_sword", "diamond", "apple", "bow", "leather_helmet", "potion", "golden_apple", "ender_pearl",
            "player_head", "shield", "red_bed", "lantern", "bookshelf", "tnt", "piston", "observer",
            "dirt_path", "farmland", "cactus", "pumpkin", "carved_pumpkin", "melon", "redstone_lamp", "beacon",
            "end_portal_frame", "enchanting_table", "brewing_stand", "cauldron", "oak_door", "oak_trapdoor", "rail", "ladder");

    @Test
    void renderSheet() throws Exception {
        Assumptions.assumeTrue("1".equals(System.getenv("SILENTUI_RENDER")), "render check only runs in CI");
        Path cache = Path.of("target", "mc-cache");
        AssetManager assets = new AssetManager(Logger.getLogger("test"), cache, System.getenv().getOrDefault("MC_VERSION", "1.21.11"));
        assets.load();
        assertEquals(AssetManager.State.READY, assets.state(), assets.error());
        assertTrue(assets.items().size() > 1000, "items: " + assets.items().size());
        int cell = 96, cols = 8, rows = (IDS.size() + cols - 1) / cols + 1;
        BufferedImage sheet = new BufferedImage(cols * cell, rows * cell, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(new Color(0x8b8b8b));
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
        int i = 0;
        for (String id : IDS) {
            byte[] png = assets.icon(id, 64);
            int x = (i % cols) * cell, y = (i / cols) * cell;
            g.setColor(new Color(0xc6c6c6));
            g.fillRect(x + 2, y + 2, cell - 4, cell - 4);
            if (png != null) g.drawImage(ImageIO.read(new ByteArrayInputStream(png)), x + 16, y + 6, null);
            g.setColor(png == null ? Color.RED : Color.BLACK);
            g.drawString(id, x + 4, y + cell - 8);
            i++;
        }
        BufferedImage steve = assets.texture("minecraft:entity/player/wide/steve");
        assertNotNull(steve);
        int y = (rows - 1) * cell;
        g.drawImage(assets.renderer().renderHead(steve, 64), 16, y + 6, null);
        g.drawImage(assets.renderer().renderBody(steve, false, 88), cell + 24, y + 2, null);
        g.dispose();
        Files.createDirectories(Path.of("target"));
        ImageIO.write(sheet, "png", Path.of("target", "icon-sheet.png").toFile());
        assertTrue(assets.textures("block", "stone").size() > 5);
    }
}
