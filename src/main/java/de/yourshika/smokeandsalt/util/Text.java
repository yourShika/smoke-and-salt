package de.yourshika.smokeandsalt.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

/**
 * Kleine Helfer rund um MiniMessage und Adventure-Components fuer GUIs und Items.
 */
public final class Text {

    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private Text() {
    }

    /** Deserialisiert MiniMessage und deaktiviert das kursive Standard-Item-Rendering. */
    public static Component line(String miniMessage) {
        return MINI.deserialize(miniMessage).decoration(TextDecoration.ITALIC, false);
    }

    public static MiniMessage mini() {
        return MINI;
    }
}
