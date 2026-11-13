"""Version-aware normal shutdown evidence shared by isolated server runners."""


def require_normal_save(text, world, minecraft, started_epoch):
    if minecraft == "26.3":
        shutdown = text.rsplit("Stopping server", 1)
        required = ("Saving players", "Saving worlds", "/minecraft:overworld",
                    "/minecraft:the_nether", "/minecraft:the_end")
        if len(shutdown) != 2 or any(marker not in shutdown[1] for marker in required):
            raise RuntimeError("missing 26.3 shutdown/dimension-save evidence")
        level = world / "level.dat"
        if not level.is_file() or level.stat().st_size == 0 or level.stat().st_mtime < started_epoch:
            raise RuntimeError("26.3 level.dat was not saved by this run")
    elif "All dimensions are saved" not in text:
        raise RuntimeError("missing normal-save evidence")
