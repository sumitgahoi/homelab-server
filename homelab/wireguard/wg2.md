# wg2 — VLAN 40 to asus-nuc (PLANNED)

Not deployed. Dialect: rolling `2026.09.16-0028`. Do not write these
lines into `../vyos/commands.txt` until they are live and exported.
Do not create CT 109.

VyOS interface is `wg2`. This image only accepts WireGuard names
`wgN`. The NUC is Linux `wg-quick`; its interface stays `wg-india`.
The names do not have to match.

```text
wg2        10.10.82.0/30    VyOS 10.10.82.1    UDP 51822
wg-india   same prefix      NUC  10.10.82.2
```

```text
VLAN40  →  PBR table 40  →  wg2  →  NUC wg-india  →  India ISP
```

CT 108 stays up until the fail-closed tests pass. Do not flip table 40
until the handshake is proven.

The NUC has **no physical access**. Tailscale on it is the rescue path
and stays installed after `wg-india` works. Nothing in this file may
flush nftables, edit Tailscale tables, routes, exit-node settings,
ACLs, or the Tailscale service, or change the NUC default route.
`wg-india` is additive. Never set `AllowedIPs = 0.0.0.0/0` on the NUC.

```text
Data:        NJ VyOS wg2  -- WireGuard -->  NUC wg-india  -->  India WAN
Rescue:      remote device -- Tailscale -->  NUC
```

VLAN 40 must not use the rescue path. The rescue path must not depend
on `wg-india`.

`wg0` is `wg0.md`. `wg1` is `wg1.md`. Design: `README.md`.

Philosophy: one change, understand, verify, continue. No automation.

---

# Preconditions

```bash
show interfaces ethernet eth1
```

From a house client, `curl -4 https://ifconfig.me` must equal the
`eth1` IPv4 and be public (not `100.64.0.0/10`, not RFC1918).

IPv6 is usable when `eth1` has a GUA. On VyOS:

```bash
ping ipv6 2001:4860:4860::8888 count 4
```

If IPv4 is not public and there is no WAN GUA, stop. No VPS.

The NUC `Endpoint` is the A name `YOUR_DDNS`. Why it is not a
dual-stack name: `README.md`. `firewall ipv6` default-drop must
already exist (`../vyos/setup.md`).

---

# 1 — VyOS key and interface (do not change PBR yet)

```text
configure
run generate pki wireguard key-pair install interface wg2
run show interfaces wireguard wg2 public-key
```

```text
set interfaces wireguard wg2 address '10.10.82.1/30'
set interfaces wireguard wg2 description 'India site-to-site'
set interfaces wireguard wg2 port '51822'
set interfaces wireguard wg2 ipv6 address no-default-link-local
set interfaces wireguard wg2 ipv6 disable-forwarding

set interfaces wireguard wg2 peer asus-nuc public-key 'NUC_PUBLIC'
set interfaces wireguard wg2 peer asus-nuc allowed-ips '0.0.0.0/0'
```

If the key was not installed above:

```text
set interfaces wireguard wg2 private-key 'SERVER_PRIVATE_WG_INDIA'
```

`allowed-ips 0.0.0.0/0` is cryptokey only. Any IPv4 destination may be
encrypted to the NUC, and any IPv4 source from the NUC (Internet
replies) is accepted. This image does not install a route from
`allowed-ips`. There is no route knob to turn off. Do not add
`10.10.82.2/32`; `0.0.0.0/0` already covers it. The connected
`10.10.82.0/30` is what reaches the NUC address. Do not set `fwmark`
(policy routing marks table 40 itself).

The two `ipv6` lines match the VLAN VIFs: no `fe80::/64`, and
`net.ipv6.conf.wg2.forwarding=0`. IPv4 forwarding stays on. `eth1` is
untouched.

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

```bash
compare
commit-confirm 60
show ip route
```

The house default must still be `eth1` / DHCP. `confirm` and `save`
only after that is true. Do not change table 40 or forward rule 400
in this commit.

---

# 2 — NUC

Open Tailscale SSH to the NUC and leave it open. Before any routing or
firewall change, read the current state and do not write it into Git:

```bash
ip route show
ip route get 1.1.1.1
sysctl net.ipv4.ip_forward
sysctl net.ipv4.conf.all.rp_filter
nft list ruleset
```

The device on `ip route get 1.1.1.1` is `<INDIA_WAN>`. If it is
`tailscale0`, stop. Do not change this default route. Do not change
`rp_filter`. If `ip_forward` is not `1`, set
`net.ipv4.ip_forward=1` persistently, then run the check below before
continuing. Do not restart Tailscale.

After every NUC routing or firewall change, before the next one:

