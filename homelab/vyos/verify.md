# Verify VyOS

Run after a fresh install or significant config change. Rebuild procedure: `setup.md`. Known-good CLI: `commands.txt`.

## 1. Interfaces

show interfaces

Expect:
- eth0.10 → 10.10.10.1/24
- eth0.20 → 10.10.20.1/24
- eth0.30 → 10.10.30.1/24
- eth0.40 → 10.10.40.1/24
- eth1 → WAN DHCPv4 + DHCPv6/autoconf (GUA when the ISP offers one)
- eth2 → 10.10.0.1/24
- LAN VIFs / eth2 → no inet6

## 2. WAN

ping 1.1.1.1 count 4

Expect replies.

ping ipv6 2001:4860:4860::8888 count 4

Expect replies when `eth1` has a GUA and `::/0`. IPv4 WAN is enough if
the ISP has no IPv6. LAN clients must not have a GUA or IPv6 default.

## 3. DNS

nslookup google.com 10.10.10.1

Expect an answer.

## 4. Routes

show ip route

Expect:
- default route via eth1 (main table; Trusted/Guest/Services)
- connected routes for all four VLANs
- connected route for Services
- table 40 default via 10.10.0.5 (plus higher-distance blackhole)

## 5. Firewall

show firewall

Expect forward:
- established/related → accept
- Trusted → accept
- Guest → eth1 → accept
- Services → eth1 → accept
- India dest RFC1918 → drop
- India → eth2 → accept
- default → drop

India must not appear in source NAT out eth1. DNS forwarding must not listen on 10.10.40.1.

India path tests: `../tailscale-india/setup.md`. India-GW (CT 108) is part of the known-good path.
