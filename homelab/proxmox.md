# Proxmox

Host `10.10.10.3/24` on `vmbr0.10`, gateway `10.10.10.1`. UI `https://10.10.10.3:8006`. SSH `root@10.10.10.3`. Interfaces: `proxmox/interfaces` (copy: `proxmox/README.md`; pre-cutover archive: `interfaces.until-cutover`). Wiring: `README.md`. SSH key: `ssh/README.md`.

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
| adguard | LXC 110 | 1 | 512 MiB | `vmbr-svc` | `10.10.0.4` (CURRENT) |
| tailscale-india | LXC 108 | 1 | 512 MiB | `vmbr-svc` + TUN | `10.10.0.5` (DEPRECATED; stop, keep the disk) |
| dev | VM 101 | 4 | 16384 MiB | `vmbr0` tag 10 | `10.10.10.10` (CURRENT) |

Create VM 100: `vyos/install.md`. Then configure from `vyos/setup.md` using `vyos/commands.txt`. Locked NICs/MACs: `README.md`. UniFi OS Server: `unifi.md`. India-GW: `tailscale-india/` (DEPRECATED; VLAN 40 is `wireguard/wg2.md`). AdGuard: `adguard/setup.md` (CURRENT). Dev VM: `dev/setup.md` (CURRENT). WireGuard: `wg0`, `wg1`, and `wg2` live. Do not create CT 109.

## VyOS down

Tiers: `README.md`. Mgmt does not hairpin through the router. DHCP will be dead.

1. CBS350 port 12 (or any VLAN 10 access). Static `10.10.10.99/24`, no gateway.
2. `ping 10.10.10.3` → UI or SSH.
3. `qm start 100`.

Switch dead: laptop on `nic0` tagged 10, or BMC. OS dead: BMC iKVM.
