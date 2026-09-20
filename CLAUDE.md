# Claude Code

Follow `agent.md` for how to work in this repository.

Before any network / VyOS / Proxmox / VM 100 change:

1. `agent.md`
2. `homelab/REQUIREMENTS.md`
3. The relevant known-good snapshot (`homelab/proxmox/interfaces` or `interfaces.until-cutover`, `homelab/vyos/commands.txt`, or `homelab/cbs350/running-config`) and its runbook

This repository is a human-executable runbook. Do not SSH into live gear unless the owner explicitly asks. Do not introduce Ansible, Terraform, generators, or apply scripts.

Rebuild / apply: `homelab/proxmox/README.md` (host interfaces); `homelab/vyos/setup.md` (VyOS); `homelab/cbs350/README.md` (switch); `homelab/tailscale-india/setup.md` (India-GW). Topology: `homelab/README.md`. Repo map: `README.md`.
