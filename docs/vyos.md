# VyOS

Single **VyOS VM** is the lab default gateway — NAT, firewall, DHCP, and (later) VLAN routing.

## Phase 1 — current rollout

| Topic | Setting |
|-------|---------|
| **NICs** | **2× virtio** on Proxmox `vmbr-wan` + `vmbr-lan` — no SR-IOV, no VLANs |
| **LAN** | One flat RFC1918 subnet (e.g. `192.168.1.0/24`, gateway `192.168.1.1`) |
| **DHCP** | On VyOS for the LAN segment |
| **Client DNS** | **Cloudflare** `1.1.1.1`, `1.0.0.1` **or** **OpenDNS** `208.67.222.222`, `208.67.220.220` via DHCP |
| **Deferred** | SR-IOV, VLAN trunk, `vmbr-svc`, Pi-hole, Tailscale, IoT policy |

Example defaults for scripts / manual config:

```text
vyos_wan_if:     eth0
vyos_lan_if:     eth1
vyos_lan_ipv4:   192.168.1.1/24
dhcp_range:      192.168.1.100 – 192.168.1.250
```

Tighten firewall beyond NAT + DHCP before exposing services.

## Target — VLANs and IPv4 (locked)

Switch trunk to VyOS **LAN** port: **802.1Q tagged only** (VLANs 10, 20, 30 — no untagged data VLAN on that uplink).

| VLAN | Name | Subnet | Gateway | Internet |
|------|------|--------|---------|----------|
| **10** | private | `10.10.10.0/24` | `10.10.10.1` | Yes — DNS → Pi-hole `10.10.0.53` |
| **20** | guest | `10.10.20.0/24` | `10.10.20.1` | Yes — tighter isolation from private |
| **30** | iot | `10.10.30.0/24` | `10.10.30.1` | **No** — see IoT rules below |

VyOS LAN subinterfaces: `ethX.10`, `ethX.20`, `ethX.30` where `ethX` is the **LAN VF** inside the guest.

## Internal stub — `vmbr-svc` (locked)

| Address | Role |
|---------|------|
| `10.10.0.0/24` | Internal bridge subnet |
| `10.10.0.1` | VyOS virtio gateway for LXCs |
| `10.10.0.52` | Tailscale LXC (example) |
| `10.10.0.53` | Pi-hole LXC (example) |

## Target interfaces

| Interface | Attachment |
|-----------|------------|
| **WAN VF** | Modem (prefer modem **2.5G** LAN) |
| **LAN VF** | Managed switch **802.1Q trunk** |
| **virtio** | `vmbr-svc` — gateway for internal LXCs |

## IoT firewall (locked)

Default **deny** from IoT unless listed:

| Traffic | Verdict |
|---------|---------|
| IoT → WAN | **DROP** |
| IoT → Pi-hole `10.10.0.53:53` | **ALLOW** (TCP + UDP) |
| IoT → VyOS `10.10.30.1:123` (NTP) | **ALLOW** |
| IoT → private / guest | **DROP** |
| Cross-VLAN mDNS (UDP 5353) | **DROP** |
| IoT → Home Assistant etc. | **DENY** until explicit rule added |

## Topology sketch (target)

```text
  Internet → modem → [WAN VF] VyOS [LAN VF] → switch trunk (VLAN 10/20/30)
                          │
                     virtio → vmbr-svc (Pi-hole, Tailscale, …)
```

**SR-IOV rule:** the **LAN VF** alone terminates the switch trunk — not Proxmox `vmbr` + PF in parallel on the same PHY.

## Automation

Phase 1 VyOS config via shell scripts in **`scripts/`** (SSH or `vyos-config` style). Re-run scripts when changing DNS provider or DHCP range.
