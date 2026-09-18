# Verify VyOS

Run after a fresh install or significant config change.

## 1. Interfaces

show interfaces

Expect:
- eth0.10 → 10.10.10.1/24
- eth0.20 → 10.10.20.1/24
- eth0.30 → 10.10.30.1/24
- eth0.40 → 10.10.40.1/24
- eth1 → WAN DHCP
- eth2 → 10.10.0.1/24
- all expected interfaces u/u

## 2. WAN

ping 1.1.1.1 count 4

Expect replies.

## 3. DNS

nslookup google.com 10.10.10.1

Expect an answer.

## 4. Routes

show ip route

Expect:
- default route via eth1
- connected routes for all four VLANs
- connected route for Services

## 5. Firewall

show firewall

Expect forward:
- established/related → accept
- Trusted → accept
- Guest → eth1 → accept
- default → drop

Then continue with client tests...