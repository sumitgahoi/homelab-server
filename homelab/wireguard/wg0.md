# wg0 — Trusted WireGuard

Live. First peer: `sumit-iphone`, `10.10.80.2/32`, proved from cellular.
Dialect: rolling `2026.09.16-0028`. Export into `../vyos/commands.txt`
only from the router, with the private key omitted. Do not create
CT 109. Do not run WireGuard on a Services guest.

`wg1` is `wg1.md`. India is `wg2.md`. Design: `README.md`.

```text
wg0   10.10.80.0/24   10.10.80.1   UDP 51820
```

Do not use `10.10.0.0/24`, `10.10.10.0/24`, `10.10.20.0/24`,
`10.10.30.0/24`, or `10.10.40.0/24`.

Each stage is change, observe, expected result, then the next change.
Do not configure the whole interface and test once at the end.

Configure-mode `show interfaces wireguard wg0` prints `private-key`.
Do not use it for a routine check, and do not copy that output into
notes or Git. Counters and state: `run show interfaces wireguard wg0`.
Server public key only: `run show interfaces wireguard wg0 public-key`.

---

# Preconditions

```bash
show interfaces ethernet eth1
```

From a house client, `curl -4 https://ifconfig.me` must equal the
`eth1` IPv4 and be public (not `100.64.0.0/10`, not RFC1918). Write
that address down. The phone test below has to match it.

If IPv4 is not public and `eth1` has no GUA, stop. No VPS.

`Endpoint` is the A name `sumitgahoi.me`. A spare AAAA profile uses
`v6.sumitgahoi.me`. Why those are separate names: `README.md`.
`firewall ipv6` default-drop must already exist (`../vyos/setup.md`).

---

# 1 — Key and interface

VyOS keeps its own private key. It does not generate, store, or display
client keys.

```text
configure
run generate pki wireguard key-pair install interface wg0
run show interfaces wireguard wg0 public-key
```

`run generate … install interface` writes `private-key` into the
candidate. The interface node is `private-key`. The `run show … public-key`
line is the value the phone needs. Do not show the rest of the interface.

```text
set interfaces wireguard wg0 address '10.10.80.1/24'
set interfaces wireguard wg0 description 'VPN private (Trusted)'
set interfaces wireguard wg0 port '51820'
set interfaces wireguard wg0 ipv6 address no-default-link-local
set interfaces wireguard wg0 ipv6 disable-forwarding
```

The two `ipv6` lines are the same lock as the VLAN VIFs. Do not set
`fwmark`.

```bash
compare
commit-confirm 60
run show interfaces wireguard wg0
```

Expected: `wg0` exists and is up, address `10.10.80.1/24`, RX/TX still
zero. Then `confirm` and `save` this stage. No peer yet, so nothing
can connect.

---

# 2 — Client key (not on VyOS)

Do this on the iPhone, or on a trusted workstation. Never on VyOS.
Do not install `qrencode` or any client tool on the router. Do not put
a client private key in Git or in the VyOS config.

Either method is valid:

- WireGuard iOS creates the tunnel and its own keypair. Read the
  public key off the phone.
- A workstation writes the config below, shows it as a QR code, and
  the phone scans it. Delete the temp file and the QR when the phone
  has it. Treat both as secret.

```text
[Interface]
Address = 10.10.80.2/32
PrivateKey = IPHONE_PRIVATE
DNS = 10.10.80.1

[Peer]
PublicKey = VYOS_WG0_PUBLIC
Endpoint = sumitgahoi.me:51820
AllowedIPs = 0.0.0.0/0
PersistentKeepalive = 25
```

`AllowedIPs` is IPv4 only. There is no `::/0`. Spare AAAA profile:
same keys, `Endpoint = v6.sumitgahoi.me:51820`.

---

# 3 — Peer, groups, WAN, NAT, DNS

```text
set interfaces wireguard wg0 peer sumit-iphone public-key 'IPHONE_PUBLIC'
set interfaces wireguard wg0 peer sumit-iphone allowed-ips '10.10.80.2/32'
```

No keepalive on VyOS. `allowed-ips` is cryptokey only. This image does
not install a route from it. `10.10.80.1/24` is already connected.

```text
show interfaces wireguard wg0 peer sumit-iphone
```

Expected in the candidate: peer `sumit-iphone`, `allowed-ips
10.10.80.2/32`, and the phone public key. This show is the peer only.
It is not a place to read the VyOS private key.

```text
set firewall group network-group NET-PRIVATE network '10.10.80.0/24'
set firewall group network-group NET-CLIENT network '10.10.80.0/24'
set firewall group interface-group IF-INTERNAL interface 'wg0'
```

No new forward rules. `NET-PRIVATE` makes forward rule 100 accept
`wg0` to Trusted, Services, and the Internet.

