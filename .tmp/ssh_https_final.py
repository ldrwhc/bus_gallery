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
        stdin, stdout, stderr = client.exec_command(cmd, timeout=30)
        out = stdout.read().decode(errors='replace')
        err = stderr.read().decode(errors='replace')
        if out:
            print(out.strip()[:4000])
        if err:
            print("STDERR:", err.strip()[:2000])
    except Exception as e:
        print(f"FAILED: {e}")

# Step 1: Generate SSL cert using config file (for old OpenSSL)
print("========== STEP 1: Generate SSL cert with config file ==========")

ssl_conf = r'''[req]
default_bits = 2048
prompt = no
default_md = sha256
distinguished_name = dn
x509_extensions = v3_req

[dn]
CN = 192.144.227.251
O = BusGallery
C = CN

[v3_req]
subjectAltName = @alt_names

[alt_names]
IP.1 = 192.144.227.251
'''

run_cmd("cat > /tmp/openssl.cnf << 'EOF'\n" + ssl_conf + "\nEOF\n")
run_cmd("mkdir -p /root/code/bus_gallery/docker/nginx/ssl && openssl req -x509 -nodes -days 3650 -newkey rsa:2048 -keyout /root/code/bus_gallery/docker/nginx/ssl/privkey.pem -out /root/code/bus_gallery/docker/nginx/ssl/fullchain.pem -config /tmp/openssl.cnf -extensions v3_req 2>&1")

time.sleep(2)

# Verify
print("\n========== Verify cert ==========")
run_cmd("openssl x509 -in /root/code/bus_gallery/docker/nginx/ssl/fullchain.pem -text -noout 2>&1 | grep -E 'Subject:|DNS:|IP Address:' | head -10")

time.sleep(1)

# Step 2: Verify nginx HTTPS config exists
print("\n========== Step 2: Verify HTTPS config ==========")
run_cmd("head -5 /root/code/bus_gallery/docker/nginx/default-https.conf 2>&1")
run_cmd("wc -l /root/code/bus_gallery/docker/nginx/default-https.conf 2>&1")

time.sleep(1)

# Step 3: Update docker-compose.yml frontend section
print("\n========== Step 3: Update docker-compose ==========")

# Use Python to modify the yaml
update_script = '''
import re

with open("/root/code/bus_gallery/docker/docker-compose.yml", "r") as f:
    content = f.read()

# Replace frontend ports: "80:80" -> add 443
content = content.replace(
    '    ports:\\n      - "80:80"',
    '    ports:\\n      - "80:80"\\n      - "443:443"'
)

# Add volume mounts for SSL and nginx config to frontend service
# Find the frontend section and add volumes
old_frontend = '''  frontend:
    build:
      context: ../
      dockerfile: frontend/Dockerfile
      args:
        - MIRROR=aliyun
    container_name: bus-gallery-frontend
    restart: always
    depends_on:
      - gateway
    ports:
      - "80:80"
      - "443:443"'''

new_frontend = '''  frontend:
    build:
      context: ../
      dockerfile: frontend/Dockerfile
      args:
        - MIRROR=aliyun
    container_name: bus-gallery-frontend
    restart: always
    depends_on:
      - gateway
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/ssl:/etc/nginx/ssl:ro
      - ./nginx/default-https.conf:/etc/nginx/conf.d/default.conf:ro'''

content = content.replace(old_frontend, new_frontend)

with open("/root/code/bus_gallery/docker/docker-compose.yml", "w") as f:
    f.write(content)

print("DONE")
'''

run_cmd("cat > /tmp/update_compose.py << 'PYEOF'\n" + update_script + "\nPYEOF\n")
run_cmd("python3 /tmp/update_compose.py 2>&1")

time.sleep(1)

# Verify changes
print("\n========== Verify docker-compose changes ==========")
run_cmd("grep -A 18 '^  frontend:' /root/code/bus_gallery/docker/docker-compose.yml 2>&1")

time.sleep(1)

# Step 4: Recreate frontend container
print("\n========== Step 4: Recreate frontend container ==========")
run_cmd("cd /root/code/bus_gallery/docker && docker compose up -d --no-deps frontend 2>&1")

time.sleep(10)

# Step 5: Check frontend status
print("\n========== Step 5: Check status ==========")
run_cmd("docker ps --format 'table {{.Names}}\\t{{.Status}}\\t{{.Ports}}' 2>&1")

time.sleep(2)

# Step 6: Check if 443 is listening
print("\n========== Step 6: Verify port 443 ==========")
run_cmd("ss -tlnp | grep -E ':(80|443) ' 2>&1")

time.sleep(1)

# Step 7: Test HTTPS
print("\n========== Step 7: Test HTTPS ==========")
run_cmd("curl -sk -o /dev/null -w 'HTTP_CODE: %{http_code}' https://localhost:443/ 2>&1")
run_cmd("curl -sk -o /dev/null -w 'HTTP_CODE: %{http_code}' https://localhost:443/api/regions 2>&1")
run_cmd("curl -sk http://localhost:80/ -o /dev/null -w 'HTTP->HTTPS redirect: %{http_code} -> %{redirect_url}' 2>&1")

time.sleep(1)

# Step 8: Check frontend logs for any SSL errors
print("\n========== Step 8: Frontend logs ==========")
run_cmd("docker logs --tail=10 bus-gallery-frontend 2>&1")

client.close()
