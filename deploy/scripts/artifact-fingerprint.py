#!/usr/bin/env python3
"""Fingerprint an artifact tree + Dockerfile (matches KindSupport.artifactFingerprint)."""

from __future__ import annotations

import hashlib
import sys
from pathlib import Path


def sha256_hex(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def hash_tree(root: Path) -> str:
    digest = hashlib.sha256()
    if not root.exists():
        return sha256_hex(b"")
    files = sorted(
        (p for p in root.rglob("*") if p.is_file()),
        key=lambda p: p.relative_to(root).as_posix(),
    )
    for file in files:
        rel = file.relative_to(root).as_posix().encode("utf-8")
        digest.update(rel)
        digest.update(b"\0")
        digest.update(file.read_bytes())
    return digest.hexdigest()


def artifact_fingerprint(artifact_dir: Path, dockerfile: Path) -> str:
    combined = hashlib.sha256()
    combined.update(hash_tree(artifact_dir).encode("utf-8"))
    combined.update(b"\0")
    combined.update(dockerfile.read_bytes() if dockerfile.is_file() else b"")
    return combined.hexdigest()


def main() -> None:
    if len(sys.argv) != 3:
        print(
            f"usage: {sys.argv[0]} <artifact-dir> <dockerfile>",
            file=sys.stderr,
        )
        sys.exit(2)
    print(artifact_fingerprint(Path(sys.argv[1]), Path(sys.argv[2])))


if __name__ == "__main__":
    main()
