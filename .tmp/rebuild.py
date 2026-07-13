import paramiko, time, select

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())
client.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=30, allow_agent=False, look_for_keys=False)

def run_with_output(cmd, timeout_sec=300):
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
    # Drain
    while channel.recv_ready():
        data = channel.recv(4096)
        output += data
        print(data.decode(errors='replace'), end='', flush=True)
    while channel.recv_stderr_ready():
        data = channel.recv_stderr(4096)
        output += data
        print(data.decode(errors='replace'), end='', flush=True)
    exit_code = channel.recv_exit_status()
    print(f'\nExit: {exit_code}')
    return exit_code

# First check what docker compose command is available
run_with_output('which docker-compose 2>/dev/null; docker compose version 2>/dev/null; ls /root/code/bus_gallery/docker/docker-compose.yml 2>/dev/null')

client.close()
