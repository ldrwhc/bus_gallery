#!/usr/bin/env python3
"""
Fix factory_date and launch_date for CK6120LGEV2 / CK6121LGEV vehicles
based on verified buspedia data.
"""
import pymysql, sys

DB = dict(host='127.0.0.1', port=13306, user='root', password='123456', database='bus_gallery', charset='utf8mb4')

# plate -> (factory_date, launch_date) verified from buspedia API
FIXES = {
    # CK6120LGEV2
    "苏A 19980D": ("2014-04-01", "2014-07-01"),
    "苏A 15700D": ("2014-04-01", "2014-07-01"),
    "辽B L4370": ("2014-06-01", "2015-09-01"),
    "冀H Z6392": ("2014-12-01", "2015-01-01"),
    "冀H Z7909": ("2014-12-01", "2015-01-01"),
    "冀H Z1811": ("2014-12-01", "2015-01-01"),

    # CK6121LGEV
    "辽B M3050": ("2016-01-01", "2017-01-01"),
    "辽B M3101": ("2016-06-01", "2017-01-01"),
    "辽B M5061": ("2016-01-01", "2018-01-01"),
    "辽B M5096": ("2016-06-01", "2017-09-01"),
    "辽B M5215": ("2016-01-01", "2017-01-01"),
    "浙A 4M143": ("2015-09-01", "2016-01-01"),
    "浙A 4H502": ("2015-09-01", "2016-02-01"),
    "浙A 9J521": ("2015-09-01", "2016-01-01"),
    "浙A 4H100": ("2015-09-01", "2016-01-01"),
    "浙A 4H397": ("2015-09-01", "2016-02-01"),
    "浙A 4H393": ("2015-09-01", "2016-02-01"),
    "浙A 4H438": ("2015-09-01", "2016-02-01"),
}

def main():
    dry = '--dry-run' in sys.argv or '-n' in sys.argv
    if dry:
        print("=== DRY RUN (no changes) ===")

    db = pymysql.connect(**DB)
    cur = db.cursor()

    for plate, (factory, launch) in FIXES.items():
        cur.execute("SELECT id, factory_date, launch_date FROM vehicle WHERE plate_number = %s", (plate,))
        row = cur.fetchone()
        if not row:
            print(f"SKIP {plate}: not found in DB")
            continue
        vid, old_f, old_l = row
        if old_f == factory and old_l == launch:
            print(f"SKIP {plate}: already correct")
            continue

        if dry:
            print(f"WOULD UPDATE #{vid} {plate}: factory [{old_f} -> {factory}], launch [{old_l} -> {launch}]")
        else:
            cur.execute("UPDATE vehicle SET factory_date = %s, launch_date = %s WHERE id = %s", (factory, launch, vid))
            print(f"UPDATED #{vid} {plate}: factory [{old_f} -> {factory}], launch [{old_l} -> {launch}]")

    if not dry:
        db.commit()
        print(f"\nCommitted {cur.rowcount} changes. (only last UPDATE shows in rowcount)")
    else:
        print("\nDry run complete. Remove --dry-run to apply.")

    cur.close()
    db.close()

if __name__ == '__main__':
    main()
