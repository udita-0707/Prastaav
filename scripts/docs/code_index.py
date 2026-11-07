"""Everything the documentation may name, read from the code: classes, members, endpoints, schema objects and files."""
import glob
import os
import re

ROOT = os.environ.get("DOCS_CHECK_ROOT") or os.path.abspath(os.path.join(os.path.dirname(__file__), "..", ".."))
JAVA_ROOTS = ["backend/src/main/java", "backend/src/test/java"]
MIGRATIONS = "backend/src/main/resources/db/migration"
FILE_PREFIXES = ["", "backend/", "backend/src/main/java/com/prastav/", "backend/src/test/java/com/prastav/", "frontend/",
                 "frontend/src/", "frontend/src/components/", "docs/", "docs/uml/", MIGRATIONS + "/"]


def read(path):
    with open(path, encoding="utf-8") as handle:
        return handle.read()


class CodeIndex:
    def __init__(self):
        self.java = {}
        for root in JAVA_ROOTS:
            for path in glob.glob(os.path.join(ROOT, root, "**/*.java"), recursive=True):
                self.java[os.path.basename(path)[:-5]] = read(path)
        self.java_blob = "\n".join(self.java.values())
        self.main_classes = {os.path.basename(p)[:-5] for p in
                             glob.glob(os.path.join(ROOT, JAVA_ROOTS[0], "**/*.java"), recursive=True)}
        self.frontend_blob = "\n".join(read(p) for p in glob.glob(os.path.join(ROOT, "frontend/src/**/*.ts*"), recursive=True))
        self.config_blob = read(os.path.join(ROOT, "backend/src/main/resources/application.yml")) + read(os.path.join(ROOT, "docker-compose.yml"))
        self.endpoints = self._endpoints()
        self.main_java = {name: source for name, source in self.java.items() if name in self.main_classes}
        self._schema()

    def _endpoints(self):
        found = set()
        for name, source in self.java.items():
            if "@RestController" not in source or name not in self.main_classes:
                continue
            base = re.search(r'@RequestMapping\("([^"]*)"\)', source)
            prefix = base.group(1) if base else ""
            for match in re.finditer(r'@(Get|Post|Put|Patch|Delete)Mapping(?:\("([^"]*)"\))?', source):
                found.add((match.group(1).upper(), normalise_path(prefix + (match.group(2) or ""))))
        return found

    def _schema(self):
        self.tables, self.indexes, self.constraints, self.triggers, self.functions = {}, {}, set(), {}, set()
        self.foreign_keys = []
        self.migrations = sorted(os.path.basename(p) for p in glob.glob(os.path.join(ROOT, MIGRATIONS, "*.sql")))
        for migration in self.migrations:
            sql = re.sub(r"--[^\n]*", "", read(os.path.join(ROOT, MIGRATIONS, migration)))
            for table, body in re.findall(r"CREATE TABLE (\w+) \((.*?)\n\);", sql, re.S):
                self.tables[table] = {}
                for line in split_columns(body):
                    self._column(table, line)
            for table, column, spec in re.findall(r"ALTER TABLE (\w+) ADD COLUMN (\w+)\s+([^;]+);", sql):
                self._column(table, f"{column} {spec}")
            for name, table, columns in re.findall(r"CREATE (?:UNIQUE )?INDEX (\w+)\s+ON (\w+) \(([^)]*)\)", sql):
                self.indexes[name] = (table, [c.strip() for c in columns.split(",")])
            for name in re.findall(r"DROP CONSTRAINT (\w+)", sql):
                self.constraints.discard(name)
            self.constraints.update(re.findall(r"ADD CONSTRAINT (\w+)", sql))
            self.functions.update(re.findall(r"CREATE FUNCTION (\w+)", sql))
            for name, table in re.findall(r"CREATE TRIGGER (\w+)\s+.*?\bON (\w+)", sql, re.S):
                self.triggers[name] = table

    def _column(self, table, line):
        line = " ".join(line.split())
        named = re.match(r"CONSTRAINT (\w+) (UNIQUE|CHECK)", line)
        if named:
            self.constraints.add(named.group(1))
            if named.group(2) == "UNIQUE":
                self.indexes[named.group(1)] = (table, [])
            return
        match = re.match(r"(\w+) (\w+(?:\(\d+\))?)(.*)", line)
        column, sql_type, rest = match.group(1), match.group(2), match.group(3)
        self.tables.setdefault(table, {})[column] = (sql_type.upper(), "NOT NULL" in rest or "PRIMARY KEY" in rest)
        if "PRIMARY KEY" in rest:
            self.constraints.add(f"{table}_pkey")
            self.indexes[f"{table}_pkey"] = (table, [column])
        if re.search(r"\bUNIQUE\b", rest):
            self.constraints.add(f"{table}_{column}_key")
            self.indexes[f"{table}_{column}_key"] = (table, [column])
        reference = re.search(r"REFERENCES (\w+)", rest)
        if reference:
            self.constraints.add(f"{table}_{column}_fkey")
            self.foreign_keys.append((reference.group(1), table, column))
        if re.search(r"\bCHECK\b", rest):
            self.constraints.add(f"{table}_{column}_check")

    def schema_names(self):
        names = set(self.tables) | set(self.indexes) | self.constraints | set(self.triggers) | self.functions
        for columns in self.tables.values():
            names |= set(columns)
        return names

    def has_class(self, name):
        return name in self.java or bool(re.search(r"\b(class|interface|record|enum)\s+" + name + r"\b", self.java_blob))

    def is_used(self, word):
        """A project type, or a library type the code imports or an annotation it uses."""
        return bool(re.search(r"\b" + word + r"\b", self.java_blob + self.frontend_blob + self.config_blob))

    def has_member(self, class_name, member):
        """True if the member is declared in the class or something it extends or implements."""
        seen, queue = set(), [class_name]
        while queue:
            current = queue.pop()
            if current in seen or current not in self.java:
                continue
            seen.add(current)
            source = self.java[current]
            if re.search(r"\b" + member + r"\s*\(", source) or re.search(r"\b" + member + r"\b\s*[,;=)\n]", source):
                return True
            header = re.search(r"\b(?:extends|implements)\s+([\w, <>]+?)\s*\{", source)
            if header:
                queue += re.findall(r"\w+", header.group(1))
        return False

    def file_exists(self, path):
        path = path.rstrip("/")
        return any(os.path.exists(os.path.join(ROOT, prefix + path)) for prefix in FILE_PREFIXES)

    def file_named(self, name):
        for _, directories, files in os.walk(ROOT):
            if name in files:
                return True
        return False


def split_columns(body):
    columns, depth, current = [], 0, ""
    for char in body:
        depth += char == "("
        depth -= char == ")"
        if char == "," and depth == 0:
            columns.append(current.strip())
            current = ""
        else:
            current += char
    if current.strip():
        columns.append(current.strip())
    return columns


def normalise_path(path):
    path = path.split("?")[0].rstrip("/")
    return re.sub(r"\{[^}]+\}", "{}", path)
