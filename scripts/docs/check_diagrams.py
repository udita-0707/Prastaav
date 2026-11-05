#!/usr/bin/env python3
"""Every diagram exists in PlantUML and Mermaid with the same content, and what it names exists in the code."""
import os
import re
import sys

from code_index import CodeIndex, ROOT, read

ALLOWED_CLASSES = {"Persistable"}
# Diagram-only ids for nested types.
NESTED = {"AuditLogBuilder": "AuditLog", "NotificationPropertiesRetry": "NotificationProperties"}
RELATION = r"<\|--|<\|\.\.|o-->|-->|\*--|o--|\.\.>"


def blocks():
    text = read(os.path.join(ROOT, "docs/diagrams.md"))
    names = dict(re.findall(r"^\| (\d+)\. [^|]+\| `(\w+)` \|$", text, re.M))
    found = {}
    for number, body in re.findall(r"^## (\d+)\. [^\n]+\n\n```mermaid\n(.*?)\n```", text, re.S | re.M):
        found[names[number]] = body
    return found


def member_name(line):
    line = re.sub(r"^\s*[+\-#~]?\s*(\{\w+\}\s*)?", "", line)
    call = re.match(r"(\w+)\(", line)
    if call:
        return call.group(1)
    typed = re.match(r"(\w+)\s*:", line)
    if typed:
        return typed.group(1)
    return line.strip().split()[-1] if line.strip() else ""


def class_puml(text):
    classes, relations, current = {}, [], None
    for line in text.split("\n"):
        head = re.match(r'(?:abstract class|class|interface|enum) (?:"[^"]+" as )?(\w+)(?: <<\w+>>)? \{', line)
        if head:
            current = head.group(1)
            classes[current] = []
        elif current and line.startswith("}"):
            current = None
        elif current and line.strip():
            classes[current].append(member_name(line))
        elif re.search(RELATION, line):
            relations.append(" ".join(line.split()))
    return {k: sorted(v) for k, v in classes.items()}, sorted(relations)


def class_mermaid(text):
    classes, relations, current = {}, [], None
    for line in text.split("\n"):
        head = re.match(r'\s*class (\w+)(?:\["[^"]+"\])? \{', line)
        if head:
            current = head.group(1)
            classes[current] = []
        elif current and line.strip() == "}":
            current = None
        elif current and line.strip() and not line.strip().startswith("<<"):
            stripped = line.strip()
            call = re.match(r"[+\-#~]?(\w+)\(", stripped)
            classes[current].append(call.group(1) if call else stripped.replace("$", "").split()[-1].lstrip("+-#~"))
        elif not line.strip().startswith("note") and re.search(RELATION, line):
            relations.append(" ".join(line.split()))
    return {k: sorted(v) for k, v in classes.items()}, sorted(relations)


def sequence_puml(text):
    parts = {m.group(2): m.group(1) for m in re.finditer(r'participant "(\w+)" as (\w+)', text)}
    parts.update({m.group(1): m.group(1) for m in re.finditer(r"^actor (\w+)", text, re.M)})
    parts.update({m.group(2): m.group(1) for m in re.finditer(r"^database (\w+) as (\w+)", text, re.M)})
    steps = []
    for line in text.split("\n"):
        message = re.match(r"(\w+) (-->|->) (\w+) : (.+)", line)
        note = re.match(r"note over (\w+), (\w+) : (.+)", line)
        block = re.match(r"(alt|else|loop)\b ?(.*)", line)
        if message:
            steps.append(("msg", message.group(1), message.group(2) == "-->", message.group(3), message.group(4)))
        elif note:
            steps.append(("note", note.group(1), note.group(2), note.group(3)))
        elif block:
            steps.append((block.group(1), block.group(2)))
        elif line.strip() == "end":
            steps.append(("end",))
    return parts, steps


def sequence_mermaid(text):
    parts = {m.group(1): m.group(2) for m in re.finditer(r"participant (\w+) as (\w+)", text)}
    parts.update({m.group(1): m.group(1) for m in re.finditer(r"^\s*actor (\w+)", text, re.M)})
    steps = []
    for line in text.split("\n"):
        line = line.strip()
        message = re.match(r"(\w+)(-->>|->>)(\w+): (.+)", line)
        note = re.match(r"Note over (\w+),(\w+): (.+)", line)
        block = re.match(r"(alt|else|loop)\b ?(.*)", line)
        if message:
            steps.append(("msg", message.group(1), message.group(2) == "-->>", message.group(3), message.group(4)))
        elif note:
            steps.append(("note", note.group(1), note.group(2), note.group(3)))
        elif block:
            steps.append((block.group(1), block.group(2)))
        elif line == "end":
            steps.append(("end",))
    return parts, steps


