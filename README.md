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
| `homelab/ansible/` | Apply / disaster-recovery runbook + playbooks |
| `homelab/vyos/config.boot.j2` | Authoritative router config (Git desired state) |
| `home-theatre/` | Signal path, channels |
| `rack/` | U-layout, power |

**As-built:** house LAN is Proxmox + VyOS + CBS350. WAN is still the house ASUS (double NAT). AdGuard and Tailscale are not deployed. Git VyOS config has not yet been authoritatively applied.

Apply from `homelab/ansible/`.
