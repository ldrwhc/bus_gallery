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
            print("STDERR:", err.strip()[:1000])
    except Exception as e:
        print(f"FAILED: {e}")

# Step 1: Create SSL directory and generate self-signed certificate
print("========== STEP 1: Generate self-signed SSL cert ==========")
run_cmd("mkdir -p /root/code/bus_gallery/docker/nginx/ssl && openssl req -x509 -nodes -days 3650 -newkey rsa:2048 -keyout /root/code/bus_gallery/docker/nginx/ssl/privkey.pem -out /root/code/bus_gallery/docker/nginx/ssl/fullchain.pem -subj '/CN=192.144.227.251/O=BusGallery/C=CN' -addext 'subjectAltName=IP:192.144.227.251' 2>&1")

time.sleep(2)

# Step 2: Verify cert generated
print("\n========== STEP 2: Verify cert ==========")
run_cmd("openssl x509 -in /root/code/bus_gallery/docker/nginx/ssl/fullchain.pem -text -noout 2>&1 | grep -A2 'Subject:\|X509v3' | head -20")

time.sleep(1)

# Step 3: Create updated nginx config with HTTPS
print("\n========== STEP 3: Create HTTPS nginx config ==========")
nginx_config = r'''limit_req_zone $binary_remote_addr zone=auth_ip:10m rate=12r/s;
limit_req_zone $server_name zone=auth_global:10m rate=120r/s;
limit_req_zone $binary_remote_addr zone=upload_ip:10m rate=6r/s;
limit_req_zone $server_name zone=upload_global:10m rate=40r/s;
limit_req_zone $binary_remote_addr zone=api_ip:10m rate=25r/s;
limit_req_zone $server_name zone=api_global:10m rate=180r/s;
limit_req_zone $binary_remote_addr zone=image_access_ip:10m rate=80r/s;
limit_req_zone $server_name zone=image_access_global:10m rate=800r/s;

# HTTP -> HTTPS redirect
server {
    listen 80;
    server_name _;
    return 301 https://$host$request_uri;
}

# HTTPS server
server {
    listen 443 ssl;
    http2 on;
    server_name _;
    client_max_body_size 50M;
    client_body_timeout 300s;

    ssl_certificate     /etc/nginx/ssl/fullchain.pem;
    ssl_certificate_key /etc/nginx/ssl/privkey.pem;
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-CHACHA20-POLY1305;
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    root /usr/share/nginx/html;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/upload {
        limit_req zone=upload_ip burst=12 nodelay;
        limit_req zone=upload_global burst=80 nodelay;
        limit_req_status 429;
        proxy_request_buffering off;
        proxy_pass http://gateway:8094;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;
    }

    location /api/auth/ {
        limit_req zone=auth_ip burst=30 nodelay;
        limit_req zone=auth_global burst=220 nodelay;
        limit_req_status 429;
        proxy_pass http://gateway:8094;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;
    }

    location /api/images/access/ {
        limit_req zone=image_access_ip burst=240;
        limit_req zone=image_access_global burst=1200;
        limit_req_status 429;
        proxy_pass http://gateway:8094;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;
    }

    location /api/v1/group/ {
        limit_req zone=api_ip burst=40 nodelay;
        limit_req zone=api_global burst=240 nodelay;
        limit_req_status 429;
        proxy_pass http://gateway:8094;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;
    }

    location /api/bridge/ {
        limit_req zone=api_ip burst=40 nodelay;
        limit_req zone=api_global burst=240 nodelay;
        limit_req_status 429;
        proxy_pass http://gateway:8094;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;
    }

    location /api/ {
        limit_req zone=api_ip burst=40 nodelay;
        limit_req zone=api_global burst=240 nodelay;
        limit_req_status 429;
        proxy_pass http://gateway:8094;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
        proxy_set_header Host              $host;
        proxy_set_header X-Real-IP         $remote_addr;
        proxy_set_header X-Forwarded-For   $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_read_timeout 300s;
        proxy_send_timeout 300s;
    }
}
'''

# Write the new nginx config
run_cmd(f"cat > /root/code/bus_gallery/docker/nginx/default-https.conf << 'NGINXEOF'\n{nginx_config}\nNGINXEOF\n")

time.sleep(1)

# Step 4: Backup current docker-compose and update frontend section
print("\n========== STEP 4: Update docker-compose frontend section ==========")

# Read current docker-compose to check frontend section
run_cmd("grep -A 15 '^  frontend:' /root/code/bus_gallery/docker/docker-compose.yml 2>&1")

time.sleep(1)

print("\nNow updating frontend service in docker-compose...")
print("(Manual edit via sed/Python needed)")

client.close()
