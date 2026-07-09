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

# Step 1: Generate SSL cert using config file
print("========== STEP 1: Generate SSL cert ==========")
# Write openssl config
run_cmd("cat > /tmp/openssl.cnf << 'EOF'\n[req]\ndefault_bits = 2048\nprompt = no\ndefault_md = sha256\ndistinguished_name = dn\nx509_extensions = v3_req\n\n[dn]\nCN = 192.144.227.251\nO = BusGallery\nC = CN\n\n[v3_req]\nsubjectAltName = @alt_names\n\n[alt_names]\nIP.1 = 192.144.227.251\nEOF\n")
run_cmd("mkdir -p /root/code/bus_gallery/docker/nginx/ssl && openssl req -x509 -nodes -days 3650 -newkey rsa:2048 -keyout /root/code/bus_gallery/docker/nginx/ssl/privkey.pem -out /root/code/bus_gallery/docker/nginx/ssl/fullchain.pem -config /tmp/openssl.cnf -extensions v3_req 2>&1")

time.sleep(2)

# Verify cert
print("\n========== Verify cert ==========")
run_cmd("openssl x509 -in /root/code/bus_gallery/docker/nginx/ssl/fullchain.pem -text -noout 2>&1 | grep -E 'Subject:|DNS:|IP Address:' | head -10")

time.sleep(1)

# Step 2: Write nginx HTTPS config to server
print("\n========== STEP 2: Write nginx HTTPS config ==========")
run_cmd("cat > /root/code/bus_gallery/docker/nginx/default-https.conf << 'NGINXEOF'\nlimit_req_zone $binary_remote_addr zone=auth_ip:10m rate=12r/s;\nlimit_req_zone $server_name zone=auth_global:10m rate=120r/s;\nlimit_req_zone $binary_remote_addr zone=upload_ip:10m rate=6r/s;\nlimit_req_zone $server_name zone=upload_global:10m rate=40r/s;\nlimit_req_zone $binary_remote_addr zone=api_ip:10m rate=25r/s;\nlimit_req_zone $server_name zone=api_global:10m rate=180r/s;\nlimit_req_zone $binary_remote_addr zone=image_access_ip:10m rate=80r/s;\nlimit_req_zone $server_name zone=image_access_global:10m rate=800r/s;\n\nserver {\n    listen 80;\n    server_name _;\n    return 301 https://$host$request_uri;\n}\n\nserver {\n    listen 443 ssl;\n    http2 on;\n    server_name _;\n    client_max_body_size 50M;\n    client_body_timeout 300s;\n\n    ssl_certificate     /etc/nginx/ssl/fullchain.pem;\n    ssl_certificate_key /etc/nginx/ssl/privkey.pem;\n    ssl_protocols TLSv1.2 TLSv1.3;\n    ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-CHACHA20-POLY1305;\n    ssl_prefer_server_ciphers on;\n    ssl_session_cache shared:SSL:10m;\n    ssl_session_timeout 10m;\n\n    root /usr/share/nginx/html;\n    index index.html;\n\n    location / {\n        try_files $uri $uri/ /index.html;\n    }\n\n    location /api/upload {\n        limit_req zone=upload_ip burst=12 nodelay;\n        limit_req zone=upload_global burst=80 nodelay;\n        limit_req_status 429;\n        proxy_request_buffering off;\n        proxy_pass http://gateway:8094;\n        proxy_http_version 1.1;\n        proxy_set_header Connection \"\";\n        proxy_set_header Host              $host;\n        proxy_set_header X-Real-IP         $remote_addr;\n        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;\n        proxy_set_header X-Forwarded-Proto $scheme;\n        proxy_read_timeout 300s;\n        proxy_send_timeout 300s;\n    }\n\n    location /api/auth/ {\n        limit_req zone=auth_ip burst=30 nodelay;\n        limit_req zone=auth_global burst=220 nodelay;\n        limit_req_status 429;\n        proxy_pass http://gateway:8094;\n        proxy_http_version 1.1;\n        proxy_set_header Connection \"\";\n        proxy_set_header Host              $host;\n        proxy_set_header X-Real-IP         $remote_addr;\n        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;\n        proxy_set_header X-Forwarded-Proto $scheme;\n        proxy_read_timeout 300s;\n        proxy_send_timeout 300s;\n    }\n\n    location /api/images/access/ {\n        limit_req zone=image_access_ip burst=240;\n        limit_req zone=image_access_global burst=1200;\n        limit_req_status 429;\n        proxy_pass http://gateway:8094;\n        proxy_http_version 1.1;\n        proxy_set_header Connection \"\";\n        proxy_set_header Host              $host;\n        proxy_set_header X-Real-IP         $remote_addr;\n        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;\n        proxy_set_header X-Forwarded-Proto $scheme;\n        proxy_read_timeout 300s;\n        proxy_send_timeout 300s;\n    }\n\n    location /api/v1/group/ {\n        limit_req zone=api_ip burst=40 nodelay;\n        limit_req zone=api_global burst=240 nodelay;\n        limit_req_status 429;\n        proxy_pass http://gateway:8094;\n        proxy_http_version 1.1;\n        proxy_set_header Connection \"\";\n        proxy_set_header Host              $host;\n        proxy_set_header X-Real-IP         $remote_addr;\n        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;\n        proxy_set_header X-Forwarded-Proto $scheme;\n        proxy_read_timeout 300s;\n        proxy_send_timeout 300s;\n    }\n\n    location /api/bridge/ {\n        limit_req zone=api_ip burst=40 nodelay;\n        limit_req zone=api_global burst=240 nodelay;\n        limit_req_status 429;\n        proxy_pass http://gateway:8094;\n        proxy_http_version 1.1;\n        proxy_set_header Connection \"\";\n        proxy_set_header Host              $host;\n        proxy_set_header X-Real-IP         $remote_addr;\n        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;\n        proxy_set_header X-Forwarded-Proto $scheme;\n        proxy_read_timeout 300s;\n        proxy_send_timeout 300s;\n    }\n\n    location /api/ {\n        limit_req zone=api_ip burst=40 nodelay;\n        limit_req zone=api_global burst=240 nodelay;\n        limit_req_status 429;\n        proxy_pass http://gateway:8094;\n        proxy_http_version 1.1;\n        proxy_set_header Connection \"\";\n        proxy_set_header Host              $host;\n        proxy_set_header X-Real-IP         $remote_addr;\n        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;\n        proxy_set_header X-Forwarded-Proto $scheme;\n        proxy_read_timeout 300s;\n        proxy_send_timeout 300s;\n    }\n}\nNGINXEOF\n")

