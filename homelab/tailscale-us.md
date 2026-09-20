# Tailscale US — PLANNED

Not deployed. Do not run Tailscale on VyOS. Do not treat this file as current infrastructure. India-GW is a different guest: `tailscale-india/setup.md`.

LXC on `vmbr-svc`, `10.10.0.3`, gateway `10.10.0.1`. Hostname `tailscale-us`. Tag `tag:homelab-sr`. Subnet router + US exit. No `--exit-node`.

Advertise from this node only: `10.10.0.0/24`, `10.10.10.0/24`. Not guest/iot/india.

One process cannot advertise a US exit and consume `asus-nuc`. Do not reuse `.2` (UniFi), `.4` (AdGuard), or `.5` (India-GW).