def er_puml(text):
    entities, current = {}, None
    for line in text.split("\n"):
        head = re.match(r"entity (\w+) \{", line)
        if head:
            current = head.group(1)
            entities[current] = {}
        elif current and line.startswith("}"):
            current = None
        elif current:
            match = re.match(r"\s*\{field\} (\* )?(\w+) : (\S+)(?: <<(\w+)>>)?(?: - (.+))?$", line)
            entities[current][match.group(2)] = (match.group(3), match.group(4) or "", match.group(5) or "", bool(match.group(1)))
    relations = sorted(m.groups() for m in re.finditer(r"^(\w+) (\S*--\S*) (\w+) : (\w+)$", text, re.M))
    return entities, relations


def er_mermaid(text):
    entities, current = {}, None
    for line in text.split("\n"):
        head = re.match(r"\s+(\w+) \{$", line)
        if head:
            current = head.group(1)
            entities[current] = {}
        elif line.strip() == "}":
            current = None
        elif current:
            match = re.match(r'\s*(\S+) (\w+)(?: (PK|FK|UK))?(?: "(.*)")?$', line)
            comment = match.group(4) or ""
            entities[current][match.group(2)] = (match.group(1), match.group(3) or "", comment, "nullable" not in comment)
    relations = sorted((m.group(1), m.group(2), m.group(3), m.group(4)) for m in
                       re.finditer(r'^\s*(\w+) (\S*--\S*) (\w+) : "(\w+)"$', text, re.M))
    return entities, relations


def labels(text):
    """Text of every node or step in a use case, architecture or activity diagram."""
    found = set(re.findall(r'(?:usecase|component|database|node|package) "([^"]+)"', text))
    found |= set(re.findall(r"^\s*:(.+);$", text, re.M))
    found |= {m for m in re.findall(r"\(([^()]+\??)\) is \(", text)}
    return {x.replace("\\n", "<br/>") for x in found}


def mermaid_labels(text):
    found = set(re.findall(r'\w+\[\(?"([^"]+)"\)?\]', text)) | set(re.findall(r'\w+\{"([^"]+)"\}', text))
    return found | set(re.findall(r"^\s*subgraph (\w+)$", text, re.M))


def edges_puml(text):
    return sorted((m.group(1), m.group(2), m.group(3) or "") for m in
                  re.finditer(r"^(\w+) (?:-->|\.\.>) (\w+)(?: : (.+))?$", text, re.M))


def edges_mermaid(text):
    return sorted((m.group(1), m.group(3), m.group(2) or "") for m in
                  re.finditer(r'^\s*(\w+) (?:-->|-\.->)(?:\|"([^"]+)"\|)? (\w+)$', text, re.M))


def check_against_code(name, puml, index, errors):
    if "@startuml" not in puml:
        return
    words = set(re.findall(r"\b[A-Z][a-z0-9]+(?:[A-Z][a-z0-9]+)+\b", puml))
    for word in words - ALLOWED_CLASSES - set(NESTED) - {"PostgreSQL", "TanStack"}:
        if not index.is_used(word):
            errors.append(f"{name}: {word} is not in the code")


