# VyOS Setup / Rebuild Guide

This document describes how to rebuild the VyOS router manually.

The complete known-good configuration is stored in:

    commands.txt

`commands.txt` is generated from the working router using:

    show configuration commands

Do not hand-maintain a `config.boot` in Git, and do not `load` one onto the router.

There is no `config.boot` in this directory. `commands.txt` is the known-good snapshot.

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

The intended routing path is:

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
10.10.0.5
       │
       ▼
India-GW
```

Table 40 contains:

```text
default → 10.10.0.5

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
table 40 → 10.10.0.5

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

After making intentional VyOS changes, regenerate `commands.txt`.

From operational mode:

```bash
# Export the running configuration while excluding the password hash.
show configuration commands | grep -v "encrypted-password" > /tmp/commands.txt
```

Verify that no password hash remains:

```bash
grep "encrypted-password" /tmp/commands.txt
```

Expected:

```text
<no output>
```

Then copy it back into the Git repository:

```bash
scp vyos@10.10.10.1:/tmp/commands.txt vyos/commands.txt
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