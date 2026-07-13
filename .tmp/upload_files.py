import paramiko
import os

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=30, allow_agent=False, look_for_keys=False)

sftp = client.open_sftp()

BASE = r'D:\code\bus-gallery'
REMOTE = '/root/code/bus_gallery'

files = [
    'backend/src/main/java/com/busgallery/busgallery/controller/VehicleController.java',
    'frontend/src/views/CompanyCatalog.vue',
    'frontend/src/views/VehicleDetail.vue',
    'frontend/src/views/RouteDetail.vue',
    'frontend/src/views/Home.vue',
    'frontend/src/views/Account.vue',
    'frontend/src/views/UserProfile.vue',
]

for f in files:
    local = os.path.join(BASE, f)
    remote = REMOTE + '/' + f
    print(f'Upload: {f}')
    try:
        sftp.put(local, remote)
        print(f'  OK')
    except Exception as e:
        print(f'  FAILED: {e}')

sftp.close()
client.close()
print('All files uploaded')