- the Tailscale SSH already open still responds
- a new Tailscale SSH from another device connects
- `ip route get 1.1.1.1` is still `<INDIA_WAN>`

An open session alone is not enough. If any of these fail, revert that
change while the open session still works, then stop.

`/etc/wireguard/wg-india.conf` — `Table = off`. No
`AllowedIPs = 0.0.0.0/0`.

```text
[Interface]
Address = 10.10.82.2/30
PrivateKey = NUC_PRIVATE
Table = off
PostUp = ip route replace 10.10.40.0/24 dev %i
PostUp = ip route replace 10.10.10.0/24 dev %i
PostUp = ip route replace 10.10.80.0/24 dev %i
PostDown = ip route del 10.10.40.0/24 dev %i
PostDown = ip route del 10.10.10.0/24 dev %i
PostDown = ip route del 10.10.80.0/24 dev %i

[Peer]
PublicKey = SERVER_PUBLIC_WG_INDIA
Endpoint = YOUR_DDNS:51822
AllowedIPs = 10.10.82.1/32, 10.10.40.0/24, 10.10.10.0/24, 10.10.80.0/24
PersistentKeepalive = 25
```

`Address = 10.10.82.2/30` already installs `10.10.82.0/30 dev wg-india`, so there is no host route for `10.10.82.1`. `Table = off`
installs nothing for AllowedIPs. The three `PostUp` routes are how
replies to VLAN 40, Trusted, and `wg0` re-enter the tunnel. Without
them those replies follow the India default.

`10.10.82.1/32` stays in AllowedIPs. That is cryptokey for packets
VyOS sources from `wg2`, not a route. `10.10.10.0/24` is what lets a
Trusted ping of the NUC in. Those routes do not open a VyOS forward
accept: a session the NUC starts toward Trusted or `wg0` is still
default-drop.

VyOS may send any IPv4 destination. The NUC sends only those four
prefixes back. The NUC default stays the India WAN.

Add this nftables table by itself (`nft -f` a file that contains only
this table). Do not run `nft flush ruleset`. Do not edit or replace a
Tailscale table, chain, or rules file. `policy accept` is required: a
base forward chain with `policy drop` would drop Tailscale forwarding.
Every rule is limited to packets that entered on `wg-india`.

```text
table ip wg-india {
    chain forward {
        type filter hook forward priority filter; policy accept;
        iifname "wg-india" oifname != "<INDIA_WAN>" drop
        iifname "wg-india" ip saddr != 10.10.40.0/24 drop
        iifname "wg-india" ip daddr { 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16 } drop
    }
    chain postrouting {
        type nat hook postrouting priority srcnat; policy accept;
        iifname "wg-india" ip saddr 10.10.40.0/24 oifname "<INDIA_WAN>" masquerade
    }
}
```

Substitute the real `<INDIA_WAN>` before loading. Persist the table as
its own file. If the only way to persist it is to rewrite a file that
also holds Tailscale rules, stop. Masquerade is VLAN 40 toward the
India ISP only. Do not masquerade NUC-originated traffic into the
tunnel. VyOS does not NAT `10.10.40.0/24` out `eth1`.

Load the table, then run the Tailscale check. Only then bring the
tunnel up.

```bash
wg-quick up wg-india
# enable wg-quick@wg-india only after the handshake and the check below
ip route
wg show
```

Run the Tailscale check again. If the default is no longer
`<INDIA_WAN>`, `wg-quick down wg-india` immediately from the open
Tailscale session.

The NUC must re-resolve `YOUR_DDNS` after Xfinity changes (timer or
`wg syncconf`). Stock `wg` will not.

---

# 3 — Handshake (CT 108 still serving VLAN 40)

From Trusted:

```bash
ping 10.10.82.2
```

On VyOS: `show interfaces wireguard wg2` — handshake recent.

On the NUC: `ping 10.10.82.1`. `ip route get 1.1.1.1` still via
`<INDIA_WAN>`, not `wg-india`. The Tailscale check from section 2
still passes.

Do not continue if the NUC default moved.

---

# 4 — MSS

```text
set interfaces wireguard wg2 ip adjust-mss '1380'
```

VyOS default WireGuard MTU is 1420. 1380 is that MTU minus the IPv4
TCP header. Commit with the table 40 flip, or in its own
`commit-confirm` before it. Check a large TCP transfer after the flip.

---

# 5 — Flip VLAN 40 to wg2

CT 108 still running. One commit. This removes the India-GW next-hop.
It does not fall back to `eth1`.

