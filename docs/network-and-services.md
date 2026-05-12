# Network and services — logical design

High-level design for **VyOS**, **Pi-hole**, and **Tailscale** on **Proxmox VE**. **VLAN IDs, IPv4 layout, `vmbr-svc` addresses, Pi-hole upstream DNS, IoT firewall exceptions, and Tailscale ACLs / subnet routes** are **locked** below as the **target** network (see also **`agent.md` § Resolved open decisions**).

**Baby steps:** The owner is rolling out **incrementally**. **Phase 1** (below) is **VyOS-only** on a **flat LAN** — **no VLANs**, **no SR-IOV**, **DHCP**, and **Cloudflare or OpenDNS** for client DNS. Everything from **§ VLANs and IPv4 layout** onward remains the **north-star design**; adopt those pieces when ready without deleting this document.

## Phase 1 — VyOS-only starter (current rollout)

**Goal:** One **VyOS** VM on **Proxmox VE** as the household **default gateway** — **NAT**, basic **firewall**, **DHCP** on LAN, **no** Pi-hole / Tailscale / **`vmbr-svc`** yet.

| Topic | Phase 1 |
|--------|---------|
| **Proxmox ↔ VyOS** | **Linux bridges** + **virtio** (or `e1000`) — **not** SR-IOV. Typical pattern: **`vmbr-wan`** with a **physical port** to the **modem** (use **2.5G** modem LAN if available); **`vmbr-lan`** with a **physical port** to the **LAN switch / APs**. VyOS VM: **two virtual NICs**, one per bridge. **Onboard I219-V** stays **Proxmox management** — keep it off the VyOS data path unless you deliberately share it (not recommended). |
| **LAN** | **Single flat subnet** — pick one **RFC1918** block (e.g. **`192.168.1.0/24`** or reuse **`10.10.10.0/24`** as a **non-VLAN** preview of the future **private** VLAN). **No 802.1Q** to VyOS yet. |
| **DHCP** | Run on **VyOS** for the LAN segment (pools, default route = VyOS LAN IP). |
| **DNS for LAN clients** | **Cloudflare** `1.1.1.1`, `1.0.0.1` **or** **Cisco OpenDNS** `208.67.222.222`, `208.67.220.220` — advertise via **VyOS DHCP `dns-server`** (exact CLI varies by VyOS major version). **No local DNS filter** in Phase 1. Optionally set **VyOS system name servers** to the same resolvers for the router’s own lookups. |
| **Deferred (documented below as target)** | **SR-IOV**, **VLAN trunk** to VyOS, **`vmbr-svc`**, **Pi-hole** (DoT upstreams), **Tailscale**, **IoT / guest / private** segmentation and firewall tables. |

**When to open the rest of this file:** Add **VLANs** when you need **guest / IoT isolation**; add **`vmbr-svc` + Pi-hole** when you want **filtering / local DNS** instead of handing clients public resolvers directly; add **SR-IOV** when you want the **PF/VF** performance model; add **Tailscale** when remote access matters.

**Automation:** Example **Ansible** playbooks to **create** the VyOS VM on Proxmox and apply **Phase 1** settings are in **`ansible/`** — see **`ansible/README.md`**.

**Switch ↔ VyOS LAN VF:** **802.1Q trunk, tagged only** — the port to the **router NIC’s LAN** carries **VLANs 10, 20, and 30** as **tagged** frames (no shared untagged “native” data VLAN on that uplink). Downstream switch ports are **access** ports with the correct **PVID** per VLAN.

| VLAN ID | Name | IPv4 subnet | VyOS gateway (typical) | Internet | Notes |
|--------:|------|-------------|------------------------|----------|--------|
| **10** | **private** | `10.10.10.0/24` | `10.10.10.1` | **Yes** | DHCP (VyOS or switch) advertises **DNS = `10.10.0.53`** (Pi-hole on **`vmbr-svc`**). |
| **20** | **guest** | `10.10.20.0/24` | `10.10.20.1` | **Yes** | Tighter firewall toward **private**; DNS may also point to **Pi-hole** if desired. |
| **30** | **iot** | `10.10.30.0/24` | `10.10.30.1` | **No** | **VyOS forward policy** locked in **§ IoT firewall exceptions** — **no internet**; **DNS** and **NTP** exceptions only as listed there. |

