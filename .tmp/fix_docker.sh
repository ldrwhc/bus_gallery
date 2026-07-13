#!/bin/bash
# Fix Docker daemon.json and restart

cat > /etc/docker/daemon.json << 'EOF'
{
  "registry-mirrors": [
    "https://docker.mirrors.ustc.edu.cn",
    "https://hub-mirror.c.163.com"
  ]
}
EOF

echo "daemon.json fixed:"
cat /etc/docker/daemon.json

systemctl restart docker
sleep 10

cd /root/code/bus_gallery/docker
docker compose up -d
sleep 10

docker ps --format "table {{.Names}}\t{{.Status}}"