```text
delete protocols static table 40 route 0.0.0.0/0 next-hop 10.10.0.5
set protocols static table 40 route 0.0.0.0/0 interface 'wg2'
set protocols static table 40 route 0.0.0.0/0 blackhole distance '254'
delete firewall ipv4 forward filter rule 400 outbound-interface name 'eth2'
set firewall ipv4 forward filter rule 400 outbound-interface name 'wg2'
set firewall ipv4 forward filter rule 400 description 'India Internet via wg2'
```

`interface` is the tag under `route` (same shape as the existing
`next-hop`). Rule 390 (India must not pivot to RFC1918) stays. No
India masquerade on `eth1`. DNS still `1.1.1.1` via PBR. `eth0.40`
IPv6 lock stays. Do not set `fwmark` on `wg2`.

PBR on `eth0.40` looks up table 40. A miss falls through to main, which
is why the distance-254 blackhole stays. While `wg2` is up, `default
dev wg2` wins. A dead NUC does not take `wg2` down, so packets die in
the tunnel and do not move to `eth1`. The blackhole is the backstop
when that interface route is withdrawn. Forward rule 400 then accepts
`NET-INDIA` only out `wg2`. Rules 100/200/300 do not match India.
Packets to `10.10.40.1` (DHCP, ping of the gateway) hit the local table
and never enter table 40.

```bash
compare
commit-confirm 60
show ip route
show ip route table 40
```

Main default is `eth1`. Table 40 default is `wg2`, with the blackhole
at distance 254. Do not `confirm` until the next section. If main’s
default is `wg2`, `rollback` or wait out commit-confirm.

---

# 6 — Verify fail-closed (US WAN stays up)

From a VLAN 40 client:

- Default router `10.10.40.1`. DNS `1.1.1.1`.
- `curl -4 https://ifconfig.me` is an Indian address, not Xfinity.
- `ping 10.10.10.1`, `10.10.0.2`, `10.10.82.2` fail (rule 390).
- `dig @10.10.40.1` fails (VyOS does not recurse for VLAN 40).
- No IPv6.

Negative (US WAN still up, Tailscale left running):

- Bring `wg2` down on VyOS, or `wg-india` down on the NUC: VLAN 40
  Internet and DNS die. `tcpdump` on `eth1` must not show new VLAN 40
  sources.
- A new Tailscale SSH to the NUC still connects.
- Restore the tunnel: the path returns.

Then `confirm` and `save` on VyOS. Export `commands.txt` with the
private key omitted.

---

# 6b — Two paths, neither required by the other

Do this only after section 6 passed, including a restored `wg-india`.
From Trusted, `ssh` to `10.10.82.2` must already work. Leave that
session open. It is the way back while Tailscale is stopped. Do not
start this test with only a Tailscale session.

Stop the service. Do not uninstall, log out, or pass new `tailscale up`
flags:

```bash
systemctl stop tailscaled
```

VLAN 40 Indian egress and the `wg2` handshake still work. From the
Trusted SSH session:

```bash
systemctl start tailscaled
```

From another device, open a new Tailscale SSH. The Trusted SSH session
does not prove that. Travel India works again.

Then bring `wg-india` down on the NUC. A new Tailscale SSH still
connects. VLAN 40 Internet dies, and `eth1` still shows no new VLAN 40
sources. Bring `wg-india` back up. VLAN 40 returns. Tailscale still
connects.

If Indian egress fails while Tailscale is stopped, start `tailscaled`
from the Trusted SSH session before any other change.

---

# 7 — Retire CT 108 (only after sections 6 and 6b)

Table 40 must not mention `10.10.0.5`. Then `pct stop 108`. Repeat the
VLAN 40 Indian-egress test. If anything regresses, `pct start 108` and
put table 40 back to `10.10.0.5` / rule 400 `eth2`.

When stable, destroy CT 108 in a later sitting. Do not remove Tailscale
from the NUC.

---

# What not to do

- Do not name the VyOS interface `wg-india`. The CLI rejects it.
- Do not put `0.0.0.0/0` AllowedIPs on the NUC.
- Do not let `wg2` become the VyOS main default.
- Do not set `fwmark` on `wg2`.
- Do not NAT VLAN 40 on VyOS toward `eth1` or toward `wg2`.
- Do not masquerade on the NUC except `10.10.40.0/24` out `<INDIA_WAN>`.
- Do not run `nft flush ruleset` on the NUC, or edit Tailscale tables, routes, ACLs, exit-node settings, or the Tailscale service.
- Do not use `policy drop` on the NUC forward chain.
- Do not stop Tailscale except in section 6b, and do not leave it stopped.
- Do not change the NUC default route.
- Do not delete CT 108 before the fail-closed tests.
- Do not enable IPv6 inside `wg2`.
- Do not assign DHCPv6-PD to a LAN VIF or `eth2`.
- Do not bulk-`load` a `config.boot`.
