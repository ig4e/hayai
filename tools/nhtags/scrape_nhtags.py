"""Scrape nhentai.net tag namespaces into JSON files.

Output mirrors `app/src/main/assets/ehtags/`:
  app/src/main/assets/nhtags/<namespace>.json -> ["namespace:foo", "namespace:bar", ...]
  app/src/main/assets/nhtags/namespaces.json   -> ["artist", "category", ...]

Each namespace's index page (e.g. https://nhentai.net/tags/?page=N) lists tag links
of the form ``<a href="/tag/<slug>/" class="tag tag-NNN"><span class="name">visible name</span>...</a>``.
We pull the visible name (with spaces) so autocomplete suggestions read naturally;
nhentai's search syntax accepts both spaced and dashed forms when quoted.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
import time
from html import unescape
from pathlib import Path
from typing import Iterable

import requests

# (URL path segment, namespace label used in JSON keys / search queries).
# Order matches e-hentai's `namespaceFiles` style: small lists first, big lists last.
NAMESPACES: list[tuple[str, str]] = [
    ("categories", "category"),
    ("languages", "language"),
    ("parodies", "parody"),
    ("characters", "character"),
    ("tags", "tag"),
    ("groups", "group"),
    ("artists", "artist"),
]

USER_AGENT = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
    "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0 Safari/537.36"
)
BASE = "https://nhentai.net"

# Match `<a href="/tag/big-breasts/" ...><span class="name">big breasts</span>...</a>`
# Capture group 1 is the URL slug (`big-breasts`), group 2 is the visible name.
TAG_RE = re.compile(
    r'<a[^>]+href="/(?:tag|artist|character|parody|group|language|category)/([^"/]+)/"'
    r'[^>]*>.*?<span class="name">([^<]+)</span>',
    re.DOTALL,
)
LAST_PAGE_RE = re.compile(
    r'<a href="/[^/"]+/\?sort=popular&amp;page=(\d+)" class="last',
)


def fetch(session: requests.Session, url: str, retries: int = 6) -> str:
    """GET with exponential backoff, retrying through Cloudflare 403 / 429 throttling."""
    last_err: Exception | None = None
    for attempt in range(retries):
        try:
            r = session.get(url, timeout=30)
            if r.status_code == 200:
                return r.text
            last_err = RuntimeError(f"HTTP {r.status_code} for {url}")
            # 403 on nhentai usually means CF asked us to slow down. Back off hard.
            if r.status_code in (403, 429):
                time.sleep(5 * (attempt + 1))
                continue
        except requests.RequestException as e:
            last_err = e
        time.sleep(2 * (attempt + 1))
    raise RuntimeError(f"failed to fetch {url}: {last_err}")


def parse_page(html: str) -> list[tuple[str, str]]:
    """Returns list of (slug, visible_name) on the page."""
    out: list[tuple[str, str]] = []
    for m in TAG_RE.finditer(html):
        slug = m.group(1).strip()
        name = unescape(m.group(2)).strip()
        if not slug or not name:
            continue
        out.append((slug, name))
    return out


def discover_last_page(html: str) -> int:
    m = LAST_PAGE_RE.search(html)
    return int(m.group(1)) if m else 1


def scrape_namespace(path_seg: str, label: str) -> list[str]:
    """Walk all paginated index pages for one namespace, return sorted unique tag list."""
    session = requests.Session()
    session.headers.update({
        "User-Agent": USER_AGENT,
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language": "en-US,en;q=0.9",
        "Referer": f"{BASE}/",
    })

    first_url = f"{BASE}/{path_seg}/?page=1"
    first_html = fetch(session, first_url)
    last = discover_last_page(first_html)
    print(f"[{label}] {path_seg}: {last} page(s)", flush=True)

    seen: set[str] = set()
    pairs: list[tuple[str, str]] = []

    def absorb(html: str) -> None:
        for slug, name in parse_page(html):
            key = name.lower()
            if key in seen:
                continue
            seen.add(key)
            pairs.append((slug, name))

    absorb(first_html)

    if last > 1:
        # nhentai sits behind Cloudflare; sequential + small delay avoids 403 storms.
        for p in range(2, last + 1):
            url = f"{BASE}/{path_seg}/?page={p}"
            html = fetch(session, url)
            absorb(html)
            time.sleep(0.5)

    pairs.sort(key=lambda p: p[1].lower())
    print(f"[{label}] collected {len(pairs)} tags", flush=True)
    # Emit prefixed forms; include the dashed slug only when it differs from the
    # spaced name, so autocomplete lookups for either typing style hit.
    out: list[str] = []
    for slug, name in pairs:
        out.append(f"{label}:{name}")
        dashed = name.replace(" ", "-").lower()
        if dashed != name and dashed == slug:
            out.append(f"{label}:{slug}")
    return out


def write_json(path: Path, data: list[str]) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    # Compact form to keep the asset size down — same shape as ehtags JSON.
    path.write_text(json.dumps(data, ensure_ascii=False, separators=(", ", ": ")), encoding="utf-8")


def main(argv: Iterable[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Scrape nhentai tag namespaces.")
    parser.add_argument(
        "--out",
        type=Path,
        default=Path(__file__).resolve().parents[2] / "app" / "src" / "main" / "assets" / "nhtags",
        help="Output directory (default: app/src/main/assets/nhtags).",
    )
    parser.add_argument(
        "--only",
        nargs="*",
        choices=[p for p, _ in NAMESPACES],
        help="Limit to specific namespace path segments.",
    )
    args = parser.parse_args(list(argv) if argv is not None else None)

    targets = NAMESPACES
    if args.only:
        wanted = set(args.only)
        targets = [t for t in NAMESPACES if t[0] in wanted]

    args.out.mkdir(parents=True, exist_ok=True)

    labels: list[str] = []
    for path_seg, label in targets:
        try:
            tags = scrape_namespace(path_seg, label)
        except Exception as e:
            print(f"[{label}] FAILED: {e}", file=sys.stderr, flush=True)
            return 1
        write_json(args.out / f"{label}.json", tags)
        labels.append(label)

    if not args.only:
        write_json(args.out / "namespaces.json", sorted(labels))

    print(f"Done. Wrote {len(labels)} namespace file(s) to {args.out}", flush=True)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
