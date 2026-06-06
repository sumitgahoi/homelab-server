# Proxmox VE

Bare-metal hypervisor for all homelab VMs and LXCs.

## Disk layout (locked)

| Disk | Proxmox role |
|------|----------------|
| **960 EVO 250 GB** | Root filesystem only — do not default VM disks here |
| **SN770 1 TB** | Primary **Storage** for VM/LXC disks, ISOs, templates |
| **WD Red 3 TB** | Bulk mount (e.g. `/mnt/data`) — media, backups, NAS exports |

**Datacenter → Storage:** set **Disk image**, **Container**, and **ISO image** defaults to the SN770-backed store.

Pool format (ext4 directory, LVM-thin, or ZFS) is your choice at install — record the storage ID (e.g. `local-lvm`, `vmdata`).

## NIC roles

| Interface | Role |
|-----------|------|
| **Onboard 2.5 GbE** | Host management — Web UI, SSH, updates. Not VyOS WAN/LAN. |
| **10Gtek dual-port NIC** | **WAN** + **LAN** to VyOS — see **`vyos.md`**. |

### Phase 1 (now)

Attach each router NIC port to a Linux bridge (`vmbr-wan`, `vmbr-lan`). VyOS gets **virtio** on both bridges — **no SR-IOV**.

Cable **modem 2.5G LAN → WAN bridge** when the modem exposes both 1G and 2.5G.

### Target (later)

VyOS uses **two SR-IOV VFs** — one VF per physical port. **Do not** put the LAN **PF** in `vmbr` as a trunk uplink while VyOS holds the production **LAN VF** on that PHY.

Enable **VT-d / IOMMU** in firmware. Host kernel typically needs:

```bash
GRUB_CMDLINE_LINUX_DEFAULT="quiet intel_iommu=on iommu=pt"
```

Set **`max_vfs`** on the host driver so each PF exposes enough VFs for VyOS plus any other VM assignments. Verify with `dmesg` and IOMMU groups after reboot.

## GPU — RTX 5060 Ti

| Item | Detail |
|------|--------|
| **Card** | **NVIDIA GeForce RTX 5060 Ti** — prefer **16 GB GDDR7** |
| **TGP** | **180 W** (reference); **1× PCIe 8-pin** |
| **PSU** | **Seasonic GX-850** — NVIDIA min **600 W** system; **850 W** has margin |
| **Use** | **GPU passthrough** to a VM, host transcode (**Jellyfin** / **Frigate**), or **AI** workloads — document assignment at install |

**Branch impact:** adds up to **~180 W** (~**1.5 A**) on **socket B** homelab peak — **single 20 A** homerun remains sufficient (see **§ Basement dedicated power**).

## Internal bridge — `vmbr-svc`

Linux bridge with **no physical slave** — internal stub for Pi-hole, Tailscale, and VyOS virtio. Subnet **`10.10.0.0/24`** — see **`vyos.md` § Internal stub**.

Create when you move past Phase 1.

## Guest sizing (initial)

| Guest | Type | vCPU | RAM | Phase 1 devices |
|-------|------|------|-----|-----------------|
| **vyos** | VM | 2 | 1536 MiB | 2× virtio on `vmbr-wan` + `vmbr-lan` |
| **pihole** | LXC | 1 | 512 MiB | `vmbr-svc` only (later) |
| **tailscale** | LXC | 1 | 512 MiB | `vmbr-svc` only (later) |

## BIOS checklist

- [ ] **270K Plus** supported on Z890 — update BIOS if needed
- [ ] **32 GB DDR5** installed; plan **64 GB+** for full stack
- [ ] **PL1 / PL2** set for efficiency vs performance
- [ ] **VT-d / IOMMU** enabled
- [ ] **GPU** clearance in case; **Above 4G Decoding** + **Re-Size BAR** if using passthrough
- [ ] Case fits **3× HDD** + **PA120 SE**

## Automation

Shell scripts under **`scripts/`** (not Ansible). Proxmox-side tasks: bridge setup, VM creation, storage wiring — keep scripts idempotent where possible.
