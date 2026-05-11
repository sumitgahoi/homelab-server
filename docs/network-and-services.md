# Network and services — logical design

High-level design for **VyOS**, **Pi-hole**, and **Tailscale** on **Proxmox VE**. Refine IP schemes, VLAN IDs, and Pi-hole upstreams as you implement.

## VLANs (required)

| VLAN | Purpose | Internet | Notes |
|------|---------|----------|--------|
| **Private** | Trusted phones, PCs, servers | **Yes** | Wired clients here; **DNS** points at **Pi-hole’s IP** (Pi-hole runs on **`vmbr-svc`**, not on this VLAN’s L2 — see **Dedicated LXC on `vmbr-svc`**). |
| **Guest** | Visitors, untrusted devices | **Yes** | Isolate from **private** L3 (and optionally L2); optional bandwidth or DNS policy in VyOS. |
| **IoT** | Cameras, plugs, speakers, etc. | **No** | **Default:** VyOS **drops forward IoT → WAN**. **Optional:** VyOS allows **IoT → Pi-hole’s static IP on `vmbr-svc`:53** (UDP/TCP) so IoT can resolve locally **without** putting Pi-hole on the IoT VLAN; Pi-hole does **not** live on the IoT segment. |

**Example IDs (change to match your switch):** `10` = private, `20` = guest, `30` = iot — document final IDs in VyOS + switch config.

## Recommended topology — X550-T2, SR-IOV, and Proxmox bridge

**Goal:** VyOS uses **VFs** for **fast WAN/LAN**, carries **three VLANs** on **LAN**, keeps **LXCs** on a simple **Linux bridge**, without putting the **X550 PF** on `vmbr` (avoids PF+VF hairpins on v1).

```text
  [ Internet ]
       │
       │  (untagged or ISP VLAN — follow modem)
       ▼
  X550  RJ45 port 1  (PF0)
       │  VF0 ─────────────────────────────►  VyOS  WAN  (ixgbevf)
       │
  X550  RJ45 port 2  (PHY / parent = PF1) ─── cable ───►  [ managed switch trunk ]
       │  VF from PF1 ──────────────────────►  VyOS  LAN  (ixgbevf) = only trunk endpoint
       │                                       (not: Proxmox vmbr + PF1 in parallel)
       │                                      eth1.10 private
       │                                      eth1.20 guest
       │                                      eth1.30 iot  ──► firewall: NO internet

  Inside Proxmox:
       vmbr-svc  (Linux bridge, NO physical slave — internal “stub”)
         ├──  veth*  LXCs (Pi-hole, Tailscale subnet router, Docker, Vaultwarden, …)
         ├──  virtio  other VMs (optional)
         └──  virtio  VyOS “inside” NIC  (gateway for vmbr-svc)

  Proxmox host management:  onboard I219-V only (separate IP space from LAN VLANs).
```

**Behavior:**

1. **WAN:** One **VF** on **port 1** → VyOS only; cable to **modem**.  
2. **LAN trunk:** One **VF** on **port 2** → VyOS only; cable to a **switch port configured as trunk** carrying **private / guest / iot** VLANs (tagged). VyOS implements **`LAN-phy.10`**, **`.20`**, **`.30`** (names vary) with **per-VLAN gateways** and **firewall zones**.  
3. **IoT:** VyOS **forward filter** — **drop** **iot** → **wan**; tune **iot ↔ private** (DNS/NTP) as needed.  
4. **LXCs / most VMs:** **`vmbr-svc`** is **internal**. Each guest has **virtio → vmbr-svc**; **default gateway = VyOS virtio IP** on that bridge. VyOS **routes/NATs** **vmbr-svc subnet** to **WAN** and treats it as **private-equivalent** trust (or a dedicated firewall zone **“svc”** you attach to **private** rules).  
5. **Pi-hole:** **Dedicated LXC** on **`vmbr-svc`** — **static IP** on the **svc** subnet (see **§ Dedicated LXC on `vmbr-svc`**). **DHCP on private VLAN** (VyOS or switch) advertises that **IP** as DNS. **VyOS** allows **private → Pi-hole:53** (and **guest → Pi-hole** if desired). **IoT → Pi-hole:53** only via **explicit** firewall rule (IoT has **no** L2 path to Pi-hole; traffic is **routed** IoT → **svc** IP).

