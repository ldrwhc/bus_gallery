#!/usr/bin/env python
# -*- coding: utf-8 -*-

import argparse
import json
import os
import re
import sys
import time
import traceback
import urllib.parse
import zlib
from typing import Any, Dict, List, Optional, Tuple

UA = (
    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36"
)

FIELD_ALIASES: Dict[str, List[str]] = {
    "品牌": ["品牌", "牌子", "brand", "brandname", "makerbrand"],
    "型号": ["型号", "车型", "model", "modelname", "vehmodel", "modelcode"],
    "制造商": ["制造商", "厂家", "生产商", "manufacturer", "maker"],
    "出厂日期": [
        "出厂日期", "生产日期", "制造日期", "factory_date", "factorydate",
        "build_date", "builddate", "production_date", "productiondate",
        "manufacture_date", "manufacturedate", "date_manuf", "datemanuf",
    ],
    "上线日期": [
        "上线日期", "投运日期", "服役日期", "运营日期", "上牌日期", "启用日期",
        "launch_date", "launchdate", "online_date", "onlinedate",
        "service_date", "servicedate", "operation_date", "operationdate",
        "first_service_date", "firstservicedate", "in_service_date", "inservicedate",
        "date_serve", "dateserve",
    ],
    "下线日期": [
        "下线日期", "退役日期", "报废日期", "退运日期", "停运日期",
        "retire_date", "retiredate", "offline_date", "offlinedate",
        "off_date", "offdate", "decommission_date", "decommissiondate",
        "date_retire", "dateretire",
    ],
    "发动机": ["发动机", "engine", "enginemodel"],
    "电机": ["电机", "motor", "motormodel"],
    "燃料": ["燃料", "燃料类型", "fuel", "fueltype"],
    "变速系统": ["变速系统", "变速器", "变速箱", "传动系统", "transmission", "gearbox", "drive"],
    "踏步": ["踏步", "踏步类型", "step", "stairs", "doorstep"],
    "悬挂": ["悬挂", "suspension"],
    "车桥品牌": ["车桥品牌", "车桥", "axle", "axlebrand"],
    "公司": ["公司", "运营公司", "所属公司", "operator", "company", "corp"],
    "地区": ["地区", "区域", "省份", "城市", "region", "area", "city", "province"],
}

KEY_LOOKUP: Dict[str, str] = {}
for std, aliases in FIELD_ALIASES.items():
    for a in aliases:
        KEY_LOOKUP[re.sub(r"[\s_\-:/：]+", "", a.strip().lower())] = std


def _force_utf8_stdio():
    try:
        if hasattr(sys.stdout, "reconfigure"):
            sys.stdout.reconfigure(encoding="utf-8", errors="replace")
        if hasattr(sys.stderr, "reconfigure"):
            sys.stderr.reconfigure(encoding="utf-8", errors="replace")
    except Exception:
        pass


def emit(payload: Dict[str, Any], code: int = 0):
    # Keep transport ASCII-only to avoid cp936/utf8 mismatch in Qt process bridge.
    sys.stdout.write(json.dumps(payload, ensure_ascii=True))
    sys.stdout.flush()
    raise SystemExit(code)


def normalize_plate(raw: str) -> str:
    if raw is None:
        return ""
    s = str(raw).strip().upper()
    s = s.replace("\u3000", " ")
    s = re.sub(r"\s+", "", s)
    if len(s) >= 3:
        s = s[:2] + " " + s[2:]
    return s.strip()


def plate_is_valid(plate: str) -> bool:
    return bool(re.match(r"^[\u4e00-\u9fa5][A-Z]\s[A-Z0-9]{5,6}$", plate))


def prepare_hyperlpr_env() -> str:
    """
    HyperLPR upstream on Windows uses HOMEPATH directly (without HOMEDRIVE),
    which may resolve to an invalid root path like '\\Users\\...'.
    We override it to a stable writable directory.
    """
    local_app = os.environ.get("LOCALAPPDATA", "").strip()
    user_profile = os.environ.get("USERPROFILE", "").strip()
    fallback = os.path.expanduser("~")
    base = local_app or user_profile or fallback
    base = os.path.abspath(base)
    safe_home = os.path.join(base, "BusGalleryLocal")
    os.makedirs(safe_home, exist_ok=True)
    os.environ["HOMEPATH"] = safe_home
    os.environ.setdefault("HOME", safe_home)
    os.environ.setdefault("TQDM_DISABLE", "1")
    return safe_home


