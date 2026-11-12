"""Export only the changed background and Assist icons from the author's ASE."""
import hashlib
import importlib.util
import json
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
spec = importlib.util.spec_from_file_location("summoner_assets", ROOT / "scripts/build-summoner-gui-v6.py")
assets_module = importlib.util.module_from_spec(spec)
sys.modules[spec.name] = assets_module
spec.loader.exec_module(assets_module)
source = ROOT / "human-work/英灵召唤器6.ase"
width, height, layers = assets_module.read_ase(source)
assert (width, height) == assets_module.SOURCE_SIZE
assets = assets_module.build_assets({layer.name: layer.image for layer in layers})
outputs = {}
for prefix in ("", "versions/1.21.1/"):
    target = ROOT / prefix / "common/src/main/resources/assets/echo_warrior/textures/gui/summoner"
    assets["static"].save(target / "summoner_screen.png")
    directory = target / "modes/assist"
    directory.mkdir(parents=True, exist_ok=True)
    for name, image in assets["assist"].items():
        assert image.size == (16, 16) and image.getbbox()
        path = directory / (name + ".png")
        image.save(path)
        outputs[str(path.relative_to(ROOT))] = hashlib.sha256(path.read_bytes()).hexdigest()
print(json.dumps({"source_sha256": hashlib.sha256(source.read_bytes()).hexdigest(),
                  "outputs": outputs}, ensure_ascii=False, indent=2))
