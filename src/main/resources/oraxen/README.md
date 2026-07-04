# Oraxen Assets for Smoke & Salt

This directory contains the bundled Oraxen assets for Smoke & Salt. When the
Oraxen module is active (`/sas modules`), the plugin deploys these files into
Oraxen with version tracking, backups, and managed texture updates.

## Structure

```text
oraxen/
|-- asset-manifest.properties     # asset-version + sha256 hashes
|-- items/                        # Oraxen item YAMLs
`-- pack/
    |-- models/                   # custom models
    `-- textures/                 # custom textures
```

Bundled item textures live under `pack/textures/sas/`. Rice crop texture stages
are bundled under `pack/textures/sas/crops/` and are referenced by the internal
`sas_reis_crop_0` through `sas_reis_crop_3` display items.

## Adding a Custom Item Texture

1. Add a PNG texture under `pack/textures/sas/...`.
2. Define an Oraxen item in `items/<file>.yml` with a stable ID.
3. Increase `asset-version` in `asset-manifest.properties`.
4. Reference the Oraxen ID from a Smoke & Salt item via `provider-id`.
5. Run `/oraxen reload` and send/rebuild the pack as usual.

The deployer backs up managed files before overwriting them. Server-owned assets
are preserved when they no longer match the previously managed default.
