# India Gateway Setup — DEPRECATED

CT 108 is deprecated. Live VLAN 40 egress is VyOS `wg2`
(`../wireguard/wg2.md`). Do not apply this file to the live router.

Keep it. It is how a human rebuilds the Tailscale India path if that
design is chosen again (`README.md`). That choice changes
`../REQUIREMENTS.md` first. Step 7 points
VLAN 40 at `10.10.0.5` and must not be applied while table 40 uses
`wg2`.

Stop the existing container in `../wireguard/wg2.md` section 7. Do not
destroy it in that sitting. Do not `tailscale logout`.

This document describes how to rebuild the `tailscale-india` gateway from scratch.

The gateway used to provide Internet access for the India Wi-Fi / VLAN 40 through
a Tailscale exit node physically located in India.

The philosophy of this document is deliberately simple:

1. Make one change.
2. Understand why it is needed.
3. Verify it.
4. Only then continue.

There is intentionally no automation around this process.

---

# Architecture

```text
India Wi-Fi
    │
    │ VLAN 40 — 10.10.40.0/24
    ▼
VyOS
10.10.40.1
    │
    │ Policy routing — table 40
    ▼
India-GW
10.10.0.5
    │
    │ tailscale0
    ▼
Tailscale
    │
    ▼
asus-nuc
India exit node
    │
    ▼
Indian ISP
    │
    ▼
Internet
```

The important design requirement is **fail-closed routing**.

If India-GW, Tailscale, or the remote India exit node fails, VLAN 40 must
lose Internet access.

It must NEVER fall back to the normal US WAN.

---

# Fixed Addresses

```text
India VLAN:          10.10.40.0/24
India VLAN gateway:  10.10.40.1

Services network:    10.10.0.0/24
VyOS Services IP:    10.10.0.1
India-GW:            10.10.0.5

India exit node:
  Hostname:          asus-nuc
  Tailscale IP:      100.124.152.41
```

---

# Step 1 — Create the India Gateway LXC

Run these commands on the **Proxmox host**.

## 1.1 Find the current Debian 13 template

```bash
# List the Debian AMD64 templates currently available from Proxmox.
#
# We look this up rather than assuming that the template version used
# during the original installation still exists.
pveam available --section system | grep debian | grep amd64
```

Choose the current Debian 13 standard template.

During the original installation this was:

```text
debian-13-standard_13.6-1_amd64.tar.zst
```

The version may be newer during a future rebuild.

---

## 1.2 Download the Debian template

```bash
# Download the selected Debian template into Proxmox's local
# template storage.
pveam download local debian-13-standard_13.6-1_amd64.tar.zst
```

If the available version has changed, substitute the current filename.

---

## 1.3 Create CT 108

```bash
# Create the India gateway container.
#
# CT 108 / tailscale-india:
#   Fixed identity for this service.
#
# 1 CPU / 512 MB:
#   More than enough for this small routing workload.
#
# swap=0:
#   No swap is required.
#
# rootfs=8 GB:
#   Plenty for Debian + Tailscale + nftables.
#
# vmbr-svc:
#   Places the gateway on the private Proxmox Services network.
#
# 10.10.0.5:
#   Permanent address of India-GW.
#
# 10.10.0.1:
#   VyOS is the Services network gateway.
#
# unprivileged:
#   Keep the container unprivileged for isolation.
#
# nesting:
#   Enabled for the container/Tailscale environment.
#
# onboot:
#   Start the gateway automatically when Proxmox boots.
#
# start=0:
#   Do not start yet. We first need to expose /dev/net/tun.

pct create 108 local:vztmpl/debian-13-standard_13.6-1_amd64.tar.zst \
  --hostname tailscale-india \
  --cores 1 \
  --memory 512 \
  --swap 0 \
  --rootfs local-lvm:8 \
  --net0 name=eth0,bridge=vmbr-svc,ip=10.10.0.5/24,gw=10.10.0.1 \
  --unprivileged 1 \
  --features nesting=1 \
  --onboot 1 \
  --start 0
```

---

## 1.4 Give the container access to TUN

Tailscale needs `/dev/net/tun` to create its `tailscale0` interface.

```bash
# Allow CT 108 to access the TUN character device.
echo 'lxc.cgroup2.devices.allow: c 10:200 rwm' >> /etc/pve/lxc/108.conf
```

```bash
# Bind the host's /dev/net/tun into the container.
echo 'lxc.mount.entry: /dev/net/tun dev/net/tun none bind,create=file' >> /etc/pve/lxc/108.conf
```

---

## 1.5 Configure DNS

