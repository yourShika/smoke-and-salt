package de.yourshika.smokeandsalt.module;

/**
 * Ein externes, optionales Modul (Hook).
 *
 * <p>Module laden automatisch, sobald das benoetigte Plugin installiert und das
 * Modul in der Config aktiviert ist (Standard: aktiviert). Fehlt das Plugin,
 * bleibt das Modul still inaktiv - das Plugin laeuft vollstaendig eigenstaendig
 * weiter. Der Live-Status ist ueber {@code /sas modules} einsehbar.</p>
 */
public interface Module {

    /** Stabiler Config-/intern-Schluessel (z.B. {@code oraxen}). */
    String id();

    /** Anzeigename fuer die GUI (z.B. {@code Oraxen}). */
    String displayName();

    /** Kurze Beschreibung des Modulzwecks. */
    String description();

    /** Name des benoetigten Bukkit-Plugins (fuer die Erkennung). */
    String requiredPlugin();

    /** Ist dieses Modul zwingend erforderlich fuer eine bestimmte Funktion? */
    boolean required();

    /** Ist das benoetigte Plugin auf dem Server installiert? */
    boolean isPluginPresent();

    /** Ist das Modul einzeln in der Config aktiviert? */
    boolean isEnabledInConfig();

    /** Laeuft das Modul aktuell? */
    boolean isActive();

    /** Versucht, das Modul zu aktivieren. Wirft bei Fehlern. */
    void enable() throws Throwable;

    /** Faehrt das Modul wieder herunter (idempotent). */
    void disable();
}
