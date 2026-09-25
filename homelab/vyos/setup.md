# VyOS Setup / Rebuild Guide

This document describes how to rebuild the VyOS router manually.

The complete known-good configuration is stored in:

    commands.txt

`commands.txt` is generated from the working router using:

    show configuration commands

Do not hand-edit `config.boot` as a second source of truth, and do not
`load` one onto the router. `commands.txt` is what you apply. `config.boot`
is the same config in tree form for reading (password `redacted`).

WAN IPv6 on `eth1` only is in `commands.txt` (CURRENT). On a rebuild,
do not apply `eth1` `dhcpv6` / `autoconf` or `firewall ipv6` in the
IPv4 steps; use the WAN IPv6 sitting later in this file (firewall
before a GUA).

`wg0`, `wg1`, and `wg2` are in `commands.txt` (`../wireguard/`). Do not
mix a second WireGuard pass into this rebuild. Table 40 is
`interface wg2`. Do not point it at `10.10.0.5`. NUC `wg-india` is
`../wireguard/wg2.md` (literal `Endpoint`, no `PostDown`, nft unit).
Do not create CT 109. Sections 6 and 6b passed. CT 108 stays
installed until `wg2.md` section 7. Each WireGuard `private-key` in
`commands.txt` is the word `redacted`; generate a new key on a rebuild
instead of pasting that word.

The rebuild philosophy is:

    configure one logical section
        ↓
    inspect it
        ↓
    understand what it does
        ↓
    continue
        ↓
    commit-confirm 60
        ↓
    test the network
        ↓
    confirm + save

---

# Network Architecture

VyOS is the only router between the homelab networks.

```text
                         Internet
                            │
                            │
                         eth1 WAN
                            │
                     ┌──────┴──────┐
                     │    VyOS     │
                     └──────┬──────┘
                            │
               eth0 LAN trunk
                            │
              ┌─────────────┼──────────────┐
              │             │              │
          VLAN 10        VLAN 20        VLAN 30        VLAN 40
          Trusted         Guest            IoT           India
        10.10.10/24    10.10.20/24    10.10.30/24    10.10.40/24

                     eth2 Services
                            │
                       10.10.0.1
                            │
                         vmbr-svc
```

Interfaces are intentionally pinned using permanent MAC addresses:

```text
eth0 = LAN trunk   = 02:00:00:00:00:00
eth1 = WAN         = 02:00:00:00:00:01
eth2 = Services    = 02:00:00:00:00:02
```

Do not change this mapping casually.

---

# Step 1 — Install VyOS

Create/install the VyOS VM according to `install.md`.

Before applying the router configuration, verify that the VM sees:

```text
eth0
eth1
eth2
```

Check:

```bash
show interfaces
```

The MAC addresses must match the expected mapping above.

Do not continue if the interfaces are mapped incorrectly.

---

# Step 2 — Enter Configuration Mode

```bash
# Enter VyOS configuration mode.
configure
```

The prompt should change from:

```text
$
```

to:

```text
#
```

From this point onward, `set` commands modify the candidate
configuration.

Nothing becomes active until it is committed.

---

# Step 3 — Configure Interfaces

Open `commands.txt`.

Apply the commands beginning with:

```text
set interfaces ...
```

These configure:

- eth0 as the LAN VLAN trunk
- VLAN 10 — Trusted
- VLAN 20 — Guest
- VLAN 30 — IoT
- VLAN 40 — India
- eth1 as DHCP WAN
- eth2 as Services
- deterministic interface MAC mappings
- IPv6 restrictions on VLAN40

WAN IPv6 on `eth1` and the same IPv6 lock on `eth0.10`/`.20`/`.30` and
`eth2` are later in this file (CURRENT; apply on a rebuild if
`commands.txt` does not yet contain them). Do not mix them into the
IPv4 section. Do not request DHCPv6-PD.

Do these commands manually.

Then inspect the candidate configuration:

```bash
# Show what has changed but has not yet been committed.
compare
```

Review it before continuing.

---

# Step 4 — Configure Firewall Groups

Apply the commands beginning with:

```text
set firewall group ...
```

Important groups include:

```text
NET-PRIVATE
NET-GUEST
NET-CLIENT
NET-INDIA
NET-RFC1918
NET-SERVICES

IF-INTERNAL
IF-DHCP-SERVER
```

These groups make the actual firewall rules easier to understand.

For example:

```text
NET-INDIA = 10.10.40.0/24
```

instead of repeating that subnet throughout the firewall.

Inspect:

```bash
compare
```

---

# Step 5 — Configure Firewall Rules

Apply:

```text
set firewall ipv4 ...
```

The firewall uses a positive allow-list design:

```text
default = DROP
```

Important intended behavior:

```text
Trusted
    → unrestricted routed access

Guest
    → Internet only

IoT
    → isolated

India
    → India-GW only
    → cannot pivot into RFC1918 networks

Services
    → Internet
    → cannot initiate connections into client VLANs
```

The first rule in both INPUT and FORWARD must allow only:

```text
state established
state related
```

Be especially careful here. An unconditional rule 10 `accept` would
effectively bypass much of the firewall.

Inspect:

```bash
compare
```

---

# Step 6 — Configure DHCP

Apply:

```text
set service dhcp-server ...
```

Expected networks:

```text
Trusted    10.10.10.100 - 10.10.10.250
Guest      10.10.20.100 - 10.10.20.250
IoT        10.10.30.100 - 10.10.30.250
India      10.10.40.100 - 10.10.40.250
```

India is intentionally different from the other networks.

Its DNS servers are:

```text
1.1.1.1
1.0.0.1
```

India clients must NOT use:

```text
10.10.40.1
```

for DNS.

This ensures their DNS traffic follows the same India policy-routing
path as normal Internet traffic.

Inspect:

```bash
compare
```

---

# Step 7 — Configure VyOS DNS Forwarding

Apply:

```text
set service dns forwarding ...
```

VyOS provides DNS forwarding for:

```text
Trusted
Guest
IoT
```

It deliberately does NOT listen on:

```text
10.10.40.1
```

and does NOT allow:

```text
10.10.40.0/24
```

India DNS is handled as ordinary Internet traffic through India-GW.

CURRENT forwarding for Trusted/Guest/IoT is a single upstream, AdGuard
`10.10.0.4` (`commands.txt`). Rebuild: `../adguard/setup.md` after
CT 110 exists. Do not mix Cloudflare back in as a second name-server.

Inspect:

```bash
compare
```

---

# Step 8 — Configure Normal WAN NAT

Apply:

```text
set nat source ...
```

Normal WAN masquerading exists for:

```text
Trusted
Guest
Services
```

There must NOT be an India/VLAN40 masquerade rule toward `eth1`.

That absence is intentional.

```text
India
   X
 eth1 / US WAN
```

This is one layer of the India fail-closed design.

Inspect:

```bash
compare
```

---

# Step 9 — Configure India Policy Routing

Apply:

```text
set policy route PBR-INDIA ...
set protocols static table 40 ...
```

`commands.txt` sends table 40 out `wg2`. Do not put `10.10.0.5`
back. The old India-GW path is rollback only (`../tailscale-india/setup.md`).

```text
10.10.40.0/24
       │
       ▼
PBR-INDIA
       │
       ▼
table 40
       │
       ▼
wg2
       │
       ▼
asus-nuc
```

Table 40 in `commands.txt` contains:

```text
default → interface wg2

blackhole default
distance 254
```

The blackhole route is deliberate.

If the usable route disappears, India traffic should be discarded
rather than finding another default route.

Inspect:

```bash
compare
```

---

# Step 10 — Configure SSH and System Settings

Apply the remaining:

```text
set service ssh ...
set system ...
```

The Git copy intentionally does NOT contain:

```text
set system login user vyos authentication encrypted-password ...
```

Password hashes are not stored in the repository.

The SSH public key may be stored because it is not secret.

Set/reset the VyOS password manually if necessary.

---

# Step 11 — Review the Entire Candidate

Before committing anything:

```bash
compare
```

Read the complete diff.

At minimum verify:

```text
eth0 = LAN
eth1 = WAN
eth2 = Services

VLANs = 10 / 20 / 30 / 40

forward default = drop
input default = drop

India PBR → table 40
table 40 → interface wg2

NO India → eth1 NAT
```

Only continue when the candidate looks correct.

---

# Step 12 — Commit With Automatic Rollback

Do NOT immediately use a normal `commit`.

Use:

```bash
# Activate the candidate configuration, but automatically roll it
# back if we don't explicitly confirm it within 60 minutes.
commit-confirm 60
```

60 minutes is intentional.

Testing this router involves several networks and multiple systems.
Short 5- or 15-minute timers create unnecessary pressure.

---

# Step 13 — Test

Do not `confirm` yet.

Test the router while rollback protection is active.

## Trusted

Verify:

```text
DHCP works
Internet works
VyOS reachable
Services reachable
```

