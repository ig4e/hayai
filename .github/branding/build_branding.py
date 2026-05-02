"""Render Android launcher / splash assets from the master Hayai SVGs.

Run with:  python .github/branding/build_branding.py
Requires:  Pillow, resvg-py  (pip install --user Pillow resvg-py)
"""

from __future__ import annotations

import re
from io import BytesIO
from pathlib import Path

from PIL import Image
import resvg_py

ROOT = Path(__file__).resolve().parents[2]
BRANDING = ROOT / ".github" / "branding"
RES = ROOT / "app" / "src" / "main" / "res"
DEBUG_RES = ROOT / "app" / "src" / "debug" / "res"
NIGHTLY_RES = ROOT / "app" / "src" / "nightly" / "res"
README_IMAGES = ROOT / ".github" / "readme-images"
APP_ROOT = ROOT / "app" / "src" / "main"

ICON_SVG = (BRANDING / "app-icon.svg").read_text(encoding="utf-8")
FG_SVG = (BRANDING / "app-icon-foreground.svg").read_text(encoding="utf-8")

DENSITY_SIZES = {
    "mdpi": 48,
    "hdpi": 72,
    "xhdpi": 96,
    "xxhdpi": 144,
    "xxxhdpi": 192,
}

# Adaptive-icon background-color variants (matches values/colors.xml).
# "classic" reuses the brand rosé so the legacy raster matches the default adaptive icon.
LAUNCHER_COLOR_VARIANTS = {
    "classic": "#B11A3B",
    "blue":    "#1E3A5F",
    "gray":    "#374151",
    "orange":  "#5C2E0E",
}

# Original brand rosé in the master SVG — what we swap out for variants.
BRAND_BG_HEX = "#B11A3B"


def render(svg: str, size: int) -> Image.Image:
    png_bytes = bytes(
        resvg_py.svg_to_bytes(svg_string=svg, width=size, height=size)
    )
    return Image.open(BytesIO(png_bytes)).convert("RGBA")


def save_png(svg: str, path: Path, size: int) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    img = render(svg, size)
    img.save(path, "PNG", optimize=True)
    print(f"  wrote {path.relative_to(ROOT)}  ({size}x{size})")


def save_webp(svg: str, path: Path, size: int, *, lossless: bool = True) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    img = render(svg, size)
    img.save(path, "WEBP", lossless=lossless, quality=100, method=6)
    print(f"  wrote {path.relative_to(ROOT)}  ({size}x{size})")


def desaturate_to_white(svg: str) -> str:
    """Return svg with every fill color forced to #FFFFFF (for monochrome adaptive icon)."""
    return re.sub(r'fill="#[0-9A-Fa-f]{3,8}"', 'fill="#FFFFFF"', svg)


PATH_RE = re.compile(
    r'<path\s+d="(?P<d>[^"]+)"\s+fill="(?P<fill>#[0-9A-Fa-f]{3,8})"\s*/>',
    re.DOTALL,
)


def extract_paths(svg: str) -> list[tuple[str, str]]:
    """Return [(pathData, fillColor)] from an SVG, in document order."""
    out: list[tuple[str, str]] = []
    for m in PATH_RE.finditer(svg):
        d = " ".join(m.group("d").split())
        out.append((d, m.group("fill").upper()))
    return out


def vector_drawable(
    paths: list[tuple[str, str]],
    *,
    width_dp: int,
    height_dp: int,
    viewport_w: float,
    viewport_h: float,
) -> str:
    lines = [
        '<?xml version="1.0" encoding="utf-8"?>',
        '<vector xmlns:android="http://schemas.android.com/apk/res/android"',
        f'    android:width="{width_dp}dp"',
        f'    android:height="{height_dp}dp"',
        f'    android:viewportWidth="{viewport_w:g}"',
        f'    android:viewportHeight="{viewport_h:g}">',
    ]
    for d, fill in paths:
        lines.append("    <path")
        lines.append(f'        android:pathData="{d}"')
        lines.append(f'        android:fillColor="{fill}" />')
    lines.append("</vector>")
    lines.append("")
    return "\n".join(lines)


def write_text(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content, encoding="utf-8")
    print(f"  wrote {path.relative_to(ROOT)}")


def write_vector_drawables() -> None:
    """Regenerate the Android vector drawables that mirror the master SVGs."""
    fg_paths = extract_paths(FG_SVG)
    icon_paths = extract_paths(ICON_SVG)

    print("== vector drawables ==")

    # Adaptive-icon foreground: white silhouette inside 108dp safe canvas
    fg_xml = vector_drawable(
        fg_paths,
        width_dp=108,
        height_dp=108,
        viewport_w=1343,
        viewport_h=1343,
    )
    write_text(RES / "drawable" / "ic_launcher_foreground.xml", fg_xml)

    # Themed-icon (monochrome) layer for adaptive icons — same silhouette
    write_text(RES / "drawable" / "ic_hayai_monochrome_launcher.xml", fg_xml)

    # Legacy/full ic_launcher drawable (with rosé background) used as drawable resource
    write_text(
        RES / "drawable" / "ic_launcher.xml",
        vector_drawable(
            icon_paths,
            width_dp=108,
            height_dp=108,
            viewport_w=1343,
            viewport_h=1343,
        ),
    )

    # Splash-screen vector — silhouette only (windowSplashScreenBackground sets the fill)
    write_text(
        RES / "drawable" / "ic_hayai.xml",
        vector_drawable(
            fg_paths,
            width_dp=288,
            height_dp=288,
            viewport_w=1343,
            viewport_h=1343,
        ),
    )


def main() -> None:
    write_vector_drawables()

    print("== mipmap launchers (PNG) ==")
    for variant in ("ic_launcher", "ic_launcher_round"):
        for density, size in DENSITY_SIZES.items():
            for res_root in (RES, DEBUG_RES, NIGHTLY_RES):
                target = res_root / f"mipmap-{density}" / f"{variant}.png"
                if not target.parent.exists():
                    continue
                save_png(ICON_SVG, target, size)

    print("== alternate-color launcher rasters (.webp) ==")
    for variant_name, color in LAUNCHER_COLOR_VARIANTS.items():
        variant_svg = ICON_SVG.replace(f'"{BRAND_BG_HEX}"', f'"{color}"')
        # Sanity: every variant must have actually swapped the bg color.
        assert color in variant_svg, f"bg fill swap failed for {variant_name}"
        for density, size in DENSITY_SIZES.items():
            for stem in (f"ic_launcher_{variant_name}", f"ic_launcher_round_{variant_name}"):
                target = RES / f"mipmap-{density}" / f"{stem}.webp"
                save_webp(variant_svg, target, size, lossless=True)
        # Branding preview at 512 for design reviews.
        save_png(variant_svg, BRANDING / f"app-icon-{variant_name}.png", 512)

    print("== Play Store / web icons ==")
    save_png(ICON_SVG, APP_ROOT / "ic_launcher-playstore.png", 512)
    save_png(ICON_SVG, APP_ROOT / "ic_launcher-web.png", 512)

    print("== README marketplace asset ==")
    save_webp(ICON_SVG, README_IMAGES / "app-icon.webp", 1024, lossless=True)

    print("== monochrome preview (debug aid only) ==")
    save_png(desaturate_to_white(FG_SVG), BRANDING / "app-icon-monochrome.png", 512)

    print("done.")


if __name__ == "__main__":
    main()
