# WireGuard — PLANNED

Not deployed. WAN cutover is complete (`S33` on `nic1`, house ASUS retired). WAN IPv6 on `eth1` is CURRENT (`../vyos/setup.md`). This sitting is still separate: do not add these interfaces to `../vyos/commands.txt` until they are live. Invariants: `../REQUIREMENTS.md`. Rebuild: `setup.md`. India-GW (CT 108) stays **CURRENT** until Part C of `setup.md` is proven.

This directory is a **runbook plus design notes**. There is intentionally no automation around it.

Do not build `tailscale-us` (CT 109). Do not put WireGuard on a Services LXC. Do not add a VPS. Do not run Tailscale on VyOS.

| Interface | Prefix | Listen | Role |
|-----------|--------|--------|------|
| `wg0` | `10.10.80.0/24` | UDP `51820` | Road warrior = Trusted |
| `wg1` | `10.10.81.0/24` | UDP `51821` | Road warrior = Guest |
| `wg-india` | `10.10.82.0/30` | UDP `51822` | Site-to-site: VyOS `.1` ↔ `asus-nuc` `.2`. VLAN 40 Internet |

Inside every tunnel stays IPv4. IPv6 is only the **outer** path to `eth1` (WAN GUA). No RA on VLANs. No IPv6 inside WireGuard.

## Packet paths

```text
Phone / Mac / later Beryl
    ├─ wg0  →  Trusted (Xfinity, Services, SSH)
    └─ wg1  →  Guest (Xfinity only)

VLAN 40  10.10.40.x
    → VyOS PBR table 40
    → wg-india
    → asus-nuc
    → India ISP
    (not Tailscale, not eth1)

Travel India (phone app)
    → Tailscale → asus-nuc exit
    (not via NJ)
```

VLAN 40 clients keep gateway `10.10.40.1` and do not run Tailscale. If `wg-india` / NUC / India ISP is down, VLAN 40 is dead. No Xfinity fallback. Tailscale is **not** a VLAN 40 fallback; it is only roamers → NUC.

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

Do not put A and AAAA on the same hostname. Prefer IPv4 for phone/Beryl/`asus-nuc` `Endpoint`. Spare AAAA profile if `eth1` has a GUA.

Confirm `curl -4 https://ifconfig.me` matches the `eth1` IPv4 before publishing an A record.

## India (`wg-india`) vs India-GW

CT 108 exists because Tailscale must not run on VyOS. Native `wg-india` can replace it now that `eth1` is the public WAN.

The NUC **initiates** (India is CGNAT). `PersistentKeepalive` keeps the mapping. VyOS then sends VLAN 40 through that session.

**VyOS:** cryptokey `AllowedIPs` for the NUC peer may be `0.0.0.0/0` so any Internet dest can enter the tunnel. That must **not** become the house **main** default. Table 40 + blackhole only. Confirm with `show ip route`.

**NUC:** never `AllowedIPs = 0.0.0.0/0`. Use `Table = off` plus routes for `10.10.82.1/32`, `10.10.40.0/24`, and (if admin) `10.10.10.0/24` / `10.10.80.0/24`. The NUC default stays the India WAN. There is no physical access; a stolen default route can brick the box. Keep Tailscale running on the NUC as the management/travel path.

Do not delete CT 108 until Part C fail-closed tests pass.

## Travel / Beryl

Beryl: WireGuard to `wg0`/`wg1`, or Direct. No Tailscale on the Beryl. India while traveling = Tailscale app on the phone over Direct. See `../beryl.md`.

## Rebuild

`setup.md`: Part B `wg0`/`wg1` → Part C `wg-india` + NUC + PBR cutover. WAN IPv6 is already on `eth1`. Private keys never go in Git.

Not the same sitting as WAN cutover (done). Export `../vyos/commands.txt` after each part that is live (omit WG private keys).
