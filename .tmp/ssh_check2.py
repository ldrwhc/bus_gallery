import paramiko
import sys

client = paramiko.SSHClient()
client.set_missing_host_key_policy(paramiko.AutoAddPolicy())

try:
    client.connect('192.144.227.251', username='root', password='whc@njupt020704', timeout=30, banner_timeout=30)

    commands = [
        "ls /root/",
        "docker ps 2>&1 | head -20",
        "docker ps -a 2>&1 | head -20",
    ]

    for cmd in commands:
        print(f"\n=== CMD: {cmd} ===")
        try:
            stdin, stdout, stderr = client.exec_command(cmd, timeout=20)
            out = stdout.read().decode()
            err = stderr.read().decode()
            if out: print(out.strip())
            if err: print("ERR:", err.strip())
        except Exception as e:
            print(f"Command failed: {e}")

except Exception as e:
    print(f'Error: {e}')
    import traceback
    traceback.print_exc()
finally:
    try:
        client.close()
    except:
        pass