```bash
# Give the gateway public DNS resolvers.
#
# We do not want this container dependent on VyOS DNS forwarding.
pct set 108 --nameserver "1.1.1.1 1.0.0.1"
```

---

## 1.6 Inspect the configuration

```bash
# Verify what Proxmox actually recorded before starting the container.
pct config 108
```

Confirm at least:

```text
hostname: tailscale-india
memory: 512
swap: 0
unprivileged: 1
onboot: 1
net0: ... bridge=vmbr-svc ... ip=10.10.0.5/24 ... gw=10.10.0.1
```

Also verify that the two TUN-related LXC lines are present.

---

## 1.7 Start the container

```bash
# Start India-GW for the first time.
pct start 108
```

---

## 1.8 Verify TUN

```bash
# Verify that /dev/net/tun exists inside the container.
pct exec 108 -- ls -l /dev/net/tun
```

Do not continue unless `/dev/net/tun` exists.

---

# Step 2 — Bootstrap Debian

Enter the container:

```bash
# Open a shell inside India-GW.
pct enter 108
```

The remaining commands in this document are run **inside India-GW**
unless explicitly stated otherwise.

---

## 2.1 Verify networking before changing anything

```bash
# Confirm that eth0 has the expected Services address.
ip addr show eth0
```

Look for:

```text
10.10.0.5/24
```

Then:

```bash
# Verify the routing table.
ip route
```

Expected important routes:

```text
default via 10.10.0.1 dev eth0
10.10.0.0/24 dev eth0
```

Test Internet connectivity:

```bash
# Verify that the container can reach the Internet through VyOS.
ping -c 4 1.1.1.1
```

Do not continue until this works.

---

## 2.2 Install required packages

```bash
# Refresh Debian's package metadata.
apt update
```

```bash
# Install:
#
# nftables — firewall and NAT for forwarded VLAN40 traffic
# curl     — HTTP testing and public-IP verification
# tcpdump  — packet-level troubleshooting
apt install -y nftables curl tcpdump
```

---

# Step 3 — Configure Linux as a Router

India-GW needs to forward packets between VyOS and Tailscale.

Create a dedicated sysctl configuration:

```bash
# Enable IPv4 forwarding.
#
# rp_filter=2 enables loose reverse-path filtering. This is important
# because this gateway intentionally uses asymmetric/policy routing.
#
# IPv6 forwarding remains disabled because VLAN40 is intentionally
# IPv4-only until an explicit IPv6 design exists.

cat >/etc/sysctl.d/99-india-gw.conf <<'EOF'
net.ipv4.ip_forward=1
net.ipv4.conf.all.rp_filter=2
net.ipv4.conf.default.rp_filter=2
net.ipv6.conf.all.forwarding=0
EOF
```

Apply it:

```bash
# Load the new kernel networking settings.
sysctl --system
```

---

## 3.1 Verify

```bash
# IPv4 forwarding must be enabled.
sysctl net.ipv4.ip_forward
```

Expected:

```text
net.ipv4.ip_forward = 1
```

```bash
# Reverse-path filtering should use loose mode.
sysctl net.ipv4.conf.all.rp_filter
```

Expected:

```text
net.ipv4.conf.all.rp_filter = 2
```

```bash
# IPv6 forwarding should remain disabled.
sysctl net.ipv6.conf.all.forwarding
```

Expected:

```text
net.ipv6.conf.all.forwarding = 0
```

---

# Step 4 — Install and Configure Tailscale

Install Tailscale using the current official Debian installation
procedure.

After installation, configure this machine to use the existing India
exit node.

```bash
# Configure India-GW as a Tailscale client.
#
# hostname:
#   Gives this gateway a recognizable identity in Tailscale.
#
# exit-node:
#   All normal Internet traffic from this gateway uses asus-nuc in India.
#
# exit-node-allow-lan-access:
#   Preserve local LAN access while using the exit node.
#
# accept-routes=false:
#   Do not import arbitrary Tailscale subnet routes.
#
# accept-dns=false:
#   Do not let Tailscale replace this machine's DNS configuration.

tailscale up \
  --hostname=tailscale-india \
  --exit-node=100.124.152.41 \
  --exit-node-allow-lan-access=true \
  --accept-routes=false \
  --accept-dns=false
```

Authenticate if Tailscale asks for authorization.

---

## 4.1 Verify Tailscale

```bash
# Verify that the gateway is connected to the tailnet and that
# asus-nuc is being used as the exit node.
tailscale status
```

Look for `asus-nuc` and `exit node`.

Then:

```bash
# Verify the public IPv4 address seen by the Internet.
curl -4 https://ifconfig.me
```

The address should belong to the Indian Internet connection.