def compare(name, puml, mermaid, index, errors):
    stats = {}
    first = mermaid.split("\n")[0].strip()
    if first == "classDiagram":
        classes, relations = class_puml(puml)
        other, other_relations = class_mermaid(mermaid)
        if classes != other:
            errors.append(f"{name}: classes or members differ: " + str({k: (classes.get(k), other.get(k)) for k in set(classes) | set(other) if classes.get(k) != other.get(k)})[:300])
        if relations != other_relations:
            errors.append(f"{name}: relations differ: {sorted(set(relations) ^ set(other_relations))[:5]}")
        for relation in relations:
            if re.search(r" (-->|\*--|o--|o-->) ", relation) and not re.search(r'"[^"]+" \S+ "[^"]+"', relation):
                errors.append(f"{name}: association without multiplicities: {relation}")
        for class_name, members in classes.items():
            target = NESTED.get(class_name, class_name)
            if target in ALLOWED_CLASSES:
                continue
            if not index.has_class(target):
                errors.append(f"{name}: class {target} is not in the code")
                continue
            for member in members:
                if not index.has_member(target, member):
                    errors.append(f"{name}: {target}.{member} is not in the code")
        stats = {"classes": len(classes), "members": sum(len(v) for v in classes.values()), "relations": len(relations)}
    elif first == "sequenceDiagram":
        parts, steps = sequence_puml(puml)
        other_parts, other_steps = sequence_mermaid(mermaid)
        if parts != other_parts:
            errors.append(f"{name}: participants differ: {set(parts.items()) ^ set(other_parts.items())}")
        if steps != other_steps:
            errors.append(f"{name}: steps differ: {[s for s in steps if s not in other_steps][:2]} / {[s for s in other_steps if s not in steps][:2]}")
        for step in steps:
            if step[0] != "msg":
                continue
            call = re.match(r"(\w+)\(", step[4])
            target = parts.get(step[3], "")
            if call and not step[2] and target in index.java and not index.has_member(target, call.group(1)):
                errors.append(f"{name}: {target}.{call.group(1)} is not in the code")
        for participant in parts.values():
            if participant not in index.java and participant not in {"PostgreSQL", "Student", "Faculty", "Coordinator"}:
                errors.append(f"{name}: participant {participant} is not in the code")
        stats = {"participants": len(parts), "steps": len(steps)}
    elif first == "erDiagram":
        entities, relations = er_puml(puml)
        other, other_relations = er_mermaid(mermaid)
        if entities != other:
            errors.append(f"{name}: entities differ")
        if relations != other_relations:
            errors.append(f"{name}: relationships differ: {set(relations) ^ set(other_relations)}")
        if set(entities) != set(index.tables):
            errors.append(f"{name}: tables differ from the migrations: {set(entities) ^ set(index.tables)}")
        for table, columns in index.tables.items():
            for column, (sql_type, not_null) in columns.items():
                drawn = entities.get(table, {}).get(column)
                if not drawn or drawn[0] != sql_type or drawn[3] != not_null:
                    errors.append(f"{name}: {table}.{column} is {sql_type} not_null={not_null}, drawn as {drawn}")
            extra = set(entities.get(table, {})) - set(columns)
            if extra:
                errors.append(f"{name}: {table} has columns not in the migrations: {extra}")
        for schema_name in set(index.indexes) | index.constraints | set(index.triggers):
            implicit_check = schema_name.endswith("_check") and schema_name not in puml
            if schema_name not in puml or schema_name not in mermaid or implicit_check:
                errors.append(f"{name}: {schema_name} is not drawn in both notations")
        drawn_keys = sorted((a, b, c) for a, _, b, c in relations)
        if drawn_keys != sorted(index.foreign_keys):
            errors.append(f"{name}: relationships differ from the foreign keys: {set(drawn_keys) ^ set(index.foreign_keys)}")
        stats = {"tables": len(entities), "relationships": len(relations)}
    elif first == "stateDiagram-v2":
        transitions = sorted((a, b, c.replace("\\n", " ")) for a, b, c in re.findall(r"^(\S+) --> (\S+)(?: : (.+))?$", puml, re.M))
        other = sorted((a, b, c) for a, b, c in re.findall(r"^\s*(\S+) --> (\S+)(?: : (.+))?$", mermaid, re.M))
        if transitions != other:
            errors.append(f"{name}: transitions differ: {set(transitions) ^ set(other)}")
        stats = {"transitions": len(transitions)}
    else:
        ours, theirs = labels(puml), mermaid_labels(mermaid)
        if ours - theirs:
            errors.append(f"{name}: only in PlantUML: {sorted(ours - theirs)[:4]}")
        if name != "15_activity_workflow" and theirs - ours:
            errors.append(f"{name}: only in Mermaid: {sorted(theirs - ours)[:4]}")
        if name != "15_activity_workflow":
            one, two = edges_puml(puml), edges_mermaid(mermaid)
            if name == "01_use_case":
                two = sorted((m.group(1), target, "") for m in re.finditer(r"^\s*(\w+) --> (.+)$", mermaid, re.M) for target in m.group(2).split(" & "))
            if one != two:
                errors.append(f"{name}: links differ: {sorted(set(one) ^ set(two))[:4]}")
        stats = {"labels": len(ours)}
    check_against_code(name, puml, index, errors)
    return stats


def main():
    errors = []
    index = CodeIndex()
    mermaid = blocks()
    pumls = {os.path.basename(p)[:-5] for p in os.listdir(os.path.join(ROOT, "docs/uml")) if p.endswith(".puml")}
    if pumls != set(mermaid):
        errors.append(f"diagrams without a partner: {sorted(pumls ^ set(mermaid))}")
    totals = {}
    for name in sorted(pumls & set(mermaid)):
        stats = compare(name, read(os.path.join(ROOT, "docs/uml", name + ".puml")), mermaid[name], index, errors)
        for key, value in stats.items():
            totals[key] = totals.get(key, 0) + value
        for folder in ("docs/uml/png", "docs/uml/mermaid-png"):
            if not os.path.exists(os.path.join(ROOT, folder, name + ".png")):
                errors.append(f"{name}: no PNG in {folder}")
    print(f"check_diagrams: {len(pumls)} diagrams; " + ", ".join(f"{v} {k}" for k, v in sorted(totals.items())))
    for error in errors:
        print("  " + error)
    print("  OK" if not errors else f"  {len(errors)} problem(s)")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
