# Network requirements

## Roles

- **Proxmox** = hypervisor, not router.
- **CBS350** = L2 switching and VLANs, not routing.
- **VyOS** = only client-facing router and DHCP server; provides firewalling, DNS forwarding, and US-WAN NAT.
- **UniFi** = Wi-Fi only; one SSID per client VLAN: Trusted → 10, Guest → 20, IoT → 30, India → 40.
- **VyOS**, not UniFi/AP isolation, is the inter-VLAN security boundary.
- **AdGuard** = DNS filtering, local `home.arpa` DNS, and encrypted public upstreams.
- One LAN trunk carries client VLANs; WAN is separate; Services is isolated from the physical LAN.
- Client/LAN IPv6 is disabled; WAN IPv6 only.
- VyOS NIC numbering and MAC assignments stay pinned.

## Networks

| Network | VLAN | Subnet | Gateway |
|---|---:|---|---|
| Trusted | 10 | `10.10.10.0/24` | `10.10.10.1` |
| Guest | 20 | `10.10.20.0/24` | `10.10.20.1` |
| IoT | 30 | `10.10.30.0/24` | `10.10.30.1` |
| India | 40 | `10.10.40.0/24` | `10.10.40.1` |
| Services | — | `10.10.0.0/24` | `10.10.0.1` |

## Network policy

- **Trusted** → Internet, other client VLANs, Services, and management.
- **Guest** → Internet + DHCP/DNS to its own gateway; no other internal access.
- **IoT** → Internet via US WAN only + DHCP/DNS to its own gateway; no other internal access.
- **India** → Internet via India only + DHCP to its own gateway; no other internal access.
- **Services** → Internet; no initiation into client VLANs by default.
- Add narrowly scoped Services-originated exceptions only when a real service requires them.
- Established/related return traffic is allowed.
- Unsolicited WAN inbound is denied by default.

## DNS

- Trusted, Guest, and IoT → VLAN gateway → VyOS → AdGuard `10.10.0.4`.
- AdGuard provides local DNS for `home.arpa` and encrypted public upstream DNS.
- Trusted receives the `home.arpa` search domain.
- Guest and IoT may resolve `home.arpa` but do not receive its search domain.
- AdGuard failure means DNS failure for Trusted, Guest, and IoT.
- India uses public DNS through its India egress only; it does not use VyOS/AdGuard for DNS.

## India egress

- VLAN 40 has exactly one Internet egress, currently `wg2` → `asus-nuc`.
- No fallback through the US WAN, including DNS or IPv6.
- No simultaneous Tailscale VLAN 40 path; switching VLAN 40 back to Tailscale is a design change, not a fallback.
- India IPv6 must not be enabled until an India IPv6 egress exists.
- Tailscale on `asus-nuc` remains its independent out-of-band management path.
- Changes to the India WireGuard path must not alter NUC Tailscale, its default route, or unrelated routing.

## Remote access

- `wg0` = Trusted remote access.
- `wg1` = Guest / Internet-only remote access.
- `wg2` = India egress.
- WireGuard tunnels remain IPv4.
- No VPS VPN hub.
- Do not deploy `tailscale-us`.
- Tailscale is not part of VyOS routing.