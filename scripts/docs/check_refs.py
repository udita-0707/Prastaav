#!/usr/bin/env python3
"""Section references, Markdown links, images and the banned-words rule from CLAUDE.md."""
import glob
import os
import re
import sys

from code_index import ROOT, read

DOCS = ["README.md", "docs/LLD.md", "docs/PRD.md", "docs/INTERVIEW_PREP.md", "docs/VIVA.md", "docs/diagrams.md"]
BANNED = re.compile(r"\bM1\b|\bM2\b|milestone|phase [12]|planned|upcoming|TODO|FlowForge", re.I)
ALLOWED_BANNED = {("backend/src/main/resources/db/migration/V1__init.sql", "-- Prastav schema, Milestone 1.")}
BANNED_SCOPE = ["README.md", "docs/**/*", "backend/src/**/*", "frontend/src/**/*"]
TEXT_SUFFIXES = (".md", ".puml", ".iuml", ".java", ".sql", ".yml", ".ts", ".tsx", ".css", ".html", ".json")


def section_references(errors):
    headings = set(re.findall(r"^#+ (\d+(?:\.\d+)*)\.? ", read(os.path.join(ROOT, "docs/LLD.md")), re.M))
    sources = {path: read(os.path.join(ROOT, path)) for path in DOCS if os.path.exists(os.path.join(ROOT, path))}
    for path in glob.glob(os.path.join(ROOT, "frontend/src/**/*.ts*"), recursive=True) + glob.glob(
            os.path.join(ROOT, "backend/src/**/*.java"), recursive=True):
        sources[os.path.relpath(path, ROOT)] = read(path)
    count = 0
    for path, text in sources.items():
        for number in re.findall(r"\b[Ss]ections? (\d+(?:\.\d+)*)", text):
            # A section number in the PRD refers to the PRD itself.
            if path == "docs/PRD.md":
                continue
            count += 1
            if number not in headings:
                errors.append(f"{path}: section {number} is not a heading of docs/LLD.md")
    return count


def links(errors):
    count = 0
    for path in DOCS:
        full = os.path.join(ROOT, path)
        if not os.path.exists(full):
            continue
        for target in re.findall(r"\]\(([^)#\s]+)(?:#[^)]*)?\)", read(full)):
            if re.match(r"[a-z]+:", target):
                continue
            count += 1
            if not os.path.exists(os.path.join(os.path.dirname(full), target)):
                errors.append(f"{path}: link or image {target} does not exist")
    return count


def banned_words(errors):
    count = 0
    for pattern in BANNED_SCOPE:
        for full in glob.glob(os.path.join(ROOT, pattern), recursive=True):
            if not os.path.isfile(full) or not full.endswith(TEXT_SUFFIXES):
                continue
            path = os.path.relpath(full, ROOT)
            count += 1
            for line in read(full).split("\n"):
                if BANNED.search(line) and (path, line.strip()) not in ALLOWED_BANNED:
                    errors.append(f"{path}: banned word in: {line.strip()[:90]}")
    return count


def main():
    errors = []
    references = section_references(errors)
    targets = links(errors)
    files = banned_words(errors)
    print(f"check_refs: {references} section references, {targets} links and images, {files} files for banned words")
    for error in errors:
        print("  " + error)
    print("  OK" if not errors else f"  {len(errors)} problem(s)")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
