#!/usr/bin/env python3
"""Every class, method, endpoint, table, column, index, migration and path named in the docs exists in the code."""
import os
import re
import sys

from code_index import CodeIndex, ROOT, normalise_path, read

DOCS = ["README.md", "docs/LLD.md", "docs/PRD.md", "docs/INTERVIEW_PREP.md", "docs/VIVA.md", "docs/diagrams.md"]
ALLOWED_FILE = os.path.join(os.path.dirname(__file__), "allowed_names.txt")
CAMEL = re.compile(r"(?<![\w.$/*-])([A-Z][a-z0-9]+(?:[A-Z][a-z0-9]*)+)(?![\w-]|\.(?:java|tsx?))")
LOWER = re.compile(r"`@?([a-z]+(?:[A-Z][a-z0-9]*)+)(?:\([^`]*\))?`")
SNAKE = re.compile(r"(?<![\w./-])([a-z]+(?:_[a-z0-9]+)+)(?![\w(/-])")
UPPER = re.compile(r"(?<![\w./-])([A-Z]{3,}(?:_[A-Z0-9]+)+)(?![\w-])")
ENDPOINT = re.compile(r"\b(GET|POST|PUT|PATCH|DELETE) (/api)?(/[\w/{}.-]+)")
TABLE_ROW = re.compile(r"^\| (GET|POST|PUT|PATCH|DELETE)(?: / (GET|POST|PUT|PATCH|DELETE))? \| `?(/[^ |`]*)`?", re.M)
PATH = re.compile(r"`([\w./-]+/[\w./-]*|[\w-]+\.(?:java|sql|ts|tsx|puml|yml|md|css|json|iuml))`")
MEMBER = re.compile(r"`([A-Z]\w+)[.#](\w+)(?:\([^`]*\))?`")
SKIP_PATH = re.compile(r"^(/|https?:|\.|src/main/java/com/prastav/\*)|\*|\{|<")


def allowed_names():
    with open(ALLOWED_FILE, encoding="utf-8") as handle:
        return {line.strip() for line in handle if line.strip() and not line.startswith("#")}


def proposed_part(text):
    """The drills describe changes a reader could make, so what they add does not exist yet."""
    start = text.find("## Part B")
    end = text.find("## Part C")
    return text[start:end] if start >= 0 and end > start else ""


def drill_paths(text, index, doc, errors):
    """In a drill, a file not marked new must already exist."""
    checked = 0
    for row in text.split("\n"):
        if not row.startswith("| `") or "(new" in row.split("|")[1]:
            continue
        for path in re.findall(r"`([\w./-]+\.(?:java|sql|ts|tsx|puml|yml|md|css))`", row.split("|")[1]):
            checked += 1
            if not index.file_exists(path) and not index.file_named(os.path.basename(path)):
                errors.append(f"{doc}: drill names {path}, which does not exist")
    return checked


def strip_markdown(text):
    return text.replace("\\_", "_").replace("\\*", "*")


def catalogue(text, index, errors):
    """Section 2.6: each catalogue entry's fields and methods exist in that class, and every class has an entry."""
    current = None
    checked = 0
    entries = set()
    for line in text.split("\n"):
        head = re.match(r"#####? `(\w+)`", line)
        if head:
            current = head.group(1)
            entries.add(current)
            continue
        if line.startswith("#"):
            current = None
        if not current or not index.has_class(current):
            continue
        method = re.match(r"- `(?:(?:public|protected|private|static|final|abstract|default) )*[\w<>\[\], .?]*?\b(\w+)\(", line)
        field = re.match(r"\| `(\w+)` \| ", line)
        name = method.group(1) if method else field.group(1) if field else None
        if name and not line.startswith("- inherited"):
            checked += 1
            source = current if current in index.java else None
            found = index.has_member(source, name) if source else name in index.java_blob
            if not found and name not in index.java_blob:
                errors.append(f"docs/LLD.md: {current}.{name} is not in the code")
            elif source and not found:
                errors.append(f"docs/LLD.md: {current}.{name} is not in that class")
    for name in sorted(index.main_classes - entries):
        errors.append(f"docs/LLD.md: class {name} has no catalogue entry")
    for name in sorted(entries - index.main_classes):
        errors.append(f"docs/LLD.md: catalogue entry {name} has no class")
    return checked


def api_table(text, index, errors):
    section = text[text.index("## 7. REST"):text.index("## 8. Database")]
    table = {(verb, normalise_path("/api" + path)) for verb, _, path in re.findall(TABLE_ROW, section)}
    if table != index.endpoints:
        errors.append(f"docs/LLD.md: API table differs from the controllers: {sorted(table ^ index.endpoints)}")
    for verb, path in sorted(index.endpoints):
        wanted = re.sub(r"\{\}", r"\\{\\w+\\}", re.escape(path).replace(r"\{\}", "{}"))
        if not re.search(r"`" + verb + " " + wanted + r"[`?]", section):
            errors.append(f"docs/LLD.md: no contract paragraph for {verb} {path}")
    return len(table)


