# AdGuard Home Setup

CURRENT. How to create or rebuild CT 110, then point VyOS DNS forwarding at it.

AdGuard is DNS only. VyOS stays the DHCP server and the resolver address
clients use. Do not enable AdGuard DHCP. VLAN 40 does not use this guest.

Until Step 5, do not change VyOS. House DNS stays Cloudflare.

```text
Trusted / Guest / IoT  →  10.10.x.1  →  VyOS  →  10.10.0.4 AdGuard
VLAN 40                →  1.1.1.1 via VyOS wg2  (not this guest)
```

IoT may use the Internet out `eth1` only (forward rule 250, NAT rule 120).
Its DNS is VyOS **input** on `10.10.30.1`, then VyOS queries AdGuard.
AdGuard’s DoT leaves as Services traffic. Do not give IoT a forward
accept except out `eth1`.

```text
10.10.0.1    VyOS eth2
10.10.0.4    adguard (this guest)
```

Do not use `.2` (UniFi) or `.5` (CT 108, reserved until destroy). `.3` is unused.

---

# Step 1 — Create CT 110

On the **Proxmox host**.

```bash
pveam available --section system | grep debian | grep amd64
```

Use the current Debian 13 standard template. Download it if needed:

```bash
pveam download local debian-13-standard_13.6-1_amd64.tar.zst
```

Substitute the filename if the available version is newer.

```bash
pct create 110 local:vztmpl/debian-13-standard_13.6-1_amd64.tar.zst \
  --hostname adguard \
  --cores 1 \
  --memory 512 \
  --swap 0 \
  --rootfs local-lvm:8 \
  --net0 name=eth0,bridge=vmbr-svc,ip=10.10.0.4/24,gw=10.10.0.1 \
  --unprivileged 1 \
  --onboot 1 \
  --start 0
```

The AdGuard LXC’s own OS resolvers must not be VyOS. After Step 5, VyOS
depends on AdGuard; apt and DoT bootstrap would loop.

```bash
pct set 110 --nameserver "1.1.1.1 1.0.0.1"
pct config 110
pct start 110
```

Confirm hostname `adguard`, `10.10.0.4/24` on `vmbr-svc`, gw `10.10.0.1`,
nameserver `1.1.1.1 1.0.0.1`.

---

# Step 2 — Debian

```bash
pct enter 110
```

Remaining commands are inside the container unless noted.

```bash
ip addr show eth0
ip route
cat /etc/resolv.conf
ping -c 4 1.1.1.1
```

Expect `10.10.0.4/24`, default via `10.10.0.1`, resolvers `1.1.1.1` /
`1.0.0.1` (not `10.10.0.1`), and a working ping.

---

# Step 3 — Install AdGuard Home

Official install from
https://github.com/AdguardTeam/AdGuardHome#automated-install-linux-and-mac

Not Docker, Snap, or the Proxmox community-scripts installer.

Port 53 must be free before AdGuard binds it. On this Debian 13 LXC
template, `systemd-resolved` is usually **not** installed. Check first:

```bash
ss -lunp | grep ':53' || true
systemctl status systemd-resolved.service
```

If the unit does not exist and nothing is listening on `:53`, continue.
If `systemd-resolved` is present and owns `:53`:

```bash
systemctl disable --now systemd-resolved
```

Expect nothing on `:53` before the installer.

As root in the container (`wget` is on the Debian 13 template; the
script uses it when `curl` is absent):

```bash
wget --no-verbose -O - https://raw.githubusercontent.com/AdguardTeam/AdGuardHome/master/scripts/install.sh | sh -s -- -v
```

That unpacks into `/opt/AdGuardHome` and runs `./AdGuardHome -s install`.
Default channel is `release`. Do not pass `-c beta` or `-c edge`.

```bash
systemctl is-active AdGuardHome
```

Expect `active`. Later: `/opt/AdGuardHome/AdGuardHome -s stop|start|restart`.

From Trusted open `http://10.10.0.4:3000` and finish the wizard:

- Web UI: `10.10.0.4` (port 80 or 3000; remember which)
- DNS: `10.10.0.4` port 53, not `127.0.0.1` only
- Admin user: not stored in Git


---

# Step 4 — AdGuard settings (Trusted UI)

Open `http://10.10.0.4/` (or `:3000`).

Keep AdGuard’s defaults for upstream (usually Quad9 DoH), bootstrap
IPs, DHCP off, and encryption off. Do not add a plain `1.1.1.1` as an
extra upstream. Filter lists are local taste; do not put them in Git.

Two changes only:

1. **Settings → DNS settings → Access settings → Allowed clients**

   ```text
   10.10.0.1
   ```

   Leave **Disallowed clients** empty. Once Allowed is non-empty, only
   that source may query DNS. VyOS still blocks Guest/IoT/India from
   reaching `10.10.0.4`; this also stops Trusted from using AdGuard
   except through the recursor.

2. **Filters → DNS rewrites** (`home.arpa`, not `homelab.local`):

