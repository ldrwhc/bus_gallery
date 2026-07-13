#!/bin/bash
# Make nginx SSL config maximally compatible

CONF=/root/code/bus_gallery/docker/nginx/default-https.conf

# Replace ssl_protocols line: allow older TLS for old phones
sed -i 's/ssl_protocols .*;/ssl_protocols TLSv1 TLSv1.1 TLSv1.2 TLSv1.3;/g' $CONF

# Replace ssl_ciphers: use nginx intermediate compatibility list
sed -i 's/ssl_ciphers .*;/ssl_ciphers ECDHE-ECDSA-CHACHA20-POLY1305:ECDHE-RSA-CHACHA20-POLY1305:ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:DHE-RSA-AES128-GCM-SHA256:DHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-AES128-SHA256:ECDHE-RSA-AES128-SHA256:ECDHE-ECDSA-AES128-SHA:ECDHE-RSA-AES256-SHA384:ECDHE-RSA-AES128-SHA:ECDHE-ECDSA-AES256-SHA384:ECDHE-ECDSA-AES256-SHA:ECDHE-RSA-AES256-SHA:DHE-RSA-AES128-SHA256:DHE-RSA-AES128-SHA:DHE-RSA-AES256-SHA256:DHE-RSA-AES256-SHA:ECDHE-ECDSA-DES-CBC3-SHA:ECDHE-RSA-DES-CBC3-SHA:EDH-RSA-DES-CBC3-SHA:AES128-GCM-SHA256:AES256-GCM-SHA384:AES128-SHA256:AES256-SHA256:AES128-SHA:AES256-SHA:DES-CBC3-SHA:!DSS;/g' $CONF

# Verify
echo "Updated config:"
grep -E "ssl_protocols|ssl_ciphers" $CONF

# Reload nginx
docker exec bus-gallery-frontend nginx -t && docker exec bus-gallery-frontend nginx -s reload
echo "Reloaded"