**VyOS LAN subinterfaces (illustrative):** `ethX.10`, `ethX.20`, `ethX.30` where **`ethX`** is the **LAN VF** device name inside VyOS.

## Internal stub — `vmbr-svc` (locked)

| Item | Value |
|------|--------|
| **Subnet** | `10.10.0.0/24` |
| **VyOS virtio (default GW for LXCs)** | `10.10.0.1` |
| **Tailscale LXC (example static IP)** | `10.10.0.52` |
| **Pi-hole LXC (example static IP)** | `10.10.0.53` |

Use these addresses in **VyOS static routes / firewall** and in **DHCP option 6 (DNS)** on **private** / **guest** as you prefer. **Re-number only if you deliberately change the plan** — update this table and Ansible vars together.

## IoT firewall exceptions (locked)

**Goal:** The **IoT VLAN** has **no internet** and **no broad access** to **private**, **guest**, or arbitrary **`vmbr-svc`** services. Only the following **forward** rules are **locked** for VyOS (treat **iot** as its own firewall zone). **Anything not listed** from **IoT** defaults to **deny** until you add an **explicit** exception (document in Ansible or runbooks).

| Traffic | Verdict | Notes |
|---------|---------|--------|
| **IoT (`10.10.30.0/24`) → WAN** | **DROP** | Matches **“Internet: No”** in the VLAN table; no default path to the public internet. |
| **IoT → Pi-hole `10.10.0.53`** | **ALLOW** | **TCP and UDP destination port 53** only. Pi-hole is on **`vmbr-svc`**; traffic is **routed** via VyOS. After rules exist, **DHCP option 6** on **IoT** may advertise **`10.10.0.53`**. |
| **IoT → VyOS IoT SVI `10.10.30.1`** | **ALLOW** | **UDP destination port 123** (**NTP**). **Run NTP on VyOS** (or another **operator-controlled** source reachable only as you define) so IoT can sync **without** WAN. |
| **IoT → `10.10.10.0/24` (private)** | **DROP** | **No** blanket access to **private** PCs or servers. Pi-hole is **`10.10.0.53`**, not on the **private** subnet. |
| **IoT → `10.10.20.0/24` (guest)** | **DROP** | **iot ↔ guest** isolation unless you add a **documented** exception later. |
| **Cross-VLAN mDNS / Bonjour (typ. UDP 5353)** | **DROP** | **No** **mDNS reflection** or **Bonjour** bridging between **IoT** and **private/guest**. **Same-VLAN** IoT-to-IoT stays on the **switch L2** where the AP/switch allows it; VyOS does not need to “help” discovery across VLANs. |
| **IoT → Home Assistant or other hubs (e.g. TCP 8123)** | **DENY by default** | **No** locked exception. When a hub has a **stable IP** (often on **private**), add **one explicit allow** (e.g. **IoT → `10.10.10.x` TCP 8123**) and **record** it in Ansible or ops notes. |

**Admin paths:** **Pi-hole web UI**, **SSH to `vmbr-svc` hosts**, and similar are **not** in this allow list — use **private** management clients or **Tailscale** per **§ Tailscale ACLs and subnet routes**.

## Pi-hole upstream DNS (locked)

Pi-hole **forwards** queries it does not block to **recursive resolvers** over **DNS-over-TLS (DoT)** — not **plain UDP/53 to the ISP** by default (better transport privacy on the WAN path; still trust the chosen provider’s policy).

| Role | Provider | IP addresses (examples) | TLS hostname (SNI) |
|------|----------|-------------------------|---------------------|
| **Primary** | **Quad9** | `9.9.9.9`, `149.112.112.112` | `dns.quad9.net` |
| **Secondary** | **Cloudflare** | `1.1.1.1`, `1.0.0.1` | `one.one.one.one` |

