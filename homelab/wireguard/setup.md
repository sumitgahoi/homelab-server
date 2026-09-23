# WireGuard Setup

WAN cutover is complete: S33 on `nic1`, house ASUS retired. WAN IPv6
on `eth1` is CURRENT (`../vyos/setup.md`). Confirm
`eth1` holds a public IPv4 and/or a WAN GUA before listening.
Do not write these lines into `../vyos/commands.txt` until they are
live and exported. Do not create CT 109. Do not run Tailscale or
WireGuard on a Services guest.

```text
Part B  wg0 / wg1  (travel → NJ)
Part C  wg-india   (VLAN 40 → asus-nuc; retire CT 108 only after tests)
```

Do **B** and **C** in either order after `eth1` is reachable. Do not
flip VLAN 40 PBR (C.5) until the NUC handshake is proven. Keep CT 108
and Tailscale on the NUC until C.6 passes.

The NUC has **no physical access**. Never set `AllowedIPs = 0.0.0.0/0`
on the NUC. Keep a Tailscale session to the NUC open for the whole of
Part C.

Philosophy: one change, understand, verify, continue. No automation.

---

# Fixed Addresses

```text
wg0 private:   10.10.80.0/24     10.10.80.1    UDP 51820
wg1 guest:     10.10.81.0/24     10.10.81.1    UDP 51821
wg-india:      10.10.82.0/30     VyOS .1  NUC .2    UDP 51822

Do not use:
  10.10.0.0/24     Services
  10.10.10.0/24    Trusted
  10.10.20.0/24    Guest Wi-Fi
  10.10.30.0/24    IoT
  10.10.40.0/24    India clients
```

---

# Preconditions (all parts)

```bash
show interfaces ethernet eth1
```

From a house client:

```bash
curl -4 https://ifconfig.me
```

IPv4 is usable when that equals the `eth1` IPv4 and is public (not
`100.64.0.0/10`, not RFC1918).

IPv6 is usable when `eth1` has a GUA. Check **on VyOS**:

```bash
ping ipv6 2001:4860:4860::8888 count 4
```

If IPv4 is not public and there is no WAN GUA, stop. No VPS.

DDNS (provider not locked):

```text
YOUR_DDNS     A only      ← default Endpoint (NUC, phone, Beryl)
YOUR_DDNS6    AAAA only   ← spare
```

Do not put A and AAAA on the same name.

WAN IPv6 lock, `firewall ipv6` default-drop, and `autoconf` + `dhcpv6`
on `eth1`: `../vyos/setup.md` (CURRENT).

---

# Part B — wg0 / wg1 (travel → NJ)

## B.1 — Keys

```text
generate pki wireguard key-pair
```

Server private keys stay on the router. Never commit them. Clients
generate their own pairs.

## B.2 — Interfaces

```text
set interfaces wireguard wg0 address '10.10.80.1/24'
set interfaces wireguard wg0 description 'VPN private (Trusted)'
set interfaces wireguard wg0 port '51820'
set interfaces wireguard wg0 private-key 'SERVER_PRIVATE_WG0'
set interfaces wireguard wg0 ipv6 address no-default-link-local
set interfaces wireguard wg0 ipv6 disable-forwarding

set interfaces wireguard wg1 address '10.10.81.1/24'
set interfaces wireguard wg1 description 'VPN guest'
set interfaces wireguard wg1 port '51821'
set interfaces wireguard wg1 private-key 'SERVER_PRIVATE_WG1'
set interfaces wireguard wg1 ipv6 address no-default-link-local
set interfaces wireguard wg1 ipv6 disable-forwarding
```

If this rolling image wants `pki wireguard` instead of
`private-key` on the interface, use that form.

## B.3 — Example peers

```text
set interfaces wireguard wg0 peer macbook public-key 'PEER_PUB'
set interfaces wireguard wg0 peer macbook allowed-ips '10.10.80.2/32'

set interfaces wireguard wg1 peer guest-phone public-key 'PEER_PUB'
set interfaces wireguard wg1 peer guest-phone allowed-ips '10.10.81.2/32'
```

No keepalive on VyOS. Clients use `PersistentKeepalive = 25`.

## B.4 — Groups

