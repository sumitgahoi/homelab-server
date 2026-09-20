# Proxmox

Until cutover: host `192.168.50.200/24` on `vmbr1` (ASUS LAN, gateway `192.168.50.1`). No `vmbr0.10`. UI `https://192.168.50.200:8006`. SSH `root@192.168.50.200`.

After VyOS is the router: known-good `proxmox/interfaces` — drop the `vmbr1` address, add `vmbr0.10` = `10.10.10.3/24`, gateway `10.10.10.1`. UI `https://10.10.10.3:8006`. Copy and `ifreload`: `proxmox/README.md`. Topology: `README.md`. SSH key: `ssh/README.md`.

| Disk | Role |
|------|------|
| 960 EVO 250 GB | root only |
| SN770 1 TB | VM/LXC disks, ISOs, templates |
| WD Red 3 TB | `/mnt/data` — media, backups |

BMC Realtek 1G is console/IPMI only — not the management NIC.

Optional BIOS: PL1 = PL2 = 65 W for 24/7. Apt: pve-no-subscription.

## Guests

| Guest | Type | vCPU | RAM | NIC | IP |
|-------|------|------|-----|-----|-----|
| vyos | VM 100 | 2 | 4096 MiB | `vmbr0` + `vmbr1` + `vmbr-svc` | `10.10.10.1` |
| unifi-os-server | LXC 107 | 2 | 4096 MiB | `vmbr-svc` | `10.10.0.2` |
| tailscale-us | LXC | 1 | 512 MiB | `vmbr-svc` | `10.10.0.3` (PLANNED) |
| adguard | LXC | 1 | 512 MiB | `vmbr-svc` | `10.10.0.4` (PLANNED) |
| tailscale-india | LXC 108 | 1 | 512 MiB | `vmbr-svc` + TUN | `10.10.0.5` (CURRENT) |

Create VM 100: `vyos/install.md`. Then configure from `vyos/setup.md` using `vyos/commands.txt`. Locked NICs/MACs: `README.md`. UniFi OS Server: `unifi.md`. India-GW: `tailscale-india/setup.md`.

## VyOS down

Mgmt does not hairpin through the router. DHCP will be dead.

1. CBS350 port 12 (or any VLAN 10 access). Static `10.10.10.99/24`, no gateway.
2. After cutover: `ping 10.10.10.3` → UI or SSH. Until cutover, GE12 does not reach `192.168.50.200` (that is ASUS/`vmbr1`); use that LAN or BMC.
3. `qm start 100`.

Switch dead: laptop on `nic0` tagged 10, or BMC. OS dead: BMC iKVM.