During the original deployment it was:

```text
49.204.132.253
```

The exact address can change.

Do not continue until India-GW itself exits through India.

---

# Step 5 — Configure Firewall and NAT

VLAN40 traffic arrives at India-GW with its original source address:

```text
10.10.40.x
```

We want to allow that traffic **only through tailscale0**.

Everything else forwarded through this machine should be dropped.

Create `/etc/nftables.conf`:

```bash
# Configure forwarding as a positive allow-list.
#
# Default policy is DROP.
#
# Existing connections may return.
#
# New traffic is accepted only when:
#   source = VLAN40
#   output = tailscale0
#
# NAT is also restricted to VLAN40 -> tailscale0.

cat >/etc/nftables.conf <<'EOF'
table inet india-gw {
    chain forward {
        type filter hook forward priority filter; policy drop;

        ct state established,related accept

        ip saddr 10.10.40.0/24 oifname "tailscale0" accept
    }
}

table ip india-gw-nat {
    chain postrouting {
        type nat hook postrouting priority srcnat; policy accept;

        ip saddr 10.10.40.0/24 oifname "tailscale0" masquerade
    }
}
EOF
```

Enable nftables:

```bash
# Enable nftables now and automatically after future boots.
systemctl enable --now nftables
```

---

## 5.1 Verify

```bash
# Display the active nftables configuration.
nft list ruleset
```

Our rules should show:

```text
VLAN40 -> tailscale0 = ACCEPT
everything else forwarded = DROP
VLAN40 -> tailscale0 = MASQUERADE
```

Tailscale may also install its own nftables tables.

Do not modify Tailscale-managed rules manually.

---

# Step 6 — Configure VLAN40 Return Routing

This is a critical part of the design.

Traffic arrives like this:

```text
10.10.40.x
     │
     ▼
VyOS
10.10.0.1
     │
     ▼
India-GW
10.10.0.5
```

Therefore replies for `10.10.40.0/24` must return to:

```text
10.10.0.1
```

However, Tailscale installs its own policy-routing rules and routing
table 52.

Without an explicit exception, Linux can incorrectly send replies for
VLAN40 back into `tailscale0`.

---

## 6.1 Create the persistent return-route service

```bash
# Create a small systemd oneshot service.
#
# The route tells Linux:
#
#   10.10.40.0/24 is reachable through VyOS at 10.10.0.1.
#
# The policy rule is equally important:
#
#   For destinations in VLAN40, consult the normal main routing table
#   BEFORE Tailscale's policy-routing table.
#
# Priority 2500 executes before Tailscale's rule around priority 5270.
#
# "onlink" is intentional. It prevents boot-time failure if this service
# runs before Linux has completely established eth0's connected route.

cat >/etc/systemd/system/india-return-route.service <<'EOF'
[Unit]
Description=India VLAN return routing
After=network-online.target tailscaled.service
Wants=network-online.target

[Service]
Type=oneshot
ExecStart=/usr/sbin/ip route replace 10.10.40.0/24 via 10.10.0.1 dev eth0 onlink
ExecStart=-/usr/sbin/ip rule del to 10.10.40.0/24 priority 2500 lookup main
ExecStart=/usr/sbin/ip rule add to 10.10.40.0/24 priority 2500 lookup main
ExecStop=-/usr/sbin/ip rule del to 10.10.40.0/24 priority 2500 lookup main
RemainAfterExit=yes

[Install]
WantedBy=multi-user.target
EOF
```

Reload systemd:

```bash
# Tell systemd that a new unit file exists.
systemctl daemon-reload
```

Enable and start it:

```bash
# Run the return-route configuration now and automatically at boot.
systemctl enable --now india-return-route.service
```

---

## 6.2 Verify the service

```bash
# Verify that the oneshot completed successfully.
systemctl status india-return-route.service --no-pager
```

Expected state:

```text
active (exited)
```

The `ip rule del` command may report that a rule did not exist.

That is harmless because the command begins with `-`, telling systemd
not to treat that condition as a service failure.

---

## 6.3 Verify the return route

```bash
# Ask Linux exactly how it would send a packet back to a VLAN40 client.
ip route get 10.10.40.101
```

Expected:

```text
10.10.40.101 via 10.10.0.1 dev eth0
```

It must NOT say:

```text
dev tailscale0 table 52
```

---

## 6.4 Verify policy-rule ordering

```bash
# Inspect Linux policy-routing rules.
ip rule show
```

Look for our rule:

```text
2500: ... to 10.10.40.0/24 lookup main
```

and Tailscale's rule later:

```text
5270: ... lookup 52
```

Conceptually:

