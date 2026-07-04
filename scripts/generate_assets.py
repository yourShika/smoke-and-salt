#!/usr/bin/env python3
"""Generiert die 16x16 Vanilla-Style-Texturen, die Oraxen-Item-Definitionen und
das Asset-Manifest fuer Smoke & Salt.

Aufruf:  python scripts/generate_assets.py
"""
import hashlib
import os
from PIL import Image

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OX = os.path.join(ROOT, "src", "main", "resources", "oraxen")
TEX_DIR = os.path.join(OX, "pack", "textures", "sas")
ITEMS_DIR = os.path.join(OX, "items")
MANIFEST = os.path.join(OX, "asset-manifest.properties")
ASSET_VERSION = "3"

# ---------------------------------------------------------------------------
#  Zeichen-Primitive auf einer 16x16 RGBA-Leinwand
# ---------------------------------------------------------------------------

def canvas():
    return Image.new("RGBA", (16, 16), (0, 0, 0, 0))

def put(img, x, y, c):
    if 0 <= x < 16 and 0 <= y < 16 and c is not None:
        img.putpixel((x, y), c)

def rect(img, x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            put(img, x, y, c)

def disc(img, cx, cy, r, c):
    for y in range(16):
        for x in range(16):
            if (x - cx) ** 2 + (y - cy) ** 2 <= r * r:
                put(img, x, y, c)

def outline(img, c):
    """Zeichnet eine 1px-Outline um alle undurchsichtigen Pixel (nach aussen)."""
    src = img.copy()
    for y in range(16):
        for x in range(16):
            if src.getpixel((x, y))[3] != 0:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < 16 and 0 <= ny < 16 and src.getpixel((nx, ny))[3] != 0:
                    put(img, x, y, c)
                    break

A = 255
def C(r, g, b, a=A):
    return (r, g, b, a)

def _mul(c, f):
    return (max(0, min(255, int(c[0] * f))),
            max(0, min(255, int(c[1] * f))),
            max(0, min(255, int(c[2] * f))), c[3])

def apply_shading(img):
    """Beveled depth: light the top-left facing edges, darken the bottom-right.
    Gives the flat pixel-art a subtle volume without redrawing each sprite."""
    src = img.copy()
    def opaque(x, y):
        return 0 <= x < 16 and 0 <= y < 16 and src.getpixel((x, y))[3] != 0
    for y in range(16):
        for x in range(16):
            p = src.getpixel((x, y))
            if p[3] == 0:
                continue
            top_open = not opaque(x, y - 1)
            left_open = not opaque(x - 1, y)
            bot_open = not opaque(x, y + 1)
            right_open = not opaque(x + 1, y)
            if top_open or left_open:
                img.putpixel((x, y), _mul(p, 1.16))     # highlight rim
            elif bot_open or right_open:
                img.putpixel((x, y), _mul(p, 0.80))      # shadow rim
            elif (x + y) % 5 == 0:
                img.putpixel((x, y), _mul(p, 1.05))      # gentle interior speckle

# ---------------------------------------------------------------------------
#  Einzelne Item-Texturen
# ---------------------------------------------------------------------------

def t_teig(i):
    disc(i, 8, 9, 5, C(232, 216, 168)); disc(i, 6, 7, 3, C(240, 226, 184))
    put(i, 6, 8, C(214, 196, 150)); put(i, 10, 10, C(214, 196, 150))

def t_spiegelei(i):
    disc(i, 8, 9, 6, C(245, 245, 236)); disc(i, 9, 6, 4, C(248, 248, 240))
    disc(i, 8, 9, 3, C(246, 190, 60)); disc(i, 8, 9, 2, C(255, 214, 92))

def t_rotebeete_chips(i):
    # kleiner Stapel runder Chips
    for (x, y, r) in [(6, 10, 3), (10, 10, 3), (8, 7, 3), (8, 11, 2)]:
        disc(i, x, y, r, C(170, 45, 72))
    for (x, y) in [(6, 10), (10, 10), (8, 7)]:
        put(i, x, y, C(120, 26, 48))
        put(i, x - 1, y - 1, C(198, 70, 96))

def t_geroestete_karotte(i):
    for dy in range(3, 14):
        w = max(0, (13 - dy))
        rect(i, 8 - w // 2, dy, 8 + (w + 1) // 2, dy, C(228, 120, 42))
    rect(i, 7, 12, 9, 13, C(196, 92, 28))
    rect(i, 7, 1, 9, 3, C(70, 150, 52)); put(i, 6, 2, C(70, 150, 52)); put(i, 10, 2, C(70, 150, 52))

def t_pommes(i):
    rect(i, 4, 9, 11, 14, C(206, 60, 48)); rect(i, 4, 9, 11, 9, C(230, 80, 66))
    for x in (5, 7, 9):
        rect(i, x, 2, x + 1, 9, C(240, 205, 90)); put(i, x, 2, C(250, 224, 120))

def t_marshmallow(i):
    rect(i, 5, 4, 11, 13, C(252, 240, 244)); rect(i, 5, 4, 11, 5, C(255, 250, 252))
    rect(i, 5, 8, 11, 9, C(250, 205, 218)); rect(i, 6, 13, 10, 13, C(232, 214, 220))

def t_stockbrot(i):
    for k in range(6):
        put(i, 3 + k, 13 - k, C(150, 110, 60))
    disc(i, 10, 6, 4, C(198, 150, 80)); disc(i, 11, 5, 2, C(150, 100, 46))
    put(i, 9, 5, C(226, 182, 120))

def t_nudeln(i):
    # runder Nudel-Haufen mit Straehnen
    disc(i, 8, 9, 5, C(238, 224, 158))
    disc(i, 7, 7, 3, C(246, 232, 172))
    for y in (6, 8, 10, 12):
        for x in range(3, 14):
            if (x + y // 2) % 2 == 0 and (x - 8) ** 2 + (y - 9) ** 2 <= 26:
                put(i, x, y, C(210, 188, 116))

def t_kaese(i):
    # gelber Keil mit Loechern
    for y in range(3, 14):
        rect(i, 3, y, 3 + (y - 3), y, C(244, 204, 72))
    for (x, y) in [(5, 8), (7, 11), (8, 6)]:
        put(i, x, y, C(222, 176, 48)); put(i, x + 1, y, C(222, 176, 48))
    for y in range(3, 14):
        put(i, 3, y, C(206, 162, 40))

def t_sauce(i):
    # Glas/Napf mit roter Sauce
    rect(i, 4, 5, 11, 13, C(178, 40, 40)); rect(i, 4, 5, 11, 6, C(206, 60, 60))
    rect(i, 4, 4, 11, 4, C(150, 26, 26)); rect(i, 3, 13, 12, 14, C(120, 20, 20))
    put(i, 6, 8, C(210, 70, 70)); put(i, 9, 10, C(210, 70, 70))

def _bun(i, top):
    rect(i, 3, top, 12, top + 2, C(206, 150, 84)); rect(i, 3, top, 12, top, C(224, 176, 110))

def t_burger(i):
    _bun(i, 2)
    rect(i, 3, 5, 12, 6, C(96, 158, 60))       # Salat
    rect(i, 3, 7, 12, 9, C(140, 78, 52))       # Fleisch
    _bun(i, 10); rect(i, 3, 13, 12, 13, C(150, 100, 45))
    for x in (5, 8, 10):
        put(i, x, 2, C(240, 226, 170))

def t_cheeseburger(i):
    _bun(i, 2)
    rect(i, 3, 7, 12, 8, C(140, 78, 52))       # Fleisch
    rect(i, 2, 9, 6, 11, C(244, 204, 72))      # Kaese haengt heraus
    rect(i, 3, 9, 12, 9, C(244, 204, 72))
    _bun(i, 10); rect(i, 3, 13, 12, 13, C(150, 100, 45))
    for x in (5, 8, 10):
        put(i, x, 2, C(240, 226, 170))

def t_chicken_nuggets(i):
    for (x, y) in [(5, 5), (9, 6), (6, 10), (10, 10)]:
        disc(i, x, y, 2, C(226, 176, 96)); put(i, x, y, C(198, 146, 70))
        put(i, x + 1, y - 1, C(240, 200, 130))

def t_schaschlik(i):
    for k in range(11):
        put(i, 3 + k, 12 - k, C(150, 110, 60))          # Spiess
    disc(i, 6, 9, 2, C(150, 80, 55))                     # Fleisch
    disc(i, 9, 6, 2, C(228, 120, 42))                    # Karotte
    disc(i, 11, 4, 2, C(150, 80, 55))
    put(i, 6, 9, C(110, 55, 35)); put(i, 11, 4, C(110, 55, 35))

def t_ofenkartoffel_sourcream(i):
    disc(i, 8, 9, 6, C(178, 132, 74)); disc(i, 8, 9, 5, C(206, 158, 96))
    rect(i, 5, 6, 11, 8, C(120, 84, 46))                 # Schnitt
    rect(i, 6, 6, 10, 7, C(246, 246, 240))               # Sauerrahm
    put(i, 7, 5, C(96, 150, 60)); put(i, 9, 5, C(96, 150, 60))  # Schnittlauch

def t_spaghetti(i):
    rect(i, 3, 9, 12, 13, C(150, 95, 55)); rect(i, 3, 9, 12, 9, C(184, 122, 74))  # Teller
    for x in range(4, 12):
        put(i, x, 6 + (x % 3), C(240, 225, 162))
        put(i, x, 5 + (x % 2), C(226, 208, 140))
    put(i, 7, 5, C(190, 40, 40)); put(i, 9, 6, C(190, 40, 40))  # Sauce-Kleckse

def t_misosuppe(i):
    rect(i, 3, 8, 12, 13, C(150, 95, 55)); rect(i, 3, 8, 12, 8, C(184, 122, 74))
    rect(i, 4, 6, 11, 8, C(198, 138, 58))                # Bruehe
    put(i, 6, 7, C(70, 130, 70)); put(i, 9, 7, C(70, 130, 70))  # Seetang
    put(i, 8, 6, C(240, 240, 236)); put(i, 5, 6, C(240, 225, 162))

def t_kandierter_apfel(i):
    disc(i, 8, 9, 5, C(216, 52, 46)); disc(i, 8, 9, 4, C(186, 36, 34))
    disc(i, 6, 7, 2, C(240, 120, 110))                   # Glanz
    rect(i, 8, 2, 8, 4, C(150, 110, 60))                 # Stiel
    for x in range(4, 13):
        put(i, x, 13, C(120, 22, 22))

def t_reis(i):
    # kleiner Haufen weisser Reiskoerner
    disc(i, 8, 9, 5, C(245, 245, 236))
    disc(i, 7, 7, 3, C(250, 250, 244))
    for (x, y) in [(6, 8), (9, 8), (7, 10), (10, 10), (8, 9), (6, 11), (9, 11)]:
        put(i, x, y, C(223, 223, 212))

def t_reis_samen(i):
    # Buendel heller Reissamen mit gruenem Halm
    for (x, y) in [(6, 6), (9, 6), (7, 9), (10, 9), (8, 8), (5, 8), (11, 7)]:
        put(i, x, y, C(222, 206, 132)); put(i, x, y + 1, C(190, 172, 100))
    put(i, 4, 11, C(110, 150, 66)); put(i, 12, 10, C(110, 150, 66))
    put(i, 8, 12, C(110, 150, 66))

DRAW = {
    "teig": t_teig, "spiegelei": t_spiegelei, "rotebeete_chips": t_rotebeete_chips,
    "geroestete_karotte": t_geroestete_karotte, "pommes": t_pommes,
    "marshmallow": t_marshmallow, "stockbrot": t_stockbrot, "nudeln": t_nudeln,
    "kaese": t_kaese, "sauce": t_sauce, "burger": t_burger,
    "cheeseburger": t_cheeseburger, "chicken_nuggets": t_chicken_nuggets,
    "schaschlik": t_schaschlik, "ofenkartoffel_sourcream": t_ofenkartoffel_sourcream,
    "spaghetti": t_spaghetti, "misosuppe": t_misosuppe,
    "kandierter_apfel": t_kandierter_apfel, "reis": t_reis, "reis_samen": t_reis_samen,
}

# id -> (display name, base material, CustomModelData)
ITEMS = {
    "teig": ("<#e8d8a8>Dough", "PAPER", 3001),
    "spiegelei": ("<#fff3c0>Fried Egg", "EGG", 3002),
    "rotebeete_chips": ("<#c0392b>Beetroot Chips", "BEETROOT", 3003),
    "geroestete_karotte": ("<#e67e22>Roasted Carrot", "CARROT", 3004),
    "pommes": ("<#f1c40f>Fries", "POTATO", 3005),
    "marshmallow": ("<#ffeef2>Marshmallow", "PAPER", 3006),
    "stockbrot": ("<#d9a441>Stick Bread", "BREAD", 3007),
    "nudeln": ("<#f0e2b0>Noodles", "PAPER", 3008),
    "kaese": ("<#f2c94c>Cheese", "HONEYCOMB", 3009),
    "sauce": ("<#c0392b>Sauce", "BRICK", 3010),
    "burger": ("<#e2a76f>Burger", "BREAD", 3011),
    "cheeseburger": ("<#f2c94c>Cheeseburger", "BREAD", 3012),
    "chicken_nuggets": ("<#e6b566>Chicken Nuggets", "COOKED_CHICKEN", 3013),
    "schaschlik": ("<#b5651d>Shashlik", "COOKED_BEEF", 3014),
    "ofenkartoffel_sourcream": ("<#e9d8a6>Baked Potato with Sour Cream", "BAKED_POTATO", 3015),
    "spaghetti": ("<#f0e2b0>Spaghetti", "PAPER", 3016),
    "misosuppe": ("<#c98a3a>Miso Soup", "MUSHROOM_STEW", 3017),
    "kandierter_apfel": ("<#e74c3c>Candy Apple", "APPLE", 3018),
    "reis": ("<#f7f3e3>Rice", "PAPER", 3019),
    "reis_samen": ("<#e6dfbf>Rice Seeds", "WHEAT_SEEDS", 3020),
}

OUTLINE = C(60, 44, 32, 255)


def generate():
    os.makedirs(TEX_DIR, exist_ok=True)
    os.makedirs(ITEMS_DIR, exist_ok=True)

    for item_id, fn in DRAW.items():
        img = canvas()
        fn(img)
        apply_shading(img)
        outline(img, OUTLINE)
        img.save(os.path.join(TEX_DIR, item_id + ".png"))

    # Oraxen-Item-YAML
    lines = ["# Auto-generiert von scripts/generate_assets.py - Smoke & Salt Custom-Items.\n"]
    for item_id, (name, mat, cmd) in ITEMS.items():
        lines.append(f"sas_{item_id}:\n")
        lines.append(f'  displayname: "{name}"\n')
        lines.append(f"  material: {mat}\n")
        lines.append("  Pack:\n")
        lines.append("    generate_model: true\n")
        lines.append('    parent_model: "item/generated"\n')
        lines.append("    textures:\n")
        lines.append(f"      - sas/{item_id}.png\n")
        lines.append(f"    custom_model_data: {cmd}\n\n")
    with open(os.path.join(ITEMS_DIR, "smoke_and_salt.yml"), "w", encoding="utf-8", newline="\n") as f:
        f.writelines(lines)

    write_manifest()
    print(f"Fertig: {len(DRAW)} Texturen, {len(ITEMS)} Oraxen-Items, Manifest v{ASSET_VERSION}.")


def write_manifest():
    entries = {}
    for item_id in DRAW:
        rel = f"oraxen/pack/textures/sas/{item_id}.png"
        entries[rel] = sha256(os.path.join(OX, "pack", "textures", "sas", item_id + ".png"))
    yml = "oraxen/items/smoke_and_salt.yml"
    entries[yml] = sha256(os.path.join(ITEMS_DIR, "smoke_and_salt.yml"))

    out = ["# Auto-generiert von scripts/generate_assets.py\n",
           f"asset-version={ASSET_VERSION}\n"]
    for key in sorted(entries):
        out.append(f"sha256.{key}={entries[key]}\n")
    with open(MANIFEST, "w", encoding="utf-8", newline="\n") as f:
        f.writelines(out)


def sha256(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        h.update(f.read())
    return h.hexdigest()


if __name__ == "__main__":
    generate()
