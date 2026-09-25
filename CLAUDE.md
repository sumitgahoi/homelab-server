# Claude Code

Follow `agent.md` for how to work in this repository.

Before any network / VyOS / Proxmox / VM 100 change:

1. `agent.md`
2. `homelab/REQUIREMENTS.md`
3. The relevant known-good snapshot (`homelab/proxmox/interfaces`, `homelab/vyos/commands.txt`, or `homelab/cbs350/running-config`) and its runbook

This repository is a human-executable runbook. Do not SSH into live gear unless the owner explicitly asks. Do not introduce Ansible, Terraform, generators, or apply scripts.

Rebuild / apply: `homelab/proxmox/README.md` (host interfaces); `homelab/vyos/setup.md` (VyOS IPv4 + WAN IPv6); `homelab/cbs350/README.md` (switch); `homelab/tailscale-india/setup.md` (India-GW, CURRENT until VyOS `wg2`); `homelab/adguard/setup.md` (AdGuard, CURRENT); `homelab/wireguard/wg0.md`, `wg1.md`, `wg2.md` (PLANNED). Travel router design: `homelab/beryl.md` (PLANNED). Topology: `homelab/README.md`. Repo map: `README.md`. Do not deploy `tailscale-us`. WAN cutover is complete. WAN IPv6 on `eth1` only.