## Guest

Verify:

```text
DHCP works
Internet works
Internal networks blocked
```

## IoT

Verify:

```text
DHCP works
Internet blocked
Internal networks blocked
```

## India

Verify:

```text
DHCP gives 10.10.40.x
Gateway = 10.10.40.1
DNS = 1.1.1.1 / 1.0.0.1

Internet works
Public IP = India
DNS = India path
RFC1918 access blocked
IPv6 does not bypass policy
```

Also perform the fail-closed test described in:

```text
../tailscale-india/setup.md
```

---

# Step 14 — Make the Configuration Permanent

Only after all tests pass:

```bash
# Tell VyOS that the commit-confirm configuration is good.
confirm
```

Then:

```bash
# Persist the running configuration across reboot.
save
```

Then leave configuration mode:

```bash
exit
```

---

# Step 15 — Capture the New Known-Good State

After making intentional VyOS changes, regenerate `commands.txt` and
the readable `config.boot`.

From operational mode:

```bash
# Export the running configuration while excluding the password hash.
show configuration commands | grep -v "encrypted-password" > /tmp/commands.txt
```

```bash
cp /config/config.boot /tmp/config.boot
```

On the Mac, replace the `encrypted-password` value with `redacted`
before commit. Verify `commands.txt` has no hash:

```bash
grep "encrypted-password" /tmp/commands.txt
```

Expected:

```text
<no output>
```

Then copy both into the Git repository:

```bash
scp vyos@10.10.10.1:/tmp/commands.txt vyos/commands.txt
scp vyos@10.10.10.1:/tmp/config.boot vyos/config.boot
```

Review the Git diff before committing.

This makes the workflow:

```text
Make deliberate change
        ↓
Test it
        ↓
confirm + save
        ↓
Export known-good commands
        ↓
Review Git diff
        ↓
Commit
```

---

# WAN IPv6 on eth1 (CURRENT)

Live on the router: IPv6 on the WAN only. Clients stay IPv4. These
lines are in `commands.txt`. On a rebuild, apply this sitting (not
mixed into the IPv4 steps) so the firewall exists before a GUA.
UDP `51820`–`51822` stays in `../wireguard/wg0.md`, `wg1.md`, and `wg2.md`.

Do the firewall **before** `eth1` has a GUA on a fresh box. After every
`commit-confirm`, `confirm` + `save` **before** the next section.

## 1 — Lock IPv6 off the LAN and Services

VLAN 40 already has this in `commands.txt`. Match it on the other
client VIFs and `eth2`. Do **not** set these on `eth1`. Per-VIF `disable-forwarding` plus a
forward-filter default-drop is intentional (IPv6 stays dead if someone
later adds a forward accept).

```text
set interfaces ethernet eth0 vif 10 ipv6 address no-default-link-local
set interfaces ethernet eth0 vif 10 ipv6 disable-forwarding
set interfaces ethernet eth0 vif 20 ipv6 address no-default-link-local
set interfaces ethernet eth0 vif 20 ipv6 disable-forwarding
set interfaces ethernet eth0 vif 30 ipv6 address no-default-link-local
set interfaces ethernet eth0 vif 30 ipv6 disable-forwarding
set interfaces ethernet eth2 ipv6 address no-default-link-local
set interfaces ethernet eth2 ipv6 disable-forwarding
```

`eth0.40` must keep its existing two lines.

```bash
compare
commit-confirm 60
```

Still in configure. Operational checks use `run`. Linux names are
`eth0.10` / `eth0.20` / `eth0.30` / `eth0.40` / `eth2`.

No IPv6 addresses at all (no GUA, no link-local). Operational `show
interfaces` must list IPv4 only — no `inet6` / no `fe80::`:

```bash
run show interfaces ethernet eth0 vif 10
run show interfaces ethernet eth0 vif 20
run show interfaces ethernet eth0 vif 30
run show interfaces ethernet eth0 vif 40
run show interfaces ethernet eth2
```

VyOS is not sending Router Advertisements. Empty is success (this
box has no `service router-advert`):

```bash
run show configuration commands | grep router-advert
```

`eth1` must not be in the lock. Empty `system ipv6` is success (do not
set `system ipv6 disable-forwarding`):

```bash
run show configuration commands | grep disable-forwarding
run show configuration commands | grep 'system ipv6'
```

Expect `disable-forwarding` only on `eth0` vif 10/20/30/40 and `eth2`.

Then `confirm` + `save`.

## 2 — IPv6 firewall (default-drop) before a GUA

