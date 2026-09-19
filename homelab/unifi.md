# UniFi — CURRENT

As-built Wi-Fi controller and AP. Isolation, DHCP, DNS, NAT, and routing stay on VyOS (`config.boot`). Do not treat UniFi as the house router.

Policy: `REQUIREMENTS.md`. Wiring: `README.md`. Guest: `proxmox.md`.

## UniFi OS Server

Proxmox LXC 107, hostname `unifi-os-server`, Debian 13, privileged (required by UniFi OS Server). 2 vCPU / 4096 MiB / 20 GB. `vmbr-svc`, static `10.10.0.2/24`, gateway `10.10.0.1`. IPv6 disabled. TUN and nesting enabled. UI `https://10.10.0.2:11443`.

Observed at deployment: UniFi OS Server 5.1.42. That is not a desired-version pin.

## U6+

Adopted, Online. CBS350 GE2: trunk, native VLAN 10, tagged 20/30/40. AP management is on native VLAN 10 (VyOS DHCP). During adoption the AP received `10.10.10.101`; that is a DHCP lease, not a reserved address.

UniFi OS Server is on Services (`10.10.0.2`), so adoption is Layer-3:

    set-inform http://10.10.0.2:8080/inform

Trusted→Services already permits this.

## Networks

Third-party-gateway networks only (VyOS is the gateway):

| Role | VLAN |
|------|------|
| Trusted | 10 |
| Guest | 20 |
| IoT | 30 |
| India | 40 |

Services is not a UniFi network (`vmbr-svc` has no VLAN).

## Wi-Fi

SSID names are not locked. Map by role:

| Role | UniFi network | Tagging | Verified client |
|------|---------------|---------|-----------------|
| Trusted / private | Native Network (AP untagged; GE2 native VLAN 10) | untagged | `10.10.10.122` |
| Guest | Guest / VLAN 20 | tagged 20 | `10.10.20.100` |
| IoT | IoT / VLAN 30 | tagged 30 | `10.10.30.100` |
| India | India / VLAN 40 | tagged 40 | `10.10.40.100` |

End-to-end verified: Wi-Fi → U6+ → CBS350 → Proxmox → VyOS DHCP.
