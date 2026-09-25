# wg2 — VLAN 40 to asus-nuc

Live. Table 40 sends VLAN 40 out `wg2`. CT 108 is still installed and
is not the path. Dialect: rolling `2026.09.16-0028`. Do not create
CT 109.

`../vyos/commands.txt` and `../vyos/config.boot` include this
interface. Private keys there are the word `redacted`. Configure-mode
`show interfaces wireguard wg2` prints the real `private-key`. Do not
copy that into Git. Public key only: `run show interfaces wireguard
wg2 public-key`.

VyOS interface is `wg2`. This image only accepts WireGuard names
`wgN`. The NUC is Linux `wg-quick`; its interface stays `wg-india`.

```text
wg2        10.10.82.0/30    VyOS 10.10.82.1    UDP 51822
wg-india   same prefix      NUC  10.10.82.2    enp1s0
```

```text
VLAN40  →  PBR table 40  →  wg2  →  NUC wg-india  →  enp1s0  →  India ISP
```

VyOS public key: `tivxWfTuzjHO6cRSmt1EgXzP7iGbr5PCuPSvg3kzK08=`

NUC public key: `OGvgDsSGw2D3Y+O+DPL7U9TYGfUSPfG1y9kBIlH3jmk=`

Peer name on VyOS: `asus-nuc`. VyOS peer `allowed-ips 0.0.0.0/0`.
IPv6 on `wg2` is off. MSS clamp is `1380`. IPv4 and IPv6 input rule 52 accept
UDP `51822` on `eth1`.
Main default stays `eth1` (`0.0.0.0/0` via `73.195.208.1`).

The NUC has **no physical access**. Tailscale on it is the rescue path
and stays installed. Nothing here may flush nftables, edit Tailscale
tables, routes, exit-node settings, ACLs, or the Tailscale service, or
change the NUC default route. `wg-india` is additive. Never set
`AllowedIPs = 0.0.0.0/0` on the NUC.

```text
Data:        NJ VyOS wg2  -- WireGuard -->  NUC wg-india  -->  India WAN
House admin: Trusted -- ssh sumit@10.10.82.2 --> wg2 --> NUC
Rescue:      remote device -- Tailscale -->  NUC
```

Trusted SSH uses the same MacBook Ed25519 key as Proxmox
(`../ssh/macbook.pub`). Password authentication on the NUC stays off.
That session needs `wg2`. It is how section 6b gets back while
Tailscale is stopped. It does not help when `wg-india` is down: fix a
dead handshake from Tailscale. VLAN 40 must not use either management
path. The Tailscale path must not depend on `wg-india`.

`wg0` is `wg0.md`. `wg1` is `wg1.md`. Design: `README.md`.

Philosophy: one change, understand, verify, continue. No automation.

---

# As-built (first deployment)

NUC `Endpoint` is the literal address `73.195.208.10:51822` (the
`eth1` IPv4 at deploy time). It is not a name. Xfinity can change it;
when it does, the handshake dies and VLAN 40 fails closed. Trusted SSH
to `10.10.82.2` dies with it. Fix the `Endpoint` from Tailscale.
Cloudflare DDNS and `Endpoint =
sumitgahoi.me:51822` are follow-up. Do not publish a dual-stack name.
Why the steady name is A-only: `README.md`.

Table 40:

```text
S>* 0.0.0.0/0 [1/0] is directly connected, wg2
S   0.0.0.0/0 [254/0] unreachable (blackhole)
```

Forward rule 400 is outbound `wg2`, description `India Internet via wg2`.
The `next-hop 10.10.0.5` route is gone.

NUC default route: `192.168.88.1 dev enp1s0 src 192.168.88.2`.
India WAN NIC in the nft table is `enp1s0`.

Observed from a VLAN 40 phone (`10.10.40.102`): IPv4 egress
`49.205.85.10`. That India address can change; the check is that it is
not the Xfinity address `73.195.208.10`. `https://10.10.10.3:8006` and
`http://10.10.0.4` did not load. `wg-quick down wg-india` killed VLAN
40 Internet, did not send it out `eth1`, and left a new Tailscale SSH
working. The phone offered cellular; that is the phone, not a house
leak. `wg-quick up` restored Indian egress. A NUC reboot brought back
Tailscale, `wg-india` (handshake and both directions), the nft table,
and the same default route.

