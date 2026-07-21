import paramiko
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=10)

sftp = ssh.open_sftp()

# Upload cert files
local_key = r'D:\code\bus-gallery\bgpic.site_nginx\bgpic.site.key'
local_pem = r'D:\code\bus-gallery\bgpic.site_nginx\bgpic.site_bundle.pem'
remote_dir = '/root/code/bus_gallery/docker/nginx/ssl'

sftp.put(local_key, remote_dir + '/privkey.pem')
sftp.put(local_pem, remote_dir + '/fullchain.pem')
sftp.close()
print('Cert uploaded')

# Restart frontend container to pick up new cert
stdin, stdout, stderr = ssh.exec_command('docker restart bus-gallery-frontend')
stdout.channel.recv_exit_status()
print('Frontend restarted')

# Verify
stdin2, stdout2, stderr2 = ssh.exec_command('docker exec bus-gallery-frontend openssl x509 -in /etc/nginx/ssl/fullchain.pem -noout -subject -dates 2>&1')
print('New cert:', stdout2.read().decode())

ssh.close()
print('Done! Try https://bgpic.site')
