#!/usr/bin/env python3
"""Upload rescrape script and run it directly on the server host."""
import paramiko, time

ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('192.144.227.251', username='root', password='whc@njupt020704')

# Clean up
ssh.exec_command('rm -f /tmp/rescrape_buspedia.py /tmp/rescrape_output.log')

# Upload
sftp = ssh.open_sftp()
sftp.put(r'D:\code\bus-gallery\scripts\rescrape_buspedia_config.py', '/tmp/rescrape_buspedia.py')
sftp.close()
print('[OK] Script uploaded')

# Run directly on server host (MySQL is at 127.0.0.1:13306 via docker port mapping)
stdin, stdout, stderr = ssh.exec_command(
    'nohup python3 /tmp/rescrape_buspedia.py > /tmp/rescrape_output.log 2>&1 & echo started'
)
print(stdout.read().decode())

# Wait and check progress
time.sleep(20)
stdin, stdout, stderr = ssh.exec_command('tail -30 /tmp/rescrape_output.log')
print('--- Progress ---')
print(stdout.read().decode())

ssh.close()
