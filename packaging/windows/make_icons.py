"""Store icon set from snap/gui/shilploka.png (Pillow). FLOW: build_msix.ps1 → Assets/*.png"""
import os, sys
from PIL import Image
SIZES = {"Square44x44Logo": (44, 44), "StoreLogo": (50, 50), "Square150x150Logo": (150, 150),
         "Wide310x150Logo": (310, 150), "Square310x310Logo": (310, 310)}
src = Image.open(sys.argv[1]).convert("RGBA")
os.makedirs(sys.argv[2], exist_ok=True)
for name, (w, h) in SIZES.items():
    for scale in (1, 2):
        W, H = w * scale, h * scale
        side = int(min(W, H) * 0.9)
        c = Image.new("RGBA", (W, H), (0, 0, 0, 0))
        c.paste(src.resize((side, side), Image.LANCZOS), ((W - side) // 2, (H - side) // 2))
        c.save(os.path.join(sys.argv[2], name + ("" if scale == 1 else ".scale-200") + ".png"))
