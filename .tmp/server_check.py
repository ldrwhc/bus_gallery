#!/usr/bin/env python3
"""SSH into server and check Docker + service status"""
import paramiko, time, sys

HOST = '192.144.227.251'
USER = 'root'
PASS = 'whc@njupt020704'

def run(client, cmd, timeout=30):
    """Run command via SSH and return output"""
    try:
        transport = client.get_transport()
        transport.set_keepalive(5)
        chan = transport.open_session(timeout=timeout)
        chan.settimeout(timeout)
        chan.exec_command(cmd)

        out = b''
        err = b''
        start = time.time()
        while not chan.exit_status_ready():
            if time.time() - start > timeout:
                break
            if chan.recv_stderr_ready():
                err += chan.recv_stderr(4096)
            if chan.recv_ready():
                out += chan.recv(4096)
            time.sleep(0.3)

        # Drain remaining data
        while chan.recv_ready():
            out += chan.recv(4096)
        while chan.recv_stderr_ready():
            err += chan.recv_stderr(4096)

        exit_code = chan.recv_exit_status()
        chan.close()
        return out.decode(), err.decode(), exit_code
    except Exception as e:
        return '', f'{type(e).__name__}: {e}', -1

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

try:
    print('=' * 60)
    print(f'Connecting to {HOST}...')
    client.connect(HOST, username=USER, password=PASS, timeout=20,
                   banner_timeout=20, auth_timeout=20,
                   allow_agent=False, look_for_keys=False)
    print('[OK] Connected!\n')

    # 1. System overview
    cmds = [
        ('System Info', 'uname -a'),
        ('Memory', 'free -h'),
        ('Disk', 'df -h /'),
        ('Docker Version', 'docker --version'),
        ('Docker Compose', 'docker compose version 2>/dev/null || docker-compose --version 2>/dev/null || echo "no compose"'),
    ]
    for title, cmd in cmds:
        print(f'--- {title} ---')
        out, err, code = run(client, cmd)
        if out.strip():
            print(out.strip())
        if err.strip():
            print(f'[ERR] {err.strip()}')

    # 2. Docker containers
    print('\n--- Docker Containers ---')
    out, err, code = run(client, 'docker ps -a --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"')
    print(out if out else '[No output]')
    if err:
        print(f'[ERR] {err}')

    # 3. Docker compose processes
    print('\n--- docker-compose.yml content ---')
    out, err, code = run(client, 'cat /root/bus-gallery/docker/docker-compose.yml 2>/dev/null || find / -name "docker-compose.yml" -maxdepth 4 2>/dev/null')
    print(out[:2000] if out else '[No docker-compose found in /root/bus-gallery]')

    # 4. Check project directory
    print('\n--- Project Directory ---')
    out, err, code = run(client, 'ls -la /root/bus-gallery/ 2>/dev/null || ls -la /opt/bus-gallery/ 2>/dev/null || ls -la /data/bus-gallery/ 2>/dev/null || echo "checking other locations..." && find / -name "docker-compose.yml" -maxdepth 5 2>/dev/null')
    print(out if out else '[No output]')

    # 5. Check nginx config
    print('\n--- Nginx ---')
    out, err, code = run(client, 'docker ps --filter name=nginx --format "{{.Names}} {{.Status}}" 2>/dev/null; docker ps --filter name=frontend --format "{{.Names}} {{.Status}}" 2>/dev/null')
    print(out if out else '[No nginx/frontend container found]')

    # 6. Check docker logs for recent errors
    print('\n--- Recent Docker Logs ---')
    out, err, code = run(client, 'docker ps -q | head -5 | xargs -I{} docker logs --tail 20 {} 2>&1 || echo "no containers"')
    print(out[:3000] if out else '[No logs]')

except Exception as e:
    print(f'Fatal: {type(e).__name__}: {e}')
    import traceback
    traceback.print_exc()
finally:
    client.close()
    print('\n' + '=' * 60)
    print('Done')
