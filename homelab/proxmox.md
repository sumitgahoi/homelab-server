# Proxmox

Until cutover: host `192.168.50.200/24` on `vmbr1` (ASUS LAN, gateway `192.168.50.1`). No `vmbr0.10`. UI `https://192.168.50.200:8006`. SSH `root@192.168.50.200`.

After VyOS is the router: Git `proxmox/interfaces` — drop the `vmbr1` address, add `vmbr0.10` = `10.10.10.3/24`, gateway `10.10.10.1`. UI `https://10.10.10.3:8006`. Copy and `ifreload`: `proxmox/README.md`. Topology: `README.md`. SSH key: `ssh/README.md`.

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
| adguard | LXC | 1 | 512 MiB | `vmbr-svc` | `10.10.0.53` (PLANNED) |
| tailscale-us | LXC | 1 | 512 MiB | `vmbr-svc` | `10.10.0.52` (PLANNED) |
| tailscale-india | LXC | 1 | 512 MiB | `vmbr-svc` | `10.10.0.54` (PLANNED) |

Create VM 100: `vyos/install.md`. Locked NICs/MACs: `README.md`. Policy: `vyos/config.boot`. Apply: `vyos/README.md`.

## VyOS down

Mgmt does not hairpin through the router. DHCP will be dead.

1. CBS350 port 12 (or any VLAN 10 access). Static `10.10.10.99/24`, no gateway.
2. `ping 192.168.50.200` (until VyOS) or `ping 10.10.10.3` (after) → UI or SSH.
3. `qm start 100`.

Switch dead: laptop on `nic0` tagged 10, or BMC. OS dead: BMC iKVM.