`ping 10.10.82.1` from the NUC does not get a reply. A Trusted ping of
`10.10.82.2` does (~222 ms on the first test).

Sections 6 and 6b passed. With `tailscaled` stopped, VLAN 40 still
egressed through `wg2`, and `tailscaled` was started again from Trusted
SSH. With `wg-india` down, VLAN 40 died, nothing new left via `eth1`,
and a new Tailscale SSH still connected.

From Trusted, `ssh sumit@10.10.82.2` works. The NUC `authorized_keys`
has the MacBook Ed25519 key (`../ssh/macbook.pub`). Password
authentication stays disabled. VLAN 40 still cannot open that address
(rule 390). When `wg-india` is down, this SSH dies with the tunnel;
Tailscale is the session that still connects.

---

# Still open

- Cloudflare DDNS so `sumitgahoi.me` is an A record of the `eth1` IPv4,
  then change the NUC `Endpoint` off the literal address. No API token
  in Git.
- CT 108 is still present. Sections 6 and 6b passed, so section 7 may
  stop it. Do not destroy it in that same sitting.

---

# Preconditions

```bash
show interfaces ethernet eth1
```

From a house client, `curl -4 https://ifconfig.me` must equal the
`eth1` IPv4 and be public (not `100.64.0.0/10`, not RFC1918). Write
that address down. The NUC `Endpoint` has to use it until DDNS exists.

IPv6 is usable when `eth1` has a GUA. On VyOS:

```bash
ping ipv6 2001:4860:4860::8888 count 4
```

If IPv4 is not public and there is no WAN GUA, stop. No VPS.

`firewall ipv6` default-drop must already exist (`../vyos/setup.md`).

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

The device on `ip route get 1.1.1.1` is `enp1s0`. If it is
`tailscale0`, stop. Do not change this default route. Do not change
`rp_filter`. If `ip_forward` is not `1`, set
`net.ipv4.ip_forward=1` persistently, then run the check below before
continuing. Do not restart Tailscale. Do not enable `nftables.service`.

After every NUC routing or firewall change, before the next one:

- the Tailscale SSH already open still responds
- a new Tailscale SSH from another device connects
- `ip route get 1.1.1.1` is still `enp1s0`

An open session alone is not enough. If any of these fail, revert that
change while the open session still works, then stop.

`/etc/wireguard/wg-india.conf` — `Table = off`. No
`AllowedIPs = 0.0.0.0/0`. No `PostDown`.

```text
[Interface]
Address = 10.10.82.2/30
PrivateKey = NUC_PRIVATE
Table = off
PostUp = ip route replace 10.10.40.0/24 dev %i
PostUp = ip route replace 10.10.10.0/24 dev %i
PostUp = ip route replace 10.10.80.0/24 dev %i

[Peer]
PublicKey = SERVER_PUBLIC_WG_INDIA
Endpoint = 73.195.208.10:51822
AllowedIPs = 10.10.82.1/32, 10.10.40.0/24, 10.10.10.0/24, 10.10.80.0/24
PersistentKeepalive = 25
```

Replace the `Endpoint` address if `eth1` has moved. Until DDNS, this
is a literal, not `sumitgahoi.me`.

`Address = 10.10.82.2/30` already installs `10.10.82.0/30 dev wg-india`,
so there is no host route for `10.10.82.1`. `Table = off` installs
nothing for AllowedIPs. The three `PostUp` routes are how replies to
VLAN 40, Trusted, and `wg0` re-enter the tunnel. Without them those
replies follow the India default.

Do not add `PostDown` route deletes. `wg-quick down` runs `PostDown`
only after `ip link delete`. Deleting the device already removes its
routes. The old lines then failed:

```text
[#] ip link delete dev wg-india
[#] ip route del 10.10.40.0/24 dev wg-india
Cannot find device "wg-india"
```

