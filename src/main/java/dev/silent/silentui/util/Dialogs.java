package dev.silent.silentui.util;

import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.dialog.DialogResponseView;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.input.TextDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Small form builder on top of Paper's dialog API (1.21.6+): text fields, toggles and dropdowns
 * with a Save / Cancel pair. Answers come back on the main thread as a key -> value map.
 */
public final class Dialogs {

    private Dialogs() {}

    public static final class Form {
        final Component title;
        final List<DialogInput> inputs = new ArrayList<>();
        final List<DialogBody> body = new ArrayList<>();
        final Map<String, String> kinds = new LinkedHashMap<>();
        String saveLabel = "Save";

        public Form(String title) {
            this.title = Text.mm(title);
        }

        public Form note(String text) {
            body.add(DialogBody.plainMessage(Text.mm(text)));
            return this;
        }

        public Form text(String key, String label, String initial, int maxLength) {
            inputs.add(DialogInput.text(key, Text.mm(label)).width(300).initial(initial == null ? "" : clip(initial, maxLength))
                    .maxLength(maxLength).build());
            kinds.put(key, "text");
            return this;
        }

        public Form area(String key, String label, String initial) {
            inputs.add(DialogInput.text(key, Text.mm(label)).width(300).initial(initial == null ? "" : clip(initial, 1024)).maxLength(1024)
                    .multiline(TextDialogInput.MultilineOptions.create(6, 60)).build());
            kinds.put(key, "text");
            return this;
        }

        public Form toggle(String key, String label, boolean initial) {
            inputs.add(DialogInput.bool(key, Text.mm(label)).initial(initial).build());
            kinds.put(key, "bool");
            return this;
        }

        public Form choice(String key, String label, List<String> options, String initial) {
            List<SingleOptionDialogInput.OptionEntry> entries = new ArrayList<>();
            boolean found = false;
            for (String o : options) {
                boolean sel = o.equals(initial) && !found;
                found |= sel;
                entries.add(SingleOptionDialogInput.OptionEntry.create(o.isEmpty() ? "_none" : o, Component.text(o.isEmpty() ? "none" : o), sel));
            }
            if (entries.isEmpty()) entries.add(SingleOptionDialogInput.OptionEntry.create("_none", Component.text("none"), true));
            inputs.add(DialogInput.singleOption(key, Text.mm(label), entries).build());
            kinds.put(key, "choice");
            return this;
        }

        public Form saveLabel(String label) {
            this.saveLabel = label;
            return this;
        }

        private static String clip(String s, int max) {
            return s.length() > max ? s.substring(0, max) : s;
        }
    }

    /** Shows the form; returns false if this server has no dialog support (caller should fall back to chat). */
    public static boolean show(Plugin plugin, Player player, Form form, Consumer<Map<String, String>> onSave) {
        try {
            ClickCallback.Options once = ClickCallback.Options.builder().uses(1).lifetime(Duration.ofMinutes(15)).build();
            ActionButton save = ActionButton.create(Text.mm("<white>" + form.saveLabel), null, 140,
                    DialogAction.customClick((view, audience) -> {
                        Map<String, String> values = read(view, form);
                        Bukkit.getScheduler().runTask(plugin, () -> onSave.accept(values));
                    }, once));
            ActionButton cancel = ActionButton.create(Text.mm("<gray>Cancel"), null, 100, null);
            Dialog dialog = Dialog.create(b -> b.empty()
                    .base(DialogBase.builder(form.title).canCloseWithEscape(true).body(form.body).inputs(form.inputs).build())
                    .type(DialogType.confirmation(save, cancel)));
            player.showDialog(dialog);
            return true;
        } catch (LinkageError | RuntimeException ex) {
            return false;
        }
    }

    private static Map<String, String> read(DialogResponseView view, Form form) {
        Map<String, String> out = new LinkedHashMap<>();
        for (Map.Entry<String, String> k : form.kinds.entrySet()) {
            String key = k.getKey();
            switch (k.getValue()) {
                case "bool" -> {
                    Boolean b = view.getBoolean(key);
                    out.put(key, String.valueOf(b != null && b));
                }
                default -> {
                    String t = view.getText(key);
                    if (t != null && t.equals("_none")) t = "";
                    out.put(key, t == null ? "" : t);
                }
            }
        }
        return out;
    }
}
