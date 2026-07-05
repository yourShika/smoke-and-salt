package de.yourshika.smokeandsalt.update;

import de.yourshika.smokeandsalt.SmokeAndSalt;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.yourshika.smokeandsalt.config.MessageManager.ph;

/**
 * Self-Updater ueber die GitHub-Releases-API.
 *
 * <p>{@code /sas update} laedt die neueste Release-JAR und legt sie im
 * {@code plugins/update/}-Ordner ab. Bukkit/Paper uebernimmt sie automatisch beim
 * naechsten Server-Neustart. Gespeicherte Config und Assets bleiben erhalten.</p>
 */
public final class GitHubUpdater {

    private static final String API_LATEST =
            "https://api.github.com/repos/yourShika/smoke-and-salt/releases/latest";

    private static final Pattern TAG = Pattern.compile("\"tag_name\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern JAR_URL = Pattern.compile(
            "\"browser_download_url\"\\s*:\\s*\"([^\"]+\\.jar)\"");

    private final SmokeAndSalt plugin;

    public GitHubUpdater(SmokeAndSalt plugin) {
        this.plugin = plugin;
    }

    /** Startet die Update-Pruefung asynchron und meldet das Ergebnis an {@code sender}. */
    public void checkAndUpdate(CommandSender sender) {
        plugin.messages().send(sender, "update.checking");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> run(sender));
    }

    /** Prueft still (nur lesen) und weist {@code sender} auf ein Update hin, falls vorhanden. */
    public void notifyIfOutdated(CommandSender sender) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(10))
                        .followRedirects(HttpClient.Redirect.NORMAL).build();
                HttpRequest request = HttpRequest.newBuilder(URI.create(API_LATEST))
                        .header("User-Agent", "SmokeAndSalt-Updater")
                        .header("Accept", "application/vnd.github+json")
                        .timeout(Duration.ofSeconds(15)).GET().build();
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) return;
                Matcher tag = TAG.matcher(response.body());
                if (!tag.find()) return;
                String latest = tag.group(1).replaceFirst("^[vV]", "");
                String current = plugin.getPluginMeta().getVersion();
                if (!latest.equalsIgnoreCase(current)) {
                    Bukkit.getScheduler().runTask(plugin, () ->
                            plugin.messages().send(sender, "update.available", ph("version", latest)));
                }
            } catch (Throwable ignored) {
            }
        });
    }

    private void run(CommandSender sender) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(15))
                    .followRedirects(HttpClient.Redirect.NORMAL)
                    .build();

            HttpRequest request = HttpRequest.newBuilder(URI.create(API_LATEST))
                    .header("User-Agent", "SmokeAndSalt-Updater")
                    .header("Accept", "application/vnd.github+json")
                    .timeout(Duration.ofSeconds(20))
                    .GET()
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                fail(sender, "Noch kein Release veroeffentlicht.");
                return;
            }
            if (response.statusCode() != 200) {
                fail(sender, "GitHub API HTTP " + response.statusCode());
                return;
            }
            String body = response.body();

            Matcher tagMatcher = TAG.matcher(body);
            Matcher urlMatcher = JAR_URL.matcher(body);
            if (!tagMatcher.find()) {
                fail(sender, "Keine Versionsangabe gefunden.");
                return;
            }
            String latest = tagMatcher.group(1).replaceFirst("^[vV]", "");
            String current = plugin.getPluginMeta().getVersion();

            if (latest.equalsIgnoreCase(current)) {
                main(() -> plugin.messages().send(sender, "update.up-to-date", ph("version", current)));
                return;
            }
            if (!urlMatcher.find()) {
                fail(sender, "Keine JAR im Release gefunden.");
                return;
            }
            String jarUrl = urlMatcher.group(1);
            main(() -> plugin.messages().send(sender, "update.downloading", ph("version", latest)));

            download(client, jarUrl);
            main(() -> plugin.messages().send(sender, "update.success", ph("version", latest)));
        } catch (Throwable t) {
            fail(sender, t.getClass().getSimpleName() + ": " + t.getMessage());
        }
    }

    private void download(HttpClient client, String jarUrl) throws Exception {
        File updateFolder = new File(plugin.getDataFolder().getParentFile(), "update");
        if (!updateFolder.exists() && !updateFolder.mkdirs()) {
            throw new IllegalStateException("Update-Ordner konnte nicht erstellt werden.");
        }
        // Dateiname MUSS dem aktuellen Plugin-JAR entsprechen, damit Bukkit/Paper
        // beim Neustart automatisch ersetzt.
        File target = new File(updateFolder, plugin.pluginJarFile().getName());

        HttpRequest request = HttpRequest.newBuilder(URI.create(jarUrl))
                .header("User-Agent", "SmokeAndSalt-Updater")
                .timeout(Duration.ofMinutes(2))
                .GET()
                .build();

        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Download HTTP " + response.statusCode());
        }
        try (InputStream in = response.body()) {
            Files.copy(in, Path.of(target.toURI()), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void fail(CommandSender sender, String error) {
        main(() -> plugin.messages().send(sender, "update.failed", ph("error", error)));
    }

    private void main(Runnable runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable);
    }
}
