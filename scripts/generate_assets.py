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
ASSET_VERSION = "8"

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

def line(img, x0, y0, x1, y1, c):
    dx = abs(x1 - x0)
    dy = -abs(y1 - y0)
    sx = 1 if x0 < x1 else -1
    sy = 1 if y0 < y1 else -1
    err = dx + dy
    while True:
        put(img, x0, y0, c)
        if x0 == x1 and y0 == y1:
            break
        e2 = 2 * err
        if e2 >= dy:
            err += dy
            x0 += sx
        if e2 <= dx:
            err += dx
            y0 += sy

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
    """Small vanilla-style bevel plus deterministic grain for less-flat sprites."""
    src = img.copy()
    def opaque(x, y):
        return 0 <= x < 16 and 0 <= y < 16 and src.getpixel((x, y))[3] != 0
    for y in range(16):
        for x in range(16):
            p = src.getpixel((x, y))
            if p[3] == 0:
                continue
            factor = 1.04 - y * 0.012 + max(0, 5 - x) * 0.006
            top_open = not opaque(x, y - 1)
            left_open = not opaque(x - 1, y)
            bot_open = not opaque(x, y + 1)
            right_open = not opaque(x + 1, y)
            if top_open or left_open:
                factor += 0.15
            elif bot_open or right_open:
                factor -= 0.20
            if (x * 5 + y * 3) % 11 == 0:
                factor += 0.05
            elif (x * 7 + y * 2) % 13 == 0:
                factor -= 0.05
            img.putpixel((x, y), _mul(p, factor))

# ---------------------------------------------------------------------------
#  Einzelne Item-Texturen
# ---------------------------------------------------------------------------

def t_teig(i):
    disc(i, 8, 9, 5, C(226, 210, 164)); disc(i, 6, 7, 3, C(242, 228, 184))
    disc(i, 10, 10, 3, C(218, 198, 148))
    for p in [(5, 9), (7, 6), (9, 8), (11, 11), (6, 12)]:
        put(i, *p, C(202, 184, 138))
    put(i, 6, 7, C(255, 244, 204)); put(i, 10, 7, C(236, 220, 174))

def t_spiegelei(i):
    disc(i, 8, 9, 6, C(244, 242, 226)); disc(i, 5, 8, 3, C(255, 252, 238))
    rect(i, 4, 12, 11, 13, C(214, 212, 198)); put(i, 12, 11, C(226, 224, 210))
    disc(i, 9, 8, 3, C(232, 156, 36)); disc(i, 9, 8, 2, C(255, 202, 70))
    put(i, 8, 7, C(255, 234, 128)); put(i, 10, 9, C(210, 122, 26)); put(i, 3, 9, C(230, 228, 216))

def t_rotebeete_chips(i):
    for (x, y, r, col) in [(6, 10, 3, C(148, 36, 70)), (10, 10, 3, C(170, 45, 82)),
                           (8, 7, 3, C(190, 54, 92)), (8, 12, 2, C(128, 30, 58))]:
        disc(i, x, y, r, col)
    for (x, y) in [(6, 10), (10, 10), (8, 7), (8, 12)]:
        put(i, x, y, C(102, 22, 46))
        put(i, x - 1, y - 1, C(218, 82, 116))
        put(i, x + 1, y, C(124, 24, 54))
    line(i, 5, 8, 9, 12, C(112, 24, 50)); line(i, 9, 6, 12, 9, C(224, 82, 118))