time.sleep(1)

# Verify config written
run_cmd("wc -l /root/code/bus_gallery/docker/nginx/default-https.conf 2>&1")

time.sleep(1)

# Step 3: Update docker-compose.yml using sed
print("\n========== STEP 3: Update docker-compose ==========")

# 3a: Check current frontend section
run_cmd("grep -n -A 15 '^  frontend:' /root/code/bus_gallery/docker/docker-compose.yml 2>&1")

time.sleep(1)

# 3b: Use sed to add "443:443" port and volumes
# Add 443 port after "80:80"
run_cmd("cd /root/code/bus_gallery/docker && sed -i 's/      - \"80:80\"/      - \"80:80\"\\n      - \"443:443\"/' docker-compose.yml 2>&1")

time.sleep(1)

# 3c: Add volumes and override nginx config after the ports section for frontend
# This is trickier with sed. Let's use Python on the server.
fix_script = '''
with open("/root/code/bus_gallery/docker/docker-compose.yml", "r") as f:
    lines = f.readlines()

out = []
for i, line in enumerate(lines):
    out.append(line)
    # After the 443 port line in frontend, add volumes
    if '      - "443:443"' in line:
        # Check if we're in the frontend section (next few lines should NOT be volumes)
        # Just add it once
        if i+1 < len(lines) and 'volumes:' not in lines[i+1]:
            out.append('    volumes:\\n')
            out.append('      - ./nginx/ssl:/etc/nginx/ssl:ro\\n')
            out.append('      - ./nginx/default-https.conf:/etc/nginx/conf.d/default.conf:ro\\n')

with open("/root/code/bus_gallery/docker/docker-compose.yml", "w") as f:
    f.writelines(out)

print("UPDATED")
'''

run_cmd(f"cat > /tmp/fix_frontend.py << 'PYEOF'\n{fix_script}\nPYEOF\n")
run_cmd("python3 /tmp/fix_frontend.py 2>&1")

time.sleep(1)

# Verify
print("\n========== Verify docker-compose ==========")
run_cmd("grep -A 20 '^  frontend:' /root/code/bus_gallery/docker/docker-compose.yml 2>&1")

time.sleep(1)

# Step 4: Recreate frontend container
print("\n========== STEP 4: Recreate frontend ==========")
run_cmd("cd /root/code/bus_gallery/docker && docker compose up -d --no-deps frontend 2>&1")

time.sleep(10)

# Step 5: Verify ports
print("\n========== STEP 5: Verify ports ==========")
run_cmd("ss -tlnp | grep -E ':(80|443) ' 2>&1")

time.sleep(1)

# Step 6: Test HTTPS
print("\n========== STEP 6: Test HTTPS ==========")
run_cmd("curl -sk -o /dev/null -w 'HTTPS front page: %{http_code}\\n' https://localhost/ 2>&1")
run_cmd("curl -sk -o /dev/null -w 'HTTPS API regions: %{http_code}\\n' https://localhost/api/regions 2>&1")
run_cmd("curl -sI http://localhost/ 2>&1 | head -5")

time.sleep(1)

# Step 7: Check frontend logs
print("\n========== STEP 7: Frontend logs ==========")
run_cmd("docker logs --tail=5 bus-gallery-frontend 2>&1")

time.sleep(1)

# Step 8: Final status
print("\n========== STEP 8: Container status ==========")
run_cmd("docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' 2>&1")

client.close()
