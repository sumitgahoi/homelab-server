# Agent context — homelab and AV home theatre

Update this file when goals, stack choices, or doc layout change.

## Purpose

Document and automate:

1. **Homelab** — a **Proxmox VE** host running **VyOS** (routing), **Pi-hole** (DNS), and **Tailscale** (remote access). Start small, grow into segmented VLANs without rewriting the north-star design.
2. **AV home theatre** — receiver, amplification, in-wall speakers, display, and source devices — **`docs/equipment-inventory.md`** + **`docs/av-theatre.md`**.

## Implementation phases

| Phase | Scope |
|-------|--------|
| **Phase 1 (now)** | **VyOS only** — Proxmox **bridges + virtio** WAN/LAN, **flat LAN**, **DHCP**, client DNS = **Cloudflare** or **OpenDNS**. No Pi-hole, Tailscale, VLANs, or SR-IOV. |
| **Later** | **`vmbr-svc`**, VLANs **10/20/30**, Pi-hole, Tailscale, **SR-IOV VFs**, IoT firewall — per service docs below. |

## Hardware (summary)

**Authoritative list:** **`docs/equipment-inventory.md`**

| Category | Detail |
|----------|--------|
| **CPU** | Core Ultra 7 **270K Plus** — optional **PL1 = PL2 = 65 W** for 24/7 |
| **Board** | Gigabyte **Z890** — onboard **2.5G** = Proxmox management only |
| **RAM** | **32 GB DDR5** — plan **64 GB+** for full stack |
| **Router NIC** | **10Gtek X550-T2 clone** — WAN + LAN to VyOS |
| **Storage** | 960 EVO = OS · SN770 = VMs/LXCs · WD Red = bulk |
| **ISP / WAN** | **Xfinity 2 Gbps** · **Arris S33** modem (**2.5G** → VyOS WAN) |
| **Rack / LAN** | Eaton **SR18UB**, **PDU1215**, Cisco **CBS350-24FP-4G** |
| **Case** | **Open** — must fit **3× HDD** + **PA120 SE** |
| **AV power** | **Furman PST-8** on **planned dedicated basement circuit** |
| **AV core** | **Denon AVR-X3700H** · **Hypex NCx500** (3ch) · **HSU Research VTF-15H MK2** |
| **AV speakers** | **3× B&W CWM73 S2** (LCR) · **2× CWM663** (surround) · **2× CCM662** (Atmos) |
| **Display / sources** | **LG B7** + speakers/sub in **family room** · **Denon / Hypex / sources** in **basement rack** · wiring **done** |

**NVMe note:** map disks by serial/model in Proxmox — enumeration order can differ from physical slots.

## Owner requirements (target design)

Long-term locked choices. **Phase 1** implements only the VyOS subset.

1. **Proxmox VE** on bare metal — VMs and LXCs for services.
2. **Disk roles** — see **`docs/proxmox.md` § Disk layout**.
3. **NIC policy** — Phase 1: bridges + virtio; target: **2× SR-IOV VF** (WAN + LAN trunk) + VyOS virtio on **`vmbr-svc`**. Onboard 2.5G = management only.
4. **Single VyOS VM** — default gateway; not split across multiple VyOS guests.
5. **Three VLANs (target)** — private **10**, guest **20**, iot **30**; subnets and IoT rules in **`docs/vyos.md`**.
6. **Pi-hole** — LXC on **`vmbr-svc`**, upstream DoT in **`docs/pihole.md`**.
7. **Tailscale** — subnet-router LXC on **`vmbr-svc`**, ACLs in **`docs/tailscale.md`**.

## Automation

**Shell scripts** under **`scripts/`** — no Ansible. Scripts should be small, readable, and safe to re-run where possible (Proxmox API/CLI, SSH to VyOS, etc.).

## Repository layout

| Path | Role |
|------|------|
| `agent.md` | This file — context for humans and agents |
| `docs/equipment-inventory.md` | Parts list and cabling |
| `docs/av-theatre.md` | AV signal flow, pre-outs, Audyssey, HDMI |
| `docs/proxmox.md` | Hypervisor, storage, bridges, SR-IOV |
| `docs/vyos.md` | Router — Phase 1 + target VLANs and firewall |
| `docs/pihole.md` | DNS filtering |
| `docs/tailscale.md` | Remote access / subnet routes |
| `scripts/` | Shell automation |

## Open decisions

| Topic | Notes |
|-------|--------|
| **Case** | Reuse **Fractal Define C** or buy new — **3× 3.5″ HDD** constraint; see **`docs/equipment-inventory.md`**. |

## Resolved decisions

| Topic | Outcome |
|-------|---------|
| **VyOS topology** | Single VM — WAN VF, LAN trunk VF, `vmbr-svc` virtio |
| **VLAN / IPv4 layout** | VLANs 10/20/30, `10.10.0.0/24` svc stub — **`docs/vyos.md`** |
| **Pi-hole upstream** | DoT: Quad9 primary, Cloudflare secondary — **`docs/pihole.md`** |
| **IoT firewall** | No internet; Pi-hole + NTP exceptions only — **`docs/vyos.md`** |
| **Tailscale** | `tag:homelab-sr`, approve `10.10.0.0/24` + `10.10.10.0/24` — **`docs/tailscale.md`** |
| **Automation** | Shell scripts, not Ansible |
| **AV physical layout** | **Basement rack** = Denon, Hypex, Furman, sources; **family room** = TV, all speakers, HSU sub; structured wiring **complete** — **`docs/av-theatre.md`** |
| **Basement rack power** | **Dedicated electrical line planned** — feeds **PDU1215** + **Furman**; family room TV/sub on separate circuits |
