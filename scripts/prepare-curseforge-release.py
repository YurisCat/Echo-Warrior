#!/usr/bin/env python3
"""Fail-closed placeholder for the retired Fabric-only CurseForge helper."""

from __future__ import annotations

import sys


def main() -> int:
    print(
        "CurseForge release preparation is disabled: the previous helper supported "
        "only one Fabric JAR. Use scripts/build-dual-candidate.ps1 for local dual "
        "output, and do not publish until a two-file CurseForge workflow is implemented.",
        file=sys.stderr,
    )
    return 2


if __name__ == "__main__":
    raise SystemExit(main())
