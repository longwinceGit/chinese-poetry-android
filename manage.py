#!/usr/bin/env python3
"""诗词管理服务器 — 在浏览器中直接读写 JSON 文件"""

import json, os, glob, shutil, time, re, math
from datetime import datetime
from flask import Flask, jsonify, request, send_from_directory

DATA_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                        'app', 'src', 'main', 'assets', 'web', 'data')
BACKUP_DIR = os.path.join(DATA_DIR, '_backup')
STATIC_DIR = os.path.dirname(os.path.abspath(__file__))
EXPLANATIONS_FILE = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                                 'app', 'src', 'main', 'assets', 'poem_explanations.json')

app = Flask(__name__, static_folder=STATIC_DIR, static_url_path='')

# ---------- helpers ----------

def load_json(fname):
    with open(os.path.join(DATA_DIR, fname), encoding='utf-8') as f:
        return json.load(f)

def save_json(fname, data):
    with open(os.path.join(DATA_DIR, fname), 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

def poem_id(file_idx, entry_idx):
    return f"f{file_idx}e{entry_idx}"

def parse_poem_id(pid):
    parts = pid.split('e')
    return int(parts[0][1:]), int(parts[1])

def backup_data():
    ts = datetime.now().strftime('%Y%m%d_%H%M%S')
    dst = os.path.join(BACKUP_DIR, ts)
    os.makedirs(dst, exist_ok=True)
    for fname in os.listdir(DATA_DIR):
        if fname.endswith('.json') and not fname.startswith('_'):
            shutil.copy2(os.path.join(DATA_DIR, fname), os.path.join(dst, fname))
    return ts

def scan_entry(entry):
    """Return list of issue tags for an entry."""
    p = entry.get('p', [])
    issues = []
    if not p:
        issues.append('empty_p')
    for li, line in enumerate(p):
        stripped = re.sub(r'[，。、；：！？「」『』（）【】《》""''\s\.\,\;\:\!\?\(\)\[\]\{\}·・]', '', line)
        if stripped.strip() == '':
            issues.append(f'only_punct@{li}')
        elif len(stripped) < 2:
            issues.append(f'short@{li}')
    if len(p) == 1 and len(p[0]) > 80:
        issues.append('long_single')
    return issues

# ---------- precomputed index ----------

EXPLANATIONS_LOOKUP = {}  # "title|author" -> explanation text

def load_explanations():
    global EXPLANATIONS_LOOKUP
    EXPLANATIONS_LOOKUP = {}
    if not os.path.exists(EXPLANATIONS_FILE):
        return
    try:
        with open(EXPLANATIONS_FILE, encoding='utf-8') as f:
            entries = json.load(f)
        for entry in entries:
            key = entry.get('k', '')
            exp = entry.get('e', '')
            if key and exp:
                EXPLANATIONS_LOOKUP[key] = exp
        print(f"  Loaded {len(EXPLANATIONS_LOOKUP)} explanations from poem_explanations.json")
    except Exception as e:
        print(f"  WARN: cannot load explanations: {e}")

FILE_INDEX = []       # [(fname, dynasty)]
ID_MAP = {}           # poem_id -> {file_idx, entry_idx, fname}
GLOBAL_ENTRIES = []   # [{id, file_idx, entry_idx, title, author, dynasty, s, first_line}]
ENTRY_ISSUES = {}     # poem_id -> [issue_tag, ...]
STATS_CACHE = {}

def build_index():
    global FILE_INDEX, ID_MAP, GLOBAL_ENTRIES, ENTRY_ISSUES, STATS_CACHE
    load_explanations()
    FILE_INDEX, ID_MAP, GLOBAL_ENTRIES, ENTRY_ISSUES = [], {}, [], {}
    STATS_CACHE = {
        'total': 0, 'files': 0, 'by_dynasty': {},
        'empty_p': 0, 'only_punct_lines': 0, 'short_lines': 0, 'long_single': 0,
    }

    nav = load_json('nav.json')
    for dyn_info in nav:
        dynasty = dyn_info['dynasty']
        for fname in dyn_info.get('files', []):
            FILE_INDEX.append((fname, dynasty))
    STATS_CACHE['files'] = len(FILE_INDEX)

    for file_idx, (fname, dynasty) in enumerate(FILE_INDEX):
        try:
            data = load_json(fname)
        except Exception as e:
            print(f"  WARN: cannot load {fname}: {e}")
            continue

        for entry_idx, entry in enumerate(data):
            pid = poem_id(file_idx, entry_idx)
            t = entry.get('t', '')
            a = entry.get('a', '')
            s = entry.get('s', '')
            p = entry.get('p', [])
            first_line = p[0] if p else ''

            ID_MAP[pid] = {'file_idx': file_idx, 'entry_idx': entry_idx, 'fname': fname}
            GLOBAL_ENTRIES.append({
                'id': pid, 'file_idx': file_idx, 'entry_idx': entry_idx,
                'title': t, 'author': a, 'dynasty': dynasty, 's': s, 'first_line': first_line,
            })

            STATS_CACHE['total'] += 1
            STATS_CACHE['by_dynasty'][dynasty] = STATS_CACHE['by_dynasty'].get(dynasty, 0) + 1

            # Pre-scan issues
            issues = scan_entry(entry)
            if issues:
                ENTRY_ISSUES[pid] = issues

            # Stats
            if not p:
                STATS_CACHE['empty_p'] += 1
            for line in p:
                stripped = re.sub(r'[，。、；：！？「」『』（）【】《》""''\s\.\,\;\:\!\?\(\)\[\]\{\}·・]', '', line)
                if stripped.strip() == '':
                    STATS_CACHE['only_punct_lines'] += 1
                elif len(stripped) < 2:
                    STATS_CACHE['short_lines'] += 1
            if len(p) == 1 and len(p[0]) > 80:
                STATS_CACHE['long_single'] += 1

    print(f"  Indexed {STATS_CACHE['total']} poems, {len(ENTRY_ISSUES)} with issues")

print("Loading explanations...")
load_explanations()
print("Building index...")
build_index()

# ---------- API routes ----------

@app.route('/api/stats')
def api_stats():
    return jsonify(STATS_CACHE)


@app.route('/api/dynasties')
def api_dynasties():
    nav = load_json('nav.json')
    return jsonify([{
        'dynasty': d['dynasty'],
        'count': d['count'],
        'sources': d.get('sources', []),
        'files': d['files'],
    } for d in nav])


@app.route('/api/poems')
def api_poems():
    dynasty = request.args.get('dynasty', '')
    search = request.args.get('search', '').strip()
    page = int(request.args.get('page', 1))
    page_size = int(request.args.get('pageSize', 50))
    issue = request.args.get('issue', '')

    results = GLOBAL_ENTRIES

    if dynasty:
        results = [e for e in results if e['dynasty'] == dynasty]
    if search:
        q = search.lower()
        results = [e for e in results if
                   q in e['title'].lower() or
                   q in e['author'].lower() or
                   q in e.get('first_line', '').lower() or
                   e['id'] == q]
    if issue:
        results = [e for e in results if e['id'] in ENTRY_ISSUES and
                   any(iss.startswith(issue) for iss in ENTRY_ISSUES[e['id']])]

    total = len(results)
    start = (page - 1) * page_size
    end = start + page_size
    page_entries = results[start:end]

    return jsonify({
        'total': total,
        'page': page,
        'pageSize': page_size,
        'totalPages': math.ceil(total / page_size) if page_size else 1,
        'poems': page_entries,
    })


@app.route('/api/poems/<pid>')
def api_poem_detail(pid):
    if pid not in ID_MAP:
        return jsonify({'error': 'not found'}), 404
    info = ID_MAP[pid]
    fname = info['fname']
    entry_idx = info['entry_idx']
    try:
        data = load_json(fname)
        entry = data[entry_idx]
    except Exception as e:
        return jsonify({'error': str(e)}), 500
    return jsonify({
        'id': pid,
        'file': fname,
        'file_idx': info['file_idx'],
        'entry_idx': entry_idx,
        'title': entry.get('t', ''),
        'author': entry.get('a', ''),
        'dynasty': entry.get('d', ''),
        's': entry.get('s', ''),
        'lines': entry.get('p', []),
        'explanation': entry.get('e', '') or EXPLANATIONS_LOOKUP.get(f"{entry.get('t', '')}|{entry.get('a', '')}", ''),
        'issues': ENTRY_ISSUES.get(pid, []),
        'explanationSource': 'inline' if entry.get('e', '') else ('external' if EXPLANATIONS_LOOKUP.get(f"{entry.get('t', '')}|{entry.get('a', '')}", '') else ''),
    })


@app.route('/api/poems/<pid>', methods=['PUT'])
def api_poem_update(pid):
    if pid not in ID_MAP:
        return jsonify({'error': 'not found'}), 404
    info = ID_MAP[pid]
    fname = info['fname']
    entry_idx = info['entry_idx']

    body = request.get_json()
    if not body:
        return jsonify({'error': 'no data'}), 400

    try:
        data = load_json(fname)
        entry = data[entry_idx]

        changed = []
        if 'title' in body and body['title'] != entry.get('t', ''):
            entry['t'] = body['title']
            changed.append('title')
        if 'author' in body and body['author'] != entry.get('a', ''):
            entry['a'] = body['author']
            changed.append('author')
        if 'dynasty' in body and body['dynasty'] != entry.get('d', ''):
            entry['d'] = body['dynasty']
            changed.append('dynasty')
        if 's' in body and body['s'] != entry.get('s', ''):
            entry['s'] = body['s']
            changed.append('s')
        if 'lines' in body:
            entry['p'] = body['lines']
            changed.append('lines')
        if 'explanation' in body and body['explanation'] != (entry.get('e', '') or EXPLANATIONS_LOOKUP.get(f"{entry.get('t', '')}|{entry.get('a', '')}", '')):
            entry['e'] = body['explanation']
            changed.append('explanation')

        if changed:
            save_json(fname, data)
            # Sync explanation back to poem_explanations.json
            if 'explanation' in changed:
                key = f"{entry.get('t', '')}|{entry.get('a', '')}"
                exp_list = []
                if os.path.exists(EXPLANATIONS_FILE):
                    with open(EXPLANATIONS_FILE, encoding='utf-8') as ef:
                        exp_list = json.load(ef)
                found = False
                for item in exp_list:
                    if item.get('k') == key:
                        item['e'] = body['explanation']
                        found = True
                        break
                if not found:
                    exp_list.append({'k': key, 'e': body['explanation']})
                with open(EXPLANATIONS_FILE, 'w', encoding='utf-8') as ef:
                    json.dump(exp_list, ef, ensure_ascii=False, indent=2)
                load_explanations()
            build_index()  # rebuild cache

        return jsonify({'ok': True, 'changed': changed})
    except Exception as e:
        return jsonify({'error': str(e)}), 500


@app.route('/api/poems', methods=['POST'])
def api_poem_create():
    """Create a new poem entry in the appropriate JSON file."""
    body = request.get_json()
    if not body:
        return jsonify({'error': 'no data'}), 400

    title = body.get('title', '').strip()
    lines = body.get('lines', [])
    if not title or not lines:
        return jsonify({'error': 'title and lines required'}), 400

    dynasty = body.get('dynasty', '').strip()
    author = body.get('author', '').strip()
    s = body.get('s', '').strip()
    explanation = body.get('explanation', '').strip()

    # Find target file for this dynasty
    fname = None
    if dynasty:
        for fn, dyn in FILE_INDEX:
            if dyn == dynasty:
                fname = fn
                break

    if not fname:
        return jsonify({'error': f'no file found for dynasty: {dynasty}'}), 400

    try:
        data = load_json(fname)
    except Exception as e:
        return jsonify({'error': f'cannot load {fname}: {e}'}), 500

    new_entry = {'t': title, 'a': author, 'd': dynasty, 's': s, 'p': lines}
    if explanation:
        new_entry['e'] = explanation

    data.append(new_entry)
    save_json(fname, data)

    # Also add to poem_explanations.json
    if explanation:
        key = f"{title}|{author}"
        exp_list = []
        if os.path.exists(EXPLANATIONS_FILE):
            with open(EXPLANATIONS_FILE, encoding='utf-8') as ef:
                exp_list = json.load(ef)
        found = False
        for item in exp_list:
            if item.get('k') == key:
                item['e'] = explanation
                found = True
                break
        if not found:
            exp_list.append({'k': key, 'e': explanation})
        with open(EXPLANATIONS_FILE, 'w', encoding='utf-8') as ef:
            json.dump(exp_list, ef, ensure_ascii=False, indent=2)
        load_explanations()

    build_index()

    # Find the new poem id
    for pid, info in ID_MAP.items():
        if info['fname'] == fname:
            new_entry = next((e for e in GLOBAL_ENTRIES if e['id'] == pid
                              and e['title'] == title and e['author'] == author), None)
            if new_entry:
                return jsonify({'ok': True, 'id': new_entry['id']}), 201

    return jsonify({'ok': True, 'id': None}), 201


@app.route('/api/backup', methods=['POST'])
def api_backup():
    ts = backup_data()
    return jsonify({'ok': True, 'backup': ts})

@app.route('/api/scan-issues')
def api_scan_issues():
    """Return entries with issues, paginated."""
    page = int(request.args.get('page', 1))
    page_size = int(request.args.get('pageSize', 200))
    dynasty = request.args.get('dynasty', '')
    issue_type = request.args.get('issueType', '')

    results = []
    for pid, issues in ENTRY_ISSUES.items():
        if pid in ID_MAP:
            if issue_type and not any(iss.startswith(issue_type) for iss in issues):
                continue
            gi = next((e for e in GLOBAL_ENTRIES if e['id'] == pid), None)
            if gi and (not dynasty or gi['dynasty'] == dynasty):
                results.append({
                    'id': pid, 'title': gi['title'], 'author': gi['author'],
                    'dynasty': gi['dynasty'], 'issues': issues,
                })

    total = len(results)
    start = (page - 1) * page_size
    end = start + page_size
    return jsonify({'total': total, 'page': page, 'pageSize': page_size,
                    'issues': results[start:end]})


@app.route('/')
def index():
    return '''
    <h1>诗词管理工具</h1>
    <p><a href="/manage.html">打开管理页面 →</a></p>
    <p><a href="/api/stats">数据统计 API</a></p>
    <p><a href="/api/poems?page=1&pageSize=10">诗词列表 API</a></p>
    '''


@app.route('/manage.html')
def serve_manage():
    return send_from_directory(STATIC_DIR, 'manage.html')


if __name__ == '__main__':
    print(f'数据目录: {DATA_DIR}')
    print(f'启动服务器: http://localhost:5000')
    app.run(host='0.0.0.0', port=5000, debug=True)
