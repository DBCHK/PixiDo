"""Remove white backgrounds from 3D doodles and export transparent PNGs."""
from __future__ import annotations

import os
from PIL import Image

SRC_DIR = r"C:\Users\debab\.grok\sessions\D%3A%5CZero_to_Hero%5CPixiDo\019fb9f8-bbdf-7be3-a237-4805b9c619ce\images"
DEST = r"D:\Zero_to_Hero\PixiDo\app\src\main\res\drawable"
DOODLES = r"D:\Zero_to_Hero\PixiDo\app\src\main\res\doodles"

# Source session image -> drawable resource
MAPPING = {
    "5.jpg": "doodle_tasks.png",
    "10.jpg": "doodle_focus.png",
    "7.jpg": "doodle_budget.png",
    "12.jpg": "doodle_account.png",  # piggy bank
    "8.jpg": "doodle_calendar.png",
    "6.jpg": "doodle_goals.png",
    "11.jpg": "doodle_splash.png",   # waving welcome
    "9.jpg": "doodle_discard.png",
}

# Back-compat aliases used by existing code until fully migrated
ALIASES = {
    "doodle_tasks.png": ["doodle_add_task.png"],
    "doodle_splash.png": ["doodle_login.png"],
}

HUMAN = {
    "doodle_tasks.png": "tasks_3d.png",
    "doodle_focus.png": "focus_3d.png",
    "doodle_budget.png": "budget_3d.png",
    "doodle_account.png": "account_3d.png",
    "doodle_calendar.png": "calendar_3d.png",
    "doodle_goals.png": "goals_3d.png",
    "doodle_splash.png": "splash_3d.png",
    "doodle_discard.png": "discard_3d.png",
}


def remove_white_bg(img: Image.Image, threshold: int = 242, soft: int = 16) -> Image.Image:
    """Make near-white pixels transparent with soft edge, then trim."""
    img = img.convert("RGBA")
    pixels = img.load()
    w, h = img.size
    out = Image.new("RGBA", (w, h))
    out_px = out.load()

    for y in range(h):
        for x in range(w):
            r, g, b, a = pixels[x, y]
            mx = max(r, g, b)
            mn = min(r, g, b)
            # Solid near-white
            if r >= threshold and g >= threshold and b >= threshold:
                out_px[x, y] = (r, g, b, 0)
            # Soft edge for off-white / light shadows on white
            elif (
                r >= threshold - soft
                and g >= threshold - soft
                and b >= threshold - soft
                and (mx - mn) < 22
            ):
                avg = (r + g + b) / 3.0
                t = (avg - (threshold - soft)) / float(soft)
                t = max(0.0, min(1.0, t))
                alpha = int(255 * (1.0 - t))
                out_px[x, y] = (r, g, b, alpha)
            else:
                out_px[x, y] = (r, g, b, a)

    bbox = out.getbbox()
    if bbox:
        pad = 10
        x0, y0, x1, y1 = bbox
        x0 = max(0, x0 - pad)
        y0 = max(0, y0 - pad)
        x1 = min(w, x1 + pad)
        y1 = min(h, y1 + pad)
        out = out.crop((x0, y0, x1, y1))
    return out


def main() -> None:
    os.makedirs(DEST, exist_ok=True)
    os.makedirs(DOODLES, exist_ok=True)

    for src_name, dest_name in MAPPING.items():
        path = os.path.join(SRC_DIR, src_name)
        if not os.path.exists(path):
            print("MISSING", src_name)
            continue
        im = Image.open(path)
        cleaned = remove_white_bg(im)
        out_path = os.path.join(DEST, dest_name)
        cleaned.save(out_path, "PNG")
        print(f"OK {src_name} -> {dest_name} size={cleaned.size}")
        for alias in ALIASES.get(dest_name, []):
            cleaned.save(os.path.join(DEST, alias), "PNG")
            print(f"  alias {alias}")

    for src, name in HUMAN.items():
        p = os.path.join(DEST, src)
        if os.path.exists(p):
            Image.open(p).save(os.path.join(DOODLES, name), "PNG")

    print("drawable doodles:", sorted(f for f in os.listdir(DEST) if f.startswith("doodle_")))


if __name__ == "__main__":
    main()
