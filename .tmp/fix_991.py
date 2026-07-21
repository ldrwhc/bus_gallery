import paramiko
ssh = paramiko.SSHClient()
ssh.set_missing_host_key_policy(paramiko.AutoAddPolicy())
ssh.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=10)
cmd = 'docker exec -i bus-gallery-mysql mysql -uroot -p123456 bus_gallery -e "UPDATE vehicle SET launch_date = NULL WHERE id = 991"'
ssh.exec_command(cmd)
stdin, stdout, stderr = ssh.exec_command('docker exec -i bus-gallery-mysql mysql -uroot -p123456 bus_gallery --default-character-set=utf8mb4 -e "SELECT id, plate_number, factory_date, launch_date FROM vehicle WHERE id = 991"')
print(stdout.read().decode())
ssh.close()
