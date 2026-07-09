import paramiko
import time

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

client.connect(
    '192.144.227.251', username='root', password='whc@njupt020704',
    timeout=30, banner_timeout=30, auth_timeout=30,
    allow_agent=False, look_for_keys=False,
)

def run_cmd(cmd):
    print(f"\n{'='*60}")
    print(f"CMD: {cmd}")
    print('='*60)
    try:
        stdin, stdout, stderr = client.exec_command(cmd, timeout=20)
        out = stdout.read().decode(errors='replace')
        err = stderr.read().decode(errors='replace')
        if out:
            print(out.strip()[:5000])
        if err:
            print("STDERR:", err.strip()[:1000])
    except Exception as e:
        print(f"FAILED: {e}")

# Read the server docker-compose.yml
run_cmd("cat /root/code/bus_gallery/docker/docker-compose.yml 2>&1")

time.sleep(2)

# Also check bridge env vars
run_cmd("docker inspect bus-gallery-bridge --format '{{range $k,$v := .Config.Env}}{{$v}}{{\"\\n\"}}{{end}}' 2>&1")

client.close()
