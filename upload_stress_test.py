#!/usr/bin/env python3
import argparse
import json
import os
import statistics
import threading
import time
import uuid
from concurrent.futures import ThreadPoolExecutor, as_completed

import requests


def parse_args():
    parser = argparse.ArgumentParser(
        description="Login then stress test /api/upload with a local image file."
    )
    parser.add_argument("--base-url", default="http://192.144.227.251/api", help="API base URL, e.g. http://localhost/api")
    parser.add_argument("--username", default="admin", help="Login username")
    parser.add_argument("--password", default="12345678", help="Login password")
    parser.add_argument("--file", default="test.png", help="Image file path")
    parser.add_argument("--total", type=int, default=40, help="Total upload requests")
    parser.add_argument("--concurrency", type=int, default=10, help="Concurrent workers")
    parser.add_argument("--timeout", type=int, default=30, help="Request timeout seconds")
    parser.add_argument("--report", default="", help="Optional report output path (.json)")
    return parser.parse_args()


def extract_token(login_json):
    if isinstance(login_json, dict):
        if isinstance(login_json.get("token"), str):
            return login_json["token"]
        data = login_json.get("data")
        if isinstance(data, dict) and isinstance(data.get("token"), str):
            return data["token"]
    return ""


def is_success_response(resp):
    if resp.status_code != 200:
        return False
    try:
        body = resp.json()
    except Exception:
        return True
    if isinstance(body, dict) and "code" in body:
        return str(body.get("code")) == "00000"
    return True


def percentile(sorted_values, p):
    if not sorted_values:
        return 0.0
    if len(sorted_values) == 1:
        return float(sorted_values[0])
    k = (len(sorted_values) - 1) * p
    f = int(k)
    c = min(f + 1, len(sorted_values) - 1)
    if f == c:
        return float(sorted_values[f])
    d = k - f
    return float(sorted_values[f] * (1.0 - d) + sorted_values[c] * d)


def main():
    args = parse_args()
    file_path = os.path.abspath(args.file)
    if not os.path.exists(file_path):
        raise FileNotFoundError(f"File not found: {file_path}")

    login_url = f"{args.base_url.rstrip('/')}/auth/login"
    upload_url = f"{args.base_url.rstrip('/')}/upload"

    session = requests.Session()
    login_resp = session.post(
        login_url,
        json={"username": args.username, "password": args.password},
        timeout=args.timeout,
    )
    login_resp.raise_for_status()
    login_json = login_resp.json()
    token = extract_token(login_json)
    if not token:
        raise RuntimeError(f"Login succeeded but token missing. response={login_json}")

    payload = {
        "plateNumber": "浙A 11111",
        "brandName": "BJ",
        "modelName": "TEST",
        "companyName": "电车公司",
        "regionProvince": "浙江省",
        "regionCity": "杭州市",
        "airConditioned": True,
        "source": "stress-test",
        "remark": "upload stress test",
    }

    lock = threading.Lock()
    latencies_ms = []
    status_counter = {}
    sample_errors = []

    total = max(1, args.total)
    workers = max(1, min(args.concurrency, total))

    start_all = time.perf_counter()

    def one_request(i):
        headers = {
            "Authorization": f"Bearer {token}",
            "Idempotency-Key": f"loadtest-{int(time.time() * 1000)}-{i}-{uuid.uuid4().hex[:8]}",
        }
        local_payload = dict(payload)
        local_payload["customNumber"] = f"LT-{i:05d}"
        start = time.perf_counter()
        try:
            with open(file_path, "rb") as f:
                files = {
                    "file": (os.path.basename(file_path), f, "image/png"),
                    "payload": (None, json.dumps(local_payload, ensure_ascii=False), "application/json"),
                }
                resp = requests.post(upload_url, headers=headers, files=files, timeout=args.timeout)
            elapsed_ms = (time.perf_counter() - start) * 1000.0
            ok = is_success_response(resp)
            key = f"http_{resp.status_code}_{'ok' if ok else 'biz_fail'}"
            with lock:
                latencies_ms.append(elapsed_ms)
                status_counter[key] = status_counter.get(key, 0) + 1
                if (not ok) and len(sample_errors) < 8:
                    body = resp.text[:300].replace("\n", " ")
                    sample_errors.append({"i": i, "status": resp.status_code, "body": body})
            return ok
        except Exception as ex:
            elapsed_ms = (time.perf_counter() - start) * 1000.0
            with lock:
                latencies_ms.append(elapsed_ms)
                status_counter["exception"] = status_counter.get("exception", 0) + 1
                if len(sample_errors) < 8:
                    sample_errors.append({"i": i, "status": "exception", "body": str(ex)})
            return False

    success = 0
    with ThreadPoolExecutor(max_workers=workers) as pool:
        futures = [pool.submit(one_request, i) for i in range(total)]
        for fu in as_completed(futures):
            if fu.result():
                success += 1

    wall_s = time.perf_counter() - start_all
    failed = total - success
    lat_sorted = sorted(latencies_ms)

    report = {
        "base_url": args.base_url,
        "upload_url": upload_url,
        "username": args.username,
        "file": file_path,
        "file_size_bytes": os.path.getsize(file_path),
        "total_requests": total,
        "concurrency": workers,
        "success": success,
        "failed": failed,
        "success_rate": round(success / total, 4),
        "wall_time_seconds": round(wall_s, 3),
        "throughput_rps": round(total / wall_s, 3) if wall_s > 0 else 0.0,
        "latency_ms": {
            "avg": round(statistics.mean(lat_sorted), 2) if lat_sorted else 0.0,
            "p50": round(percentile(lat_sorted, 0.50), 2),
            "p95": round(percentile(lat_sorted, 0.95), 2),
            "p99": round(percentile(lat_sorted, 0.99), 2),
            "max": round(max(lat_sorted), 2) if lat_sorted else 0.0,
        },
        "status_counter": status_counter,
        "sample_errors": sample_errors,
    }

    print(json.dumps(report, ensure_ascii=False, indent=2))

    report_path = args.report.strip()
    if report_path:
        with open(report_path, "w", encoding="utf-8") as wf:
            json.dump(report, wf, ensure_ascii=False, indent=2)
        print(f"\nReport saved to: {os.path.abspath(report_path)}")


if __name__ == "__main__":
    main()
