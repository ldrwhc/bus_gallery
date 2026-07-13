#!/bin/bash
# Switch back to self-signed certificate

SSL_DIR=/root/code/bus_gallery/docker/nginx/ssl
CONF=/root/code/bus_gallery/docker/nginx/default-https.conf

# Backup current TrustAsia cert
cp $SSL_DIR/fullchain.pem $SSL_DIR/fullchain.pem.trustasia.bak
cp $SSL_DIR/privkey.pem $SSL_DIR/privkey.pem.trustasia.bak
echo "Backed up TrustAsia cert"

# Generate self-signed cert
openssl req -x509 -nodes -days 365 \
  -newkey rsa:2048 \
  -keyout $SSL_DIR/privkey.pem \
  -out $SSL_DIR/fullchain.pem \
  -subj "/CN=192.144.227.251/O=BusGallery/C=CN"
echo "Self-signed cert generated"

# Restore cipher config to original (RSA compatible)
sed -i 's/ssl_ciphers .*;/ssl_ciphers ECDHE-RSA-AES128-GCM-SHA256:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-CHACHA20-POLY1305;/' $CONF
sed -i 's/ssl_protocols .*;/ssl_protocols TLSv1.2 TLSv1.3;/' $CONF
# Re-enable http2
sed -i 's/#http2 on; # mobile compat/http2 on;/' $CONF

# Verify config
echo "=== New config ==="
grep -E "ssl_ciphers|ssl_protocols|http2" $CONF | head -6

# Reload
docker exec bus-gallery-frontend nginx -t && docker exec bus-gallery-frontend nginx -s reload
echo "Nginx reloaded"

# Test
sleep 1
curl -sk --connect-timeout 3 -o /dev/null -w "Local https: %{http_code}\n" https://127.0.0.1/ -H "Host: bgpic.site"