def schema_section(text, index, errors):
    section = text[text.index("## 8. Database"):text.index("## 9. Error")]
    for name in sorted(set(index.indexes) | index.constraints | set(index.triggers)):
        if not re.search(r"\b" + name + r"\b", section):
            errors.append(f"docs/LLD.md: section 8 does not name {name}")
    for table, columns in index.tables.items():
        part = re.search(r"#+ 8\.\d+ `" + table + r"`\n(.*?)(?=\n#+ )", section, re.S)
        if not part:
            errors.append(f"docs/LLD.md: section 8 has no table {table}")
            continue
        for column, (sql_type, not_null) in columns.items():
            row = [r for r in part.group(1).split("\n") if r.startswith("|") and re.search(r"(^\| |, )" + column + r"\b", r)]
            if not row:
                errors.append(f"docs/LLD.md: {table}.{column} is not in section 8")
            elif sql_type not in row[0]:
                errors.append(f"docs/LLD.md: {table}.{column} should be {sql_type}: {row[0][:70]}")
            elif column != "id" and not_null != ("NOT NULL" in row[0]):
                errors.append(f"docs/LLD.md: {table}.{column} not_null={not_null}: {row[0][:70]}")
    for name in re.findall(r"`(V\d+__\w+\.sql)`", section):
        if name not in index.migrations:
            errors.append(f"docs/LLD.md: migration {name} does not exist")
    return sum(len(c) for c in index.tables.values())


def main():
    index = CodeIndex()
    allowed = allowed_names()
    schema = index.schema_names()
    errors, counts = [], {"class": 0, "member": 0, "endpoint": 0, "path": 0, "schema": 0, "constant": 0}
    for doc in DOCS:
        full = os.path.join(ROOT, doc)
        if not os.path.exists(full):
            continue
        raw = strip_markdown(read(full))
        proposed = proposed_part(raw)
        text = raw.replace(proposed, "") if proposed else raw
        if proposed:
            counts["path"] += drill_paths(proposed, index, doc, errors)
        for name in set(CAMEL.findall(text)):
            counts["class"] += 1
            if name not in allowed and not index.is_used(name):
                errors.append(f"{doc}: {name} is not in the code")
        for name in set(LOWER.findall(text)):
            counts["member"] += 1
            if name not in allowed and not index.is_used(name):
                errors.append(f"{doc}: {name} is not in the code")
        for name in set(SNAKE.findall(text)):
            counts["schema"] += 1
            if name not in schema and name not in allowed and name not in index.config_blob and not re.search(r"\b" + name + r"\b", index.frontend_blob):
                errors.append(f"{doc}: {name} is not a table, column, index, constraint, trigger or setting")
        for name in set(UPPER.findall(text)):
            counts["constant"] += 1
            if name not in allowed and not re.search(r"\b" + name + r"\b", index.java_blob + index.frontend_blob + index.config_blob):
                errors.append(f"{doc}: {name} is not in the code")
        for class_name, member in set(MEMBER.findall(text)):
            if class_name in allowed or class_name not in index.java:
                continue
            counts["member"] += 1
            if f"{class_name}.{member}" not in allowed and not index.has_member(class_name, member):
                errors.append(f"{doc}: {class_name}.{member} is not in the code")
        for verb, _, path in ENDPOINT.findall(text):
            counts["endpoint"] += 1
            wanted = normalise_path(path.rstrip("."))
            if wanted and not any(v == verb and p.endswith(wanted) for v, p in index.endpoints):
                errors.append(f"{doc}: {verb} {path} is not an endpoint")
        for path in set(PATH.findall(text)):
            if SKIP_PATH.search(path) or path in allowed:
                continue
            counts["path"] += 1
            if not index.file_exists(path) and not index.file_named(os.path.basename(path.rstrip("/"))):
                errors.append(f"{doc}: {path} does not exist")
    lld = strip_markdown(read(os.path.join(ROOT, "docs/LLD.md")))
    counts["catalogue member"] = catalogue(lld, index, errors)
    counts["API row"] = api_table(lld, index, errors)
    counts["schema column"] = schema_section(lld, index, errors)
    print("check_names: " + ", ".join(f"{v} {k} names" for k, v in counts.items()))
    for error in sorted(set(errors)):
        print("  " + error)
    print("  OK" if not errors else f"  {len(set(errors))} problem(s)")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
