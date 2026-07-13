import paramiko, time, select

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
            print(data.decode(errors='replace'), end='', flush=True)
        if channel.recv_stderr_ready():
            data = channel.recv_stderr(4096)
            output += data
            print(data.decode(errors='replace'), end='', flush=True)
        if channel.exit_status_ready():
            break
        time.sleep(0.3)
    while channel.recv_ready():
        data = channel.recv(4096)
        output += data
        print(data.decode(errors='replace'), end='', flush=True)
    exit_code = channel.recv_exit_status()
    print(f'\nExit: {exit_code}')
    return exit_code

# Step 1: Rebuild backend (Java Maven build inside Docker)
print('=== Step 1: Build Backend ===')
rc = run_with_output(
    'cd /root/code/bus_gallery/docker && docker compose build --no-cache backend 2>&1',
    timeout_sec=600
)

# Step 2: Rebuild frontend (Vue npm build inside Docker)
print('\n=== Step 2: Build Frontend ===')
rc2 = run_with_output(
    'cd /root/code/bus_gallery/docker && docker compose build --no-cache frontend 2>&1',
    timeout_sec=600
)

client.close()
