import paramiko, time, sys

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=30, allow_agent=False, look_for_keys=False)

def run_with_output(cmd, timeout_sec=600):
    print(f'\n>>> {cmd}')
    channel = client.get_transport().open_session()
    channel.exec_command(cmd)
    end = time.time() + timeout_sec
    output = b''
    while time.time() < end:
        if channel.recv_ready():
            data = channel.recv(4096)
            output += data
            try:
                sys.stdout.buffer.write(data)
                sys.stdout.buffer.flush()
            except:
                pass
        if channel.recv_stderr_ready():
            data = channel.recv_stderr(4096)
            output += data
            try:
                sys.stdout.buffer.write(data)
                sys.stdout.buffer.flush()
            except:
                pass
        if channel.exit_status_ready():
            break
        time.sleep(0.3)
    # Drain
    while channel.recv_ready():
        data = channel.recv(4096)
        sys.stdout.buffer.write(data)
    while channel.recv_stderr_ready():
        data = channel.recv_stderr(4096)
        sys.stdout.buffer.write(data)
    exit_code = channel.recv_exit_status()
    print(f'\nExit: {exit_code}')
    return exit_code

print('Building frontend...')
rc = run_with_output(
    'cd /root/code/bus_gallery/docker && docker compose build --no-cache frontend 2>&1',
    timeout_sec=600
)

if rc == 0:
    print('\nFrontend build success! Restarting...')
    run_with_output('cd /root/code/bus_gallery/docker && docker compose up -d frontend 2>&1', timeout_sec=60)
    run_with_output('docker ps --format "table {{.Names}}\t{{.Status}}"', timeout_sec=10)

client.close()