```text
set firewall ipv4 input filter rule 50 action 'accept'
set firewall ipv4 input filter rule 50 description 'WireGuard private'
set firewall ipv4 input filter rule 50 destination port '51820'
set firewall ipv4 input filter rule 50 inbound-interface name 'eth1'
set firewall ipv4 input filter rule 50 protocol 'udp'

set firewall ipv6 input filter rule 50 action 'accept'
set firewall ipv6 input filter rule 50 description 'WireGuard private'
set firewall ipv6 input filter rule 50 destination port '51820'
set firewall ipv6 input filter rule 50 inbound-interface name 'eth1'
set firewall ipv6 input filter rule 50 protocol 'udp'

set nat source rule 80 description 'NAT WG private to WAN'
set nat source rule 80 outbound-interface name 'eth1'
set nat source rule 80 source address '10.10.80.0/24'
set nat source rule 80 translation address 'masquerade'

set service dns forwarding allow-from '10.10.80.0/24'
set service dns forwarding listen-address '10.10.80.1'
```

Rule 80 matches `eth1` only. `wg0` to the house LAN is routed, not
NATed. If there is no GUA, the IPv6 listen rule does nothing until one
appears.

```bash
compare
commit-confirm 60
run show interfaces wireguard wg0
```

Expected before the phone connects: still up, `10.10.80.1/24`, RX/TX
still zero. Do not `confirm` yet.

---

# 4 — Cellular, then the tunnel

On the iPhone, turn Wi-Fi off. Cellular data must work on its own.
Note the cellular public address if you have it. This is what makes
the test enter through `eth1` instead of the house LAN.

Turn the tunnel on. On VyOS:

```bash
run show interfaces wireguard wg0
```

Expected: RX and TX both increase. That command shows counters. Do not
treat it as a handshake display.

IPv4 egress, from the phone, must be an IPv4 lookup. A normal
dual-stack “what is my IP” page can show the phone’s cellular IPv6,
because the tunnel does not carry `::/0`.

Expected IPv4: the `eth1` address from the preconditions.

```text
iPhone → wg0 → VyOS → NAT rule 80 on eth1 → Xfinity
```

---

# 5 — Trusted host, routed

Do not use `10.10.10.1`. That address is VyOS itself.

From the phone, open Proxmox at `10.10.10.3` (port 8006). Expected:
the UI loads. That is the forward path (rule 100).

On Proxmox, while the phone loads the UI:

```bash
tcpdump -ni any host 10.10.80.2
```

Expected:

```text
10.10.80.2.<port> > 10.10.10.3.8006
10.10.10.3.8006 > 10.10.80.2.<port>
```

Proxmox must see source `10.10.80.2`. That is routed, not NATed.
Rule 80 did not match because the egress is `eth0.10`, not `eth1`.

---

# 6 — Services and DNS

From the phone, open AdGuard at `10.10.0.4`. Expected: it loads.
That is `wg0` → Services, also rule 100, also routed.

DNS is a separate check. The phone’s resolver is `10.10.80.1`. Look
up a name you have never queried, then look at the AdGuard query log
immediately.

Expected client in that log: `10.10.0.1`.

```text
iPhone 10.10.80.2
    → DNS 10.10.80.1 (VyOS on wg0)
    → VyOS forwarder
    → source 10.10.0.1 (VyOS on eth2)
    → AdGuard 10.10.0.4
```

AdGuard is supposed to see forwarded client DNS as `10.10.0.1`.
Leave that. Do not point the phone at `10.10.0.4` for DNS, and do not
open AdGuard to per-client tunnel addresses.

---

# 7 — Confirm

From a house client, Internet and `10.10.10.1` still work.

Do not `confirm` until all of these are true:

- `wg0` is up at `10.10.80.1/24`.
- Cellular RX and TX on `wg0` both increased.
- Phone IPv4 egress is the `eth1` public address.
- The odd name appeared in AdGuard, client `10.10.0.1`.
- Proxmox `10.10.10.3` loaded, and tcpdump showed source `10.10.80.2`.
- AdGuard `10.10.0.4` loaded.
- The house LAN still works.

```text
confirm
save
```

Export `commands.txt` with the private key omitted.

---

# What not to do

- Do not listen until `eth1` IPv4 matches `curl -4 https://ifconfig.me` (or a GUA exists for the AAAA profile).
- Do not create CT 109 or add a VPS.
- Do not install client-provisioning tools on VyOS, or store a client private key there.
- Do not put guest keys or the NUC on `wg0`.
- Do not enable IPv6 inside `wg0`, and do not add `::/0` to the phone.
- Do not set `fwmark`.
- Do not add a forward rule for this. Rule 100 is the accept.
- Do not NAT `wg0` toward the house LAN.
- Do not use configure-mode `show interfaces wireguard wg0` to check the tunnel.
