# WireGuard

`wg0` is live (`wg0.md`, peer `sumit-iphone`). `wg1` is live (`wg1.md`, `10.10.81.1/24`). `wg2` is live (`wg2.md`, peer `asus-nuc`); VLAN 40 uses it. The NUC `Endpoint` is `nj.sumitgahoi.me:51822` (`../vyos/ddns.md`). WAN cutover is complete (`S33` on `nic1`, house ASUS retired). WAN IPv6 on `eth1` is CURRENT (`../vyos/setup.md`). All three are in `../vyos/commands.txt` (`private-key` is `redacted`). Invariants: `../REQUIREMENTS.md`.

| Interface | Where | Prefix | Listen | Procedure |
|-----------|--------|--------|--------|-----------|
| `wg0` | VyOS | `10.10.80.0/24` | UDP `51820` | `wg0.md` — road warrior = Trusted. Live |
| `wg1` | VyOS | `10.10.81.0/24` | UDP `51821` | `wg1.md` — road warrior = Guest. Live |
| `wg2` | VyOS | `10.10.82.0/30` | UDP `51822` | `wg2.md` — VLAN 40 → `asus-nuc` `.2`. Live |
| `wg-india` | NUC | same `/30` | — | NUC side of `wg2`. Linux `wg-quick`, not a VyOS name. Live |

VyOS rolling `2026.09.16-0028` only accepts WireGuard interface names `wgN`. `wg0`, `wg1`, and `wg2` are already up. CT 108 is deprecated. It is not the VLAN 40 path. Sections 6 and 6b passed. Stop it in `wg2.md` section 7; keep the disk. Destroy it in a later sitting.

This directory is a **runbook plus design notes**. There is intentionally no automation around it.

Do not build `tailscale-us` (CT 109). Do not put WireGuard on a Services LXC. Do not add a VPS. Do not run Tailscale on VyOS.

Inside every tunnel stays IPv4. IPv6 is only the **outer** path to `eth1` (WAN GUA). Each VyOS procedure sets `ipv6 address no-default-link-local` and `ipv6 disable-forwarding` on that interface. VyOS would otherwise add `fe80::/64` from a synthetic MAC. `disable-forwarding` is `net.ipv6.conf.<if>.forwarding=0` only. IPv4 forwarding stays on. Do not set WireGuard `fwmark`; policy routing marks table 40 itself.

`allowed-ips` on this image is cryptokey only. It does not install a kernel route.

## Packet paths

```text
Phone / Mac / later Beryl
    ├─ wg0  →  Trusted (Xfinity, Services, SSH)
    └─ wg1  →  Guest (Xfinity only)

VLAN 40  10.10.40.x
    → VyOS PBR table 40
    → wg2
    → NUC wg-india
    → India ISP
    (not Tailscale, not eth1)

Travel India (phone app)
    → Tailscale → asus-nuc exit
    (not via NJ)
```

`wg0` joins `NET-PRIVATE`. Forward rule 100 already accepts that source; no new forward rule. Internet NAT is source rule 80, outbound `eth1` only, so `wg0` → LAN is routed.

`wg1` joins `NET-GUEST`. Forward rule 200 accepts that source only out `eth1`. Other egress hits default-drop. Internet NAT is source rule 81, same `eth1` limit.

VLAN 40 clients keep gateway `10.10.40.1` and do not run Tailscale. If `wg2`, the NUC, or the India ISP is down, VLAN 40 is dead. No Xfinity fallback. Tailscale is **not** a VLAN 40 fallback; it is only roamers → NUC.

## WAN IPv6 (outer path)

House policy lives on VyOS: `../vyos/setup.md` (CURRENT — WAN IPv6 on `eth1` only).
This sitting only **uses** a WAN GUA as a spare inbound path.

Do not assign a delegated prefix to `eth0.x` or `eth2`. Do not enable RA. IPv6 forward into the house stays off. No IPv6 inside the tunnels.

| WAN you have | WireGuard listeners |
|--------------|---------------------|
| Public IPv4 and GUA | Listen on both. Default `Endpoint` is the **A** name. |
| Public IPv4, no IPv6 | A name only. |
| CGNAT / no public IPv4, GUA present | AAAA name only (client needs IPv6). |
| Neither | Dead. No VPS. |

The registered zone is `sumitgahoi.me`. WireGuard does not require split DNS names. One `Endpoint` uses whatever address the resolver returns, and many clients try AAAA first and do not fall back. The NUC has no physical access. Its `Endpoint` is the A-only name `nj.sumitgahoi.me` (VyOS DDNS, `../vyos/ddns.md`). The spare AAAA-only name is `v6.sumitgahoi.me`, a separate profile when `eth1` has a GUA. Use that profile only after a client is shown to prefer A or to fall back. Do not put an AAAA on `nj`.

## India (`wg2`) vs India-GW

CT 108 existed because Tailscale must not run on VyOS. `wg2` replaced it on the path. The LXC is deprecated. Sections 6 and 6b passed. Stop it in `wg2.md` section 7 and keep the disk. The Tailscale procedure stays in `../tailscale-india/` for a later choice, and is not current.

The NUC **initiates** (India is CGNAT). `PersistentKeepalive` keeps the mapping. VyOS then sends VLAN 40 through that session.

**VyOS:** peer `allowed-ips 0.0.0.0/0` so any Internet destination can enter the tunnel and any Internet reply source can return. That does not install a default route. Table 40 has `default dev wg2` plus a distance-254 blackhole. Main stays `eth1`. Confirm with `show ip route` and `show ip route table 40`.

**NUC:** never `AllowedIPs = 0.0.0.0/0`. `Table = off`, so wg-quick installs no AllowedIPs routes. `Address = 10.10.82.2/30` already covers `10.10.82.1`. Explicit `PostUp` routes are required for `10.10.40.0/24`, `10.10.10.0/24`, and `10.10.80.0/24` or replies follow the India default. No `PostDown`: `wg-quick` deletes the device first, and the kernel drops those routes with it. The NUC default stays the India WAN (`enp1s0`). There is no physical access; a stolen default route can brick the box. Tailscale on the NUC is the rescue path when `wg-india` is down and stays installed. From Trusted, `ssh sumit@10.10.82.2` is a second management path and needs the tunnel (`../ssh/macbook.pub`, password authentication off). `wg-india` is additive: it must not flush nftables, edit Tailscale’s tables or service, or change that default route (`wg2.md`).

NUC nftables (`table ip wg-india`, device `enp1s0`) accepts forwarded VLAN 40 only out the India WAN NIC, with `policy accept` so Tailscale forwarding is left alone. NAT for `10.10.40.0/24` is on that NIC, not on VyOS. `nftables.service` stays disabled. `wg-india-nft.service` loads the table; `wg-quick@wg-india` `Requires=` and `After=` that unit.

Sections 6 and 6b passed. CT 108 is deprecated. Stop it in `wg2.md` section 7; keep the disk. Destroy it in a later sitting.

## Travel / Beryl

Beryl: WireGuard to `wg0`/`wg1`, or Direct. No Tailscale on the Beryl. India while traveling = Tailscale app on the phone over Direct. See `../beryl.md`.

## Rebuild

`wg0`, `wg1`, and `wg2` are live and exported. WAN IPv6 is already on `eth1`. Private keys in Git are the word `redacted`. No Cloudflare API token in Git.

Not the same sitting as WAN cutover (done). Export `../vyos/commands.txt` after each interface that is live (omit WG private keys).
