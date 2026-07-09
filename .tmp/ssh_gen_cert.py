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
    print(f"\nCMD: {cmd}")
    try:
        stdin, stdout, stderr = client.exec_command(cmd, timeout=30)
        out = stdout.read().decode(errors='replace')
        err = stderr.read().decode(errors='replace')
        if out: print(out.strip())
        if err: print("ERR:", err.strip()[:500])
    except Exception as e:
        print(f"FAILED: {e}")

# Step 1: Generate cert directly
print("=== Generating SSL cert ===")
# Write config first
run_cmd("printf '[req]\\ndefault_bits=2048\\nprompt=no\\ndefault_md=sha256\\ndistinguished_name=dn\\nx509_extensions=v3_req\\n[dn]\\nCN=192.144.227.251\\nO=BusGallery\\nC=CN\\n[v3_req]\\nsubjectAltName=@alt_names\\n[alt_names]\\nIP.1=192.144.227.251\\n' > /tmp/openssl.cnf")
run_cmd("cat /tmp/openssl.cnf")
run_cmd("mkdir -p /root/code/bus_gallery/docker/nginx/ssl && openssl req -x509 -nodes -days 3650 -newkey rsa:2048 -keyout /root/code/bus_gallery/docker/nginx/ssl/privkey.pem -out /root/code/bus_gallery/docker/nginx/ssl/fullchain.pem -config /tmp/openssl.cnf -extensions v3_req")
run_cmd("ls -la /root/code/bus_gallery/docker/nginx/ssl/")
run_cmd("openssl x509 -in /root/code/bus_gallery/docker/nginx/ssl/fullchain.pem -text -noout | grep -E 'Subject:|IP Address:'")

# Step 2: Restart frontend
print("\n=== Restarting frontend ===")
run_cmd("cd /root/code/bus_gallery/docker && docker compose up -d --no-deps frontend")

time.sleep(8)

# Step 3: Check
print("\n=== Status ===")
run_cmd("docker ps --format 'table {{.Names}}\t{{.Status}}' | head -5")
run_cmd("docker logs --tail=5 bus-gallery-frontend")
run_cmd("ss -tlnp | grep -E ':(80|443) '")
run_cmd("curl -sk -o /dev/null -w '%{http_code}' https://localhost/")

client.close()
