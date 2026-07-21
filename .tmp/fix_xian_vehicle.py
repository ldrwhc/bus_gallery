#!/usr/bin/env python3
"""Fix company/region for 陕A L8500 — should be Xi'an, not whatever was set."""
import paramiko, urllib.request, json, zlib, ssl, pymysql, time

# --- 1. Fetch from buspedia ---
ctx = ssl.create_default_context()
ctx.check_hostname = False
ctx.verify_mode = ssl.CERT_NONE

req = urllib.request.Request("https://api.buspedia.top/bus/p1zhms")
req.add_header("User-Agent", "Mozilla/5.0 BusGalleryFix/1.0")
req.add_header("Accept", "application/json")
req.add_header("Origin", "https://buspedia.top")
req.add_header("Referer", "https://buspedia.top/")

resp = urllib.request.urlopen(req, timeout=15)
raw = resp.read()
if raw[:2] == b'\x78\x9c':
    raw = zlib.decompress(raw)
data = json.loads(raw)
veh = data.get("veh", {})
comp_name = veh.get("comp", {}).get("name", "")
region_name = veh.get("region", {}).get("name", "")
print(f"Buspedia: plate={veh.get('regist')}, company={comp_name}, region={region_name}")

# --- 2. Connect to production DB ---
db = pymysql.connect(host="192.144.227.251", port=13306,
                     user="root", password="123456",
                     database="bus_gallery", charset="utf8mb4")
cur = db.cursor()

# Find the vehicle
cur.execute("SELECT v.id, v.plate_number, v.company_id, v.region_id, c.name FROM vehicle v LEFT JOIN company c ON v.company_id = c.id WHERE v.plate_number LIKE '%L8500%'")
row = cur.fetchone()
if not row:
    print("NOT FOUND in DB")
    cur.close(); db.close(); exit(1)

vid, plate, old_cid, old_rid, old_comp = row
print(f"DB: id={vid}, plate={plate}, company_id={old_cid} ({old_comp}), region_id={old_rid}")

# --- 3. Find or create the correct company ---
cur.execute("SELECT id, name FROM company WHERE name = %s", (comp_name,))
comp_row = cur.fetchone()
if comp_row:
    new_cid = comp_row[0]
    print(f"Company found: id={new_cid} ({comp_name})")
else:
    # Insert new company
    cur.execute("INSERT INTO company (name) VALUES (%s)", (comp_name,))
    new_cid = cur.lastrowid
    print(f"Company created: id={new_cid} ({comp_name})")

# --- 4. Find the correct region (search by name) ---
cur.execute("SELECT id, name FROM region WHERE name = %s OR name LIKE %s", (region_name, f"%{region_name}%"))
reg_row = cur.fetchone()
if reg_row:
    new_rid = reg_row[0]
    print(f"Region found: id={new_rid} ({reg_row[1]})")
else:
    new_rid = old_rid
    print(f"Region not found, keeping region_id={old_rid}")

# --- 5. Update vehicle ---
cur.execute("UPDATE vehicle SET company_id = %s, region_id = %s WHERE id = %s",
            (new_cid, new_rid, vid))
db.commit()
print(f"Updated vehicle {vid}: company {old_cid}->{new_cid}, region {old_rid}->{new_rid}")

cur.close()
db.close()
print("Done!")
