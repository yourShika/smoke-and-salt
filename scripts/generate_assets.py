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
ASSET_VERSION = "11"

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
            factor = 1.08 - y * 0.016 + max(0, 5 - x) * 0.008
            top_open = not opaque(x, y - 1)
            left_open = not opaque(x - 1, y)
            bot_open = not opaque(x, y + 1)
            right_open = not opaque(x + 1, y)
            if top_open or left_open:
                factor += 0.18
            elif bot_open or right_open:
                factor -= 0.24
            if (x * 5 + y * 3) % 11 == 0:
                factor += 0.06
            elif (x * 7 + y * 2) % 13 == 0:
                factor -= 0.07
            img.putpixel((x, y), _mul(p, factor))

# ---------------------------------------------------------------------------
#  Einzelne Item-Texturen
# ---------------------------------------------------------------------------

def t_teig(i):
    disc(i, 8, 9, 5, C(224, 204, 156)); disc(i, 6, 7, 3, C(244, 228, 184))
    disc(i, 10, 10, 3, C(214, 190, 136)); disc(i, 9, 7, 2, C(236, 216, 168))
    line(i, 5, 10, 11, 6, C(196, 172, 124)); line(i, 6, 12, 11, 11, C(184, 158, 112))
    for p in [(5, 9), (7, 6), (9, 8), (11, 11), (6, 12), (8, 11)]:
        put(i, *p, C(176, 150, 106))
    put(i, 6, 6, C(255, 244, 204)); put(i, 10, 7, C(248, 232, 186)); put(i, 4, 8, C(238, 224, 178))

def t_spiegelei(i):
    disc(i, 8, 9, 6, C(242, 240, 224)); disc(i, 5, 8, 3, C(255, 252, 238))
    rect(i, 4, 12, 11, 13, C(210, 206, 190)); put(i, 12, 11, C(226, 224, 210)); put(i, 3, 9, C(230, 228, 216))
    disc(i, 9, 8, 3, C(224, 142, 30)); disc(i, 9, 8, 2, C(255, 200, 64))
    put(i, 8, 7, C(255, 238, 130)); put(i, 10, 9, C(190, 104, 22)); put(i, 6, 10, C(222, 220, 206)); put(i, 11, 6, C(255, 250, 230))

def t_rotebeete_chips(i):
    for (x, y, r, col) in [(6, 10, 3, C(148, 36, 70)), (10, 10, 3, C(170, 45, 82)),
                           (8, 7, 3, C(190, 54, 92)), (8, 12, 2, C(128, 30, 58))]:
        disc(i, x, y, r, col)
    for (x, y) in [(6, 10), (10, 10), (8, 7), (8, 12)]:
        put(i, x, y, C(102, 22, 46))
        put(i, x - 1, y - 1, C(218, 82, 116))
        put(i, x + 1, y, C(124, 24, 54))
    line(i, 5, 8, 9, 12, C(112, 24, 50)); line(i, 9, 6, 12, 9, C(224, 82, 118))
    put(i, 5, 11, C(236, 92, 130)); put(i, 11, 8, C(96, 20, 44)); put(i, 9, 13, C(98, 18, 42))