| Name | Address |
|------|---------|
| `vyos.home.arpa` | `10.10.10.1` |
| `cisco.home.arpa` | `10.10.10.2` |
| `pve.home.arpa` | `10.10.10.3` |
| `unifi.home.arpa` | `10.10.0.2` |
| `adguard.home.arpa` | `10.10.0.4` |
| `dev.home.arpa` | `10.10.10.10` |

Do not enable AdGuard DHCP.

---

# Step 5 — Prove AdGuard, then cut VyOS over

House DNS is still Cloudflare. AdGuard allows only `10.10.0.1`. Prove
it from VyOS:

```bash
nslookup vyos.home.arpa 10.10.0.4
# 10.10.10.1

nslookup example.com 10.10.0.4
# public A
```

From Trusted, Guest, IoT, or India: `dig @10.10.0.4` must fail
(refused or timeout). Do not add a forward accept and do not add
those subnets to Allowed clients.

On the AdGuard LXC: `ss -lunp | grep ':53'` should show DNS on port 53.

Do not continue until the VyOS `nslookup` checks work.

On VyOS, replace the Cloudflare upstreams in `../vyos/commands.txt`.
Leave listen/allow on VLANs 10/20/30. Do not touch VLAN 40, PBR, NAT,
firewall, or DHCP `domain-name`.

```bash
delete service dns forwarding name-server 1.0.0.1
delete service dns forwarding name-server 1.1.1.1
set service dns forwarding name-server '10.10.0.4'
```

No second VyOS `name-server`. No `domain home.arpa` on VyOS. No DHCP
option 6 changes. Search-domain is Step 6, after this cutover is
confirmed.

```bash
compare
commit-confirm 60
```

Do not `confirm` yet.

From Trusted:

```bash
dig @10.10.10.1 vyos.home.arpa +short
dig @10.10.10.1 example.com +short
```

Expect answers. `dig @10.10.0.4` from Trusted must fail.

Guest: `dig @10.10.20.1 example.com` works; `dig @10.10.0.4` fails.

IoT: `dig @10.10.30.1 example.com` works (VyOS → AdGuard). `ping 1.1.1.1`
works. `ping 10.10.10.1` still fails. IoT forward is out `eth1` only.

India: DHCP DNS still `1.1.1.1` / `1.0.0.1`. `dig @10.10.40.1` fails.
`dig @1.1.1.1` and `curl -4 https://ifconfig.me` still go via India.
`dig @10.10.0.4` fails.

AdGuard query log source for the 10/20/30 gateway tests: `10.10.0.1`.

Then `confirm`, and `save` as in `../vyos/setup.md`. Export live CLI into
`../vyos/commands.txt` (strip the password hash). Do not bulk-load
`config.boot`.

**Rollback** (restore house DNS before debugging AdGuard):

```bash
delete service dns forwarding name-server '10.10.0.4'
set service dns forwarding name-server 1.0.0.1
set service dns forwarding name-server 1.1.1.1
```

`commit-confirm 60`, test, `confirm`.

---

# Step 6 — Trusted search domain `home.arpa`

Do this after Step 5 is confirmed. Known-good DHCP `domain-name` for
Trusted is `home.arpa` (`../vyos/commands.txt`). It makes short names
(`ping vyos`, `ping dev`) work on Trusted clients that honor DHCP
domain/search. Full names such as `dig vyos.home.arpa` already work
without it.

Skip this step when the PRIVATE `domain-name` is already `home.arpa`.
Apply it when the router still has `homelab.local`:

```bash
delete service dhcp-server shared-network-name PRIVATE subnet 10.10.10.0/24 option domain-name 'homelab.local'
set service dhcp-server shared-network-name PRIVATE subnet 10.10.10.0/24 option domain-name 'home.arpa'
```

If the image has `option domain-search`, set it to `home.arpa`.
Tab-complete; do not invent a node. Do not change Guest, IoT, or India
DHCP.

```bash
compare
commit-confirm 60
```

Renew a Trusted lease, then `ping -c 1 vyos`. FQDN via `10.10.10.1` must
still work.

**Rollback:**

```bash
delete service dhcp-server shared-network-name PRIVATE subnet 10.10.10.0/24 option domain-name 'home.arpa'
set service dhcp-server shared-network-name PRIVATE subnet 10.10.10.0/24 option domain-name 'homelab.local'
```

`commit-confirm 60`, test, `confirm`.

---

# Step 7 — Reboot the guest

On the Proxmox host: `pct reboot 110`. Do not repair by hand.

Inside CT 110: `10.10.0.4/24`, public resolvers, AdGuard active, DNS on
`10.10.0.4:53`.

From Trusted, `dig @10.10.10.1` for `vyos.home.arpa` and `example.com`
still works.

---

# Restore yaml only

If the container is still there and you only lost settings:

1. Keep off-box copies of `AdGuardHome.yaml` and `data/` when healthy.
   Do not commit them (admin hash, stats).
2. `/opt/AdGuardHome/AdGuardHome -s stop`
3. Replace those files, then `-s start`
4. On VyOS, `nslookup vyos.home.arpa 10.10.0.4`. From Trusted,
   `dig @10.10.10.1`.

If the container is gone, start at Step 1. Restore yaml/data before
the VyOS cutover if VyOS is not already using `10.10.0.4`.