def acquire_lock(lock_file: str, timeout_sec: float = 120.0):
    start = time.time()
    while True:
        try:
            fd = os.open(lock_file, os.O_CREAT | os.O_EXCL | os.O_WRONLY)
            os.write(fd, f"{os.getpid()} {time.time()}".encode("utf-8", errors="ignore"))
            return fd
        except FileExistsError:
            if time.time() - start > timeout_sec:
                raise TimeoutError(f"等待模型初始化锁超时: {lock_file}")
            time.sleep(0.2)


def release_lock(fd: int, lock_file: str):
    try:
        os.close(fd)
    except Exception:
        pass
    try:
        os.remove(lock_file)
    except Exception:
        pass


def append_hyperlpr_paths():
    base = os.path.dirname(os.path.abspath(__file__))
    candidates = [
        os.path.join(base, "_thirdparty_hyperlpr", "Prj-Python"),
        os.path.join(base, "tools", "hyperlpr", "Prj-Python"),
        os.path.join(base, "third_party", "HyperLPR", "Prj-Python"),
        os.path.join(base, "..", "_thirdparty_hyperlpr", "Prj-Python"),
    ]
    for p in candidates:
        p = os.path.abspath(p)
        if os.path.isdir(p) and p not in sys.path:
            sys.path.insert(0, p)


def recognize_plate(image_path: str):
    if not image_path or not os.path.isfile(image_path):
        return {
            "ok": False,
            "error": f"图片不存在: {image_path}",
            "plate": "",
            "confidence": 0.0,
            "source": "HyperLPR",
        }

    safe_home = prepare_hyperlpr_env()
    append_hyperlpr_paths()

    lpr3 = None
    cv2 = None
    lock_fd = None
    lock_file = os.path.join(safe_home, ".hyperlpr3", "model_init.lock")
    os.makedirs(os.path.dirname(lock_file), exist_ok=True)

    try:
        import cv2 as _cv2  # type: ignore

        cv2 = _cv2
    except Exception as e:
        return {
            "ok": False,
            "error": f"依赖缺失，无法调用 HyperLPR: {type(e).__name__}: {e}",
            "plate": "",
            "confidence": 0.0,
            "source": "HyperLPR",
        }

    for attempt in range(1, 4):
        try:
            lock_fd = acquire_lock(lock_file, timeout_sec=120.0)
            import hyperlpr3 as _lpr3  # type: ignore

            lpr3 = _lpr3
            break
        except Exception as e:
            err = f"{type(e).__name__}: {e}"
            if attempt >= 3:
                return {
                    "ok": False,
                    "error": f"依赖缺失，无法调用 HyperLPR: {err}",
                    "plate": "",
                    "confidence": 0.0,
                    "source": "HyperLPR",
                }
            time.sleep(0.8 * attempt)
        finally:
            if lock_fd is not None:
                release_lock(lock_fd, lock_file)
                lock_fd = None

    try:
        # cv2.imread may fail on Windows non-ASCII path; use fromfile + imdecode fallback.
        img = cv2.imread(image_path)
        if img is None:
            try:
                import numpy as np  # type: ignore

                data = np.fromfile(image_path, dtype=np.uint8)
                if data is not None and data.size > 0:
                    img = cv2.imdecode(data, cv2.IMREAD_COLOR)
            except Exception:
                img = None
        if img is None:
            return {
                "ok": False,
                "error": f"图片读取失败: {image_path}",
                "plate": "",
                "confidence": 0.0,
                "source": "HyperLPR",
            }

        catcher = lpr3.LicensePlateCatcher(detect_level=lpr3.DETECT_LEVEL_HIGH)
        rows = catcher(img)
        best_plate = ""
        best_conf = 0.0
        for row in rows:
            if not isinstance(row, (list, tuple)) or len(row) < 2:
                continue
            code = str(row[0]).strip()
            try:
                conf = float(row[1])
            except Exception:
                conf = 0.0
            if not code:
                continue
            plate = normalize_plate(code)
            if conf >= best_conf:
                best_conf = conf
                best_plate = plate

        if not best_plate:
            return {
                "ok": False,
                "error": "未识别到车牌",
                "plate": "",
                "confidence": 0.0,
                "source": "HyperLPR",
            }

        return {
            "ok": True,
            "error": "",
            "plate": best_plate,
            "confidence": round(best_conf, 4),
            "source": "HyperLPR",
        }
    except Exception as e:
        return {
            "ok": False,
            "error": f"识别异常: {type(e).__name__}: {e}",
            "plate": "",
            "confidence": 0.0,
            "source": "HyperLPR",
        }


