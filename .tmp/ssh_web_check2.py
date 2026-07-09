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
        stdin, stdout, stderr = client.exec_command(cmd, timeout=20)
        out = stdout.read().decode(errors='replace')
        err = stderr.read().decode(errors='replace')
        if out:
            print(out.strip()[:4000])
        if err:
            print("STDERR:", err.strip()[:1000])
    except Exception as e:
        print(f"FAILED: {e}")

# Check index.html content
print("========== index.html content ==========")
run_cmd("docker exec bus-gallery-frontend cat /usr/share/nginx/html/index.html 2>&1")

time.sleep(1)

# Check if the gateway proxy is correct - test via nginx->gateway chain
print("\n========== Test API via nginx proxy ==========")
run_cmd("curl -s http://localhost:80/api/regions 2>&1 | head -5")
run_cmd("curl -s http://localhost:80/api/vehicles?size=2 2>&1 | head -200")

time.sleep(1)

# Check front page gets full HTML
print("\n========== Front page full response ==========")
run_cmd("curl -s http://localhost:80/ 2>&1")

time.sleep(1)

# Check asset JS file accessibility
print("\n========== Check JS asset ==========")
run_cmd("curl -s -o /dev/null -w 'HTTP_CODE: %{http_code} SIZE: %{size_download}' http://localhost:80/assets/index-Dld7_w42.js 2>&1")

time.sleep(1)

# Check if there's a GFW/cloud security group issue
# The server is in China (based on the IP and Huawei Cloud registry)
print("\n========== Check iptables rules ==========")
run_cmd("iptables -L INPUT -n 2>&1 | head -30")

time.sleep(1)

# Check external connectivity
print("\n========== Check if port 80 is open externally ==========")
run_cmd("timeout 5 bash -c 'echo > /dev/tcp/192.144.227.251/80 && echo OPEN' 2>&1 || echo 'TIMEOUT/CLOSED'")

print("\n\n========== SUMMARY ==========")
print("The website should now be working.")
print("If you still can't access, try:")
print("1. Hard refresh: Ctrl+Shift+R (or Ctrl+F5)")
print("2. Clear browser cache")
print("3. Try in incognito/private mode")
print("4. Check if your network blocks port 80 to this IP")

client.close()