```text
set firewall group network-group NET-PRIVATE network '10.10.80.0/24'
set firewall group network-group NET-GUEST network '10.10.81.0/24'
set firewall group network-group NET-CLIENT network '10.10.80.0/24'
set firewall group network-group NET-CLIENT network '10.10.81.0/24'
set firewall group interface-group IF-INTERNAL interface 'wg0'
set firewall group interface-group IF-INTERNAL interface 'wg1'
```

## B.5 — WAN UDP (IPv4 and IPv6)

```text
set firewall ipv4 input filter rule 50 action 'accept'
set firewall ipv4 input filter rule 50 description 'WireGuard private'
set firewall ipv4 input filter rule 50 destination port '51820'
set firewall ipv4 input filter rule 50 inbound-interface name 'eth1'
set firewall ipv4 input filter rule 50 protocol 'udp'

set firewall ipv4 input filter rule 51 action 'accept'
set firewall ipv4 input filter rule 51 description 'WireGuard guest'
set firewall ipv4 input filter rule 51 destination port '51821'
set firewall ipv4 input filter rule 51 inbound-interface name 'eth1'
set firewall ipv4 input filter rule 51 protocol 'udp'

set firewall ipv6 input filter rule 50 action 'accept'
set firewall ipv6 input filter rule 50 description 'WireGuard private'
set firewall ipv6 input filter rule 50 destination port '51820'
set firewall ipv6 input filter rule 50 inbound-interface name 'eth1'
set firewall ipv6 input filter rule 50 protocol 'udp'
set firewall ipv6 input filter rule 51 action 'accept'
set firewall ipv6 input filter rule 51 description 'WireGuard guest'
set firewall ipv6 input filter rule 51 destination port '51821'
set firewall ipv6 input filter rule 51 inbound-interface name 'eth1'
set firewall ipv6 input filter rule 51 protocol 'udp'
```

`firewall ipv6` default-drop must already exist (`../vyos/setup.md`).
If there is no GUA, the IPv6 rules are inert until one appears.

## B.6 — NAT and DNS

```text
set nat source rule 80 description 'NAT WG private to WAN'
set nat source rule 80 outbound-interface name 'eth1'
set nat source rule 80 source address '10.10.80.0/24'
set nat source rule 80 translation address 'masquerade'

set nat source rule 81 description 'NAT WG guest to WAN'
set nat source rule 81 outbound-interface name 'eth1'
set nat source rule 81 source address '10.10.81.0/24'
set nat source rule 81 translation address 'masquerade'

set service dns forwarding allow-from '10.10.80.0/24'
set service dns forwarding allow-from '10.10.81.0/24'
set service dns forwarding listen-address '10.10.80.1'
set service dns forwarding listen-address '10.10.81.1'
```

```bash
compare
commit-confirm 60
```

Do not `confirm` until B.8.

## B.7 — Client configs

`YOUR_DDNS` is the A name.

Private:

```text
[Interface]
Address = 10.10.80.2/32
PrivateKey = CLIENT_PRIVATE
DNS = 10.10.80.1

[Peer]
PublicKey = SERVER_PUBLIC_WG0
Endpoint = YOUR_DDNS:51820
AllowedIPs = 0.0.0.0/0
PersistentKeepalive = 25
```

Guest: `10.10.81.2/32`, `DNS = 10.10.81.1`, port `51821`,
`SERVER_PUBLIC_WG1`.

Spare AAAA profile: same keys, `YOUR_DDNS6`. Do not put client private
keys in Git.

## B.8 — Verify (off the house LAN)

`wg0`: handshake; DNS at `10.10.80.1`; Xfinity egress; reach
`10.10.0.2` and `10.10.10.1`; `dig @10.10.0.4` fails.

`wg1`: handshake; DNS at `10.10.81.1`; Xfinity egress; no
`10.10.10.1` / `10.10.0.2` / SSH / `10.10.0.4`.

VLAN 40 must still use CT 108 until Part C.

```text
confirm
save
```

---

# Part C — wg-india (VLAN 40 → asus-nuc)

CT 108 stays up until C.6. Tailscale stays on the NUC forever (travel +
remote hands).

```text
VLAN40  →  PBR table 40  →  wg-india  →  NUC  →  India ISP
```