def strip_html_text(html: str) -> str:
    text = re.sub(r"(?is)<script.*?>.*?</script>", "\n", html)
    text = re.sub(r"(?is)<style.*?>.*?</style>", "\n", text)
    text = re.sub(r"(?is)<[^>]+>", "\n", text)
    text = text.replace("&nbsp;", " ").replace("&amp;", "&")
    text = re.sub(r"\n+", "\n", text)
    return text


def _normalize_key(s: str) -> str:
    return re.sub(r"[\s_\-:/：]+", "", (s or "").strip().lower())


def _canonical_key(raw_key: str) -> Optional[str]:
    if not raw_key:
        return None
    norm = _normalize_key(raw_key)
    if not norm:
        return None
    if norm in KEY_LOOKUP:
        return KEY_LOOKUP[norm]
    for alias_norm, std in KEY_LOOKUP.items():
        if alias_norm and alias_norm in norm:
            return std
    return None


def _clean_value(v: Any) -> str:
    if v is None:
        return ""
    if isinstance(v, bool):
        return "是" if v else "否"
    s = str(v).strip()
    s = s.strip(" \t\r\n|,，。;；\"'")
    s = re.sub(r"\s+", " ", s)
    if len(s) > 240:
        s = s[:240].strip()
    return s


def _looks_noisy_value(v: str) -> bool:
    s = (v or "").strip()
    if not s:
        return True
    if "<" in s or ">" in s:
        return True
    lower = s.lower()
    if "nan" in lower and ("progress" in lower or "opacity" in lower):
        return True
    if "v-progress" in lower or "indeterminate" in lower:
        return True
    visible = re.findall(r"[A-Za-z0-9\u4e00-\u9fa5]", s)
    if len(visible) < max(2, len(s) // 4):
        return True
    return False


def merge_field(fields: Dict[str, str], key: str, value: str):
    k = _canonical_key(key)
    v = _clean_value(value)
    if not k or not v:
        return
    if _looks_noisy_value(v):
        return
    old = fields.get(k, "").strip()
    if not old:
        fields[k] = v
        return
    if old == v:
        return
    if len(v) > len(old):
        fields[k] = v


def parse_buspedia_fields(text: str) -> Dict[str, str]:
    fields: Dict[str, str] = {}
    if not text:
        return fields

    for std_key, aliases in FIELD_ALIASES.items():
        for alias in aliases:
            m = re.search(rf"{re.escape(alias)}\s*[:：]\s*([^\n\r]{{1,160}})", text, re.IGNORECASE)
            if m:
                v = _clean_value(m.group(1))
                if v:
                    fields[std_key] = v
                    break

    # Generic "key: value" extraction as supplement.
    for m in re.finditer(r"([A-Za-z\u4e00-\u9fa5]{1,24})\s*[:：]\s*([^\n\r]{1,180})", text):
        merge_field(fields, m.group(1), m.group(2))

    return fields


def _extract_fields_from_object(obj: Any, out_fields: Dict[str, str], _depth: int = 0):
    if _depth > 8 or obj is None:
        return
    if isinstance(obj, dict):
        for k, v in obj.items():
            if isinstance(v, (dict, list)):
                _extract_fields_from_object(v, out_fields, _depth + 1)
            else:
                merge_field(out_fields, str(k), v)
    elif isinstance(obj, list):
        for it in obj[:50]:
            _extract_fields_from_object(it, out_fields, _depth + 1)


def _decode_payload_bytes(raw: bytes) -> str:
    if not raw:
        return ""
    data = raw
    try:
        if len(raw) >= 2 and raw[0] == 0x78:
            data = zlib.decompress(raw)
    except Exception:
        data = raw

    for enc in ("utf-8", "utf-8-sig", "gb18030", "latin-1"):
        try:
            return data.decode(enc, errors="replace")
        except Exception:
            continue
    return ""


def _extract_fields_from_payload(raw: bytes) -> Dict[str, str]:
    text = _decode_payload_bytes(raw)
    out: Dict[str, str] = {}
    if not text:
        return out
    text = text.strip()
    if text.startswith("{") or text.startswith("["):
        try:
            obj = json.loads(text)
            _extract_fields_from_object(obj, out)
        except Exception:
            pass
    return out


def _score_fields(fields: Dict[str, str]) -> int:
    if not fields:
        return 0
    score = len(fields) * 10
    for k in ("品牌", "型号", "制造商", "发动机", "燃料"):
        if fields.get(k):
            score += 3
    return score


def _json_from_payload(raw: bytes) -> Optional[Any]:
    text = _decode_payload_bytes(raw).strip()
    if not text:
        return None
    if not (text.startswith("{") or text.startswith("[")):
        return None
    try:
        return json.loads(text)
    except Exception:
        return None


def _str_from_value(v: Any) -> str:
    if v is None:
        return ""
    if isinstance(v, str):
        return _clean_value(v)
    if isinstance(v, (int, float, bool)):
        return _clean_value(v)
    if isinstance(v, dict):
        for k in ("name", "model", "regist", "no", "id", "uid"):
            if k in v:
                s = _str_from_value(v.get(k))
                if s:
                    return s
        return ""
    if isinstance(v, list):
        parts: List[str] = []
        for x in v:
            s = _str_from_value(x)
            if s and s not in parts:
                parts.append(s)
        return "|".join(parts[:8])
    return ""


def _extract_search_ids_and_fields(obj: Any) -> Tuple[List[str], Dict[str, str]]:
    ids: List[str] = []
    fields: Dict[str, str] = {}
    if not isinstance(obj, dict):
        return ids, fields
    rows = obj.get("v")
    if not isinstance(rows, list):
        return ids, fields
    for row in rows:
        if not isinstance(row, dict):
            continue
        uid = _clean_value(row.get("id") or row.get("uid"))
        if uid and uid not in ids:
            ids.append(uid)
        # Search list usually returns plate/company/number.
        merge_field(fields, "公司", row.get("comp"))
        merge_field(fields, "型号", row.get("model"))
    return ids, fields


def _extract_bus_detail_fields(obj: Any) -> Dict[str, str]:
    out: Dict[str, str] = {}
    if not isinstance(obj, dict):
        return out

    veh = obj.get("veh")
    model = obj.get("model")

    if isinstance(veh, dict):
        region = veh.get("region")
        comp = veh.get("comp")
        merge_field(out, "地区", region.get("name") if isinstance(region, dict) else region)
        merge_field(out, "公司", comp.get("name") if isinstance(comp, dict) else comp)
        for k in (
            "factory_date", "factoryDate", "build_date", "buildDate",
            "production_date", "productionDate", "manufacture_date", "manufactureDate",
            "product_date", "productDate", "date_manuf", "dateManuf",
        ):
            merge_field(out, "出厂日期", veh.get(k))
        for k in (
            "launch_date", "launchDate", "online_date", "onlineDate",
            "service_date", "serviceDate", "operation_date", "operationDate",
            "first_service_date", "firstServiceDate", "in_service_date", "inServiceDate",
            "date_serve", "dateServe",
        ):
            merge_field(out, "上线日期", veh.get(k))
        for k in (
            "retire_date", "retireDate", "offline_date", "offlineDate",
            "off_date", "offDate", "decommission_date", "decommissionDate",
            "date_retire", "dateRetire",
        ):
            merge_field(out, "下线日期", veh.get(k))

    if isinstance(model, dict):
        manuf_text = _str_from_value(model.get("manuf"))
        merge_field(out, "品牌", manuf_text)
        merge_field(out, "制造商", manuf_text)
        merge_field(out, "型号", model.get("model") or model.get("name") or model.get("series"))
        merge_field(out, "发动机", model.get("ice") or model.get("engine"))
        merge_field(out, "电机", model.get("motor"))
        merge_field(out, "燃料", model.get("fuel") or model.get("fuel_cell"))
        merge_field(out, "变速系统", model.get("trans"))
        merge_field(out, "踏步", model.get("step"))
        merge_field(out, "悬挂", model.get("suspension"))
        merge_field(out, "车桥品牌", model.get("axle"))
        for k in (
            "factory_date", "factoryDate", "build_date", "buildDate",
            "production_date", "productionDate", "manufacture_date", "manufactureDate",
            "product_date", "productDate", "date_manuf", "dateManuf",
        ):
            merge_field(out, "出厂日期", model.get(k))
        for k in (
            "launch_date", "launchDate", "online_date", "onlineDate",
            "service_date", "serviceDate", "operation_date", "operationDate",
            "first_service_date", "firstServiceDate", "in_service_date", "inServiceDate",
            "date_serve", "dateServe",
        ):
            merge_field(out, "上线日期", model.get(k))
        for k in (
            "retire_date", "retireDate", "offline_date", "offlineDate",
            "off_date", "offDate", "decommission_date", "decommissionDate",
            "date_retire", "dateRetire",
        ):
            merge_field(out, "下线日期", model.get(k))

        meta = model.get("meta")
        if isinstance(meta, dict):
            for k, v in meta.items():
                merge_field(out, str(k), v)
        elif isinstance(meta, list):
            for it in meta[:80]:
                if isinstance(it, dict):
                    for k, v in it.items():
                        merge_field(out, str(k), v)

    return out


def fetch_from_buspedia_requests(plate: str) -> Dict[str, Any]:
    try:
        import requests  # type: ignore
    except Exception as e:
        return {
            "ok": False,
            "found": False,
            "source": "BUSPEDIA_REQUESTS",
            "error": f"缺少 requests 依赖: {type(e).__name__}: {e}",
            "fields": {},
        }

    normalized = normalize_plate(plate)
    compact_plate = normalized.replace(" ", "")
    headers = {"User-Agent": UA, "Accept": "text/html,application/json,text/plain,*/*"}

    best_fields: Dict[str, str] = {}
    best_url = ""

    urls = [
        "https://api.buspedia.top/search?name=" + urllib.parse.quote_plus(normalized),
        "https://api.buspedia.top/search?name=" + urllib.parse.quote_plus(compact_plate),
        "https://buspedia.top/search?name=" + urllib.parse.quote_plus(normalized),
        "https://buspedia.top/search?name=" + urllib.parse.quote_plus(compact_plate),
        "https://buspedia.top/search?q=" + urllib.parse.quote(compact_plate),
        "https://buspedia.top/search?q=" + urllib.parse.quote(normalized),
        "https://api.buspedia.top/search?q=" + urllib.parse.quote(compact_plate),
        "https://api.buspedia.top/search?q=" + urllib.parse.quote(normalized),
        "https://api.buspedia.top/bus/query?q=" + urllib.parse.quote(compact_plate),
        "https://api.buspedia.top/bus/query?q=" + urllib.parse.quote(normalized),
    ]
    bus_ids: List[str] = []

    for url in urls:
        try:
            resp = requests.get(url, timeout=20, headers=headers)
            if resp.status_code != 200:
                continue

            candidate: Dict[str, str] = {}
            ctype = str(resp.headers.get("content-type", "")).lower()
            if "octet-stream" in ctype or "application/json" in ctype:
                obj = _json_from_payload(resp.content)
                if obj is not None:
                    # Extract id list from search result, then resolve details via /bus/{id}.
                    ids, f0 = _extract_search_ids_and_fields(obj)
                    for i in ids:
                        if i and i not in bus_ids:
                            bus_ids.append(i)
                    for k, v in f0.items():
                        merge_field(candidate, k, v)
                payload_fields = _extract_fields_from_payload(resp.content)
                for k, v in payload_fields.items():
                    merge_field(candidate, k, v)
            else:
                text_fields = parse_buspedia_fields(strip_html_text(resp.text))
                for k, v in text_fields.items():
                    merge_field(candidate, k, v)

            if _score_fields(candidate) > _score_fields(best_fields):
                best_fields = candidate
                best_url = url
        except Exception:
            continue

    # Resolve bus detail if search gave vehicle ids.
    for uid in bus_ids[:8]:
        detail_url = "https://api.buspedia.top/bus/" + urllib.parse.quote(uid)
        try:
            resp = requests.get(detail_url, timeout=20, headers=headers)
            if resp.status_code != 200:
                continue
            obj = _json_from_payload(resp.content)
            if obj is None:
                continue
            candidate = _extract_bus_detail_fields(obj)
            payload_fields = _extract_fields_from_payload(resp.content)
            for k, v in payload_fields.items():
                merge_field(candidate, k, v)
            if _score_fields(candidate) > _score_fields(best_fields):
                best_fields = candidate
                best_url = detail_url
        except Exception:
            continue

    if _score_fields(best_fields) >= 13:
        return {
            "ok": True,
            "found": True,
            "source": "BUSPEDIA_REQUESTS",
            "error": "",
            "fields": best_fields,
            "url": best_url,
        }

    return {
        "ok": True,
        "found": False,
        "source": "BUSPEDIA_REQUESTS",
        "error": "requests 链路未抓到可解析配置。",
        "fields": {},
    }


def _candidate_links(page, compact_plate: str) -> List[str]:
    links: List[str] = []
    try:
        rows = page.eval_on_selector_all(
            "a[href]",
            """
            (els) => els.map(a => ({
              href: a.href || '',
              text: (a.innerText || a.textContent || '').trim()
            }))
            """,
        )
    except Exception:
        rows = []

    wanted: List[Tuple[int, str]] = []
    for row in rows:
        href = str((row or {}).get("href") or "").strip()
        text = str((row or {}).get("text") or "").strip()
        if not href.startswith("http"):
            continue
        if "buspedia.top" not in href:
            continue
        if any(skip in href for skip in ("/login", "/register", "/about", "/privacy")):
            continue
        score = 0
        ht = (href + " " + text).lower()
        if compact_plate.lower() in ht:
            score += 20
        if any(tag in href for tag in ("/bus/", "/photo/", "/line/", "/model/", "/comp/")):
            score += 8
        if "search" in href:
            score -= 3
        if score <= 0:
            continue
        wanted.append((score, href))

    wanted.sort(key=lambda x: x[0], reverse=True)
    for _, u in wanted:
        if u not in links:
            links.append(u)
        if len(links) >= 5:
            break
    return links


def fetch_from_buspedia_playwright(plate: str) -> Dict[str, Any]:
    normalized = normalize_plate(plate)
    compact_plate = normalized.replace(" ", "")

    try:
        from playwright.sync_api import TimeoutError as PlaywrightTimeoutError  # type: ignore
        from playwright.sync_api import sync_playwright  # type: ignore
    except Exception as e:
        return {
            "ok": False,
            "found": False,
            "source": "BUSPEDIA_PLAYWRIGHT",
            "error": (
                f"缺少 Playwright 依赖: {type(e).__name__}: {e}。"
                "请先执行: pip install playwright && playwright install chromium"
            ),
            "fields": {},
        }

    best_fields: Dict[str, str] = {}
    best_url = ""
    logs: List[str] = []
    last_err = ""

    queries = [compact_plate, normalized]
    with sync_playwright() as p:
        browser = p.chromium.launch(
            headless=True,
            args=[
                "--disable-blink-features=AutomationControlled",
                "--disable-dev-shm-usage",
                "--no-sandbox",
            ],
        )
        context = browser.new_context(
            locale="zh-CN",
            user_agent=UA,
            viewport={"width": 1366, "height": 900},
        )
        context.set_default_timeout(35000)
        page = context.new_page()
        api_blobs: List[Tuple[str, bytes]] = []

        def on_response(resp):
            try:
                if "api.buspedia.top/" not in resp.url:
                    return
                if resp.status != 200:
                    return
                body = resp.body()
                if not body or len(body) > 4 * 1024 * 1024:
                    return
                api_blobs.append((resp.url, body))
                if len(api_blobs) > 80:
                    del api_blobs[:20]
            except Exception:
                return

        page.on("response", on_response)

        for q in queries:
            search_url = "https://buspedia.top/search?name=" + urllib.parse.quote_plus(q)
            try:
                logs.append(f"goto:{search_url}")
                page.goto(search_url, wait_until="domcontentloaded", timeout=35000)
                page.wait_for_timeout(1800)

                # Simulate manual operation: focus input, fill, press Enter.
                try:
                    inps = page.locator("input[type='text']")
                    if inps.count() > 0:
                        inp = inps.first
                        inp.click(timeout=1500)
                        inp.fill(normalized, timeout=1500)
                        page.keyboard.press("Enter")
                        page.wait_for_timeout(1800)
                        logs.append("manual-search-enter")
                except Exception:
                    pass

                # Extract from current page text/html.
                try:
                    body_text = page.inner_text("body")
                    fs = parse_buspedia_fields(body_text)
                    if _score_fields(fs) > _score_fields(best_fields):
                        best_fields = fs
                        best_url = page.url
                except Exception:
                    pass

                # Follow likely result links (simulate click/open detail).
                for href in _candidate_links(page, compact_plate):
                    try:
                        page.goto(href, wait_until="domcontentloaded", timeout=25000)
                        page.wait_for_timeout(1200)
                        body_text = page.inner_text("body")
                        fs = parse_buspedia_fields(body_text)
                        if _score_fields(fs) > _score_fields(best_fields):
                            best_fields = fs
                            best_url = href
                    except PlaywrightTimeoutError:
                        continue
                    except Exception:
                        continue
            except PlaywrightTimeoutError:
                last_err = f"Playwright 页面加载超时: {search_url}"
                continue
            except Exception as e:
                last_err = f"{type(e).__name__}: {e}"
                continue

        # Parse captured API payloads.
        for api_url, blob in api_blobs:
            fs = _extract_fields_from_payload(blob)
            if _score_fields(fs) > _score_fields(best_fields):
                best_fields = fs
                best_url = api_url

        try:
            context.close()
        except Exception:
            pass
        try:
            browser.close()
        except Exception:
            pass

    if _score_fields(best_fields) >= 13:
        return {
            "ok": True,
            "found": True,
            "source": "BUSPEDIA_PLAYWRIGHT",
            "error": "",
            "fields": best_fields,
            "url": best_url,
            "log": "; ".join(logs[-8:]),
        }

    return {
        "ok": True,
        "found": False,
        "source": "BUSPEDIA_PLAYWRIGHT",
        "error": last_err or "Playwright 链路未抓到可解析配置。",
        "fields": {},
        "log": "; ".join(logs[-8:]),
    }


def fetch_from_buspedia(plate: str):
    normalized = normalize_plate(plate)
    if not plate_is_valid(normalized):
        return {
            "ok": False,
            "found": False,
            "source": "BUSPEDIA",
            "error": "车牌格式不正确",
            "fields": {},
        }

    # Priority: Playwright simulated manual chain -> requests/API fallback.
    errors: List[str] = []

    try:
        pw = fetch_from_buspedia_playwright(normalized)
        if pw.get("ok") and pw.get("found") and isinstance(pw.get("fields"), dict) and pw.get("fields"):
            return pw
        if pw.get("error"):
            errors.append(f"Playwright: {pw.get('error')}")
    except Exception as e:
        errors.append(f"Playwright: {type(e).__name__}: {e}")

    try:
        req = fetch_from_buspedia_requests(normalized)
        if req.get("ok") and req.get("found") and isinstance(req.get("fields"), dict) and req.get("fields"):
            return req
        if req.get("error"):
            errors.append(f"requests: {req.get('error')}")
    except Exception as e:
        errors.append(f"requests: {type(e).__name__}: {e}")

    detail = "；".join([x for x in errors if x]) or "远程链路无返回"
    return {
        "ok": True,
        "found": False,
        "source": "BUSPEDIA",
        "error": f"Buspedia 未返回可解析配置（将回退本地库）。{detail}",
        "fields": {},
    }


def main():
    parser = argparse.ArgumentParser(description="Plate assistant for BusGalleryLocal")
    sub = parser.add_subparsers(dest="cmd", required=True)

    p_rec = sub.add_parser("recognize", help="Recognize plate from image")
    p_rec.add_argument("--image", required=True, help="Image file path")

    p_fetch = sub.add_parser("fetch", help="Fetch config by plate from buspedia")
    p_fetch.add_argument("--plate", required=True, help="Plate string")

    args = parser.parse_args()
    if args.cmd == "recognize":
        emit(recognize_plate(args.image), 0)
    if args.cmd == "fetch":
        emit(fetch_from_buspedia(args.plate), 0)

    emit({"ok": False, "error": "unknown command"}, 2)


if __name__ == "__main__":
    try:
        _force_utf8_stdio()
        main()
    except SystemExit:
        raise
    except Exception as e:
        emit(
            {
                "ok": False,
                "found": False,
                "source": "PLATE_ASSISTANT",
                "error": f"{type(e).__name__}: {e}",
                "trace": traceback.format_exc(limit=1),
            },
            1,
        )
