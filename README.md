# 🍲 Smoke & Salt

> *A vanilla-faithful cooking expansion for Minecraft — crafted with fire, patience, and a pinch of magic.*

<div align="center">

![Minecraft](https://img.shields.io/badge/Minecraft-26.1.2-62b47a?style=flat-square&logo=minecraft&logoColor=white)
![Paper](https://img.shields.io/badge/Platform-Paper%2FSpigot-f7a41d?style=flat-square)
![Java](https://img.shields.io/badge/Java-25-ED8B00?style=flat-square&logo=openjdk&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)
![Status](https://img.shields.io/badge/Status-In%20Development-orange?style=flat-square)

</div>

---

## ✨ Was ist Smoke & Salt?

**Smoke & Salt** bringt ein tiefes, immersives Koch-System auf deinen Server –
ohne den Vanilla-Charme zu brechen. Kein aufgeblähtes GUI-Gewusel, keine
Magie-Maschinen. Nur ein Kessel, ein Feuer und die richtigen Zutaten.

Diese Version liefert die **Koch-Funktionen** (Mechaniken, Partikel, Timing,
Stationen). Konkrete **Rezepte, Zutaten und Gerichte** kommen bewusst später und
werden komplett über die `config.yml` definiert – ganz ohne Code.

---

## 🔥 Funktionen

| Station | Interaktion | Effekt |
|---------|-------------|--------|
| 🪵 **Smoker** | Rechtsklick auf einen Smoker mit Zutat | Zeitliches Räuchern mit Rauch-Partikeln |
| 🔥 **Lagerfeuer** | Rechtsklick auf ein brennendes Lagerfeuer | Garen mit Rauch-Partikeln |
| 🫧 **Wasserkessel** | Item in kochendes Wasser werfen | Item schwebt & kocht (Kochen, Waschen, Brühen, Suppe) |
| 🌋 **Lavakessel** | Item hineinwerfen | Vorsichtige Frittier-/Bratstation – nur bestimmte Items |
| 🪓 **Schneiden** | Axt (Haupthand) + Zutat (Zweithand), Rechtsklick | Schnitt-Effekt + Partikel |
| ⛓️ **Ketten** | Rechtsklick auf eine Kette | Aufhängung für Kessel-Behang / Räucherware |
| 🌱 **Custom Seeds** | Anpflanzen auf Ackerland | Wächst zur Ernte; Drops von Gras & Komposter |
| 🪜 **Sequentielles Kochen** | mehrstufige Rezepte | Ergebnis einer Stufe ist Zutat der nächsten |

**Kochendes Wasser** entsteht nur, wenn direkt unter einem Wasserkessel eine
Wärmequelle liegt (Lagerfeuer, Feuer, Lava oder Magmablock) – der Kessel blubbert
dann sichtbar.

Alle Custom-Items, Seeds und Ergebnisse können über das **Oraxen-Modul**
eigene Texturen erhalten.

---

## 🎮 Befehle

| Befehl | Beschreibung | Permission |
|--------|--------------|------------|
| `/sas` · `/sas help` | Schön gestaltete Übersichts-GUI | `smokeandsalt.command.help` |
| `/sas recipes` | Rezept-Übersicht (GUI) | `smokeandsalt.command.recipes` |
| `/sas version` | Version, Server, aktive Hooks | `smokeandsalt.command.version` |
| `/sas give [Spieler] <id> [Menge]` | Custom-Item/Seed ausgeben (GUI ohne Args) | `smokeandsalt.admin.give` |
| `/sas modules` | Externe Module live verwalten (GUI) | `smokeandsalt.admin.modules` |
| `/sas assets [status\|redeploy]` | Oraxen-Assets prüfen/erneut ausrollen | `smokeandsalt.admin.assets` |
| `/sas update` | Neueste Release von GitHub laden | `smokeandsalt.admin.update` |
| `/sas reload` | Config, Nachrichten, Items, Rezepte, Module neu laden | `smokeandsalt.admin.reload` |

Aliase: `/smokeandsalt`, `/smokesalt`. Fast alles ist auch **per GUI** bedienbar.

### Permissions

- `smokeandsalt.*` – alles (Standard: OP)
- `smokeandsalt.use` – Koch-Funktionen benutzen (Standard: an)
- `smokeandsalt.seed.plant` – Custom-Seeds anpflanzen (Standard: an)
- `smokeandsalt.command.*` – öffentliche Unterbefehle (Standard: an)
- `smokeandsalt.admin.*` – Admin-Funktionen (Standard: OP)

---

## 🧩 Module (Hooks)

Über `/sas modules` (oder die `config.yml` unter `hooks.modules`) lassen sich
externe Hooks **live** ab-/anschalten. Sie aktivieren sich automatisch, sobald
das jeweilige Plugin installiert ist – fehlt es, bleibt das Modul still inaktiv
und das Plugin läuft vollständig eigenständig weiter.

| Modul | Zweck | Erforderlich |
|-------|-------|--------------|
| **Oraxen** | Custom-Modelle/Texturen für alle Items, Seeds und Ergebnisse | optional (nötig für Custom-Texturen) |
| **PlaceholderAPI** | Platzhalter `%sas_version%`, `%sas_items%`, `%sas_recipes%`, `%sas_active_cooks%` | optional |

### Oraxen-Assets

Ist das Oraxen-Modul aktiv, stellt der Asset-Deployer die mitgelieferten
Item-YAMLs und Texturen **versioniert und mit Backup** in Oraxen bereit. Status
und Redeploy laufen über `/sas assets`. Details siehe
[`src/main/resources/oraxen/README.md`](src/main/resources/oraxen/README.md).

---

## ⚙️ Konfiguration

Alle Inhalte werden über die `config.yml` definiert (standardmäßig **leer**):

- `items:` – Custom-Items (Zutaten, Werkzeuge, Ergebnisse)
- `seeds.definitions:` – Custom-Seeds inkl. Wachstum & Drop-Chancen
- `recipes:` – Koch-Rezepte pro Station (inkl. sequentieller Ketten)

Jeder Abschnitt enthält ein dokumentiertes Schema als Kommentar. Zusätzlich:
globale Schalter pro Station, Partikel/Sounds, Welten-Whitelist/Blacklist und die
Sprache (`de` / `en`).

---

## 📋 Voraussetzungen

- Paper oder Spigot `26.1.2`
- Java `25+`
- *(optional)* Oraxen · PlaceholderAPI

---

## 🛠️ Bauen

```bash
mvn clean package
```

Das fertige Plugin liegt danach unter `target/Smoke-and-Salt-<version>.jar`.
GitHub Actions bauen jeden Push (`build.yml`) und veröffentlichen bei einem
`v*`-Tag automatisch ein Release (`release.yml`) – von dem `/sas update` die
neueste JAR zieht.

---

## 🗺️ Roadmap

- [x] Koch-Engine (Stationen, Partikel, Timing, sequentiell)
- [x] Smoker · Lagerfeuer · Wasserkessel · Lavakessel · Schneiden
- [x] Custom Seeds (Anpflanzen, Gras-/Komposter-Drops)
- [x] Ketten als Aufhängung
- [x] Modul-System (Oraxen, PlaceholderAPI) + Oraxen-Asset-Deployer
- [x] GUIs, Befehle, Permissions, Self-Updater
- [ ] Konkrete Rezepte, Zutaten & Gerichte
- [ ] Mitgelieferte Oraxen-Texturen

---

## 📄 Lizenz

MIT — siehe [`LICENSE`](LICENSE).

---

<div align="center">
  <sub>Made with 🔥 and a wooden spoon · <i>Smoke & Salt</i></sub>
</div>