## C.1 — VyOS keys and interface (do not change PBR yet)

Generate a third key pair for `wg-india`.

```text
set interfaces wireguard wg-india address '10.10.82.1/30'
set interfaces wireguard wg-india description 'India site-to-site'
set interfaces wireguard wg-india port '51822'
set interfaces wireguard wg-india private-key 'SERVER_PRIVATE_WG_INDIA'
set interfaces wireguard wg-india ipv6 address no-default-link-local
set interfaces wireguard wg-india ipv6 disable-forwarding

set interfaces wireguard wg-india peer asus-nuc public-key 'NUC_PUBLIC'
set interfaces wireguard wg-india peer asus-nuc allowed-ips '0.0.0.0/0'
set interfaces wireguard wg-india peer asus-nuc allowed-ips '10.10.82.2/32'
```

`0.0.0.0/0` here is **cryptokey routing** (VLAN 40 dests may be any
Internet address). Inspect the **main** table immediately:

```bash
compare
commit-confirm 60
show ip route
```

The house default must still be `eth1` / DHCP. If `0.0.0.0/0` via
`wg-india` appeared in **main**, remove that route (this rolling
image’s knob may be `route-allowed-ips` or an equivalent — use whatever
stops AllowedIPs from installing into main). Table 40 is the only place
a default via `wg-india` is allowed.

```text
set firewall ipv4 input filter rule 52 action 'accept'
set firewall ipv4 input filter rule 52 description 'WireGuard India'
set firewall ipv4 input filter rule 52 destination port '51822'
set firewall ipv4 input filter rule 52 inbound-interface name 'eth1'
set firewall ipv4 input filter rule 52 protocol 'udp'

set firewall ipv6 input filter rule 52 action 'accept'
set firewall ipv6 input filter rule 52 description 'WireGuard India'
set firewall ipv6 input filter rule 52 destination port '51822'
set firewall ipv6 input filter rule 52 inbound-interface name 'eth1'
set firewall ipv6 input filter rule 52 protocol 'udp'
```

Do **not** yet change table 40 or rule 400. `confirm` + `save` only
after main still uses `eth1`.

## C.2 — NUC (narrow, additive)

Open Tailscale SSH (or equivalent) to the NUC and **leave it open**.

On the NUC:

```bash
sysctl net.ipv4.ip_forward
# expect 1, or set persist: net.ipv4.ip_forward=1
sysctl net.ipv4.conf.all.rp_filter
# prefer 2 (loose), same as CT 108
```

`/etc/wireguard/wg-india.conf` — **Table = off**. **No**
`AllowedIPs = 0.0.0.0/0`.

```text
[Interface]
Address = 10.10.82.2/30
PrivateKey = NUC_PRIVATE
Table = off
PostUp = ip route replace 10.10.82.1/32 dev %i
PostUp = ip route replace 10.10.40.0/24 dev %i
PostUp = ip route replace 10.10.10.0/24 dev %i
PostUp = ip route replace 10.10.80.0/24 dev %i
PostDown = ip route del 10.10.82.1/32 dev %i
PostDown = ip route del 10.10.40.0/24 dev %i
PostDown = ip route del 10.10.10.0/24 dev %i
PostDown = ip route del 10.10.80.0/24 dev %i

[Peer]
PublicKey = SERVER_PUBLIC_WG_INDIA
Endpoint = YOUR_DDNS:51822
AllowedIPs = 10.10.82.1/32, 10.10.40.0/24, 10.10.10.0/24, 10.10.80.0/24
PersistentKeepalive = 25
```

The NUC default must remain the India WAN. `ip route` default must not
point at `wg-india`.

nftables **add** (do not wipe Tailscale’s tables):

```text
table ip wg-india {
    chain forward {
        type filter hook forward priority filter; policy drop;
        ct state established,related accept
        iifname "wg-india" oifname != "wg-india" ip saddr 10.10.40.0/24 ip daddr != { 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16 } accept
    }
    chain postrouting {
        type nat hook postrouting priority srcnat; policy accept;
        ip saddr 10.10.40.0/24 oifname != "wg-india" masquerade
    }
}
```

