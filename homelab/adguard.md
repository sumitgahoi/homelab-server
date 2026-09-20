# AdGuard Home — PLANNED

Not deployed. Do not treat this file as current infrastructure.

LXC on `vmbr-svc`, `10.10.0.4`, 1 vCPU / 512 MiB, SN770. Clients reach it via VyOS, not L2 on VLANs 10/20/30/40.

VyOS remains the DHCP server. AdGuard is DNS only. Later, DHCP option 6 may point appropriate clients at `10.10.0.4`. This is not an AdGuard DHCP migration.

Upstream DoT: Quad9 `tls://dns.quad9.net`, Cloudflare `tls://one.one.one.one`. Bootstrap `9.9.9.9` / `1.1.1.1`. Not ISP DNS. UI `:3000` from Trusted only.

Skip until after WAN cutover. Services already NATs to WAN. Hairpin only if this guest actually needs it — see `REQUIREMENTS.md`.

Do not point India (VLAN 40) at AdGuard. India DNS must follow the India-GW path (`tailscale-india/setup.md`), not US-side recursion.
