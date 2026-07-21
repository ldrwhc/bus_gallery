#!/usr/bin/env python3
"""Upload and run rescrape script on production server."""
import paramiko, sys

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('192.144.227.251', username='root', password='whc@njupt020704')

# Upload the script
sftp = ssh.open_sftp()
sftp.put(r'D:\code\bus-gallery\scripts\rescrape_buspedia_config.py', '/tmp/rescrape_buspedia.py')
sftp.close()
print('[OK] Script uploaded')

# Fix DB host to localhost for running on server
ssh.exec_command("sed -i 's/host.*192.144.227.251/host: \"127.0.0.1\"/' /tmp/rescrape_buspedia.py")

# Check pymysql is available
stdin, stdout, stderr = ssh.exec_command('pip3 install pymysql -q 2>&1 && echo pymysql_ok')
print(stdout.read().decode())

# Run the script (long-running, so use nohup)
stdin, stdout, stderr = ssh.exec_command(
    'nohup python3 /tmp/rescrape_buspedia.py > /tmp/rescrape_output.log 2>&1 &'
)
print('[OK] Rescrape started in background on server')
print('Monitor with: tail -f /tmp/rescrape_output.log')

ssh.close()