## Dedicated LXC on `vmbr-svc`

**`vmbr-svc`** is the **internal-only** Linux bridge (no physical X550 port). **Dedicated LXCs** are guests that **only** attach here — **virtio/veth**, **not** SR-IOV VFs. They reach **wired VLANs** and **internet** **only** via **VyOS** (default gateway = **VyOS virtio** on `vmbr-svc`).

| LXC (locked roles) | Role |
|--------------------|------|
| **Pi-hole** | DNS filtering for the house; **static IP** on **svc** subnet; DHCP on **private** (and optionally **guest**) points DNS here. |
| **Tailscale** | **Subnet router** (and mesh node) — **locked placement:** dedicated **LXC** on **`vmbr-svc`** (not on VyOS). Advertises lab routes per ACLs; keeps router upgrades decoupled from Tailscale client churn. |

Other **future** LXCs (Vaultwarden, Docker host, etc.) can share **`vmbr-svc`** or get a second internal bridge if you want stronger isolation later.

**Why not bridge the X550 PF here:** Keeping **both PFs** **unbridged** on the host (only **VFs** assigned to VMs) avoids **v1** complexity where **PF + VF** share **port 2** and the same **trunk**. If you later need a VM **on-wire in VLAN 10**, add an **extra VF** on **PF1** or revisit **PF trunk → vlan-aware `vmbr`**.

### SR-IOV nuance — LAN port: PF1 vs VyOS’s VF (read this)

On **RJ45 port 2**, the **hardware** exposes **PF1** (parent function). **SR-IOV** creates **VFs** that are **children** of that PF. **VyOS’s LAN trunk** is **not** “Proxmox bridges PF1 to the switch.” It is: **the VF assigned to VyOS** is the **endpoint** that **terminates the 802.1Q trunk** toward the **managed switch**. Frames to/from **private / guest / iot** use **that VF inside the VyOS VM**.

| Question | Answer in this design |
|----------|------------------------|
| Does **Proxmox** also put **PF1** into **`vmbr0`** as the **trunk uplink** while VyOS uses a **VF from PF1**? | **No.** That pattern **conflicts**: you cannot cleanly **hand the same port** to a **Linux bridge as PF** *and* treat **VyOS’s VF** as the **sole router trunk** without **overlapping L2 roles** and **hard-to-debug** behavior. **Pick one owner** for **production** trunk traffic: here it is **VyOS via VF**. |
| What is **PF1** doing on the **Proxmox host** then? | **`ixgbe`** keeps **PF1** to **parent** SR-IOV: **create VFs**, **link state**, driver housekeeping. **Do not** rely on **PF1** as a **second parallel path** on the **same VLANs** as VyOS’s LAN VF (no **PF IP** on **private/guest/iot**, no **PF as `vmbr` slave** for that trunk). |
| Can the **PF** still pass traffic on Intel X550 with SR-IOV enabled? | Often **yes** at the hardware/driver level, but **using PF1 for real traffic** while **VyOS uses a VF on the same port** is **easy to get wrong** (duplicate MAC/IP expectations, VLAN hairpins, “who answers ARP?”). **Treat PF1 as SR-IOV parent only** unless you are deliberately redesigning. |
| Where does the **physical cable** go? | **Switch trunk port** ↔ **X550 RJ45 port 2**. Electrically that is **PF1’s PHY**; **logically** the **trunk** is **consumed by VyOS** on its **LAN VF**. |

