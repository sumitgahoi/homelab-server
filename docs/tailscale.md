# Tailscale

Mesh VPN and **subnet router** for remote access to lab networks.

## Deployment (locked)

- **Dedicated LXC** on **`vmbr-svc`** — example static IP **`10.10.0.52`**
- **Not** on VyOS — keeps router upgrades independent of Tailscale releases
- Default gateway = VyOS **`10.10.0.1`**

## Machine tag (locked)

| Tag | Attach to |
|-----|-----------|
| **`tag:homelab-sr`** | Subnet-router LXC only |

Restrict **`tagOwners`** so only tailnet owners/admins can assign the tag.

## Subnet routes (locked)

Advertise and **approve** on the subnet router:

| Prefix | Approve? |
|--------|----------|
| `10.10.0.0/24` (vmbr-svc) | **Yes** |
| `10.10.10.0/24` (private) | **Yes** |
| `10.10.20.0/24` (guest) | **No** by default |
| `10.10.30.0/24` (iot) | **No** by default |

## ACL intent (locked)

1. Restrict which **sources** may reach **`10.10.0.0/24`** and **`10.10.10.0/24`** — not every tailnet member.
2. Prefer explicit **dst** ports on svc hosts — avoid blanket `10.10.0.0/24:*` for all users.
3. **Proxmox UI (`8006`)** stays on onboard management — do not expose via tailnet unless deliberate.
4. Revoke unused devices; keep ACLs aligned with who should reach lab resources.

Reference: [Tailscale ACLs](https://tailscale.com/kb/1018/acls/), [Tags](https://tailscale.com/kb/1068/tags/).

## VyOS notes

- Allow Tailscale LXC **egress to WAN** (HTTPS to Tailscale control plane)
- Allow **`vmbr-svc` → private** for flows relayed by the subnet router

## Phase

**Phase 2+** — after **`vmbr-svc`** and VyOS svc routing are in place.
