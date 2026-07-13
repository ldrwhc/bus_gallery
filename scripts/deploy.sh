#!/bin/bash
# Deploy frontend dist to server
# Requires sshpass: choco install sshpass / apt install sshpass

SSHPASS='whc@njupt020704'
HOST='192.144.227.251'
USER='root'
LOCAL='D:/code/bus-gallery/frontend/dist'
REMOTE='/tmp/dist-frontend'

# Upload
sshpass -e ssh -o StrictHostKeyChecking=no $USER@$HOST "rm -rf $REMOTE && mkdir -p $REMOTE"
SSHPASS='whc@njupt020704' sshpass -e scp -o StrictHostKeyChecking=no -r "$LOCAL"/* $USER@$HOST:$REMOTE/

# Docker cp
sshpass -e ssh -o StrictHostKeyChecking=no $USER@$HOST "docker cp $REMOTE/. bus-gallery-frontend:/usr/share/nginx/html/ && rm -rf $REMOTE"

echo "Deploy complete!"
