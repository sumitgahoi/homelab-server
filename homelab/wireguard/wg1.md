# wg1 — Guest WireGuard (PLANNED)

Not deployed. Dialect: rolling `2026.09.16-0028`. Do not write these
lines into `../vyos/commands.txt` until they are live and exported.
Do not create CT 109. Do not run WireGuard on a Services guest.

`wg0` is `wg0.md` (live). India is `wg2.md`. Design: `README.md`.

```text
wg1   10.10.81.0/24   10.10.81.1   UDP 51821
```

Do not use `10.10.0.0/24`, `10.10.10.0/24`, `10.10.20.0/24`,
`10.10.30.0/24`, or `10.10.40.0/24`.

Each stage is change, observe, expected result, then the next change.
Test from cellular with Wi-Fi off, the same way `wg0` was proved.
Guest must get Xfinity and DNS, and must not reach Trusted or Services.

Configure-mode `show interfaces wireguard wg1` prints `private-key`.
Do not use it for a routine check. Counters: `run show interfaces
wireguard wg1`. Public key only: `run show interfaces wireguard wg1
public-key`.

---

# Preconditions

```bash
show interfaces ethernet eth1
```

From a house client, `curl -4 https://ifconfig.me` must equal the
`eth1` IPv4 and be public (not `100.64.0.0/10`, not RFC1918). Write
that address down.

If IPv4 is not public and `eth1` has no GUA, stop. No VPS.

`Endpoint` is the A name `YOUR_DDNS`. A spare AAAA profile uses
`YOUR_DDNS6` (`README.md`). `firewall ipv6` default-drop must already
exist (`../vyos/setup.md`).

---

# 1 — Key and interface

VyOS does not generate or store client keys. No `qrencode` on the router.

```text
configure
run generate pki wireguard key-pair install interface wg1
run show interfaces wireguard wg1 public-key
```

```text
set interfaces wireguard wg1 address '10.10.81.1/24'
set interfaces wireguard wg1 description 'VPN guest'
set interfaces wireguard wg1 port '51821'
set interfaces wireguard wg1 ipv6 address no-default-link-local
set interfaces wireguard wg1 ipv6 disable-forwarding
```

Do not set `fwmark`. The `ipv6` lines match the VLAN VIFs.

```bash
compare
commit-confirm 60
run show interfaces wireguard wg1
```

Expected: `wg1` is up, address `10.10.81.1/24`, RX/TX zero. `confirm`
and `save`. No peer yet.

---

# 2 — Client key, then VyOS peer and policy

Client provisioning is the `wg0.md` choice: the phone creates its own
keypair, or a workstation shows a QR and then deletes it. Not on VyOS.

```text
[Interface]
Address = 10.10.81.2/32
PrivateKey = CLIENT_PRIVATE
DNS = 10.10.81.1

[Peer]
PublicKey = VYOS_WG1_PUBLIC
Endpoint = YOUR_DDNS:51821
AllowedIPs = 0.0.0.0/0
PersistentKeepalive = 25
```

No `::/0`. Then:

```text
set interfaces wireguard wg1 peer guest-phone public-key 'CLIENT_PUBLIC'
set interfaces wireguard wg1 peer guest-phone allowed-ips '10.10.81.2/32'
show interfaces wireguard wg1 peer guest-phone
```

Expected: peer `guest-phone`, `allowed-ips 10.10.81.2/32`, client
public key. Not the VyOS private key.

```text
set firewall group network-group NET-GUEST network '10.10.81.0/24'
set firewall group network-group NET-CLIENT network '10.10.81.0/24'
set firewall group interface-group IF-INTERNAL interface 'wg1'

set firewall ipv4 input filter rule 51 action 'accept'
set firewall ipv4 input filter rule 51 description 'WireGuard guest'
set firewall ipv4 input filter rule 51 destination port '51821'
set firewall ipv4 input filter rule 51 inbound-interface name 'eth1'
set firewall ipv4 input filter rule 51 protocol 'udp'

set firewall ipv6 input filter rule 51 action 'accept'
set firewall ipv6 input filter rule 51 description 'WireGuard guest'
set firewall ipv6 input filter rule 51 destination port '51821'
set firewall ipv6 input filter rule 51 inbound-interface name 'eth1'
set firewall ipv6 input filter rule 51 protocol 'udp'

set nat source rule 81 description 'NAT WG guest to WAN'
set nat source rule 81 outbound-interface name 'eth1'
set nat source rule 81 source address '10.10.81.0/24'
set nat source rule 81 translation address 'masquerade'

set service dns forwarding allow-from '10.10.81.0/24'
set service dns forwarding listen-address '10.10.81.1'
```

No new forward accept. Rule 200 allows `NET-GUEST` only out `eth1`.
Anything else, including Trusted and Services, is default-drop.
Rule 81 is that `eth1` path only.

```bash
compare
commit-confirm 60
run show interfaces wireguard wg1
```

Expected: still up, RX/TX zero. Do not `confirm` yet.

---

# 3 — Cellular, Internet, DNS

Wi-Fi off. Cellular works. Turn the tunnel on.

```bash
run show interfaces wireguard wg1
```

Expected: RX and TX both increase. Do not read that command as a
handshake log.

IPv4 egress must be an IPv4 lookup. A dual-stack page can show
cellular IPv6 because there is no `::/0`. Expected IPv4: the `eth1`
address from the preconditions.

```text
phone → wg1 → VyOS → NAT rule 81 on eth1 → Xfinity
```

Look up a name you have never queried. AdGuard’s query log should show
it at once, client `10.10.0.1`.

```text
phone 10.10.81.2
    → DNS 10.10.81.1
    → VyOS forwarder
    → source 10.10.0.1
    → AdGuard 10.10.0.4
```

That source is intentional. Do not point the phone at AdGuard for DNS.

---

# 4 — Trusted and Services stay closed

From the phone:

- Proxmox `https://10.10.10.3:8006` does not load.
- AdGuard `http://10.10.0.4` does not load.
- SSH to VyOS does not connect.

On Proxmox, while the phone tries the UI:

```bash
tcpdump -ni any host 10.10.81.2
```

Expected: no packets. Forward rule 200 does not match `eth0.10` or
`eth2`, so the drop happens on VyOS. Ping of `10.10.10.1` may still
work; that is ICMP to the router (input rule 30), the same as VLAN 20,
not a forward into Trusted.

---

# 5 — Confirm

From a house client, Internet and `10.10.10.1` still work. `wg0` still
works if that phone is the one you set aside.

Do not `confirm` until all of these are true:

- `wg1` is up at `10.10.81.1/24`.
- Cellular RX and TX on `wg1` both increased.
- Phone IPv4 egress is the `eth1` public address.
- The odd name appeared in AdGuard, client `10.10.0.1`.
- Proxmox and the AdGuard UI did not load, and Proxmox saw no
  `10.10.81.2`.
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
- Do not put Trusted keys or the NUC on `wg1`.
- Do not enable IPv6 inside `wg1`.
- Do not set `fwmark`.
- Do not add a forward accept toward Trusted, Services, or other VLANs.
- Do not NAT `wg1` toward the house LAN.
- Do not use configure-mode `show interfaces wireguard wg1` to check the tunnel.