`ip route del 10.10.40.0/24` without `dev` fails the same way
(`No such process`): the route is already gone. With the `PostDown`
lines removed, `wg-quick down wg-india` ends at `ip link delete`.

`10.10.82.1/32` stays in AllowedIPs. That is cryptokey for packets
VyOS sources from `wg2`, not a route. `10.10.10.0/24` is what lets a
Trusted ping of the NUC in. Those routes do not open a VyOS forward
accept: a session the NUC starts toward Trusted or `wg0` is still
default-drop.

VyOS may send any IPv4 destination. The NUC sends only those four
prefixes back. The NUC default stays the India WAN
(`192.168.88.1 dev enp1s0`).

## nftables

Add this table by itself. Do not run `nft flush ruleset`. Do not edit
or replace a Tailscale table, chain, or rules file. `policy accept` is
required: a base forward chain with `policy drop` would drop Tailscale
forwarding. Every rule is limited to packets that entered on
`wg-india`.

`/etc/nftables-wg-india.conf`:

```text
table ip wg-india {
    chain forward {
        type filter hook forward priority filter; policy accept;

        iifname "wg-india" oifname != "enp1s0" drop
        iifname "wg-india" ip saddr != 10.10.40.0/24 drop
        iifname "wg-india" ip daddr { 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16 } drop
    }

    chain postrouting {
        type nat hook postrouting priority srcnat; policy accept;

        iifname "wg-india" ip saddr 10.10.40.0/24 oifname "enp1s0" masquerade
    }
}
```

`enp1s0` is the India WAN from `ip route get 1.1.1.1`. If that device
name is different on a rebuild, substitute it before loading, then
stop if it is `tailscale0`. Masquerade is VLAN 40 toward the India ISP
only. Do not masquerade NUC-originated traffic into the tunnel. VyOS
does not NAT `10.10.40.0/24` out `eth1`.

These rules do not cover packets the NUC itself originates. They cover
forwarded packets that arrived on `wg-india`.

Do not enable `nftables.service`. A general reload can flush the
ruleset and take Tailscale's iptables-nft tables with it. Leave that
unit disabled. Persist only this table:

`/etc/systemd/system/wg-india-nft.service`

```text
[Unit]
Description=WireGuard India nftables rules
Before=wg-quick@wg-india.service

[Service]
Type=oneshot
ExecStartPre=-/usr/sbin/nft delete table ip wg-india
ExecStart=/usr/sbin/nft -f /etc/nftables-wg-india.conf
RemainAfterExit=yes

[Install]
WantedBy=multi-user.target
```

The leading `-` on `ExecStartPre` ignores a missing table. On a fresh
boot the log says `No such file or directory` for that delete; that is
the expected nft error, and the following `nft -f` creates the table.
If the table already exists, the delete removes only `table ip
wg-india`, then `nft -f` recreates it. Do not drop the `-`. Without
it, boot fails on the first delete.

`RemainAfterExit=yes` is required. A oneshot that exits inactive would
not satisfy the `Requires=` below.

`/etc/systemd/system/wg-quick@wg-india.service.d/override.conf`

```text
[Unit]
Requires=wg-india-nft.service
After=wg-india-nft.service
```

`Requires=` plus `After=` is the relationship to keep. `wg-quick` must
not come up if the guardrail unit failed: with `ip_forward=1` and no
`wg-india` table, VLAN 40 would forward out whatever route wins.
`Wants=` alone would start the tunnel anyway. `Before=` on the nft
unit is only ordering; the drop-in is what pulls the nft unit in and
fails the tunnel when the nft unit fails.

```bash
systemctl daemon-reload
systemctl enable --now wg-india-nft.service
systemctl enable wg-quick@wg-india.service
```

Confirm the drop-in is actually in effect:

```bash
systemctl show wg-quick@wg-india.service -p After -p Requires
```

Both lines must list `wg-india-nft.service`. Then:

```bash
systemctl start wg-quick@wg-india.service
ip route
wg show wg-india
nft list table ip wg-india
ip route get 1.1.1.1
```