Replace `oifname != "wg-india"` with the real WAN interface name if you
know it (`eth0`, etc.). Masquerade VLAN 40 toward India Internet only.
Do not masquerade NUC-originated traffic into the tunnel.

```bash
wg-quick up wg-india
# or systemd wg-quick@wg-india — enable only after C.3
ip route
# default still India WAN
wg show
```

If default flipped to the tunnel: `wg-quick down wg-india` immediately.
Use the still-open Tailscale session.

The NUC must re-resolve `YOUR_DDNS` after Xfinity changes (timer or
`wg syncconf`). Stock `wg` will not.

## C.3 — Handshake (CT 108 still serving VLAN 40)

From Trusted:

```bash
ping 10.10.82.2
```

On VyOS: `show interfaces wireguard wg-india` — handshake recent.

On the NUC: `ping 10.10.82.1`. `ip route get 8.8.8.8` still via India
WAN, not `wg-india`.

Do not continue if the NUC default moved.

## C.4 — MSS (optional but expected)

Clamp TCP MSS on `wg-india` (~1380) using this rolling image’s
`adjust-mss` / firewall modify form. Verify large TCP after C.5.

## C.5 — Flip VLAN 40 to wg-india

CT 108 still running. One commit.

```text
delete protocols static table 40 route 0.0.0.0/0 next-hop 10.10.0.5
set protocols static table 40 route 0.0.0.0/0 interface 'wg-india'
set protocols static table 40 route 0.0.0.0/0 blackhole distance '254'
delete firewall ipv4 forward filter rule 400 outbound-interface name 'eth2'
set firewall ipv4 forward filter rule 400 outbound-interface name 'wg-india'
set firewall ipv4 forward filter rule 400 description 'India Internet via wg-india'
```

Rule 390 (India must not pivot to RFC1918) stays. No India masquerade
on `eth1`. DNS still `1.1.1.1` via PBR. `eth0.40` IPv6 lock stays.

```bash
compare
commit-confirm 60
```

Do not `confirm` until C.6.

If the house default in **main** is now `wg-india`, **`rollback`** /
wait out commit-confirm. Fix C.1 before retrying.

## C.6 — Verify fail-closed (US WAN stays up)

From a VLAN 40 client:

- Default router `10.10.40.1`. DNS `1.1.1.1`.
- `curl -4 https://ifconfig.me` is an **Indian** address, not Xfinity.
- `ping 10.10.10.1`, `10.10.0.2`, `10.10.82.2` fail (rule 390).
- `dig @10.10.40.1` fails (VyOS does not recurse for VLAN 40).
- No IPv6.

Negative (US WAN still up):

- Bring `wg-india` down on VyOS **or** on the NUC: VLAN 40 Internet and
  DNS die. `tcpdump` on `eth1` must not show new VLAN 40 sources.
- Restore tunnel: path returns.
- Stop Tailscale on the NUC: VLAN 40 **stays up** (WG path). Travel
  India app dies.
- Restore Tailscale.

Then `confirm` + `save` on VyOS. Export `commands.txt` (no private
keys).

## C.7 — Retire CT 108 (only after C.6)

On VyOS, table 40 must not mention `10.10.0.5`. Then stop CT 108
(`pct stop 108`). Repeat the VLAN 40 Indian-egress test. If anything
regresses, `pct start 108` and put table 40 back to `10.10.0.5` /
rule 400 `eth2`.

When stable, destroy CT 108 in a later sitting. Do not remove Tailscale
from the NUC.

---

# What not to do

- Do not listen until `eth1` IPv4 matches `curl -4 https://ifconfig.me` (or a GUA for the AAAA profile).
- Do not create CT 109 / `tailscale-us`.
- Do not add a VPS.
- Do not run Tailscale on VyOS.
- Do not put `0.0.0.0/0` AllowedIPs on the NUC.
- Do not let `wg-india` become the VyOS **main** default.
- Do not delete CT 108 before C.6.
- Do not enable IPv6 inside any WG interface.
- Do not assign DHCPv6-PD to a LAN VIF or `eth2`.
- Do not put A and AAAA on the same DDNS name.
- Do not put guest keys on `wg0` or the NUC on `wg0`.
- Do not add Services→Trusted forward rules for this.
- Do not bulk-`load` a `config.boot`.
