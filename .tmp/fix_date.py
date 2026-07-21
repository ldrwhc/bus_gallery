import paramiko
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=10)

def sql(q):
    cmd = f"docker exec -i bus-gallery-mysql mysql -uroot -p123456 bus_gallery --default-character-set=utf8mb4 -e \"{q}\""
    stdin, stdout, stderr = ssh.exec_command(cmd)
    out = stdout.read().decode()
    err = stderr.read().decode()
    if err: print('ERR:', err)
    return out

# Try various search patterns
out = sql("SELECT id, plate_number, factory_date, launch_date FROM vehicle WHERE plate_number LIKE '%80797%' LIMIT 5")
print('Search 80797:', out if out.strip() else 'NOT FOUND')

out = sql("SELECT id, plate_number, factory_date, launch_date FROM vehicle WHERE plate_number LIKE '%B%80797%' LIMIT 5")
print('Search B80797:', out if out.strip() else 'NOT FOUND')

out = sql("SELECT id, plate_number FROM vehicle WHERE plate_number LIKE '%80797' LIMIT 10")
print('Ends with 80797:', out if out.strip() else 'NOT FOUND')

ssh.close()