Run the Tailscale check again. If the default is no longer `enp1s0`,
`wg-quick down wg-india` immediately from the open Tailscale session.

Restarting `wg-india-nft.service` is not a harmless reload. `delete`
and `nft -f` are two transactions, so the table is absent for a
moment. `Requires=` also means stopping that unit stops
`wg-quick@wg-india`, and a restart does not always start it again.
After any nft restart, check both:

```bash
systemctl is-active wg-quick@wg-india.service
wg show wg-india
nft list table ip wg-india
```

If the tunnel is inactive, `systemctl start wg-quick@wg-india.service`.
Do not `systemctl restart wg-india-nft.service` as a routine edit.

The literal `Endpoint` does not track Xfinity. After DDNS, the NUC
must re-resolve `sumitgahoi.me` (timer or `wg syncconf`). Stock `wg`
will not.

---

# 3 — Handshake (CT 108 still installed, not serving VLAN 40)

From Trusted:

```bash
ping 10.10.82.2
```

On VyOS: `show interfaces wireguard wg2` — handshake recent.

On the NUC: `ip route get 1.1.1.1` still via `enp1s0`, not `wg-india`.
The Tailscale check from section 2 still passes.

Do not continue if the NUC default moved.

`ping 10.10.82.1` from the NUC is expected to fail, and that failure
is not a broken tunnel. VyOS IPv4 input default-action is `drop`.
ICMP rule 30 accepts ICMP only from `IF-INTERNAL` (`eth0.10`,
`eth0.20`, `eth0.30`, `eth0.40`, `eth2`). `wg2` is not in that group.
`tcpdump interface wg2` shows `10.10.82.2 > 10.10.82.1: ICMP echo
request` and no answer. Do not add `wg2` to `IF-INTERNAL` to make this
ping work. That group means "house interfaces." Today it only gates
ICMP, and a later rule that uses the group would open the tunnel into
the house. The liveness checks are the handshake, Trusted
`ping 10.10.82.2`, and VLAN 40 egress. A separate ICMP accept from
`10.10.82.2` on `wg2` is unnecessary.

Input rule 1000 accepts every source in `NET-PRIVATE` and does not
match the inbound interface. Forward rule 100 does the same for
forwarded traffic. `allowed-ips 0.0.0.0/0` will decrypt a packet the
NUC injects with a spoofed house source. The NUC nft table does not
see packets the NUC originates. Rules 5–7 drop those sources when they
arrive on `wg2`, before the established-state accept. `NET-CLIENT` on
the router includes `10.10.10.0/24`, `10.10.20.0/24`, `10.10.30.0/24`,
`10.10.80.0/24`, and `10.10.81.0/24`, so rule 5 also covers `wg0` and
`wg1`. Replies stay allowed: they are sourced from `10.10.82.2` or
from a public address. After this commit, Trusted `ping` / `ssh` to
`10.10.82.2` and VLAN 40 Indian egress still worked. Do not add `wg2`
to `IF-INTERNAL`.

```text
set firewall ipv4 forward filter rule 5 action 'drop'
set firewall ipv4 forward filter rule 5 description 'client sources arriving on wg2'
set firewall ipv4 forward filter rule 5 inbound-interface name 'wg2'
set firewall ipv4 forward filter rule 5 source group network-group 'NET-CLIENT'

set firewall ipv4 forward filter rule 6 action 'drop'
set firewall ipv4 forward filter rule 6 description 'services sources arriving on wg2'
set firewall ipv4 forward filter rule 6 inbound-interface name 'wg2'
set firewall ipv4 forward filter rule 6 source group network-group 'NET-SERVICES'

set firewall ipv4 forward filter rule 7 action 'drop'
set firewall ipv4 forward filter rule 7 description 'india sources arriving on wg2'
set firewall ipv4 forward filter rule 7 inbound-interface name 'wg2'
set firewall ipv4 forward filter rule 7 source group network-group 'NET-INDIA'

set firewall ipv4 input filter rule 5 action 'drop'
set firewall ipv4 input filter rule 5 description 'client sources arriving on wg2'
set firewall ipv4 input filter rule 5 inbound-interface name 'wg2'
set firewall ipv4 input filter rule 5 source group network-group 'NET-CLIENT'

set firewall ipv4 input filter rule 6 action 'drop'
set firewall ipv4 input filter rule 6 description 'services sources arriving on wg2'
set firewall ipv4 input filter rule 6 inbound-interface name 'wg2'
set firewall ipv4 input filter rule 6 source group network-group 'NET-SERVICES'

set firewall ipv4 input filter rule 7 action 'drop'
set firewall ipv4 input filter rule 7 description 'india sources arriving on wg2'
set firewall ipv4 input filter rule 7 inbound-interface name 'wg2'
set firewall ipv4 input filter rule 7 source group network-group 'NET-INDIA'
```