Rule 15 is not a substitute for default-drop. It exists because rule 30
accepts ICMPv6 without a conntrack state match. Do not type-filter
ICMPv6 on `eth1` input (ND, RA, PTB). Keep the DHCPv6 client hole:
`address dhcpv6` is always on `eth1`, and UDP 546 replies are not
always `related`.

No `output filter`: router-originated IPv6 (RS, DHCPv6 SOLICIT, ping)
stays implicit accept. Do not add IPv6 SSH. Management stays Trusted
IPv4 (`10.10.10.1`). WAN IPv6 input default-drop means SSH to a GUA
fails; that is intended.

```text
set firewall ipv6 input filter default-action 'drop'
set firewall ipv6 input filter rule 10 action 'accept'
set firewall ipv6 input filter rule 10 state 'established'
set firewall ipv6 input filter rule 10 state 'related'
set firewall ipv6 input filter rule 15 action 'drop'
set firewall ipv6 input filter rule 15 state 'invalid'
set firewall ipv6 input filter rule 20 action 'accept'
set firewall ipv6 input filter rule 20 description 'WAN DHCPv6 client'
set firewall ipv6 input filter rule 20 destination port '546'
set firewall ipv6 input filter rule 20 inbound-interface name 'eth1'
set firewall ipv6 input filter rule 20 protocol 'udp'
set firewall ipv6 input filter rule 20 source port '547'
set firewall ipv6 input filter rule 30 action 'accept'
set firewall ipv6 input filter rule 30 description 'ICMPv6 on WAN'
set firewall ipv6 input filter rule 30 inbound-interface name 'eth1'
set firewall ipv6 input filter rule 30 protocol 'icmpv6'
set firewall ipv6 forward filter default-action 'drop'
```

```bash
compare
commit-confirm 60
```

Then `confirm` + `save`.

## 3 — Ask the ISP for IPv6 on eth1 only

Always both. Never PD. This is the WAN IPv6 policy, not an ISP recipe.

`ipv6 address autoconf` accepts RAs on a forwarding WAN (`accept_ra=2`)
and is how `::/0` is learned (DHCPv6 does not carry a default route).
`address dhcpv6` requests IA_NA if the ISP offers it. Do not set
`ipv6 disable-forwarding` on `eth1`.

```text
set interfaces ethernet eth1 ipv6 address autoconf
set interfaces ethernet eth1 address dhcpv6
```

Do **not**:

```text
set interfaces ethernet eth1 dhcpv6-options pd ...
```

PD is a prefix for downstream networks. This house does not want LAN
IPv6.

```bash
compare
commit-confirm 60
```

Still in configure. `ping6` and `cat` are not op-mode commands on this
image.

```bash
run show interfaces ethernet eth1
run show ipv6 route
run show ipv6 forwarding
run ping ipv6 2001:4860:4860::8888 count 4
```

A GUA and `::/0` can take a minute after commit. If `eth1` still has
only `fe80::` and no default, wait and re-run the two `show` lines.
IPv4 on `eth1` is unrelated.

Expect a GUA on `eth1` (SLAAC, IA_NA, or both; two GUAs are fine),
`::/0` via the ISP link-local, and IPv6 forwarding enabled. Then
`confirm` + `save`.

If there is no GUA and no `::/0` after a few minutes, leave this
sitting in place (lock + firewall + both clients) and continue on
IPv4. Do not add PD to “make IPv6 work.”

## 4 — Confirm the house has no LAN IPv6

Repeat the §1 `show interfaces` VIF/`eth2` checks and `grep router-advert`.
Then:

```bash
run show ipv6 route
```

Expect no `::/0` via a LAN VIF. `eth0.40` already has the IPv6 lock.

From a Trusted client (not `curl -6` alone):

```bash
ip -6 addr
ip -6 route
```

Expect: link-local only. No GUA, no `::/0`. Then `ping6` / `curl -6`
must fail.

Export `commands.txt` (Step 15). Omit nothing from this sitting;
there are no WireGuard keys here.

---

# Important Rule

`commands.txt` describes the complete known-good configuration.

It is primarily a **rebuild reference**.

Do not blindly apply the entire file to an already-configured router
when making normal changes.

For an existing router:

```text
understand desired change
        ↓
set / delete only what is necessary
        ↓
compare
        ↓
commit-confirm 60
        ↓
test
        ↓
confirm
        ↓
save
        ↓
regenerate commands.txt
```

This keeps the live router understandable and keeps Git synchronized
with reality.