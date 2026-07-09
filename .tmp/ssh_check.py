import paramiko
import sys

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

try:
    client.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=15)
    print('=== SSH连接成功 ===')

    # First find where docker-compose is
    commands = [
        ("Docker容器状态", "docker ps -a --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' 2>&1"),
        ("Gateway日志(最近50行)", "docker logs --tail=50 bus-gallery-gateway 2>&1 || docker logs --tail=50 gateway 2>&1"),
        ("Backend日志(最近50行)", "docker logs --tail=50 bus-gallery-backend 2>&1 || docker logs --tail=50 backend 2>&1"),
        ("Bridge日志(最近50行)", "docker logs --tail=50 bus-gallery-bridge 2>&1 || docker logs --tail=50 bridge 2>&1"),
        ("查找docker-compose目录", "find /root -name 'docker-compose.yml' -o -name 'docker-compose.yaml' 2>/dev/null | head -5"),
        ("系统资源", "free -h && echo '---' && df -h /"),
    ]

    for title, cmd in commands:
        print(f'\n========== {title} ==========')
        stdin, stdout, stderr = client.exec_command(cmd, timeout=15)
        out = stdout.read().decode()
        err = stderr.read().decode()
        if out:
            print(out.strip())
        if err:
            print('STDERR:', err.strip())

except Exception as e:
    print(f'连接失败: {e}')
    sys.exit(1)
finally:
    client.close()