**Short version:** **one RJ45, one production L2 “face” to the switch for routing:** **VyOS’s VF**. **Proxmox** reaches the world via **VyOS** (**virtio on `vmbr-svc`**) or via **onboard management** — **not** by bridging **PF1**.

## Physical interfaces (locked)

| Interface | Role |
|-----------|------|
| **Intel X550-T2** (two RJ45, two **PFs**) | Proxmox loads **`ixgbe`** on each **PF**. **VyOS VM:** **two SR-IOV VFs** — **one VF per PF** (e.g. **PF0 → VyOS WAN**, **PF1 → VyOS LAN**). **Other VMs:** may each use **additional VF(s)** from either PF (common pattern: **spare VFs on the LAN PF** for line-rate workloads). Guest driver on VF consumers: **`ixgbevf`**. Most lab traffic still **routes through VyOS**; a VM with its **own LAN VF** is **on-wire at L2** on that segment (plan **VLANs / routes** so policy stays clear). |
| **Onboard Intel I219-V** (1 GbE) | **Proxmox VE host only** — Web UI, SSH, updates. **Not** the VyOS WAN/LAN path; **not** where Pi-hole or other guests bind as their primary physical uplink. |

**VMs / LXCs (Pi-hole, Tailscale node, Vaultwarden, file share, etc.):** use **virtual Ethernet** on **`vmbr-svc`** (internal bridge); **default gateway = VyOS virtio** on that bridge. Traffic to **internet** or **wired VLANs** is **routed by VyOS** from **svc** out **WAN VF** or **LAN trunk VF** as appropriate. Wired clients live in **private / guest / iot** on the **switch trunk**; they do not need to be on **`vmbr-svc`** unless you bridge otherwise.

**Implementation (locked):** **not** whole-card PCIe passthrough of the X550 to VyOS for WAN/LAN; **not** virtio from Proxmox bridges onto VyOS for those links. **Management stays on onboard**; **VyOS WAN/LAN = two VFs** (one per PF). **Optional:** further **VFs** to **other VMs** (see SR-IOV section).

### SR-IOV — chosen model and ops notes

- **VyOS:** exactly **2 VFs** — **one VF tied to each physical port** (each port has one **PF** in the host; the first **VF** on that port, or whichever VF BDFs you assign, goes to VyOS). Document **PCI BDFs** in the Proxmox VM config after first boot (`lspci -nn` on the host).
- **Proxmox:** add each VF as a **PCI device** (`hostdev`) on the **VyOS** VM; ensure **IOMMU** grouping allows it (Intel **VT-d** on, **`intel_iommu=on iommu=pt`** on the host where applicable).
- **Host PFs:** keep **`ixgbe`** managing the **PFs** so VFs exist. **Do not** add **PF0/PF1** to **`vmbr`** as **WAN/LAN uplinks** while **VyOS uses VFs** from those PFs — see **§ SR-IOV nuance — LAN port**. **Avoid** **PF IPs** on the **same VLANs / subnets** as VyOS **WAN/LAN VF** (prevents ARP/gateway confusion).
- **Other VMs:** any **VM** (not typical **LXCs**) that needs **near-line-rate** or lower **CPU per packet** can be given **its own VF(s)** — usually from the **LAN-side PF** so the guest sits on the **same Ethernet broadcast domain** as the switch leg behind VyOS **LAN**. Avoid handing random VMs a **WAN-PF VF** unless you intend **parallel internet paths** and know the **firewall** implications.
- **Capacity:** set **`options ixgbe max_vfs=<N>`** high enough that **each PF** exposes **1 VF for VyOS** on that port **plus** every **extra VF** you assign to **other VMs** (validate **N** per port in **`dmesg`** / Intel docs — driver and firmware caps apply). Each VF is a **distinct PCI function**; attach via Proxmox **`hostdev`** per VM.