```text
Destination = 10.10.40.x
       │
       ▼
priority 2500
       │
       ▼
main routing table
       │
       ▼
10.10.0.1 / VyOS
```

This happens before Tailscale gets an opportunity to use table 52.

---

# Step 7 — Configure VyOS for India Routing

Do not apply this step while VLAN 40 uses `wg2`. It is the rollback
path only (`table 40` next-hop `10.10.0.5`, forward rule 400 out
`eth2`).

The Linux gateway is now ready.

On a rollback, VyOS must send VLAN40 traffic to it.

The desired path is:

```text
VLAN40 client
10.10.40.x
      │
      ▼
VyOS
10.10.40.1
      │
      │ PBR table 40
      ▼
India-GW
10.10.0.5
```

The VyOS configuration must provide:

- `NET-INDIA = 10.10.40.0/24`
- `NET-RFC1918`
- VLAN40 removed from normal VyOS DNS clients
- RFC1918 destinations blocked from India
- India traffic allowed toward `eth2`
- policy route `PBR-INDIA`
- VLAN40 assigned to that policy route
- table 40 default route through `10.10.0.5`
- blackhole fallback route
- VLAN40 DHCP DNS = `1.1.1.1` and `1.0.0.1`
- IPv6 disabled for VLAN40
- NO VLAN40 masquerade/NAT through normal WAN `eth1`

Use `../vyos/setup.md` and `../vyos/commands.txt` for the actual
VyOS commands.

Do not duplicate the complete VyOS configuration here. Do not bulk-load
a `config.boot`.

---

# Step 8 — Test an India Client

Connect a laptop to the **India Wi-Fi**.

---

## 8.1 Verify DHCP

The client should receive approximately:

```text
IP:       10.10.40.x
Subnet:   /24
Gateway:  10.10.40.1

DNS:
1.1.1.1
1.0.0.1
```

---

## 8.2 Test raw Internet connectivity

```bash
# Test Internet connectivity without depending on DNS.
ping 1.1.1.1
```

This must work.

---

## 8.3 Verify the public IP

```bash
# Ask an external service which public IPv4 address it sees.
curl -4 https://ifconfig.me
```

The address must be the **Indian public IP**, not the US/Xfinity
address.

---

# Step 9 — Trace Traffic if Something Does Not Work

Do not start changing firewall rules randomly.

Trace the packet path one layer at a time.

---

## 9.1 Check traffic arriving from VyOS

On India-GW:

```bash
# Watch traffic between the VLAN40 client and Cloudflare on eth0.
tcpdump -ni eth0 host 1.1.1.1
```

Generate traffic from the India client:

```bash
ping 1.1.1.1
```

You should see packets originating from something like:

```text
10.10.40.101
```

If you see them, then:

```text
Client
   ↓
Wi-Fi
   ↓
VLAN40
   ↓
VyOS/PBR
   ↓
India-GW eth0
```

is working.

---

## 9.2 Check traffic entering Tailscale

```bash
# Watch the same traffic on the Tailscale interface.
tcpdump -ni tailscale0 host 1.1.1.1
```

A healthy flow should show traffic being NATed into Tailscale and
responses returning.

---

## 9.3 Check return routing

If replies reach India-GW but the client does not receive them:

```bash
# This is one of the first commands to run.
ip route get 10.10.40.101
```

Correct:

```text
via 10.10.0.1 dev eth0
```

Incorrect:

```text
dev tailscale0 table 52
```

If the latter appears, investigate the `india-return-route.service`
and policy rule before changing anything else.

---

# Step 10 — Verify DNS and Location Leakage

From an India client, use a browser-based IP/DNS leak test.

Verify:

- public IPv4 appears in India
- DNS queries appear to originate through the India path
- no Xfinity/US public address appears
- no public IPv6 connection bypasses the India path

During the original deployment, BrowserLeaks showed India for both
public IP and DNS.

The important requirement is not the identity of a particular DNS
server.

The requirement is:

```text
India DNS query
      │
      ▼
VyOS PBR
      │
      ▼
India-GW
      │
      ▼
Tailscale
      │
      ▼
India
      │
      ▼
Public DNS
```

---

# Step 11 — Prove Fail-Closed Behavior

This test is mandatory.

A configuration that works normally but leaks through the US WAN when
Tailscale fails does NOT satisfy the design.

On India-GW:

```bash
# Deliberately take down Tailscale.
tailscale down
```

From an India client:

```bash
ping 1.1.1.1
```

This must fail.

Try browsing to a website.

It must fail.

The expected behavior is:

```text
India client
     │
     ▼
VyOS table 40
     │
     ▼
10.10.0.5
     │
     X Tailscale unavailable
     │
     X NO US WAN fallback
```