def t_geroestete_karotte(i):
    for dy in range(3, 14):
        w = max(0, 13 - dy)
        rect(i, 8 - w // 2, dy, 8 + (w + 1) // 2, dy, C(222, 112, 36))
    rect(i, 7, 12, 9, 13, C(172, 76, 24))
    line(i, 6, 7, 9, 8, C(116, 54, 22)); line(i, 8, 10, 10, 11, C(116, 54, 22))
    put(i, 7, 5, C(248, 154, 58)); put(i, 9, 6, C(236, 132, 42))
    rect(i, 7, 1, 9, 3, C(70, 150, 52)); put(i, 6, 2, C(92, 174, 70)); put(i, 10, 2, C(50, 124, 44)); put(i, 8, 0, C(96, 184, 72))

def t_pommes(i):
    rect(i, 4, 9, 11, 14, C(176, 42, 38)); rect(i, 4, 9, 11, 10, C(224, 70, 54))
    rect(i, 5, 13, 10, 14, C(116, 28, 28)); put(i, 4, 11, C(214, 58, 48)); put(i, 11, 12, C(122, 26, 26))
    for x, top, col in ((4, 4, C(236, 196, 74)), (6, 2, C(248, 216, 104)),
                        (8, 3, C(232, 188, 64)), (10, 2, C(250, 224, 120))):
        rect(i, x, top, x + 1, 10, col); put(i, x, top, C(255, 236, 140))
    put(i, 5, 7, C(204, 160, 52)); put(i, 9, 5, C(255, 242, 150)); put(i, 11, 8, C(218, 170, 56))

def t_marshmallow(i):
    rect(i, 5, 4, 11, 13, C(250, 236, 240)); rect(i, 5, 4, 11, 5, C(255, 252, 253))
    rect(i, 5, 8, 11, 9, C(248, 204, 216)); rect(i, 6, 13, 10, 13, C(226, 206, 212))
    rect(i, 5, 11, 11, 12, C(222, 176, 156))
    put(i, 7, 6, C(255, 255, 255)); put(i, 9, 10, C(230, 190, 204)); put(i, 10, 12, C(170, 104, 74))

def t_kaiserbroetchen(i):
    disc(i, 8, 9, 5, C(204, 144, 76))
    rect(i, 4, 9, 12, 12, C(190, 126, 62))
    disc(i, 8, 7, 4, C(226, 174, 104))
    line(i, 5, 9, 8, 6, C(132, 82, 42)); line(i, 8, 6, 11, 9, C(132, 82, 42))
    line(i, 6, 7, 10, 7, C(246, 218, 156))
    for x in (5, 8, 11):
        put(i, x, 5 + (x % 2), C(248, 232, 172))

def t_nudeln(i):
    # runder Nudel-Haufen mit Straehnen
    disc(i, 8, 9, 5, C(238, 224, 158))
    disc(i, 7, 7, 3, C(246, 232, 172))
    for y in (6, 8, 10, 12):
        for x in range(3, 14):
            if (x + y // 2) % 2 == 0 and (x - 8) ** 2 + (y - 9) ** 2 <= 26:
                put(i, x, y, C(210, 188, 116))
    line(i, 4, 9, 11, 6, C(250, 236, 176)); line(i, 5, 11, 12, 10, C(218, 196, 126))

def t_kaese(i):
    # gelber Keil mit Loechern
    for y in range(3, 14):
        rect(i, 3, y, 3 + (y - 3), y, C(244, 204, 72))
    for (x, y) in [(5, 8), (7, 11), (8, 6)]:
        put(i, x, y, C(222, 176, 48)); put(i, x + 1, y, C(222, 176, 48))
    for y in range(3, 14):
        put(i, 3, y, C(206, 162, 40))
    line(i, 4, 4, 12, 12, C(255, 226, 96)); put(i, 9, 10, C(190, 146, 34))

def t_sourcream(i):
    rect(i, 4, 7, 11, 13, C(218, 218, 206)); rect(i, 3, 6, 12, 8, C(244, 242, 230))
    rect(i, 4, 5, 11, 6, C(255, 252, 238)); rect(i, 5, 9, 10, 12, C(236, 236, 224))
    line(i, 5, 9, 10, 11, C(255, 255, 248)); line(i, 6, 11, 11, 9, C(208, 208, 196))
    put(i, 6, 6, C(255, 255, 250)); put(i, 9, 8, C(250, 250, 238)); put(i, 10, 10, C(198, 198, 186))

def t_sauce(i):
    # Glas/Napf mit roter Sauce
    rect(i, 4, 5, 11, 13, C(178, 40, 40)); rect(i, 4, 5, 11, 6, C(206, 60, 60))
    rect(i, 4, 4, 11, 4, C(150, 26, 26)); rect(i, 3, 13, 12, 14, C(120, 20, 20))
    put(i, 6, 8, C(220, 76, 70)); put(i, 9, 10, C(210, 70, 70)); put(i, 7, 6, C(242, 92, 84))
    put(i, 8, 9, C(126, 24, 22)); put(i, 10, 7, C(90, 120, 46))

def _bun_top(i):
    # gewoelbte Broetchen-Oberseite (keine harte Rechteck-Kante)
    rect(i, 5, 2, 10, 2, C(226, 178, 112))
    rect(i, 4, 3, 11, 3, C(216, 166, 100))
    rect(i, 3, 4, 12, 4, C(206, 150, 84))
    for x in (6, 8, 10):
        put(i, x, 3, C(242, 228, 172))          # Sesam

def _bun_bottom(i):
    # nach unten verjuengte Broetchen-Unterseite
    rect(i, 3, 10, 12, 10, C(202, 146, 82))
    rect(i, 4, 11, 11, 11, C(180, 124, 68))
    rect(i, 5, 12, 10, 12, C(150, 98, 46))

def t_burger(i):
    _bun_top(i)
    rect(i, 3, 5, 12, 5, C(100, 162, 62))       # Salat
    put(i, 5, 6, C(128, 190, 84)); put(i, 9, 6, C(120, 182, 78))
    rect(i, 3, 6, 12, 8, C(122, 64, 44))        # Fleisch
    rect(i, 4, 8, 11, 8, C(88, 44, 30))
    put(i, 10, 7, C(150, 84, 56))
    _bun_bottom(i)

def t_cheeseburger(i):
    _bun_top(i)
    rect(i, 3, 5, 12, 6, C(122, 64, 44))        # Fleisch
    rect(i, 4, 6, 11, 6, C(88, 44, 30))
    rect(i, 3, 7, 12, 7, C(244, 204, 72))       # Kaese
    put(i, 2, 8, C(240, 196, 64)); put(i, 12, 8, C(240, 196, 64))   # Kaese-Tropfen
    put(i, 5, 8, C(255, 224, 96)); put(i, 9, 8, C(255, 224, 96))
    rect(i, 3, 9, 12, 9, C(122, 64, 44))        # zweite Fleischkante
    _bun_bottom(i)

def t_chicken_nuggets(i):
    for (x, y, r, col) in [(5, 5, 2, C(226, 174, 92)), (10, 6, 2, C(214, 158, 78)),
                           (6, 10, 3, C(232, 184, 104)), (11, 11, 2, C(220, 168, 86))]:
        disc(i, x, y, r, col); put(i, x, y, C(178, 126, 54))
        put(i, x + 1, y - 1, C(252, 216, 140)); put(i, x - 1, y + 1, C(150, 94, 40))

def t_schaschlik(i):
    for k in range(12):
        put(i, 2 + k, 13 - k, C(144, 104, 56))          # Spiess
    disc(i, 5, 10, 2, C(124, 62, 44))                    # Fleisch
    disc(i, 8, 7, 2, C(224, 112, 38))                    # Karotte
    disc(i, 11, 4, 2, C(170, 132, 70))
    disc(i, 10, 6, 1, C(92, 150, 52))
    put(i, 5, 10, C(94, 44, 30)); put(i, 11, 4, C(110, 55, 35)); put(i, 8, 6, C(250, 152, 58))

def t_ofenkartoffel_sourcream(i):
    disc(i, 8, 9, 6, C(178, 132, 74)); disc(i, 8, 9, 5, C(206, 158, 96))
    rect(i, 5, 6, 11, 8, C(120, 84, 46))                 # Schnitt
    rect(i, 6, 6, 10, 7, C(246, 246, 240))               # Sauerrahm
    put(i, 7, 5, C(96, 150, 60)); put(i, 9, 5, C(96, 150, 60)); put(i, 10, 7, C(86, 136, 54))  # Schnittlauch
    put(i, 5, 10, C(146, 94, 52)); put(i, 11, 10, C(230, 180, 112))

def t_spaghetti(i):
    rect(i, 3, 9, 12, 13, C(150, 95, 55)); rect(i, 3, 9, 12, 9, C(184, 122, 74))  # Teller
    for x in range(4, 12):
        put(i, x, 6 + (x % 3), C(240, 225, 162))
        put(i, x, 5 + (x % 2), C(226, 208, 140))
    line(i, 4, 8, 12, 6, C(250, 236, 176)); line(i, 5, 7, 11, 10, C(216, 196, 128))
    put(i, 7, 5, C(190, 40, 40)); put(i, 9, 6, C(190, 40, 40)); put(i, 8, 8, C(212, 70, 42)); put(i, 10, 8, C(90, 128, 48))

def t_misosuppe(i):
    rect(i, 3, 8, 12, 13, C(150, 95, 55)); rect(i, 3, 8, 12, 8, C(184, 122, 74))
    rect(i, 4, 6, 11, 8, C(198, 138, 58))                # Bruehe
    put(i, 6, 7, C(70, 130, 70)); put(i, 9, 7, C(70, 130, 70)); put(i, 10, 6, C(52, 96, 52))  # Seetang
    put(i, 8, 6, C(240, 240, 236)); put(i, 5, 6, C(240, 225, 162)); put(i, 7, 8, C(160, 90, 42))

def t_kandierter_apfel(i):
    disc(i, 8, 9, 5, C(216, 52, 46)); disc(i, 8, 9, 4, C(186, 36, 34))
    disc(i, 6, 7, 2, C(240, 120, 110)); put(i, 5, 6, C(255, 178, 160))  # Glanz
    rect(i, 8, 2, 8, 4, C(150, 110, 60))                 # Stiel
    line(i, 7, 13, 10, 15, C(146, 30, 28))
    for x in range(4, 13):
        put(i, x, 13, C(120, 22, 22))

def t_reis(i):
    # kleiner Haufen weisser Reiskoerner
    disc(i, 8, 9, 5, C(245, 245, 236))
    disc(i, 7, 7, 3, C(250, 250, 244))
    for (x, y) in [(6, 8), (9, 8), (7, 10), (10, 10), (8, 9), (6, 11), (9, 11), (11, 9), (5, 10)]:
        put(i, x, y, C(220, 220, 208)); put(i, x + 1, y, C(250, 250, 244))

def t_reis_samen(i):
    # Buendel heller Reissamen mit gruenem Halm
    for (x, y) in [(6, 6), (9, 6), (7, 9), (10, 9), (8, 8), (5, 8), (11, 7)]:
        put(i, x, y, C(222, 206, 132)); put(i, x, y + 1, C(190, 172, 100))
    put(i, 4, 11, C(110, 150, 66)); put(i, 12, 10, C(110, 150, 66))
    put(i, 8, 12, C(110, 150, 66)); line(i, 6, 12, 10, 4, C(80, 128, 54))

# --- Reis-Wachstumsstufen (8 Stufen fuer Weizens Alter 0..7) -----------------
# Duenne, leicht geneigte Halme auf dunkler Erde, die hoeher und dichter werden;
# goldene Reiskoerner erscheinen an den Spitzen und lassen die Aehren am Ende
# leicht ueberhaengen. Bewusst organisch statt kantig/rechteckig.

_G = [C(96, 158, 74), C(114, 174, 86), C(80, 138, 64), C(90, 150, 68)]

def _soil(i, x0, x1):
    rect(i, x0, 14, x1, 14, C(90, 62, 40))
    rect(i, x0 + 1, 15, x1 - 1, 15, C(70, 48, 30))

def _blade(i, x, top, col, lean=1):
    line(i, x, 14, x - lean, top, col)
    line(i, x, 14, x + lean, top + 1, _mul(col, 1.14))

def _grain(i, x, y):
    put(i, x, y, C(228, 206, 122))
    put(i, x, y + 1, C(182, 156, 86))

def t_reis_crop_stage0(i):
    _soil(i, 6, 10)
    put(i, 8, 13, C(104, 158, 76)); put(i, 8, 12, C(124, 178, 90)); put(i, 7, 13, C(86, 138, 64))

def t_reis_crop_stage1(i):
    _soil(i, 5, 11)
    for x, top, c in [(6, 11, _G[0]), (8, 10, _G[1]), (10, 12, _G[2])]:
        _blade(i, x, top, c)

def t_reis_crop_stage2(i):
    _soil(i, 4, 12)
    for x, top, c in [(5, 9, _G[0]), (7, 8, _G[1]), (9, 9, _G[2]), (11, 10, _G[3])]:
        _blade(i, x, top, c)

def t_reis_crop_stage3(i):
    _soil(i, 3, 13)
    for x, top, c in [(4, 8, _G[2]), (6, 6, _G[1]), (8, 7, _G[0]), (10, 6, _G[3]), (12, 8, _G[2])]:
        _blade(i, x, top, c)

def t_reis_crop_stage4(i):
    _soil(i, 3, 13)
    for x, top, c in [(4, 7, _G[2]), (6, 5, _G[1]), (8, 6, _G[0]), (10, 5, _G[3]), (12, 7, _G[2])]:
        _blade(i, x, top, c)
    _grain(i, 6, 4); _grain(i, 10, 4)

def t_reis_crop_stage5(i):
    _soil(i, 2, 13)
    for x, top, c in [(3, 6, _G[2]), (5, 4, _G[1]), (7, 5, _G[0]), (9, 4, _G[3]), (11, 5, _G[1]), (13, 7, _G[2])]:
        _blade(i, x, top, c)
    for gx, gy in [(5, 3), (7, 4), (9, 3), (11, 4)]:
        _grain(i, gx, gy)

def t_reis_crop_stage6(i):
    _soil(i, 2, 13)
    for x, top, c in [(3, 5, _G[2]), (5, 3, _G[1]), (7, 4, _G[0]), (9, 3, _G[3]), (11, 4, _G[1]), (13, 6, _G[2])]:
        _blade(i, x, top, c)
    for gx, gy in [(4, 3), (5, 2), (7, 3), (9, 2), (11, 3), (12, 4)]:
        _grain(i, gx, gy)

def t_reis_crop_stage7(i):
    _soil(i, 2, 13)
    for x, top, c in [(3, 4, _G[2]), (5, 3, _G[1]), (7, 3, _G[0]), (9, 3, _G[3]), (11, 3, _G[1]), (13, 5, _G[2])]:
        _blade(i, x, top, c)
    # dichte, goldene, leicht ueberhaengende Aehren
    for gx, gy in [(3, 3), (5, 2), (6, 2), (7, 2), (9, 2), (10, 2), (11, 2), (13, 4)]:
        _grain(i, gx, gy)
    put(i, 4, 2, C(206, 180, 100)); put(i, 12, 3, C(206, 180, 100))
    put(i, 5, 1, C(224, 202, 120)); put(i, 10, 1, C(224, 202, 120))

DRAW = {
    "teig": t_teig, "spiegelei": t_spiegelei, "rotebeete_chips": t_rotebeete_chips,
    "geroestete_karotte": t_geroestete_karotte, "pommes": t_pommes,
    "marshmallow": t_marshmallow, "kaiserbroetchen": t_kaiserbroetchen, "nudeln": t_nudeln,
    "kaese": t_kaese, "sourcream": t_sourcream, "sauce": t_sauce, "burger": t_burger,
    "cheeseburger": t_cheeseburger, "chicken_nuggets": t_chicken_nuggets,
    "schaschlik": t_schaschlik, "ofenkartoffel_sourcream": t_ofenkartoffel_sourcream,
    "spaghetti": t_spaghetti, "misosuppe": t_misosuppe,
    "kandierter_apfel": t_kandierter_apfel, "reis": t_reis, "reis_samen": t_reis_samen,
}

EXTRA_TEXTURES = {
    "crops/reis_stage0": t_reis_crop_stage0,
    "crops/reis_stage1": t_reis_crop_stage1,
    "crops/reis_stage2": t_reis_crop_stage2,
    "crops/reis_stage3": t_reis_crop_stage3,
    "crops/reis_stage4": t_reis_crop_stage4,
    "crops/reis_stage5": t_reis_crop_stage5,
    "crops/reis_stage6": t_reis_crop_stage6,
    "crops/reis_stage7": t_reis_crop_stage7,
}

# id -> (display name, base material, CustomModelData[, texture path])
ITEMS = {
    "teig": ("<#e8d8a8>Dough", "PAPER", 3001),
    "spiegelei": ("<#fff3c0>Fried Egg", "PAPER", 3002),
    "rotebeete_chips": ("<#c0392b>Beetroot Chips", "BEETROOT", 3003),
    "geroestete_karotte": ("<#e67e22>Roasted Carrot", "CARROT", 3004),
    "pommes": ("<#f1c40f>Fries", "POTATO", 3005),
    "marshmallow": ("<#ffeef2>Marshmallow", "PAPER", 3006),
    "kaiserbroetchen": ("<#d9a441>Kaiser Roll", "BREAD", 3007),
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
    "sourcream": ("<#fff8e7>Sour Cream", "PAPER", 3021),
    "reis_crop_0": ("<#9ccc65>Rice Crop 0", "PAPER", 3022, "sas/crops/reis_stage0.png"),
    "reis_crop_1": ("<#9ccc65>Rice Crop 1", "PAPER", 3023, "sas/crops/reis_stage1.png"),
    "reis_crop_2": ("<#a6cc6f>Rice Crop 2", "PAPER", 3024, "sas/crops/reis_stage2.png"),
    "reis_crop_3": ("<#b7c873>Rice Crop 3", "PAPER", 3025, "sas/crops/reis_stage3.png"),
    "reis_crop_4": ("<#c5b86d>Rice Crop 4", "PAPER", 3026, "sas/crops/reis_stage4.png"),
    "reis_crop_5": ("<#d3bd72>Rice Crop 5", "PAPER", 3027, "sas/crops/reis_stage5.png"),
    "reis_crop_6": ("<#e0c877>Rice Crop 6", "PAPER", 3028, "sas/crops/reis_stage6.png"),
    "reis_crop_7": ("<#e6cf82>Rice Crop 7", "PAPER", 3029, "sas/crops/reis_stage7.png"),
}

# ---------------------------------------------------------------------------
#  Neue Pflanzen: Ernte-Sprites + getoente Samen
# ---------------------------------------------------------------------------

def seed_sprite(img, col):
    for (x, y) in [(6, 6), (9, 6), (7, 9), (10, 9), (8, 8), (5, 8), (11, 7)]:
        put(img, x, y, col); put(img, x, y + 1, _mul(col, 0.78))
    put(img, 4, 11, C(120, 150, 66)); put(img, 12, 10, C(120, 150, 66))

def t_tomato(i):
    disc(i, 8, 9, 5, C(202, 52, 44)); disc(i, 6, 7, 2, C(238, 104, 92))
    for p in [(8, 3), (7, 4), (9, 4), (8, 5)]: put(i, *p, C(72, 142, 60))
    put(i, 6, 4, C(72, 142, 60)); put(i, 10, 4, C(72, 142, 60))

def t_onion(i):
    disc(i, 8, 9, 5, C(224, 204, 172))
    for x in range(5, 12): put(i, x, 6, C(206, 158, 158))
    line(i, 6, 12, 7, 8, C(206, 184, 150)); line(i, 10, 12, 9, 8, C(206, 184, 150))
    line(i, 8, 5, 8, 1, C(96, 162, 72)); put(i, 9, 3, C(96, 162, 72))
    put(i, 7, 14, C(230, 230, 220)); put(i, 9, 14, C(230, 230, 220))

def t_lettuce(i):
    disc(i, 8, 9, 6, C(74, 138, 58)); disc(i, 8, 8, 4, C(110, 176, 86))
    disc(i, 8, 8, 2, C(150, 200, 120))
    for p in [(4, 7), (12, 7), (5, 12), (11, 12), (8, 3)]: put(i, *p, C(60, 120, 48))

def t_corn(i):
    for y in range(3, 14):
        for x in range(6, 10):
            put(i, x, y, C(236, 200, 72) if (x + y) % 2 == 0 else C(210, 170, 50))
    line(i, 5, 4, 4, 12, C(84, 150, 62)); line(i, 10, 4, 11, 12, C(84, 150, 62))
    put(i, 7, 2, C(120, 180, 90)); put(i, 8, 2, C(120, 180, 90))

def t_cucumber(i):
    for k in range(11):
        x = 3 + k; y = 12 - k
        put(i, x, y, C(70, 132, 52)); put(i, x, y - 1, C(96, 158, 72)); put(i, x + 1, y, C(56, 114, 44))
    for k in range(0, 11, 3): put(i, 3 + k, 12 - k, C(150, 190, 110))

def t_garlic(i):
    disc(i, 8, 9, 5, C(240, 236, 228))
    line(i, 8, 4, 8, 13, C(208, 202, 194)); line(i, 6, 6, 6, 13, C(220, 214, 206))
    line(i, 10, 6, 10, 13, C(220, 214, 206)); line(i, 8, 4, 8, 1, C(200, 196, 190))
    put(i, 7, 14, C(220, 214, 206)); put(i, 9, 14, C(220, 214, 206))

def t_chili(i):
    for k in range(9): put(i, 4 + k, 5 + (k * k) // 12, C(198, 44, 38))
    for k in range(9): put(i, 4 + k, 6 + (k * k) // 12, C(150, 26, 22))
    put(i, 4, 4, C(80, 150, 64)); line(i, 4, 4, 3, 2, C(80, 150, 64)); put(i, 6, 6, C(236, 96, 84))

def t_strawberry(i):
    disc(i, 8, 9, 5, C(206, 48, 44))
    put(i, 8, 14, C(180, 34, 30)); put(i, 7, 13, C(196, 44, 40)); put(i, 9, 13, C(196, 44, 40))
    for p in [(6, 7), (9, 8), (7, 10), (10, 10), (8, 12), (6, 11)]: put(i, *p, C(244, 220, 90))
    for x in range(5, 12): put(i, x, 4, C(74, 146, 60))
    put(i, 8, 3, C(96, 166, 78))

def t_blueberry(i):
    for (x, y) in [(6, 7), (10, 7), (8, 6), (7, 10), (10, 10), (9, 12)]:
        disc(i, x, y, 2, C(70, 86, 168)); put(i, x - 1, y - 1, C(122, 142, 212)); put(i, x, y, C(48, 62, 138))

def t_soybean(i):
    for k in range(10): put(i, 4 + k, 11 - (k * k) // 10, C(120, 168, 72))
    for (cx, cy) in [(6, 10), (8, 9), (10, 7)]: disc(i, cx, cy, 1, C(152, 194, 98))
    put(i, 4, 12, C(96, 140, 58))

def t_cotton(i):
    for (x, y) in [(6, 7), (10, 7), (8, 6), (8, 9)]:
        disc(i, x, y, 2, C(244, 244, 240)); put(i, x, y, C(255, 255, 252))
    for p in [(6, 10), (8, 11), (10, 10)]: put(i, *p, C(120, 90, 54))

def t_cabbage(i):
    disc(i, 8, 9, 6, C(70, 132, 54)); disc(i, 8, 9, 4, C(104, 168, 80)); disc(i, 8, 9, 2, C(140, 192, 110))
    for r in [(8, 4), (4, 9), (12, 9), (8, 14)]: put(i, *r, C(56, 116, 44))

def t_bell_pepper(i):
    disc(i, 7, 10, 4, C(198, 48, 42)); disc(i, 10, 10, 3, C(198, 48, 42))
    disc(i, 6, 8, 2, C(238, 96, 84)); line(i, 8, 5, 8, 3, C(84, 150, 62)); put(i, 7, 4, C(84, 150, 62))

def t_pineapple(i):
    for y in range(6, 14):
        for x in range(5, 12):
            put(i, x, y, C(224, 180, 64) if (x + y) % 2 == 0 else C(196, 150, 48))
    for p in [(8, 1), (7, 2), (9, 2), (6, 3), (10, 3), (8, 3)]: put(i, *p, C(78, 146, 60))
    line(i, 5, 6, 11, 12, C(160, 120, 40))

def t_grapes(i):
    for (x, y) in [(6, 9), (9, 9), (7, 11), (10, 11), (8, 10), (8, 12), (11, 9)]:
        disc(i, x, y, 1, C(120, 66, 150)); put(i, x - 1, y - 1, C(160, 110, 190))
    put(i, 8, 6, C(96, 150, 64)); line(i, 8, 7, 8, 5, C(120, 90, 50))

def t_coffee_beans(i):
    for (x, y) in [(6, 8), (10, 9)]:
        disc(i, x, y, 3, C(120, 74, 40)); line(i, x, y - 2, x, y + 2, C(80, 48, 26)); put(i, x - 1, y - 1, C(150, 100, 60))

def t_zucchini(i):
    for k in range(11):
        x = 4 + k; y = 12 - k
        put(i, x, y, C(46, 102, 40)); put(i, x, y - 1, C(64, 124, 52)); put(i, x + 1, y, C(36, 86, 32))
    for k in range(0, 11, 3): put(i, 4 + k, 12 - k, C(96, 150, 72))
    put(i, 4, 13, C(90, 140, 60))

def t_eggplant(i):
    disc(i, 8, 10, 5, C(96, 52, 120)); disc(i, 8, 12, 3, C(110, 64, 138))
    disc(i, 6, 8, 2, C(150, 110, 180))
    for p in [(8, 3), (7, 4), (9, 4), (8, 5)]: put(i, *p, C(78, 146, 60))
    line(i, 8, 4, 8, 2, C(70, 130, 54))

# id -> (display, draw, seed-tint, custom_model_data-base)
PLANTS = [
    ("tomato", "Tomato", t_tomato, C(200, 60, 50)),
    ("onion", "Onion", t_onion, C(206, 184, 150)),
    ("lettuce", "Lettuce", t_lettuce, C(90, 160, 74)),
    ("corn", "Corn", t_corn, C(232, 196, 70)),
    ("cucumber", "Cucumber", t_cucumber, C(90, 158, 72)),
    ("garlic", "Garlic", t_garlic, C(230, 224, 214)),
    ("chili", "Chili", t_chili, C(198, 44, 38)),
    ("strawberry", "Strawberry", t_strawberry, C(206, 48, 44)),
    ("blueberry", "Blueberry", t_blueberry, C(90, 110, 190)),
    ("soybean", "Soybean", t_soybean, C(120, 168, 72)),
    ("cotton", "Cotton", t_cotton, C(240, 240, 236)),
    ("cabbage", "Cabbage", t_cabbage, C(104, 168, 80)),
    ("bell_pepper", "Bell Pepper", t_bell_pepper, C(198, 48, 42)),
    ("pineapple", "Pineapple", t_pineapple, C(224, 180, 64)),
    ("grapes", "Grapes", t_grapes, C(140, 90, 170)),
    ("coffee_beans", "Coffee Beans", t_coffee_beans, C(140, 94, 60)),
    ("zucchini", "Zucchini", t_zucchini, C(64, 124, 52)),
    ("eggplant", "Eggplant", t_eggplant, C(120, 70, 150)),
]

_cmd = 3030
for _pid, _pname, _pdraw, _scol in PLANTS:
    DRAW[_pid] = _pdraw
    DRAW[_pid + "_seeds"] = (lambda col: (lambda im: seed_sprite(im, col)))(_scol)
    ITEMS[_pid] = ("<white>" + _pname, "PAPER", _cmd)
    ITEMS[_pid + "_seeds"] = ("<white>" + _pname + " Seeds", "WHEAT_SEEDS", _cmd + 1)
    _cmd += 2

# ---------------------------------------------------------------------------
#  Custom-Crop-Texturen (eine je Pflanze, wird beim Wachsen skaliert)
# ---------------------------------------------------------------------------

def crop_sprite(img, fruit):
    stem = C(86, 150, 66)
    stem_d = C(58, 118, 48)
    for x0, top in [(6, 4), (8, 3), (10, 5)]:
        line(img, x0, 14, x0, top, stem)
        line(img, x0, 14, x0 - 1, top + 2, stem_d)
        put(img, x0, top, C(120, 182, 92))
    for p in [(5, 12), (11, 12), (7, 13), (9, 13)]:
        put(img, *p, stem)
    for (x, y) in [(7, 5), (9, 6), (8, 4)]:
        put(img, x, y, fruit); put(img, x, y + 1, _mul(fruit, 0.78))

# produce-id -> Frucht-Farbe (Crop-Item: sas_<produce>_crop)
CROPS = [
    ("reis", C(226, 204, 120)),
    ("tomato", C(200, 60, 50)),
    ("onion", C(206, 184, 150)),
    ("lettuce", C(120, 186, 96)),
    ("corn", C(232, 196, 70)),
    ("cucumber", C(90, 158, 72)),
    ("garlic", C(232, 226, 216)),
    ("chili", C(198, 44, 38)),
    ("strawberry", C(206, 48, 44)),
    ("blueberry", C(90, 110, 190)),
    ("soybean", C(120, 168, 72)),
    ("cotton", C(240, 240, 236)),
    ("cabbage", C(104, 168, 80)),
    ("bell_pepper", C(198, 48, 42)),
    ("pineapple", C(224, 180, 64)),
    ("grapes", C(140, 90, 170)),
    ("coffee_beans", C(140, 94, 60)),
    ("zucchini", C(64, 124, 52)),
    ("eggplant", C(120, 70, 150)),
]

_crop_cmd = 3070
for _cid, _fcol in CROPS:
    EXTRA_TEXTURES["crops/" + _cid + "_crop"] = (lambda col: (lambda im: crop_sprite(im, col)))(_fcol)
    ITEMS[_cid + "_crop"] = ("<green>" + _cid.replace("_", " ").title() + " Crop", "PAPER",
                             _crop_cmd, "sas/crops/" + _cid + "_crop.png")
    _crop_cmd += 1

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

    for tex_id, fn in EXTRA_TEXTURES.items():
        img = canvas()
        fn(img)
        apply_shading(img)
        outline(img, OUTLINE)
        out = os.path.join(TEX_DIR, tex_id + ".png")
        os.makedirs(os.path.dirname(out), exist_ok=True)
        img.save(out)

    # Oraxen-Item-YAML
    lines = ["# Auto-generiert von scripts/generate_assets.py - Smoke & Salt Custom-Items.\n"]
    for item_id, data in ITEMS.items():
        name, mat, cmd = data[:3]
        texture = data[3] if len(data) > 3 else f"sas/{item_id}.png"
        lines.append(f"sas_{item_id}:\n")
        lines.append(f'  displayname: "{name}"\n')
        lines.append(f"  material: {mat}\n")
        lines.append("  Pack:\n")
        lines.append("    generate_model: true\n")
        lines.append('    parent_model: "item/generated"\n')
        lines.append("    textures:\n")
        lines.append(f"      - {texture}\n")
        lines.append(f"    custom_model_data: {cmd}\n\n")
    with open(os.path.join(ITEMS_DIR, "smoke_and_salt.yml"), "w", encoding="utf-8", newline="\n") as f:
        f.writelines(lines)

    write_manifest()
    print(f"Fertig: {len(DRAW) + len(EXTRA_TEXTURES)} Texturen, {len(ITEMS)} Oraxen-Items, Manifest v{ASSET_VERSION}.")


def write_manifest():
    entries = {}
    for item_id in DRAW:
        rel = f"oraxen/pack/textures/sas/{item_id}.png"
        entries[rel] = sha256(os.path.join(OX, "pack", "textures", "sas", item_id + ".png"))
    for tex_id in EXTRA_TEXTURES:
        rel = f"oraxen/pack/textures/sas/{tex_id}.png"
        entries[rel] = sha256(os.path.join(OX, "pack", "textures", "sas", tex_id + ".png"))
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