---

# 4 — MSS

```text
set interfaces wireguard wg2 ip adjust-mss '1380'
```

VyOS default WireGuard MTU is 1420. 1380 is that MTU minus the IPv4
TCP header. Commit with the table 40 flip, or in its own
`commit-confirm` before it. Check a large TCP transfer after the flip.
This is already set on the live router.

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
`next-hop`). Rule 390 (India must not pivot to RFC1918) stays. Its
description in `commands.txt` still says `via India-GW`. The PBR
description still says `uses India-GW`. Wording only.
No India masquerade on `eth1`. DNS still `1.1.1.1` via PBR. `eth0.40`
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
- `curl -4 https://ifconfig.me` is an Indian address, not Xfinity
  (`73.195.208.10` at the first deploy).
- `ping 10.10.10.1`, `10.10.0.2`, `10.10.82.2` fail (rule 390).
- `https://10.10.10.3:8006` and `http://10.10.0.4` do not load.
- `dig @10.10.40.1` fails (VyOS does not recurse for VLAN 40).
- No IPv6.

Negative (US WAN still up, Tailscale left running):

- `wg-quick down wg-india` on the NUC, or bring `wg2` down on VyOS:
  VLAN 40 Internet and DNS die. `tcpdump` on `eth1` must not show new
  VLAN 40 sources. A phone may offer cellular; that is not egress
  through Xfinity.
- A new Tailscale SSH to the NUC still connects.
- Restore the tunnel: the path returns.

Then `confirm` and `save` on VyOS. The export is in `commands.txt`,
with each `private-key` set to `redacted`.

---

# 6b — Two paths, neither required by the other

Passed on the first deploy. On a rebuild, do this only after section 6,
including a restored `wg-india`. From Trusted, open
`ssh sumit@10.10.82.2` and leave it open. It is the way back while
Tailscale is stopped. Do not start this test with only a Tailscale
session. Key: `../ssh/macbook.pub`. Password authentication stays off.

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
connects. Both halves passed on the first deploy, and `tailscaled` was
left running.

If Indian egress fails while Tailscale is stopped, start `tailscaled`
from the Trusted SSH session before any other change.

---

# 7 — Retire CT 108 (only after sections 6 and 6b)

Those sections passed. This one has not. Table 40 already does not
mention `10.10.0.5`. The LXC is still there. `pct stop 108`. Repeat the
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
- Do not masquerade on the NUC except `10.10.40.0/24` out `enp1s0`.
- Do not add `PostDown` route deletes for those prefixes.
- Do not run `nft flush ruleset` on the NUC, or edit Tailscale tables, routes, ACLs, exit-node settings, or the Tailscale service.
- Do not enable `nftables.service` on the NUC.
- Do not use `policy drop` on the NUC forward chain.
- Do not stop Tailscale except in section 6b, and do not leave it stopped.
- Do not change the NUC default route.
- Do not destroy CT 108 in the same sitting as `pct stop 108`.
- Do not add `wg2` to `IF-INTERNAL`.
- Do not enable IPv6 inside `wg2`.
- Do not assign DHCPv6-PD to a LAN VIF or `eth2`.
- Do not put a WireGuard private key or a Cloudflare API token in Git.
- Do not bulk-`load` a `config.boot`.
- Do not point the NUC `Endpoint` at a name that has an AAAA.