Restore Tailscale:

```bash
# Restore the exact intended Tailscale configuration.
tailscale up \
  --hostname=tailscale-india \
  --exit-node=100.124.152.41 \
  --exit-node-allow-lan-access=true \
  --accept-routes=false \
  --accept-dns=false
```

Verify:

```bash
tailscale status
```

Then test again from the India client:

```bash
ping 1.1.1.1
curl -4 https://ifconfig.me
```

Internet should return through India.

---

# Step 12 — Prove Reboot Persistence

The final test is a complete gateway reboot.

This catches configuration that works only because of manually entered
runtime commands.

```bash
# Reboot India-GW.
reboot
```

Wait for the container to return.

Do NOT manually repair anything.

---

## 12.1 Verify normal Linux networking

```bash
ip route
```

Confirm:

```text
default via 10.10.0.1 dev eth0
10.10.0.0/24 dev eth0
```

---

## 12.2 Verify the return-route service

```bash
systemctl status india-return-route.service --no-pager
```

Expected:

```text
active (exited)
```

---

## 12.3 Verify VLAN40 return routing

```bash
ip route get 10.10.40.101
```

Expected:

```text
10.10.40.101 via 10.10.0.1 dev eth0
```

---

## 12.4 Verify policy routing

```bash
ip rule show
```

Confirm our priority `2500` rule exists before Tailscale's table-52
policy rule.

---

## 12.5 Verify Tailscale

```bash
tailscale status
```

Confirm `asus-nuc` is still configured as the exit node.

---

## 12.6 Verify firewall persistence

```bash
nft list ruleset
```

Confirm the India-GW forwarding and NAT rules exist.

---

## 12.7 Final client test

From a device connected to India Wi-Fi:

```bash
ping 1.1.1.1
```

Then:

```bash
curl -4 https://ifconfig.me
```

Both must work.

The public IP must be Indian.

If they work immediately after a complete India-GW reboot without any
manual intervention, the gateway rebuild is complete.

---

# Troubleshooting Mental Model

Always troubleshoot from left to right:

```text
India client
10.10.40.x
      │
      ▼
Wi-Fi / VLAN40
      │
      ▼
VyOS
PBR table 40
      │
      ▼
10.10.0.5
India-GW eth0
      │
      ▼
nftables
      │
      ▼
tailscale0
      │
      ▼
asus-nuc
India
      │
      ▼
Indian ISP
      │
      ▼
Internet
```

Find the first place where the packet disappears.

Fix that layer.

Verify it.

Only then continue.

---

# Important Lessons From the Original Deployment

## Tailscale policy routing changes return routing

Installing Tailscale creates policy-routing rules and routing table 52.

A normal route for:

```text
10.10.40.0/24 via 10.10.0.1
```

by itself was NOT sufficient.

We needed the priority-2500 policy rule so VLAN40 return traffic
consults the Linux main routing table before Tailscale's table 52.

---

## Do not use systemd-networkd to add this route

During the original deployment we tried creating:

```text
/etc/systemd/network/10-india-return.network
```

This was the wrong approach.

Proxmox already manages the container's `eth0` networking. Having
systemd-networkd also participate caused the normal connected/default
routes to disappear after reboot.

The final solution deliberately leaves interface ownership alone and
uses:

```text
india-return-route.service
```

only to add the special route and policy rule.

---

## `onlink` is intentional

The first version of the systemd service used:

```text
via 10.10.0.1 dev eth0
```

During boot it could fail with:

```text
Error: Nexthop has invalid gateway.
```

because the service could execute before the connected route for
`10.10.0.0/24` was fully established.

The final route therefore uses:

```text
via 10.10.0.1 dev eth0 onlink
```

Do not remove `onlink` without understanding why it is there.

---

# Definition of Done

The India gateway is considered working only when ALL of these are
true:

- India-GW boots automatically.
- `/dev/net/tun` exists.
- IPv4 forwarding is enabled.
- Tailscale automatically reconnects.
- `asus-nuc` is the exit node.
- nftables rules survive reboot.
- VLAN40 return route survives reboot.
- priority-2500 policy rule survives reboot.
- India client receives `10.10.40.x`.
- India client uses `10.10.40.1` as gateway.
- India client uses public DNS through the India route.
- India client's public IPv4 is Indian.
- DNS leak test shows no US-path leakage.
- IPv6 cannot bypass the India path.
- India cannot pivot into RFC1918 internal networks.
- Taking Tailscale down causes India Internet to stop.
- There is no fallback through the normal US WAN.
- A complete India-GW reboot requires no manual repair.