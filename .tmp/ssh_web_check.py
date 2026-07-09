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
            print(out.strip()[:4000])
        if err:
            print("STDERR:", err.strip()[:1000])
    except Exception as e:
        print(f"FAILED: {e}")

# Check firewall
print("========== Firewall status ==========")
run_cmd("firewall-cmd --list-all 2>&1 || ufw status 2>&1 || iptables -L INPUT -n --line-numbers 2>&1 | head -30")

time.sleep(1)

# Check if port 80 is listening
print("\n========== Port 80 listening ==========")
run_cmd("ss -tlnp | grep ':80 ' 2>&1 || netstat -tlnp | grep ':80 ' 2>&1")

time.sleep(1)

# Check nginx/frontend container
print("\n========== Frontend container inspect ==========")
run_cmd("docker inspect bus-gallery-frontend --format '{{.State.Status}} {{.Config.Image}}' 2>&1")
run_cmd("docker logs --tail=20 bus-gallery-frontend 2>&1")

time.sleep(1)

# Test the front page from server
print("\n========== Test front page via curl ==========")
run_cmd("curl -s -o /dev/null -w 'HTTP_CODE: %{http_code}\\nSIZE: %{size_download}\\n' http://localhost:80/ 2>&1")

time.sleep(1)

# Check if it's a cloud server with security groups (Huawei Cloud / Alibaba Cloud)
print("\n========== Check cloud metadata ==========")
run_cmd("curl -s --connect-timeout 2 http://169.254.169.254/openstack/latest/meta_data.json 2>&1 | head -5 || echo 'not openstack'")
run_cmd("curl -s --connect-timeout 2 http://100.100.100.200/latest/meta-data/ 2>&1 | head -5 || echo 'not aliyun'")

time.sleep(1)

# Check nginx config in frontend
print("\n========== Frontend nginx config ==========")
run_cmd("docker exec bus-gallery-frontend cat /etc/nginx/conf.d/default.conf 2>&1")

time.sleep(1)

# Check frontend files
print("\n========== Frontend files ==========")
run_cmd("docker exec bus-gallery-frontend ls -la /usr/share/nginx/html/ 2>&1")

client.close()
