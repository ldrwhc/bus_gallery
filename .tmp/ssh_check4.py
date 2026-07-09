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

# Check MySQL container
run_cmd("docker inspect bus-gallery-mysql --format '{{.State.Status}} {{.State.Health.Status}}' 2>&1")
run_cmd("docker exec bus-gallery-mysql mysqladmin ping -h localhost -u root -proot123 2>&1")
run_cmd("docker exec bus-gallery-mysql mysql -u root -proot123 -e 'SHOW DATABASES;' 2>&1")

# Check docker network
run_cmd("docker network ls 2>&1")
run_cmd("docker network inspect bus-gallery_default 2>&1 || docker network inspect $(docker inspect bus-gallery-mysql --format '{{range .NetworkSettings.Networks}}{{.NetworkID}}{{end}}') 2>&1")

# Check backend app config from inside container
run_cmd("docker exec bus-gallery-backend cat /app/application.yml 2>&1 || docker exec bus-gallery-backend ls /app/ 2>&1")

# Check if backend container can reach mysql
run_cmd("docker exec bus-gallery-backend sh -c 'echo > /dev/tcp/bus-gallery-mysql/3306 && echo OK || echo FAIL' 2>&1")

time.sleep(2)

# Check the MySQL host configured in backend
run_cmd("docker exec bus-gallery-backend cat /app/config/application.yml 2>&1 || docker exec bus-gallery-backend find /app -name '*.yml' -o -name '*.yaml' -o -name '*.properties' 2>&1")

# Try gateway logs again
run_cmd("docker logs --tail=30 bus-gallery-gateway 2>&1")

# Check docker-compose env variables
run_cmd("cat /root/docker-compose.yml 2>&1 | head -100")

client.close()
