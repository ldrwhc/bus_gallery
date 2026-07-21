import paramiko

HOST = '192.144.227.251'
USER = 'root'
PWD = 'whc@njupt020704'

# Fix: clear bogus launch_date (2026 scrape artifact) where factory_date is real
# Also: clear bogus factory_date (2026) where launch_date is real (pre-2024)
sql = """
UPDATE vehicle SET launch_date = NULL
WHERE launch_date >= '2026-01-01' AND factory_date < '2025-01-01';

UPDATE vehicle SET factory_date = NULL
WHERE factory_date >= '2026-01-01' AND launch_date < '2025-01-01';
"""

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect(HOST, username=USER, password=PWD, timeout=10)

for stmt in sql.strip().split(';'):
    stmt = stmt.strip()
    if not stmt: continue
    cmd = f'docker exec bus-gallery-mysql mysql -uroot -p123456 bus_gallery -e "{stmt}"'
    stdin, stdout, stderr = ssh.exec_command(cmd)
    out = stdout.read().decode().strip()
    err = stderr.read().decode().strip()
    print(f'SQL: {stmt[:60]}...')
    print(f'  Result: {out}')
    if err and 'Warning' not in err: print(f'  Error: {err}')

# Verify
cmd2 = 'docker exec bus-gallery-mysql mysql -uroot -p123456 bus_gallery -e "SELECT COUNT(*) AS cnt FROM vehicle WHERE (launch_date >= \'2026-01-01\' AND factory_date < \'2025-01-01\') OR (factory_date >= \'2026-01-01\' AND launch_date < \'2025-01-01\')"'
stdin, stdout, stderr = ssh.exec_command(cmd2)
print(f'\nRemaining bogus: {stdout.read().decode().strip()}')

ssh.close()