def t_geroestete_karotte(i):
    for dy in range(3, 14):
        w = max(0, 13 - dy)
        rect(i, 8 - w // 2, dy, 8 + (w + 1) // 2, dy, C(222, 112, 36))
    rect(i, 7, 12, 9, 13, C(172, 76, 24))
    line(i, 6, 7, 9, 8, C(112, 50, 20)); line(i, 8, 10, 10, 11, C(112, 50, 20)); put(i, 7, 12, C(104, 44, 18))
    put(i, 7, 5, C(255, 166, 64)); put(i, 9, 6, C(236, 132, 42)); put(i, 8, 8, C(246, 146, 46))
    rect(i, 7, 1, 9, 3, C(70, 150, 52)); put(i, 6, 2, C(92, 174, 70)); put(i, 10, 2, C(50, 124, 44)); put(i, 8, 0, C(96, 184, 72))

def t_pommes(i):
    rect(i, 4, 9, 11, 14, C(168, 38, 34)); rect(i, 4, 9, 11, 10, C(230, 72, 52))
    rect(i, 5, 13, 10, 14, C(104, 24, 24)); put(i, 4, 11, C(214, 58, 48)); put(i, 11, 12, C(122, 26, 26)); put(i, 6, 10, C(242, 92, 68))
    for x, top, col in ((4, 4, C(236, 196, 74)), (6, 2, C(248, 216, 104)),
                        (8, 3, C(232, 188, 64)), (10, 2, C(250, 224, 120))):
        rect(i, x, top, x + 1, 10, col); put(i, x, top, C(255, 236, 140))
    put(i, 5, 7, C(204, 160, 52)); put(i, 9, 5, C(255, 242, 150)); put(i, 11, 8, C(218, 170, 56))

def t_marshmallow(i):
    line(i, 3, 15, 13, 2, C(126, 86, 46))
    rect(i, 5, 4, 11, 12, C(248, 234, 238)); rect(i, 5, 4, 11, 5, C(255, 252, 253))
    rect(i, 5, 8, 11, 9, C(246, 202, 214)); rect(i, 6, 12, 10, 13, C(214, 160, 138))
    put(i, 7, 6, C(255, 255, 255)); put(i, 9, 10, C(226, 184, 198)); put(i, 10, 12, C(150, 82, 54)); put(i, 6, 11, C(236, 190, 162))

def t_kaiserbroetchen(i):
    disc(i, 8, 9, 5, C(204, 144, 76))
    rect(i, 4, 9, 12, 12, C(190, 126, 62))
    disc(i, 8, 7, 4, C(226, 174, 104))
    line(i, 5, 9, 8, 6, C(120, 72, 36)); line(i, 8, 6, 11, 9, C(120, 72, 36)); line(i, 6, 11, 11, 10, C(146, 86, 42))
    line(i, 6, 7, 10, 7, C(250, 224, 160)); put(i, 5, 6, C(238, 190, 120)); put(i, 11, 7, C(176, 104, 50))
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
    line(i, 4, 6, 10, 12, C(238, 220, 150)); put(i, 6, 12, C(188, 160, 96)); put(i, 12, 8, C(255, 244, 190))

def t_kaese(i):
    # gelber Keil mit Loechern
    for y in range(3, 14):
        rect(i, 3, y, 3 + (y - 3), y, C(244, 204, 72))
    for (x, y) in [(5, 8), (7, 11), (8, 6)]:
        put(i, x, y, C(222, 176, 48)); put(i, x + 1, y, C(222, 176, 48))
    for y in range(3, 14):
        put(i, 3, y, C(206, 162, 40))
    line(i, 4, 4, 12, 12, C(255, 226, 96)); put(i, 9, 10, C(190, 146, 34)); put(i, 11, 12, C(188, 140, 30))
    put(i, 6, 5, C(255, 236, 118)); put(i, 4, 12, C(178, 126, 30))

def t_sourcream(i):
    rect(i, 4, 7, 11, 13, C(206, 210, 202)); rect(i, 3, 6, 12, 8, C(236, 236, 226))
    rect(i, 4, 5, 11, 6, C(255, 252, 238)); rect(i, 5, 9, 10, 12, C(244, 244, 234))
    line(i, 5, 9, 10, 11, C(255, 255, 248)); line(i, 6, 11, 11, 9, C(190, 196, 186))
    put(i, 6, 6, C(255, 255, 250)); put(i, 8, 8, C(250, 250, 238)); put(i, 10, 10, C(178, 184, 174))
    put(i, 6, 10, C(102, 150, 74)); put(i, 9, 10, C(102, 150, 74)); put(i, 11, 8, C(154, 160, 150))

def t_sauce(i):
    # Glas/Napf mit roter Sauce, Tomatenstuecken und Kraeutern
    rect(i, 4, 5, 11, 13, C(168, 34, 34)); rect(i, 4, 5, 11, 6, C(226, 64, 56))
    rect(i, 5, 4, 10, 4, C(120, 22, 22)); rect(i, 3, 13, 12, 14, C(94, 18, 18))
    put(i, 6, 8, C(238, 86, 76)); put(i, 9, 10, C(214, 64, 58)); put(i, 7, 6, C(255, 104, 92))
    put(i, 8, 9, C(112, 20, 18)); put(i, 10, 7, C(78, 126, 48)); put(i, 5, 11, C(232, 80, 68)); put(i, 11, 9, C(92, 146, 54))

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
    rect(i, 4, 9, 11, 9, C(226, 52, 42))        # Tomate
    _bun_bottom(i)

def t_cheeseburger(i):
    _bun_top(i)
    rect(i, 3, 5, 12, 6, C(122, 64, 44))        # Fleisch
    rect(i, 4, 6, 11, 6, C(88, 44, 30))
    rect(i, 3, 7, 12, 7, C(244, 204, 72))       # Kaese
    put(i, 2, 8, C(240, 196, 64)); put(i, 12, 8, C(240, 196, 64))   # Kaese-Tropfen
    put(i, 5, 8, C(255, 224, 96)); put(i, 9, 8, C(255, 224, 96))
    rect(i, 3, 9, 12, 9, C(118, 60, 40))        # zweite Fleischkante
    put(i, 4, 5, C(150, 82, 54)); put(i, 10, 9, C(78, 34, 24))
    _bun_bottom(i)

def t_chicken_nuggets(i):
    for (x, y, r, col) in [(5, 5, 2, C(226, 174, 92)), (10, 6, 2, C(214, 158, 78)),
                           (6, 10, 3, C(232, 184, 104)), (11, 11, 2, C(220, 168, 86))]:
        disc(i, x, y, r, col); put(i, x, y, C(178, 126, 54))
        put(i, x + 1, y - 1, C(252, 216, 140)); put(i, x - 1, y + 1, C(150, 94, 40))
    put(i, 4, 11, C(246, 206, 130)); put(i, 12, 10, C(150, 92, 38))

def t_schaschlik(i):
    for k in range(12):
        put(i, 2 + k, 13 - k, C(144, 104, 56))          # Spiess
    disc(i, 5, 10, 2, C(124, 62, 44))                    # Fleisch
    disc(i, 8, 7, 2, C(224, 112, 38))                    # Karotte
    disc(i, 11, 4, 2, C(170, 132, 70))
    disc(i, 10, 6, 1, C(92, 150, 52))
    put(i, 5, 10, C(94, 44, 30)); put(i, 11, 4, C(110, 55, 35)); put(i, 8, 6, C(250, 152, 58)); put(i, 7, 8, C(230, 210, 120))

def t_ofenkartoffel_sourcream(i):
    disc(i, 8, 9, 6, C(178, 132, 74)); disc(i, 8, 9, 5, C(206, 158, 96))
    rect(i, 5, 6, 11, 8, C(120, 84, 46))                 # Schnitt
    rect(i, 6, 6, 10, 7, C(246, 246, 240))               # Sauerrahm
    put(i, 7, 5, C(96, 150, 60)); put(i, 9, 5, C(96, 150, 60)); put(i, 10, 7, C(86, 136, 54))  # Schnittlauch
    put(i, 5, 10, C(146, 94, 52)); put(i, 11, 10, C(230, 180, 112)); put(i, 4, 8, C(120, 72, 42)); put(i, 10, 6, C(255, 255, 248))

def t_spaghetti(i):
    rect(i, 3, 9, 12, 13, C(134, 82, 48)); rect(i, 3, 9, 12, 9, C(184, 122, 74))  # Teller
    for x in range(4, 12):
        put(i, x, 6 + (x % 3), C(240, 225, 162))
        put(i, x, 5 + (x % 2), C(226, 208, 140))
    line(i, 4, 8, 12, 6, C(250, 236, 176)); line(i, 5, 7, 11, 10, C(216, 196, 128))
    put(i, 7, 5, C(190, 40, 40)); put(i, 9, 6, C(190, 40, 40)); put(i, 8, 8, C(212, 70, 42)); put(i, 10, 8, C(90, 128, 48)); put(i, 6, 6, C(255, 238, 176))

def t_misosuppe(i):
    rect(i, 3, 8, 12, 13, C(132, 78, 48)); rect(i, 3, 8, 12, 8, C(196, 134, 76))
    rect(i, 4, 6, 11, 8, C(204, 142, 58)); rect(i, 5, 7, 10, 8, C(170, 98, 44))  # Bruehe
    put(i, 6, 7, C(70, 130, 70)); put(i, 9, 7, C(70, 130, 70)); put(i, 10, 6, C(52, 96, 52))  # Seetang
    put(i, 8, 6, C(240, 240, 236)); put(i, 5, 6, C(240, 225, 162)); put(i, 7, 8, C(150, 80, 36)); put(i, 11, 8, C(224, 170, 72))

def t_kandierter_apfel(i):
    disc(i, 8, 9, 5, C(216, 52, 46)); disc(i, 8, 9, 4, C(186, 36, 34))
    disc(i, 6, 7, 2, C(240, 120, 110)); put(i, 5, 6, C(255, 178, 160))  # Glanz
    rect(i, 8, 2, 8, 4, C(150, 110, 60))                 # Stiel
    line(i, 7, 13, 10, 15, C(146, 30, 28))
    for x in range(4, 13):
        put(i, x, 13, C(120, 22, 22))
    put(i, 10, 6, C(236, 86, 70)); put(i, 7, 12, C(144, 24, 24))

def t_reis(i):
    # kleiner Haufen weisser Reiskoerner
    disc(i, 8, 9, 5, C(245, 245, 236))
    disc(i, 7, 7, 3, C(250, 250, 244))
    for (x, y) in [(6, 8), (9, 8), (7, 10), (10, 10), (8, 9), (6, 11), (9, 11), (11, 9), (5, 10)]:
        put(i, x, y, C(220, 220, 208)); put(i, x + 1, y, C(250, 250, 244))
    put(i, 5, 12, C(200, 200, 190)); put(i, 11, 11, C(255, 255, 248))

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
#  Neue Pflanzen: Ernte-Sprites, eigene Samen und eigene Crop-Overlays
# ---------------------------------------------------------------------------

def leaf(i, x, y, col):
    put(i, x, y, col)
    put(i, x + 1, y, _mul(col, 1.12))
    put(i, x, y + 1, _mul(col, 0.78))

def sparkle(i, x, y, col=C(255, 238, 170)):
    put(i, x, y, col)
    put(i, x + 1, y, _mul(col, 0.82))

def t_tomato_seeds(i):
    for x, y in [(5, 7), (7, 6), (10, 6), (6, 10), (9, 10), (11, 8)]:
        put(i, x, y, C(212, 54, 44)); put(i, x, y + 1, C(134, 28, 26))
    line(i, 4, 12, 10, 4, C(78, 142, 58)); leaf(i, 9, 5, C(108, 176, 80))

def t_onion_seeds(i):
    for x, y in [(5, 7), (7, 6), (9, 7), (11, 8), (6, 10), (10, 11)]:
        put(i, x, y, C(230, 214, 184)); put(i, x, y + 1, C(176, 144, 120))
    line(i, 8, 12, 8, 4, C(98, 154, 76)); put(i, 7, 5, C(128, 182, 94)); put(i, 9, 6, C(128, 182, 94))

def t_lettuce_seeds(i):
    for x, y in [(6, 8), (8, 7), (10, 8), (7, 10), (9, 11)]:
        leaf(i, x, y, C(102, 170, 78))
    disc(i, 8, 9, 1, C(148, 202, 110)); put(i, 5, 12, C(82, 136, 62)); put(i, 11, 12, C(82, 136, 62))

def t_corn_seeds(i):
    for x, y in [(6, 5), (8, 5), (10, 6), (5, 8), (7, 8), (9, 9), (11, 10)]:
        put(i, x, y, C(236, 202, 74)); put(i, x, y + 1, C(178, 128, 42))
    line(i, 4, 13, 12, 6, C(82, 142, 58)); leaf(i, 10, 7, C(108, 174, 76))

def t_cucumber_seeds(i):
    for k in range(5):
        x, y = 5 + k, 9 - k
        put(i, x, y, C(168, 212, 116)); put(i, x + 1, y, C(90, 148, 70)); put(i, x, y + 1, C(68, 118, 54))
    for x, y in [(9, 10), (11, 11), (6, 12)]:
        put(i, x, y, C(126, 184, 92))

def t_garlic_seeds(i):
    for x, y in [(5, 9), (7, 7), (9, 8), (11, 10), (8, 11)]:
        disc(i, x, y, 1, C(238, 234, 222)); put(i, x, y + 1, C(178, 168, 154))
    line(i, 8, 12, 9, 4, C(112, 154, 84)); leaf(i, 9, 5, C(134, 182, 96))

def t_chili_seeds(i):
    for x, y in [(5, 7), (7, 6), (9, 7), (11, 9), (8, 10), (6, 11)]:
        put(i, x, y, C(202, 44, 38)); put(i, x + 1, y, C(246, 96, 82)); put(i, x, y + 1, C(128, 22, 20))
    line(i, 4, 12, 7, 4, C(72, 142, 58))

def t_strawberry_seeds(i):
    disc(i, 8, 9, 3, C(202, 44, 42)); put(i, 6, 7, C(244, 218, 90)); put(i, 9, 8, C(244, 218, 90))
    put(i, 7, 10, C(244, 218, 90)); put(i, 10, 10, C(244, 218, 90))
    for x in range(6, 11): put(i, x, 5, C(78, 150, 62))

def t_blueberry_seeds(i):
    for x, y in [(6, 7), (9, 6), (11, 9), (7, 10), (10, 11)]:
        disc(i, x, y, 1, C(70, 86, 168)); put(i, x - 1, y - 1, C(120, 142, 212))
    line(i, 5, 12, 11, 5, C(74, 132, 62)); leaf(i, 10, 5, C(100, 168, 78))

def t_soybean_seeds(i):
    for x, y in [(6, 8), (8, 7), (10, 8), (7, 11), (10, 11)]:
        disc(i, x, y, 1, C(142, 190, 94)); put(i, x, y + 1, C(88, 132, 56))
    line(i, 5, 13, 11, 5, C(88, 140, 62)); put(i, 12, 7, C(122, 170, 82))

def t_cotton_seeds(i):
    for x, y in [(6, 8), (9, 7), (11, 9), (7, 11)]:
        disc(i, x, y, 1, C(244, 244, 238)); put(i, x, y + 1, C(176, 154, 132))
    line(i, 5, 13, 8, 5, C(118, 86, 54)); leaf(i, 8, 6, C(112, 152, 84))

def t_cabbage_seeds(i):
    disc(i, 8, 9, 3, C(82, 144, 62)); disc(i, 8, 9, 2, C(122, 186, 92)); put(i, 8, 9, C(160, 212, 126))
    for p in [(5, 8), (11, 8), (7, 12), (10, 12)]: put(i, *p, C(58, 116, 46))

def t_bell_pepper_seeds(i):
    disc(i, 7, 9, 2, C(202, 54, 46)); disc(i, 10, 9, 2, C(226, 74, 58))
    put(i, 6, 8, C(250, 116, 96)); put(i, 9, 8, C(250, 116, 96))
    line(i, 8, 7, 8, 4, C(76, 142, 58)); leaf(i, 7, 5, C(108, 174, 76))

def t_pineapple_seeds(i):
    rect(i, 6, 7, 10, 12, C(220, 176, 62)); rect(i, 7, 6, 9, 6, C(246, 210, 92))
    for p in [(8, 3), (7, 4), (9, 4), (6, 5), (10, 5)]: put(i, *p, C(80, 150, 62))
    line(i, 6, 8, 10, 12, C(158, 116, 36))

def t_grapes_seeds(i):
    for x, y in [(6, 8), (8, 7), (10, 8), (7, 10), (9, 11), (11, 10)]:
        put(i, x, y, C(124, 70, 156)); put(i, x - 1, y - 1, C(176, 124, 204))
    line(i, 8, 6, 8, 3, C(112, 82, 48)); leaf(i, 9, 4, C(92, 150, 70))

def t_coffee_beans_seeds(i):
    for x, y in [(6, 8), (9, 7), (11, 10), (7, 11)]:
        disc(i, x, y, 1, C(128, 80, 44)); put(i, x, y, C(82, 48, 26)); put(i, x - 1, y - 1, C(170, 116, 70))
    line(i, 5, 13, 11, 5, C(78, 132, 62))

def t_zucchini_seeds(i):
    for k in range(6):
        x, y = 5 + k, 12 - k
        put(i, x, y, C(58, 118, 46)); put(i, x + 1, y - 1, C(92, 154, 70)); put(i, x + 1, y, C(34, 82, 32))
    sparkle(i, 8, 8, C(152, 190, 112))

def t_eggplant_seeds(i):
    for x, y in [(6, 8), (9, 7), (11, 9), (7, 11), (10, 11)]:
        disc(i, x, y, 1, C(112, 64, 148)); put(i, x - 1, y - 1, C(170, 128, 204))
    line(i, 8, 7, 8, 3, C(72, 140, 58)); leaf(i, 7, 4, C(102, 166, 78))

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

# id -> (display, draw, seed-draw, custom_model_data-base)
PLANTS = [
    ("tomato", "Tomato", t_tomato, t_tomato_seeds),
    ("onion", "Onion", t_onion, t_onion_seeds),
    ("lettuce", "Lettuce", t_lettuce, t_lettuce_seeds),
    ("corn", "Corn", t_corn, t_corn_seeds),
    ("cucumber", "Cucumber", t_cucumber, t_cucumber_seeds),
    ("garlic", "Garlic", t_garlic, t_garlic_seeds),
    ("chili", "Chili", t_chili, t_chili_seeds),
    ("strawberry", "Strawberry", t_strawberry, t_strawberry_seeds),
    ("blueberry", "Blueberry", t_blueberry, t_blueberry_seeds),
    ("soybean", "Soybean", t_soybean, t_soybean_seeds),
    ("cotton", "Cotton", t_cotton, t_cotton_seeds),
    ("cabbage", "Cabbage", t_cabbage, t_cabbage_seeds),
    ("bell_pepper", "Bell Pepper", t_bell_pepper, t_bell_pepper_seeds),
    ("pineapple", "Pineapple", t_pineapple, t_pineapple_seeds),
    ("grapes", "Grapes", t_grapes, t_grapes_seeds),
    ("coffee_beans", "Coffee Beans", t_coffee_beans, t_coffee_beans_seeds),
    ("zucchini", "Zucchini", t_zucchini, t_zucchini_seeds),
    ("eggplant", "Eggplant", t_eggplant, t_eggplant_seeds),
]

_cmd = 3030
for _pid, _pname, _pdraw, _seed_draw in PLANTS:
    DRAW[_pid] = _pdraw
    DRAW[_pid + "_seeds"] = _seed_draw
    ITEMS[_pid] = ("<white>" + _pname, "PAPER", _cmd)
    ITEMS[_pid + "_seeds"] = ("<white>" + _pname + " Seeds", "WHEAT_SEEDS", _cmd + 1)
    _cmd += 2

# ---------------------------------------------------------------------------
#  Custom-Crop-Texturen (eine je Pflanze, wird beim Wachsen skaliert)
# ---------------------------------------------------------------------------

def crop_base(i, xs=(5, 8, 11), tops=(5, 3, 6), stem=C(86, 150, 66)):
    _soil(i, min(xs) - 1, max(xs) + 1)
    for x0, top in zip(xs, tops):
        line(i, x0, 14, x0, top, stem)
        line(i, x0, 14, x0 - 1, top + 2, _mul(stem, 0.72))
        put(i, x0 + 1, top + 1, _mul(stem, 1.2))

def crop_reis(i):
    t_reis_crop_stage7(i)
    # allgemeines Rice-Crop-Overlay: reife Aehren plus kleiner Wasser-/Feldsaum,
    # damit es nicht die finale Age-Stage dupliziert.
    rect(i, 2, 13, 13, 13, C(58, 96, 92))
    put(i, 4, 13, C(90, 132, 122)); put(i, 10, 13, C(90, 132, 122))
    put(i, 2, 12, C(202, 178, 92)); put(i, 13, 12, C(202, 178, 92))

def crop_tomato(i):
    crop_base(i, (4, 7, 10, 12), (6, 4, 5, 7))
    for p in [(6, 6), (9, 6), (10, 8), (5, 9)]: disc(i, p[0], p[1], 1, C(204, 54, 46)); sparkle(i, p[0]-1, p[1]-1, C(246, 116, 96))

def crop_onion(i):
    crop_base(i, (5, 8, 11), (6, 4, 6), C(96, 160, 82))
    for p in [(5, 12), (8, 11), (11, 12)]: disc(i, p[0], p[1], 1, C(214, 194, 164))
    line(i, 8, 4, 8, 1, C(126, 184, 96))

def crop_lettuce(i):
    crop_base(i, (5, 8, 11), (7, 5, 7))
    disc(i, 8, 10, 4, C(74, 138, 58)); disc(i, 8, 9, 2, C(138, 198, 110))
    for p in [(5, 8), (11, 8), (6, 12), (10, 12)]: put(i, *p, C(56, 116, 44))

def crop_corn(i):
    crop_base(i, (5, 8, 11), (5, 2, 5), C(86, 150, 64))
    rect(i, 7, 4, 9, 11, C(226, 190, 60)); line(i, 6, 5, 5, 12, C(78, 132, 56)); line(i, 10, 5, 11, 12, C(78, 132, 56))
    for y in (5, 7, 9): put(i, 8, y, C(252, 226, 102))

def crop_cucumber(i):
    crop_base(i, (4, 7, 10, 12), (7, 5, 6, 8))
    line(i, 5, 10, 11, 6, C(72, 134, 52)); line(i, 5, 11, 11, 7, C(46, 94, 38)); sparkle(i, 7, 8, C(152, 196, 110))

def crop_garlic(i):
    crop_base(i, (5, 8, 11), (7, 3, 7), C(104, 152, 84))
    for p in [(6, 12), (8, 11), (10, 12)]: disc(i, p[0], p[1], 1, C(236, 232, 222))
    line(i, 8, 3, 8, 1, C(146, 184, 116))

def crop_chili(i):
    crop_base(i, (4, 7, 10, 12), (5, 3, 5, 7), C(78, 146, 60))
    line(i, 5, 7, 7, 10, C(202, 44, 38)); line(i, 9, 5, 12, 8, C(202, 44, 38)); put(i, 6, 8, C(246, 96, 82)); put(i, 10, 6, C(246, 96, 82))

def crop_strawberry(i):
    crop_base(i, (4, 7, 10, 12), (7, 5, 7, 8), C(72, 142, 60))
    for p in [(6, 8), (9, 8), (8, 10), (11, 9)]: disc(i, p[0], p[1], 1, C(206, 48, 44)); put(i, p[0], p[1], C(246, 108, 92))

def crop_blueberry(i):
    crop_base(i, (5, 8, 11), (4, 3, 5), C(72, 128, 62))
    for p in [(6, 5), (9, 5), (11, 7), (7, 8), (10, 9)]: disc(i, p[0], p[1], 1, C(70, 86, 168)); put(i, p[0]-1, p[1]-1, C(128, 148, 214))

def crop_soybean(i):
    crop_base(i, (5, 8, 11), (5, 3, 5), C(82, 138, 62))
    for p in [(6, 8), (8, 6), (10, 7), (9, 10)]: disc(i, p[0], p[1], 1, C(144, 190, 96))
    line(i, 5, 12, 11, 6, C(104, 158, 70))

def crop_cotton(i):
    crop_base(i, (5, 8, 11), (5, 3, 5), C(104, 138, 76))
    for p in [(6, 6), (9, 5), (11, 7), (8, 8)]: disc(i, p[0], p[1], 1, C(246, 246, 240)); put(i, p[0], p[1], C(255, 255, 252))

def crop_cabbage(i):
    crop_base(i, (5, 8, 11), (7, 5, 7), C(82, 140, 62))
    disc(i, 8, 10, 4, C(66, 128, 54)); disc(i, 8, 9, 3, C(104, 170, 80)); disc(i, 8, 9, 1, C(152, 210, 116))

def crop_bell_pepper(i):
    crop_base(i, (4, 7, 10, 12), (5, 3, 5, 7), C(80, 148, 60))
    for p in [(6, 7), (9, 7), (11, 9)]: disc(i, p[0], p[1], 2, C(200, 54, 46)); put(i, p[0]-1, p[1]-1, C(246, 112, 92))

def crop_pineapple(i):
    crop_base(i, (5, 8, 11), (8, 5, 8), C(72, 132, 58))
    rect(i, 6, 7, 10, 13, C(218, 174, 58)); line(i, 6, 8, 10, 12, C(156, 112, 34))
    for p in [(8, 3), (7, 4), (9, 4), (6, 5), (10, 5)]: put(i, *p, C(72, 142, 58))

def crop_grapes(i):
    crop_base(i, (5, 8, 11), (4, 3, 5), C(82, 138, 62))
    for p in [(6, 7), (8, 6), (10, 7), (7, 9), (9, 10), (11, 9)]: disc(i, p[0], p[1], 1, C(124, 70, 156)); put(i, p[0]-1, p[1]-1, C(176, 124, 204))

def crop_coffee_beans(i):
    crop_base(i, (5, 8, 11), (4, 3, 5), C(76, 130, 62))
    for p in [(6, 6), (9, 5), (11, 8), (7, 10)]: disc(i, p[0], p[1], 1, C(126, 78, 42)); line(i, p[0], p[1]-1, p[0], p[1]+1, C(76, 44, 24))

def crop_zucchini(i):
    crop_base(i, (4, 7, 10, 12), (7, 5, 6, 8), C(72, 136, 58))
    line(i, 5, 11, 12, 6, C(50, 108, 42)); line(i, 5, 10, 12, 5, C(78, 142, 62)); sparkle(i, 8, 8, C(132, 180, 96))

def crop_eggplant(i):
    crop_base(i, (5, 8, 11), (5, 3, 5), C(76, 142, 60))
    for p in [(7, 8), (10, 9), (9, 6)]: disc(i, p[0], p[1], 2, C(104, 58, 136)); put(i, p[0]-1, p[1]-1, C(168, 120, 202))

# produce-id -> eigener Crop-Renderer (Crop-Item: sas_<produce>_crop)
CROPS = [
    ("reis", crop_reis),
    ("tomato", crop_tomato),
    ("onion", crop_onion),
    ("lettuce", crop_lettuce),
    ("corn", crop_corn),
    ("cucumber", crop_cucumber),
    ("garlic", crop_garlic),
    ("chili", crop_chili),
    ("strawberry", crop_strawberry),
    ("blueberry", crop_blueberry),
    ("soybean", crop_soybean),
    ("cotton", crop_cotton),
    ("cabbage", crop_cabbage),
    ("bell_pepper", crop_bell_pepper),
    ("pineapple", crop_pineapple),
    ("grapes", crop_grapes),
    ("coffee_beans", crop_coffee_beans),
    ("zucchini", crop_zucchini),
    ("eggplant", crop_eggplant),
]

_crop_cmd = 3070
for _cid, _crop_draw in CROPS:
    EXTRA_TEXTURES["crops/" + _cid + "_crop"] = _crop_draw
    ITEMS[_cid + "_crop"] = ("<green>" + _cid.replace("_", " ").title() + " Crop", "PAPER",
                             _crop_cmd, "sas/crops/" + _cid + "_crop.png")
    _crop_cmd += 1

# ---------------------------------------------------------------------------
#  0.8.0: Neue Gerichte (Kessel-GUI-Rezepte und Werkbank)
# ---------------------------------------------------------------------------

def t_beerenkekse(i):
    disc(i, 8, 9, 5, C(196, 140, 82)); disc(i, 7, 7, 3, C(216, 162, 102))
    for p in [(6, 8), (10, 9), (8, 11), (9, 6), (6, 11)]:
        put(i, *p, C(120, 40, 70)); put(i, p[0], p[1] + 1, C(90, 28, 54))
    put(i, 5, 9, C(150, 100, 56)); put(i, 11, 7, C(224, 176, 116))

def t_kaesekuchen_beeren(i):
    for dy in range(5, 13):
        w = dy - 4
        rect(i, 4, dy, 4 + w, dy, C(240, 220, 150))
    rect(i, 4, 12, 12, 13, C(178, 128, 70))          # Boden
    for x in range(4, 12):
        put(i, x, 5 + (x - 4), C(184, 40, 62))        # Beerensauce an der Kante
    put(i, 6, 8, C(220, 70, 92)); put(i, 8, 10, C(220, 70, 92)); put(i, 5, 11, C(250, 236, 176))

def t_sushi(i):
    rect(i, 4, 9, 12, 13, C(244, 242, 232))           # Reis
    rect(i, 4, 6, 12, 9, C(224, 96, 92))              # Lachs
    for x in range(5, 12, 2): put(i, x, 8, C(242, 148, 132))
    rect(i, 7, 6, 9, 13, C(42, 62, 50))               # Nori
    put(i, 8, 7, C(60, 84, 68))

def t_sakura_sushi(i):
    rect(i, 4, 9, 12, 13, C(244, 242, 232))           # Reis
    disc(i, 8, 7, 3, C(244, 178, 200))                # Kirschbluete
    for p in [(6, 6), (10, 6), (7, 9), (9, 9), (8, 4)]: put(i, *p, C(236, 140, 176))
    put(i, 8, 7, C(250, 220, 120))

def t_onigiri(i):
    for dy in range(3, 13):
        w = dy - 2
        rect(i, 8 - w // 2, dy, 8 + (w + 1) // 2, dy, C(246, 244, 236))
    rect(i, 6, 11, 10, 13, C(40, 44, 40))             # Nori
    put(i, 7, 7, C(224, 222, 214)); put(i, 9, 9, C(255, 255, 250))

def t_apfelsaft(i):
    rect(i, 5, 4, 10, 13, C(214, 220, 228))           # Glas
    rect(i, 6, 6, 9, 12, C(232, 150, 40))             # Saft
    rect(i, 6, 6, 9, 7, C(248, 182, 74))
    put(i, 7, 9, C(255, 196, 96)); put(i, 8, 3, C(200, 60, 50))

def t_kirschlimo(i):
    rect(i, 5, 4, 10, 13, C(214, 220, 228))           # Glas
    rect(i, 6, 6, 9, 12, C(236, 96, 130))             # Limo
    for p in [(7, 7), (8, 9), (7, 11)]: put(i, *p, C(252, 202, 222))
    put(i, 8, 3, C(228, 84, 118))

def t_oel(i):
    rect(i, 6, 5, 9, 13, C(228, 200, 90))             # Oel-Koerper
    rect(i, 7, 2, 8, 4, C(184, 184, 184))             # Hals/Deckel
    rect(i, 6, 5, 9, 6, C(246, 226, 132))
    put(i, 7, 9, C(255, 240, 160)); put(i, 8, 11, C(206, 176, 70))

def t_tintenfischringe(i):
    for (x, y) in [(6, 7), (10, 8), (7, 11)]:
        disc(i, x, y, 2, C(226, 176, 96)); put(i, x, y, C(178, 128, 68))
        put(i, x - 1, y - 1, C(244, 206, 140))

def t_chips(i):
    for (x, y, c) in [(6, 6, C(238, 206, 96)), (9, 8, C(230, 196, 80)), (7, 11, C(244, 214, 110))]:
        disc(i, x, y, 2, c); put(i, x, y, _mul(c, 0.82))
    put(i, 5, 9, C(214, 176, 66)); put(i, 11, 10, C(248, 220, 120))

def t_creeper_keks(i):
    disc(i, 8, 9, 5, C(76, 170, 74)); disc(i, 7, 7, 3, C(102, 192, 98))
    rect(i, 6, 6, 7, 7, C(28, 58, 30)); rect(i, 9, 6, 10, 7, C(28, 58, 30))   # Augen
    rect(i, 7, 8, 9, 11, C(28, 58, 30))                                        # Mund
    put(i, 7, 11, C(28, 58, 30)); put(i, 9, 11, C(28, 58, 30))

def t_schmalzgebaeck(i):
    disc(i, 8, 9, 5, C(206, 150, 82)); disc(i, 8, 9, 4, C(228, 174, 106))
    line(i, 5, 7, 11, 12, C(168, 118, 62)); line(i, 11, 7, 5, 12, C(168, 118, 62))  # Zopf
    for p in [(6, 6), (10, 6), (8, 12)]: put(i, *p, C(246, 214, 154))               # Zucker

# id -> (display, material, custom_model_data, draw)
NEW_DISHES = [
    ("beerenkekse", "<#d98a4a>Berry Cookies", "COOKIE", 3089, t_beerenkekse),
    ("kaesekuchen_beeren", "<#f3d9a0>Cheesecake with Berry Sauce", "PAPER", 3090, t_kaesekuchen_beeren),
    ("sushi", "<#e8e0d0>Sushi", "PAPER", 3091, t_sushi),
    ("sakura_sushi", "<#f4c2d0>Sakura Sushi", "PAPER", 3092, t_sakura_sushi),
    ("onigiri", "<#f2efe6>Onigiri", "PAPER", 3093, t_onigiri),
    ("apfelsaft", "<#e2a33a>Apple Juice", "HONEY_BOTTLE", 3094, t_apfelsaft),
    ("kirschlimo", "<#f0668a>Cherry Lemonade", "HONEY_BOTTLE", 3095, t_kirschlimo),
    ("oel", "<#e8c34a>Oil", "HONEY_BOTTLE", 3096, t_oel),
    ("tintenfischringe", "<#d9b48a>Calamari Rings", "PAPER", 3097, t_tintenfischringe),
    ("chips", "<#f0d060>Chips", "PAPER", 3098, t_chips),
    ("creeper_keks", "<#4caf50>Creeper Cookie", "COOKIE", 3099, t_creeper_keks),
    ("schmalzgebaeck", "<#e8c890>Lard Pastry", "PAPER", 3100, t_schmalzgebaeck),
]
for _did, _dname, _dmat, _dcmd, _dfn in NEW_DISHES:
    DRAW[_did] = _dfn
    ITEMS[_did] = (_dname, _dmat, _dcmd)

OUTLINE = C(44, 34, 28, 255)

# ---------------------------------------------------------------------------
#  Handgemalte Overrides: fertige 16x16-Texturen aus textures/ werden 1:1
#  uebernommen (ohne Shading/Outline). Fehlt eine Datei, wird auf die
#  prozedurale Zeichnung zurueckgefallen.
# ---------------------------------------------------------------------------

OVERRIDE_DIR = os.path.join(ROOT, "textures")

OVERRIDES = {
    "rotebeete_chips": "pixellab-Beetroot-Chips-Food-1783247070643.png",
    "bell_pepper": "bell_pepper.png",
    "bell_pepper_seeds": "pixellab-Bell-Pepper-more-small-Seeds-1783243783574.png",
    "blueberry": "pixellab-Blueberry-Food-1783244059622.png",
    "blueberry_seeds": "pixellab-Blueberry-Seed-1783244005350.png",
    "teig": "pixellab-Bread-Dough-1783247510769.png",
    "burger": "pixellab-Burger-Food-1783244180445.png",
    "cabbage": "pixellab-Cabbage-Food-1783244219809.png",
    "cabbage_seeds": "pixellab-Cabbage-Seeds-1783244279340.png",
    "kandierter_apfel": "pixellab-Candy-Apple-Food-1783246248381.png",
    "cheeseburger": "pixellab-Cheese-Burger-Food--1783244324497.png",
    "kaese": "pixellab-Cheese-Food-1783245790388.png",
    "chicken_nuggets": "pixellab-Chicken-Nuggets-Food---1783244380107.png",
    "chili": "pixellab-Chilli-Food--1783244430130.png",
    "chili_seeds": "pixellab-Chilli-Seeds-1783244486254.png",
    "coffee_beans": "pixellab-Coffee-Beans-Food-1783244572620.png",
    "coffee_beans_seeds": "pixellab-Coffee-Beans-Seeds-Food-1783244603352.png",
    "corn": "pixellab-Corn-Food-1783244674323.png",
    "corn_seeds": "pixellab-Corn-Seeds-Food-1783244698825.png",
    "cotton": "pixellab-Cotton-Food-1783244732173.png",
    "cotton_seeds": "pixellab-Cotton-Seeds-Food-1783244875510.png",
    "cucumber": "pixellab-Cucumber-as-a-food-1783245117238.png",
    "cucumber_seeds": "pixellab-Cucumber-seeds-1783245160861.png",
    "eggplant": "pixellab-Eggplant-Food-1783245252106.png",
    "eggplant_seeds": "pixellab-Eggplant-Seeds-Food-1783245286554.png",
    "spiegelei": "pixellab-Fried-Egg-1783247367810.png",
    "pommes": "pixellab-Fries-Food-1783246915017.png",
    "garlic": "pixellab-Garlic-Food-1783245334326.png",
    "garlic_seeds": "pixellab-Garlic-seeds-Food-1783245360740.png",
    "grapes": "pixellab-Grapes-Food-1783245630326.png",
    "grapes_seeds": "pixellab-Grapes-Seeds-Food-1783245734904.png",
    "kaiserbroetchen": "pixellab-kaiser-bread1783246175515.png",
    "lettuce": "pixellab-Lettuce-Food-1783246343003.png",
    "lettuce_seeds": "pixellab-Lettuce-Seeds-Food-1783246369879.png",
    "marshmallow": "pixellab-Marshmallow-Food-1783246456644.png",
    "misosuppe": "pixellab-Miso-Soup-in-a-bowl-1783246490761.png",
    "nudeln": "pixellab-Noddle-Food-1783246636927.png",
    "onion": "pixellab-Onion-Food-1783246739913.png",
    "onion_seeds": "pixellab-Onion-Seeds-Vegetable-1783246768453.png",
    "ofenkartoffel_sourcream": "pixellab-Oven-Potato-with-Sour-Cream-1783246705636.png",
    "pineapple": "pixellab-Pineapple-Vegetable-1783246802590.png",
    "pineapple_seeds": "pixellab-Pineapple-Seeds-Vegetable-1783246844976.png",
    "sauce": "pixellab-Red-Sauce--1783247138422.png",
    "reis": "pixellab-Rice-grain--1783246980190.png",
    "reis_samen": "pixellab-Rice-seeds-Vegetable-1783247020431.png",
    "geroestete_karotte": "pixellab-Roasted-Carrot-Food-1783245532974.png",
    "schaschlik": "pixellab-Schaschlik-on-a-stick-Food-1783247194772.png",
    "sourcream": "pixellab-Sourcream-Sauce-1783247231959.png",
    "soybean": "pixellab-Soybean-Vegetable-1783247281408.png",
    "soybean_seeds": "pixellab-Soybean-Seeds-Vegetable-1783247304872.png",
    "spaghetti": "pixellab-Spaghetti-Food-1783247332524.png",
    "strawberry": "pixellab-Strawberry-Vegetable-1783247428008.png",
    "strawberry_seeds": "pixellab-Strawberry-Seeds-Vegetable-1783247467574.png",
    "tomato_seeds": "pixellab-Tomato-Seeds-Vegetable-1783247641978.png",
    "zucchini": "pixellab-Zucchini-Vegetable-1783247680210.png",
    "zucchini_seeds": "pixellab-Zucchini-Seeds-Vegetable-1783247780349.png",
}


def override_image(item_id):
    """Fertige Override-Textur (16x16 RGBA) fuer item_id oder None."""
    name = OVERRIDES.get(item_id)
    if not name:
        return None
    path = os.path.join(OVERRIDE_DIR, name)
    if not os.path.exists(path):
        print(f"  ! Override fuer '{item_id}' fehlt ({name}) - zeichne prozedural.")
        return None
    img = Image.open(path).convert("RGBA")
    if img.size != (16, 16):
        img = img.resize((16, 16), Image.NEAREST)
    return img


def generate():
    os.makedirs(TEX_DIR, exist_ok=True)
    os.makedirs(ITEMS_DIR, exist_ok=True)

    for item_id, fn in DRAW.items():
        override = override_image(item_id)
        if override is not None:
            override.save(os.path.join(TEX_DIR, item_id + ".png"))
            continue
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