**Implementation notes (Ansible / manual):** Configure in **Pi-hole → Settings → DNS** (exact UI labels vary by Pi-hole major version). Many setups use **Custom DNS** entries in the form supported by your release (e.g. `9.9.9.9#dns.quad9.net` where `#` denotes the TLS name — **confirm** against [Pi-hole DNS documentation](https://docs.pi-hole.net/) at install time). Enable **DNS-over-TLS** only if the chosen upstream list supports it in your version.

**Out of scope as default:** **ISP DNS** is **not** the default upstream; you may temporarily point Pi-hole at the ISP (or use VyOS forwarder) for **captive-portal** or **ISP-outage debugging** — document if you add a playbook toggle.

## Tailscale ACLs and subnet routes (locked)

**Goal:** The **Tailscale LXC** on **`vmbr-svc`** (`10.10.0.52` in the locked layout) is the **only** **subnet router** for this lab. **Advertised IPv4 routes** and **tailnet ACL posture** follow the tables below so **guest** and **IoT** stay off the **remote path** by default, and **LAN access over Tailscale** is **owner/admin–scoped**, not “every tailnet member.”

**Implementation reference:** Encode the details in **Tailscale admin → Access controls** (and **machine tags** / **route approval**); syntax changes over time — use [Tailscale ACLs](https://tailscale.com/kb/1018/acls/) and [Tags](https://tailscale.com/kb/1068/tags/) as the source of truth when you paste JSON.

### Machine tag (locked)

| Tag | Attach to | Purpose |
|-----|-----------|---------|
| **`tag:homelab-sr`** | **Subnet-router LXC** only | Identifies the node that may offer **approved** subnet routes; use **`tagOwners`** in ACLs so **only tailnet owners** (or your chosen admin principal) can assign this tag. |

### Subnet routes — advertise and approve (locked)

On the **subnet-router LXC**, **configure** Tailscale to **advertise** exactly these **RFC1918** routes (CLI flags or **`/etc/default/tailscaled`** / unit drop-ins — follow current Tailscale docs). In **Tailscale admin → Machines → Subnet routes**, **approve** only these prefixes for that node:

| Prefix | Role | Approve for remote use? |
|--------|------|-------------------------|
| **`10.10.0.0/24`** | **`vmbr-svc`** (Pi-hole, Tailscale LXC, VyOS virtio, future LXCs) | **Yes** |
| **`10.10.10.0/24`** | **Private** VLAN | **Yes** |
| **`10.10.20.0/24`** | **Guest** VLAN | **No** (default) — **do not** approve unless you **document** a reason and add matching **ACL** rows |
| **`10.10.30.0/24`** | **IoT** VLAN | **No** (default) — keeps **IoT** off **Tailscale** path; aligns with **§ IoT firewall exceptions** |

If you later **approve** **guest** or **IoT**, update this doc and **Ansible** together.

### ACL intent (locked)

These are **policy requirements**, not a drop-in JSON file:

1. **Who may use subnet routes:** Once routes to **`10.10.0.0/24`** and **`10.10.10.0/24`** exist, **restrict** which **sources** (`autogroup:owner`, named users, `autogroup:admin`, etc.) may **`dst`** those prefixes. **Do not** treat “any tailnet member” as trusted for **LAN** reachability.
2. **Least privilege on `vmbr-svc`:** Prefer **explicit** `dst` port lists for **Pi-hole** (`80`/`443`/`53` as you need), **SSH** to specific hosts, and other services — avoid a single **“allow `10.10.0.0/24:*` for everyone”** rule when you tighten ACLs.
3. **Proxmox host UI:** **Proxmox** is intended on **onboard management** (`I219-V`), **not** on **`vmbr-svc`**. **Default:** **do not** expose **Proxmox TCP `8006`** to the **tailnet** via **`10.10.0.0/24`** unless you **deliberately** add it; use **SSH tunnels** or **admin-only** paths from **private** if needed.
4. **Mesh between nodes:** Keep **device sharing** and **ACL `src` lists** aligned with **who** should reach **which** lab resources; **revoke** unused devices.

### VyOS and egress

- **Tailscale LXC → internet:** The LXC uses **VyOS on `vmbr-svc`** as **default gateway**; **VyOS** must **allow** that host **egress to WAN** for **Tailscale control plane** (HTTPS to Tailscale coordination endpoints — see Tailscale’s **firewall / outbound** docs for current hostnames/IPs). **Pi-hole** may or may not see that traffic depending on whether the LXC uses **Pi-hole** as DNS; either path is fine if **DNS resolution** for Tailscale works.
- **Return path for subnet-routed traffic:** Traffic **from tailnet → `10.10.10.x`** arrives at the **LXC**, then is **forwarded** via **`10.10.0.1`**. **VyOS** should **allow** **`vmbr-svc` → private** for flows **relayed** by the subnet router (same trust posture you use for other **svc → private** management traffic).

## Recommended topology — dual-port NIC, SR-IOV, Proxmox bridge

**Goal:** VyOS uses **VFs** for **fast WAN/LAN**, carries **three VLANs** on **LAN**, keeps **LXCs** on a simple **Linux bridge**, without putting the **router NIC’s PF** on `vmbr` in parallel with **VyOS production VFs** (avoids PF+VF hairpins on v1).

**Illustrated NIC:** drawings below use **Intel X550-T2** (`ixgbe`); the same **logical** layout applies to any **dual-PF** NIC you run with **SR-IOV** to VyOS (see **`docs/hardware-requirements.md` § NIC alternatives** if you change SKU).

```text
  [ Internet ]
       │
       │  (untagged or ISP VLAN — follow modem)
       ▼
  Router NIC  RJ45 WAN port  (PF0)
       │  VF0 ─────────────────────────────►  VyOS  WAN  (guest VF driver, e.g. ixgbevf)
       │
  Router NIC  RJ45 LAN port  (PHY / parent = PF1) ─── cable ───►  [ managed switch trunk ]
       │  VF from PF1 ──────────────────────►  VyOS  LAN  (guest VF driver) = only trunk endpoint
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

1. **WAN:** One **VF** on **port 1** → VyOS only; cable to **modem** — if the modem has **both 1 GbE and 2.5 GbE LAN**, use **2.5 GbE** for this link (see **`docs/hardware-requirements.md` § WAN speed**).  
2. **LAN trunk:** One **VF** on **port 2** → VyOS only; cable to a **switch port configured as trunk** carrying **private / guest / iot** VLANs (tagged). VyOS implements **`LAN-phy.10`**, **`.20`**, **`.30`** (names vary) with **per-VLAN gateways** and **firewall zones**.  
3. **IoT:** VyOS **forward filter** per **§ IoT firewall exceptions** — **drop** **iot** → **WAN**; **allow** **IoT → `10.10.0.53:53`** and **IoT → `10.10.30.1:123`** only as locked there.  
4. **LXCs / most VMs:** **`vmbr-svc`** is **internal**. Each guest has **virtio → vmbr-svc**; **default gateway = VyOS virtio IP** on that bridge. VyOS **routes/NATs** **vmbr-svc subnet** to **WAN** and treats it as **private-equivalent** trust (or a dedicated firewall zone **“svc”** you attach to **private** rules).  
5. **Pi-hole:** **Dedicated LXC** on **`vmbr-svc`** — **static IP** on the **svc** subnet (see **§ Dedicated LXC on `vmbr-svc`**). **DHCP on private VLAN** (VyOS or switch) advertises that **IP** as DNS. **VyOS** allows **private → Pi-hole:53** (and **guest → Pi-hole** if desired). **IoT → Pi-hole:53** is **locked allow** in **§ IoT firewall exceptions** (IoT has **no** L2 path to Pi-hole; traffic is **routed** IoT → **`vmbr-svc`**).

## Dedicated LXC on `vmbr-svc`

**`vmbr-svc`** is the **internal-only** Linux bridge (no physical router-NIC port). **Dedicated LXCs** are guests that **only** attach here — **virtio/veth**, **not** SR-IOV VFs. They reach **wired VLANs** and **internet** **only** via **VyOS** (default gateway = **VyOS virtio** on `vmbr-svc`).

| LXC (locked roles) | Role |
|--------------------|------|
| **Pi-hole** | DNS filtering; **static IP `10.10.0.53`** on **`vmbr-svc`**; DHCP on **private** (and optionally **guest**) sets DNS to **`10.10.0.53`**. |
| **Tailscale** | **Subnet router** (and mesh node) — **locked placement:** dedicated **LXC** on **`vmbr-svc`** (not on VyOS). Example static IP **`10.10.0.52`**; **subnet routes + ACLs** per **§ Tailscale ACLs and subnet routes** (**approve** **`10.10.0.0/24`** + **`10.10.10.0/24`** only by default). |

Other **future** LXCs (Vaultwarden, Docker host, etc.) can share **`vmbr-svc`** or get a second internal bridge if you want stronger isolation later.

**Why not bridge the router NIC PF here:** Keeping **both PFs** **unbridged** on the host (only **VFs** assigned to VMs) avoids **v1** complexity where **PF + VF** share **the LAN PHY** and the same **trunk**. If you later need a VM **on-wire in VLAN 10**, add an **extra VF** on the **LAN PF** or revisit **PF trunk → vlan-aware `vmbr`**.

### SR-IOV nuance — LAN port: PF1 vs VyOS’s VF (read this)

On **RJ45 port 2**, the **hardware** exposes **PF1** (parent function). **SR-IOV** creates **VFs** that are **children** of that PF. **VyOS’s LAN trunk** is **not** “Proxmox bridges PF1 to the switch.” It is: **the VF assigned to VyOS** is the **endpoint** that **terminates the 802.1Q trunk** toward the **managed switch**. Frames to/from **private / guest / iot** use **that VF inside the VyOS VM**.

| Question | Answer in this design |
|----------|------------------------|
| Does **Proxmox** also put **PF1** into **`vmbr0`** as the **trunk uplink** while VyOS uses a **VF from PF1**? | **No.** That pattern **conflicts**: you cannot cleanly **hand the same port** to a **Linux bridge as PF** *and* treat **VyOS’s VF** as the **sole router trunk** without **overlapping L2 roles** and **hard-to-debug** behavior. **Pick one owner** for **production** trunk traffic: here it is **VyOS via VF**. |
| What is **PF1** doing on the **Proxmox host** then? | The **host NIC driver** keeps **PF1** to **parent** SR-IOV: **create VFs**, **link state**, driver housekeeping. **Do not** rely on **PF1** as a **second parallel path** on the **same VLANs** as VyOS’s LAN VF (no **PF IP** on **private/guest/iot**, no **PF as `vmbr` slave** for that trunk). |
| Can the **PF** still pass traffic with SR-IOV enabled? | Often **yes** at the hardware/driver level, but **using the PF for production traffic** while **VyOS uses a VF on the same PHY** is **easy to get wrong** (duplicate MAC/IP expectations, VLAN hairpins, “who answers ARP?”). **Treat the LAN PF as SR-IOV parent only** unless you are deliberately redesigning. |
| Where does the **physical cable** go? | **Switch trunk port** ↔ **LAN RJ45** (second port in the **two-port** layout). Electrically that is the **LAN PF’s PHY**; **logically** the **trunk** is **consumed by VyOS** on its **LAN VF**. |

**Short version:** **one RJ45, one production L2 “face” to the switch for routing:** **VyOS’s LAN VF**. **Proxmox** reaches the world via **VyOS** (**virtio on `vmbr-svc`**) or via **onboard management** — **not** by bridging the **LAN PF**.

## Physical interfaces (locked roles; NIC SKU flexible)

| Interface | Role |
|-----------|------|
| **Onboard Intel I219-V** (1 GbE) | **Proxmox VE host only** — Web UI, SSH, updates. **Not** the VyOS WAN/LAN path; **not** where Pi-hole or other guests bind as their primary physical uplink. |
| **Add-in dual-port NIC** (inventory: **Intel X550-T2**; see **`docs/hardware-requirements.md` § NIC alternatives**) | Proxmox loads the **host driver** on each **PF** (e.g. **`ixgbe`** for X550). **VyOS VM:** **two SR-IOV VFs** — **one VF per PF** (e.g. **PF0 → VyOS WAN**, **PF1 → VyOS LAN**). **Other VMs:** may each use **additional VF(s)** from either PF. Guest VF driver depends on NIC (**`ixgbevf`**, **`i40evf`**, **Mellanox VF**, …). Most lab traffic still **routes through VyOS**; a VM with its **own LAN VF** is **on-wire at L2** on that segment (plan **VLANs / routes** so policy stays clear). |

**VMs / LXCs (Pi-hole, Tailscale node, Vaultwarden, file share, etc.):** use **virtual Ethernet** on **`vmbr-svc`** (internal bridge); **default gateway = VyOS virtio** on that bridge. Traffic to **internet** or **wired VLANs** is **routed by VyOS** from **svc** out **WAN VF** or **LAN trunk VF** as appropriate. Wired clients live in **private / guest / iot** on the **switch trunk**; they do not need to be on **`vmbr-svc`** unless you bridge otherwise.

**Implementation (locked pattern):** **not** whole-card PCIe passthrough of the **router NIC** to VyOS for WAN/LAN; **not** virtio from Proxmox bridges onto VyOS for those links. **Management stays on onboard**; **VyOS WAN/LAN = two VFs** (one per PF). **Optional:** further **VFs** to **other VMs** (see SR-IOV section).

### SR-IOV — chosen model and ops notes

- **VyOS:** exactly **2 VFs** — **one VF tied to each physical port** (each port has one **PF** in the host; the first **VF** on that port, or whichever VF BDFs you assign, goes to VyOS). Document **PCI BDFs** in the Proxmox VM config after first boot (`lspci -nn` on the host).
- **Proxmox:** add each VF as a **PCI device** (`hostdev`) on the **VyOS** VM; ensure **IOMMU** grouping allows it (Intel **VT-d** on, **`intel_iommu=on iommu=pt`** on the host where applicable).
- **Host PFs:** keep the **host driver** managing the **PFs** so VFs exist. **Do not** add **PF0/PF1** (or equivalent) to **`vmbr`** as **WAN/LAN uplinks** while **VyOS uses VFs** from those PFs — see **§ SR-IOV nuance — LAN port**. **Avoid** **PF IPs** on the **same VLANs / subnets** as VyOS **WAN/LAN VF** (prevents ARP/gateway confusion).
- **Other VMs:** any **VM** (not typical **LXCs**) that needs **near-line-rate** or lower **CPU per packet** can be given **its own VF(s)** — usually from the **LAN-side PF** so the guest sits on the **same Ethernet broadcast domain** as the switch leg behind VyOS **LAN**. Avoid handing random VMs a **WAN-PF VF** unless you intend **parallel internet paths** and know the **firewall** implications.
- **Capacity:** set **`max_vfs`** (or your driver’s equivalent) high enough that **each PF** exposes **1 VF for VyOS** on that port **plus** every **extra VF** you assign to **other VMs** (validate **N** per port in **`dmesg`** / vendor docs — driver and firmware caps apply). Each VF is a **distinct PCI function**; attach via Proxmox **`hostdev`** per VM.

**Enablement sketch (host, validate on your kernel):** firmware **VT-d** on; **`/etc/modprobe.d/<driver>.conf`** with **`options <driver> max_vfs=<N>`** (exact semantics: **per driver/kernel** — for **`ixgbe`** see Intel docs and `dmesg`).

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
              wired PCs, APs, IoT (DNS = 10.10.0.53; not L2 on IoT VLAN)
```

## Component roles

### VyOS

- **Topology (locked — option A):** **single VyOS VM** is the lab's **default gateway**; it carries **WAN VF**, **LAN trunk VF** (private/guest/iot), and one **virtio** interface to **`vmbr-svc`** (no split WAN/LAN VyOS pair).
- **Purpose:** Default gateway for homelab LAN(s), **NAT**, **firewall**, static/dynamic routing as needed. **Tailscale** runs on a **dedicated LXC** on **`vmbr-svc`**, not on VyOS (see **Tailscale** section).
- **Interfaces (WAN/LAN):** **Two SR-IOV VFs** on the **dual-port router NIC** (inventory **X550-T2** on **`ixgbe`/`ixgbevf`**) — **WAN VF** = **modem**; **LAN VF** = **802.1Q trunk** to switch with **private / guest / iot** VLAN subinterfaces.  
- **Interfaces (services stub):** **One virtio** NIC on **`vmbr-svc`** — gateway for **LXCs** and internal VMs; firewall zone typically **same trust as private** (or dedicated **svc** zone).  
- **Documentation to add:** **WAN** public addressing (DHCP/static per ISP), port forwards, site-to-site or remote access **policy**; record **VF PCI BDFs** next to **WAN/LAN** role. **VLAN IDs, RFC1918 layout, and IoT forward policy** are **locked** in **§ VLANs and IPv4 layout** and **§ IoT firewall exceptions**.

### Pi-hole

- **Purpose:** **LAN DNS** for clients — **blocklists**, **local DNS records**, **caching**, and **UI** for whitelist / timed “disable blocking.” Forwards non-blocked queries per **§ Pi-hole upstream DNS (locked)** (**DoT** to **Quad9** primary, **Cloudflare** secondary); **not** a full recursive resolver by default (no separate Unbound required for a typical home).
- **Resources:** Light for homelab use; RAM grows with **blocklists** and **query history** — tune on **16 GB** host (see mapping table).
- **Deployment (locked):** **Dedicated LXC** on **`vmbr-svc`** only — **static IP `10.10.0.53`** (see **§ Internal stub — `vmbr-svc`**); **not** on **private / guest / iot** VLANs at L2. DHCP on **private** (VyOS or switch) advertises **`10.10.0.53`** as DNS.
- **VyOS integration:** Allow **private → `10.10.0.53:53`** (and **guest** if desired); **IoT → `10.10.0.53:53`** (**TCP/UDP**) per **§ IoT firewall exceptions**. Admin access to Pi-hole web UI: restrict source zones/IPs in VyOS (policy detail when implemented); **not** from **IoT** by default.

### DNS query path (chosen model)

| Flow | Notes |
|------|--------|
| **Client (wired VLAN or svc) → Pi-hole (`10.10.0.53`) → Quad9 / Cloudflare (DoT) → Internet** | Upstreams **locked** in **§ Pi-hole upstream DNS**. |

### Tailscale

- **Purpose:** **Mesh VPN** for remote admin and **subnet routing** so remote clients reach lab subnets without hairpinning every app through the mesh.
- **Placement (locked):** **Dedicated LXC** on **`vmbr-svc`** — **subnet router** for **approved** IPv4 routes. **Not** installed on **VyOS** (router upgrades stay independent of Tailscale package cadence).
- **Tags, routes, ACLs (locked):** **§ Tailscale ACLs and subnet routes** — machine tag **`tag:homelab-sr`**, **approve** **`10.10.0.0/24`** + **`10.10.10.0/24`** only by default, **owner/admin–scoped** ACL intent, **VyOS** notes for **WAN egress** and **`svc` → private** relay.

## Proxmox mapping (initial allocations)

**Phase 1:** provision **only `vyos`** with **2× virtio** on your **WAN/LAN `vmbr`** pair; skip **pihole** / **tailscale** until **`vmbr-svc`** exists.

Rough sizing for the **current 16 GB** host; adjust after `pveperf` / real load. **Ballpark totals:** VyOS **~1.5 GiB** + Pi-hole **~512 MiB** + Tailscale **~512 MiB** + Proxmox **~2–4 GiB** leaves headroom for a few more small LXCs.

| Guest name | Type | vCPU | RAM | Primary role | Bridges / devices |
|------------|------|------|-----|--------------|-------------------|
| **vyos** | VM | 2 | **1536 MiB** | Gateway, NAT, firewall, DHCP (**Phase 1:** flat LAN; **target:** VLAN private/guest/iot) | **Target:** **2× SR-IOV VF** (WAN + LAN trunk) + **1× virtio** on **`vmbr-svc`**. **Phase 1:** **2× virtio** on **`vmbr-wan`** + **`vmbr-lan`** (or your bridge names). |
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
