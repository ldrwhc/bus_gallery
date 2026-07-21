import pymysql, json
db = pymysql.connect(host='127.0.0.1', port=13306, user='root', password='123456', database='bus_gallery', charset='utf8mb4')
cur = db.cursor()
cur.execute("SELECT v.id, v.plate_number, v.custom_number, m.name AS model, c.name AS company, v.factory_date, v.launch_date FROM vehicle v JOIN model m ON v.model_id = m.id LEFT JOIN company c ON v.company_id = c.id WHERE m.name IN ('CK6120LGEV2', 'CK6121LGEV') ORDER BY v.id")
rows = cur.fetchall()
cols = [d[0] for d in cur.description]
result = [dict(zip(cols, row)) for row in rows]
print(json.dumps(result, ensure_ascii=False, indent=2, default=str))
db.close()
