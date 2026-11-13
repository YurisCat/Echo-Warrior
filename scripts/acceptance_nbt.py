"""Read-only NBT inspection for isolated acceptance worlds; never edits a save."""
import gzip
import io
import struct
from pathlib import Path


def read_nbt(path):
    stream = io.BytesIO(gzip.decompress(Path(path).read_bytes()))

    def number(fmt):
        size = struct.calcsize(fmt)
        raw = stream.read(size)
        if len(raw) != size:
            raise ValueError("Truncated NBT")
        return struct.unpack(fmt, raw)[0]

    def length():
        n = number(">i")
        if not 0 <= n <= 4_000_000:
            raise ValueError("Invalid acceptance NBT length")
        return n

    def string():
        size = number(">H")
        raw = stream.read(size)
        if len(raw) != size:
            raise ValueError("Truncated NBT string")
        # Fixture names/keys use BMP text; tolerate Java's modified UTF-8 NUL.
        return raw.replace(b"\xc0\x80", b"\x00").decode("utf-8", errors="surrogatepass")

    def payload(kind, depth=0):
        if depth > 64:
            raise ValueError("NBT nesting exceeds acceptance bound")
        if kind in (1, 2, 3, 4, 5, 6):
            return number({1: ">b", 2: ">h", 3: ">i", 4: ">q", 5: ">f", 6: ">d"}[kind])
        if kind == 7:
            return [number(">b") for _ in range(length())]
        if kind == 8:
            return string()
        if kind == 9:
            child = number(">B")
            return [payload(child, depth+1) for _ in range(length())]
        if kind == 10:
            result = {}
            while child := number(">B"):
                name = string()
                if name in result:
                    raise ValueError("Duplicate NBT key")
                result[name] = payload(child, depth+1)
            return result
        if kind in (11, 12):
            return [number(">i" if kind == 11 else ">q") for _ in range(length())]
        raise ValueError(f"Unexpected NBT type {kind}")

    kind = number(">B")
    if kind != 10:
        raise ValueError("Expected compound root")
    string()
    result = payload(kind)
    if stream.read(1):
        raise ValueError("Trailing NBT bytes")
    return result