**Enablement sketch (host, validate on your kernel):** firmware **VT-d** on; **`/etc/modprobe.d/ixgbe.conf`** with **`options ixgbe max_vfs=<N>`** (exact semantics: **per driver/kernel** — confirm with `dmesg` and Intel docs).

- **PF0 (WAN port):** In the **current** design, **only VyOS** uses a VF here (**WAN**). **Do not** assign **WAN-PF VFs** to other VMs unless intentional. In practice you only need **one usable VF pool** on this port → often **`max_vfs=1`** on PF0 is enough (or a global `max_vfs` where **only the first VF** on PF0 is consumed).
- **PF1 (LAN / trunk port):** **VyOS LAN** uses **one VF**; each **extra VM** that needs a **LAN-side VF** needs **another** VF from **PF1**. Example: VyOS **+** two other VMs each with **one** VF on PF1 ⇒ **at least 3** VFs must exist on **that** port’s pool (same math as before). **Asymmetric needs are normal:** WAN side **minimal**, LAN side **higher** if the module applies one **N** to both ports, set **N** to the **maximum** required by **either** PF (e.g. **LAN’s** count), then **only attach** the VyOS WAN VF from PF0 and leave spare VFs unused — or use driver-specific options if your distro documents **per-port** VF counts.

**Pi-hole / most LXCs:** use **virtio** (or veth) on **`vmbr-svc`** or other **internal Linux bridges** — **not** on the **LAN VF** path (no physical port; traffic goes **L3 via VyOS**). **Do not** put Pi-hole on an **SR-IOV VF** unless you redesign (not required).

**Paths not used in this design:** full **PF** passthrough of the entire card to VyOS; **virtio-only** WAN/LAN from Proxmox bridges for VyOS.

## Reference topology (conceptual)

Pi-hole and Tailscale are **inside Proxmox** on **`vmbr-svc`** (no physical cable to the switch). Wired **private / guest / iot** clients attach only via the **managed switch** trunk to VyOS **LAN VF**.

```text
                         Internet
                              │
                     [ modem / ONT ]
                              │
              ┌───────────────┴───────────────┐
              │  Proxmox VE (this host)        │
              │  ┌──────────────────────────┐  │
              │  │ vmbr-svc (internal only) │  │
              │  │  · Pi-hole LXC           │  │
              │  │  · Tailscale LXC (subnet │  │
              │  │    router, locked)      │  │
              │  │  · virtio ← VyOS stub NIC │  │
              │  └───────────┬──────────────┘  │
              │              │                   │
              │  ┌───────────▼──────────────┐   │
              │  │ VyOS VM                  │   │
              │  │  WAN VF ────────────────────┼─┼──► port 1 → modem
              │  │  LAN VF ────────────────────┼─┼──► port 2 → [ switch trunk ]
              │  └──────────────────────────┘   │
              └───────────────────────────────────┘
                              │
                              ▼
              [ managed switch: private | guest | iot VLANs ]
                              │
              wired PCs, APs, IoT (DNS = Pi-hole IP on vmbr-svc; not L2 on IoT VLAN)
```

## Component roles

### VyOS

- **Purpose:** Default gateway for homelab LAN(s), **NAT**, **firewall**, static/dynamic routing as needed. **Tailscale** runs on a **dedicated LXC** on **`vmbr-svc`**, not on VyOS (see **Tailscale** section).
- **Interfaces (WAN/LAN):** **Two SR-IOV VFs** on the **Intel X550-T2** — **`ixgbevf`** in VyOS; **WAN VF** = **port 1** (modem); **LAN VF** = **port 2** **802.1Q trunk** to switch with **private / guest / iot** VLAN subinterfaces.  
- **Interfaces (services stub):** **One virtio** NIC on **`vmbr-svc`** — gateway for **LXCs** and internal VMs; firewall zone typically **same trust as private** (or dedicated **svc** zone).  
- **Documentation to add:** Public vs private addressing, port forwards, **VLAN ID ↔ zone** table, **IoT → WAN drop** + exceptions, any site-to-site or remote access rules at a **policy** level (not full config until Ansible); record **VF PCI BDFs** next to **WAN/LAN** role.

