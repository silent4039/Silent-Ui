package dev.silent.silentui;

import dev.silent.silentui.model.UiDefinition;
import dev.silent.silentui.model.UiRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class UiRegistryTest {
    @TempDir Path folder;

    @Test void rejectsMalformedDefinitionsWithoutCrashing() {
        assertNotNull(UiRegistry.check(null));
        UiDefinition def = new UiDefinition();
        def.elements.add(null);
        assertEquals("null element", UiRegistry.check(def));
        def.elements.clear();
        def.animations.add(null);
        assertEquals("null animation", UiRegistry.check(def));
    }

    @Test void rejectsNonFiniteElementAndAnimationValues() {
        UiDefinition def = new UiDefinition();
        UiDefinition.ElementDef element = new UiDefinition.ElementDef();
        element.x = Double.NaN;
        def.elements.add(element);
        assertTrue(UiRegistry.check(def).contains("non-finite"));
        def.elements.clear();
        UiDefinition.AnimationDef animation = new UiDefinition.AnimationDef();
        UiDefinition.TrackDef track = new UiDefinition.TrackDef();
        UiDefinition.KeyDef key = new UiDefinition.KeyDef();
        key.v = Double.POSITIVE_INFINITY;
        track.keys.add(key); animation.tracks.add(track); def.animations.add(animation);
        assertEquals("invalid animation keyframe", UiRegistry.check(def));
    }

    @Test void rejectsAmbiguousScriptIds() {
        UiDefinition def = new UiDefinition();
        for (int i = 0; i < 2; i++) {
            UiDefinition.NodeDef node = new UiDefinition.NodeDef();
            node.id = "event"; def.script.nodes.add(node);
        }
        assertEquals("duplicate script node id event", UiRegistry.check(def));
    }

    @Test void savesReplacementsWithoutLeavingTemporaryFiles() throws Exception {
        UiRegistry registry = new UiRegistry(folder, Logger.getLogger("test"));
        UiDefinition def = new UiDefinition(); def.id = "my_menu"; def.name = "Before";
        registry.save(def); def.name = "After"; registry.save(def);
        registry.load(); assertEquals("After", registry.get("my_menu").name);
        try (var files = Files.list(folder)) { assertEquals(1, files.count()); }
    }

    @Test void invalidSaveKeepsThePreviousDocument() throws Exception {
        UiRegistry registry = new UiRegistry(folder, Logger.getLogger("test"));
        UiDefinition def = new UiDefinition(); def.id = "menu"; def.name = "Original";
        Path file = registry.save(def); String original = Files.readString(file);
        def.elements.add(null);
        assertThrows(java.io.IOException.class, () -> registry.save(def));
        assertEquals(original, Files.readString(file));
    }

    @Test void chestDimensionsAndPathAreNormalized() throws Exception {
        UiDefinition def = new UiDefinition(); def.id = "../../Inventory"; def.type = "CHEST";
        def.settings.rows = 3; def.settings.guiTop = 12;
        UiRegistry registry = new UiRegistry(folder, Logger.getLogger("test"));
        assertEquals(folder.resolve("inventory.json"), registry.save(def));
        assertEquals(176, def.canvas.width); assertEquals(180, def.canvas.height);
    }
}
