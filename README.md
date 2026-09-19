# House

Source of truth for networking, home theatre, and future gadgets.

One fact, one file. Label **CURRENT / as-built**, **Git desired state**, **PLANNED**, and **DEFERRED**. Do not mix them.

| Path | What |
|------|------|
| `agent.md` | How coding agents must work (IaC philosophy + pre-change procedure) |
| `CLAUDE.md` | Claude Code entry point → `agent.md` |
| `inventory.md` | Physical hardware catalog |
| `homelab/REQUIREMENTS.md` | Required network behavior and invariants |
| `homelab/README.md` | Current topology and wiring |
| `homelab/proxmox/interfaces` | Proxmox `/etc/network/interfaces` after VyOS is the router |
| `homelab/proxmox/interfaces.until-cutover` | Same file until cutover (`192.168.50.200` on `vmbr1` only) |
| `homelab/proxmox/README.md` | How to copy host interfaces and `ifreload` |
| `homelab/proxmox.md` | Host disks, guests, recovery |
| `homelab/vyos/config.boot` | Authoritative router config (Git desired state) |
| `homelab/vyos/README.md` | How to copy and apply `config.boot` |
| `homelab/ssh/` | Operator key and sshd drop-in; copy steps in `homelab/ssh/README.md` |
| `homelab/cbs350/running-config` | Authoritative switch config (Git desired state) |
| `homelab/cbs350/README.md` | How to copy `running-config` onto the switch |
| `home-theatre/` | Signal path, channels |
| `rack/` | U-layout, power |

**As-built:** Proxmox `192.168.50.200` on `vmbr1` (ASUS LAN) until VyOS is up; then `10.10.10.3` on `vmbr0.10`. WAN is still the house ASUS (double NAT). AdGuard and Tailscale are not deployed. Git VyOS config has not yet been authoritatively applied.

Apply host interfaces from `homelab/proxmox/README.md`. Apply VyOS from `homelab/vyos/README.md`. Operator SSH key: `homelab/ssh/README.md`.