### Pi-hole

- **Purpose:** **LAN DNS** for clients — **blocklists**, **local DNS records**, **caching**, and **UI** for whitelist / timed “disable blocking.” Forwards non-blocked queries to **upstream resolvers** you configure (e.g. Cloudflare, Quad9, ISP); it is **not** a full recursive resolver by default (no separate Unbound required for a typical home).
- **Resources:** Light for homelab use; RAM grows with **blocklists** and **query history** — tune on **16 GB** host (see mapping table).
- **Deployment (locked):** **Dedicated LXC** on **`vmbr-svc`** only — **static IP** on **svc** subnet; **not** on **private / guest / iot** VLANs at L2. DHCP on **private** (VyOS or switch) advertises **Pi-hole’s svc IP** as DNS.
- **VyOS integration:** Allow **private → Pi-hole IP:53** (and **guest** if desired); optional **IoT → Pi-hole IP:53** per firewall. Admin access to Pi-hole web UI: restrict source zones/IPs in VyOS (policy detail when implemented).

### DNS query path (chosen model)

| Flow | Notes |
|------|--------|
| **Client (wired VLAN or svc) → Pi-hole (`vmbr-svc` IP) → upstream resolvers → Internet** | DNS hits **Pi-hole’s routed IP**, not an L2 hop on the IoT VLAN; upstream choice in `agent.md` open decisions. |

### Tailscale

- **Purpose:** **Mesh VPN** for remote admin and **subnet routing** so remote clients reach lab subnets without hairpinning every app through the mesh.
- **Placement (locked):** **Dedicated LXC** on **`vmbr-svc`** — **subnet router** for advertised routes (e.g. **svc** subnet, **private / guest / iot** prefixes as policy allows). **Not** installed on **VyOS** (router upgrades stay independent of Tailscale package cadence).
- **Documentation to add:** Node tags, **subnet routes**, **ACL** posture in Tailscale admin (high level until Ansible encodes it); confirm **VyOS** allows **tailscale LXC → DNS / APIs** as needed.

## Proxmox mapping (initial allocations)

Rough sizing for the **current 16 GB** host; adjust after `pveperf` / real load. **Ballpark totals:** VyOS **~1.5 GiB** + Pi-hole **~512 MiB** + Tailscale **~512 MiB** + Proxmox **~2–4 GiB** leaves headroom for a few more small LXCs.

| Guest name | Type | vCPU | RAM | Primary role | Bridges / devices |
|------------|------|------|-----|--------------|-------------------|
| **vyos** | VM | 2 | **1536 MiB** | Gateway, NAT, firewall, **VLAN private/guest/iot**, DHCP (optional) | **2× SR-IOV VF** (WAN PF0 + LAN trunk PF1) + **1× virtio** on **`vmbr-svc`** |
| **pihole** | LXC | 1 | **512 MiB** | DNS filtering (locked) | **`vmbr-svc`** only; static **svc** IP |
| **tailscale** | LXC | 1 | **512 MiB** | Tailscale **subnet router** (locked) | **`vmbr-svc`** only |
| *future:* vaultwarden, docker, … | LXC | *TBD* | *TBD* | As planned | **`vmbr-svc`** (or new internal bridge) |

## Security and operations (documentation hooks)

- **Management:** Proxmox web UI exposure — VPN-only vs. restricted LAN; backup destination.
- **Updates:** Hypervisor and guest patch cadence; VyOS upgrade strategy.
- **Backups:** What to snapshot (VyOS config export, Pi-hole gravity/lists + config, Proxmox backup jobs).

## Related documents

- [Hardware requirements](hardware-requirements.md) — NIC speeds and cabling.
- [agent.md](../agent.md) — project context and open decisions.
