# Oraxen-Assets für Smoke & Salt

Dieses Verzeichnis ist die **Ablage für die mitgelieferten Oraxen-Assets** von
Smoke & Salt. Sobald das Oraxen-Modul aktiv ist (`/sas modules`), stellt der
Asset-Deployer alle hier abgelegten Dateien automatisch in Oraxen bereit –
versioniert, mit Backup und ohne eigene Server-Texturen zu überschreiben.

## Struktur

```
oraxen/
├── asset-manifest.properties     # asset-version + sha256-Hashes (Deployer-State)
├── items/                        # Oraxen-Item-YAMLs (nach plugins/Oraxen/items/)
└── pack/
    ├── models/                   # Custom-Modelle (nach plugins/Oraxen/pack/models/)
    └── textures/                 # Custom-Texturen (nach plugins/Oraxen/pack/textures/)
```

Aktuell werden **bewusst keine Beispiel-Items mitgeliefert** – konkrete Zutaten,
Gerichte und Seeds kommen später. Sobald ein Item eine `provider-id` (z. B.
`sas_item_xyz`) trägt und ein passendes Oraxen-Item unter `items/` existiert,
übernimmt Smoke & Salt automatisch dessen Modell/Textur.

## Neues Custom-Item mit Textur anlegen

1. Textur als PNG unter `pack/textures/sas/...` ablegen.
2. Ein Oraxen-Item unter `items/<datei>.yml` mit einer stabilen ID definieren
   (z. B. `sas_item_xyz`) und darin die Textur referenzieren.
3. `asset-version` in `asset-manifest.properties` erhöhen.
4. In der Plugin-`config.yml` beim jeweiligen Item/Seed die `provider-id` auf die
   Oraxen-ID setzen.
5. Nach dem Deploy einmalig `/oraxen reload` ausführen und das Pack an die Spieler
   senden (`/oraxen pack send @a`).

Der Deployer legt vor jedem Überschreiben ein Backup unter
`plugins/Smoke & Salt/AssetBackups/` an. Eigene Server-Texturen bleiben erhalten,
solange sie nicht mehr dem zuletzt verwalteten Default entsprechen.
