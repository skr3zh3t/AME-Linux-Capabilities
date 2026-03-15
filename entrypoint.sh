#!/bin/bash

USER_FLAG_VAL=${USER_FLAG:-practice{default_user_flag}}
ROOT_FLAG_VAL=${ROOT_FLAG:-practice{default_root_flag}}

echo "$USER_FLAG_VAL" > /home/appuser/user.txt
chown appuser:appuser /home/appuser/user.txt
chmod 400 /home/appuser/user.txt

echo "$ROOT_FLAG_VAL" > /root/root.txt
chmod 400 /root/root.txt

exec su - appuser -c "java -cp /app VulnApp"