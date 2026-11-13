"""
Comprehensive source-code statistics for FreeCol.
Outputs CSV: package, file, class_name, public_methods, loc, priority
"""
import os, re, sys, csv

src_root = sys.argv[1] if len(sys.argv) > 1 else 'src'
out_csv  = sys.argv[2] if len(sys.argv) > 2 else 'test_docs/source_stats.csv'

# Priority mapping: package prefix -> (priority, rationale)
PRIORITY_MAP = [
    # High — public APIs that affect game state or core functionality
    ("client/control",          "High",   "Client-side controllers, command dispatch"),
    ("common/model",            "High",   "Core data model (game objects, units, map, colony)"),
    ("common/util",             "High",   "Shared utility methods used everywhere"),
    ("server/model",            "High",   "Server-side game-state and session classes"),
    ("server/ai",               "High",   "AI decision logic, missions, colony planning"),
    ("server/control",          "High",   "Server-side command handling"),
    # Low — must appear before broader Medium prefixes so client/gui wins over client
    ("client/gui",              "Low",    "UI / Swing components (integration-tested)"),
    ("common/debug",            "Low",    "Debug utilities"),
    ("common/logging",          "Low",    "Logging helpers"),
    ("common/resources",        "Low",    "Resource loading (images, audio, fonts)"),
    ("common/sound",            "Low",    "Sound playback"),
    # Medium — networking, utilities, correctness-critical but mostly data-pass-through
    ("client/networking",       "Medium", "Client-side networking"),
    ("client",                  "Medium", "Client bootstrap (FreeColClient, ClientOptions)"),
    ("common/networking",       "Medium", "Protocol message classes"),
    ("common/io",               "Medium", "Serialization, save/load, data files"),
    ("common/i18n",             "Medium", "Internationalization helpers"),
    ("common/option",           "Medium", "Option framework"),
    ("common/metaserver",       "Medium", "Meta-server utilities"),
    ("common",                  "Medium", "Common root (exceptions, seed, memory)"),
    ("server/generator",        "Medium", "Map generation"),
    ("server/networking",       "Medium", "Server-side connection handling"),
    ("server",                  "Medium", "Server root (FreeColServer)"),
    ("metaserver",              "Medium", "Meta-server process"),
    ("tools",                   "Medium", "Build-time / conversion utilities"),
]

# Root-level FreeCol.java special case
ROOT_PRIORITY = ("High", "Application entry point")

def classify(rel_pkg):
    """Return (priority, rationale) for a relative package path."""
    for prefix, pri, rationale in PRIORITY_MAP:
        if rel_pkg == prefix or rel_pkg.startswith(prefix + "/"):
            return pri, rationale
    return "Low", "Unclassified"

def count_public_methods(lines, class_name):
    count = 0
    in_block_comment = False
    for line in lines:
        stripped = line.strip()
        if in_block_comment:
            if '*/' in stripped:
                in_block_comment = False
            continue
        if stripped.startswith('/*'):
            if '*/' not in stripped:
                in_block_comment = True
            continue
        if not stripped or stripped.startswith('//') or stripped.startswith('*') or stripped.startswith('@'):
            continue
        if stripped.startswith('public') and '(' in stripped:
            if re.match(r'public\s+(abstract\s+)?(class|interface|enum|@interface)\s', stripped):
                continue
            if re.match(r'public\s+' + re.escape(class_name) + r'\s*\(', stripped):
                continue
            count += 1
    return count

results = []
freecol_root = os.path.join(src_root, 'net', 'sf', 'freecol')

for root, dirs, files in os.walk(freecol_root):
    for f in sorted(files):
        if not f.endswith('.java') or f == 'package-info.java':
            continue
        path = os.path.join(root, f)
        # relative path from src root  (e.g. net/sf/freecol/common/model/Unit.java)
        rel_from_src = os.path.relpath(path, src_root).replace(os.sep, '/')
        # package relative to freecol root (e.g. common/model)
        rel_pkg = os.path.relpath(root, freecol_root).replace(os.sep, '/')
        if rel_pkg == '.':
            rel_pkg = ''  # root package

        class_name = os.path.splitext(f)[0]

        try:
            with open(path, encoding='utf-8', errors='ignore') as fp:
                lines = fp.readlines()
        except Exception:
            continue

        loc = len(lines)
        pub_methods = count_public_methods(lines, class_name)

        if rel_pkg == '':
            pri, rationale = ROOT_PRIORITY
            pkg_display = "(root)"
        else:
            pri, rationale = classify(rel_pkg)
            pkg_display = rel_pkg

        results.append({
            'package': pkg_display,
            'file': rel_from_src,
            'class_name': class_name,
            'public_methods': pub_methods,
            'loc': loc,
            'priority': pri,
            'rationale': rationale,
        })

# Sort by package then file
results.sort(key=lambda r: (r['package'], r['file']))

# Write CSV
os.makedirs(os.path.dirname(out_csv), exist_ok=True)
with open(out_csv, 'w', newline='', encoding='utf-8') as fp:
    writer = csv.DictWriter(fp, fieldnames=['package', 'file', 'class_name', 'public_methods', 'loc', 'priority', 'rationale'])
    writer.writeheader()
    writer.writerows(results)

# Print summary to stdout
print(f"Wrote {len(results)} rows to {out_csv}")
print()

# Package-level summary
pkg_summary = {}
for r in results:
    pkg = r['package']
    if pkg not in pkg_summary:
        pkg_summary[pkg] = {'files': 0, 'methods': 0, 'loc': 0, 'priority': r['priority']}
    pkg_summary[pkg]['files'] += 1
    pkg_summary[pkg]['methods'] += r['public_methods']
    pkg_summary[pkg]['loc'] += r['loc']

print(f"{'Package':<40} {'Files':>6} {'Methods':>8} {'LoC':>8} {'Priority'}")
print("-" * 90)
grand_files = grand_methods = grand_loc = 0
for pkg in sorted(pkg_summary):
    s = pkg_summary[pkg]
    print(f"{pkg:<40} {s['files']:>6} {s['methods']:>8} {s['loc']:>8} {s['priority']}")
    grand_files += s['files']
    grand_methods += s['methods']
    grand_loc += s['loc']
print("-" * 90)
print(f"{'TOTAL':<40} {grand_files:>6} {grand_methods:>8} {grand_loc:>8}")
