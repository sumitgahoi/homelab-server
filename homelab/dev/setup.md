# Dev VM setup

CURRENT. How to create VM 101 and install the Debian workstation described in `README.md`.

Do this from the Mac on Trusted (VLAN 10) and from the Proxmox host. Do not change VyOS, the CBS350, or `/etc/network/interfaces` on the host. Do not create CT 109.

The installer ISO this procedure uses is Debian 13.7.0 netinst. If that file has been removed from `current`, take the newer `debian-13.*-amd64-netinst.iso` from the same directory and use that filename in the `qm` commands below.

---

# Step 1 — Create VM 101

On the Proxmox host. The host is `root@10.10.10.3`.

```bash
ssh root@10.10.10.3
```

Abort if VM 101 already exists. This refuses to overwrite a guest that happens to use the same ID.

```bash
test ! -f /etc/pve/qemu-server/101.conf
```

`local-lvm` is the SN770 thin pool (VM disks). `Avail` must be greater than 100G so the disk can fill. Stop here if it is not.

```bash
pvesm status
```

Download the netinst ISO onto `local` when that file is missing. `local:iso/` on this host is `/var/lib/vz/template/iso/`.

```bash
ls /var/lib/vz/template/iso/debian-13.7.0-amd64-netinst.iso
```

If `ls` fails:

```bash
wget -O /var/lib/vz/template/iso/debian-13.7.0-amd64-netinst.iso https://cdimage.debian.org/debian-cd/current/amd64/iso-cd/debian-13.7.0-amd64-netinst.iso

```

Create the VM. One virtio NIC on `vmbr0` with `tag=10` (Trusted). No MAC pin. Balloon off so Docker always sees 16 GB. `--agent 1` opens the guest-agent channel; the package is installed later. The CD is first in the boot order so the installer runs.

```bash
qm create 101 \
  --name dev \
  --cores 4 \
  --sockets 1 \
  --memory 16384 \
  --balloon 0 \
  --cpu host \
  --ostype l26 \
  --onboot 1 \
  --agent 1 \
  --net0 virtio,bridge=vmbr0,tag=10 \
  --virtio0 local-lvm:100 \
  --ide2 local:iso/debian-13.7.0-amd64-netinst.iso,media=cdrom \
  --boot order='ide2;virtio0'
```

```bash
qm start 101
```

---

# Step 2 — Install Debian

Console: `https://10.10.10.3:8006` → VM 101 → Console. There is no serial port on this guest.

Choose **Install** (text installer).

| Installer question | Answer |
|--------------------|--------|
| Hostname | `dev` |
| Domain | `home.arpa` |
| Root password | leave empty, and confirm empty |
| User | `sumit` (full name can be Sumit) |
| User password | a password you will type once more for `ssh-copy-id` |
| Disk | Guided — use entire disk. The ~100 GB virtio disk, not the CD |
| Partition layout | All files in one partition |
| Mirror | the default Debian mirror |
| Software | SSH server and standard system utilities only |

Leave the root password empty so Debian puts `sumit` in the `sudo` group. Uncheck the desktop environment, GNOME, and the print server.

The installer will DHCP an address in `10.10.10.100`–`.250`. That is temporary. Step 3 replaces it.

Let the installer reboot. If the ISO menu appears again, do not start the installer. The guest agent is not installed, so the host cannot ask the guest to shut down cleanly. Power it off:

```bash
qm stop 101
```

Drop the CD. The ISO file stays on disk. Boot the virtio disk.

```bash
qm set 101 --delete ide2
```

```bash
qm set 101 --boot order=virtio0
```

```bash
qm start 101
```

---

# Step 3 — Static address

Still on the Proxmox console. Log in as `sumit`.

The virtio NIC on a default Proxmox machine is usually `ens18`. Confirm the name: the line that is not `lo`.

```bash
ip -br link
```

The rest of this step writes `ens18`. If the name differs, use that name instead.

Replace the DHCP stanza with a static address. `10.10.10.10/24` is outside the Trusted pool, so VyOS will not hand it to another client. The gateway is VyOS on VLAN 10.

Edit `/etc/network/interfaces` so the `ens18` lines are:

```text
allow-hotplug ens18
iface ens18 inet static
    address 10.10.10.10/24
    gateway 10.10.10.1
    dns-nameservers 10.10.10.1
    dns-search home.arpa
```

Leave the `lo` stanza and the `source /etc/network/interfaces.d/*` line as they are. Do not add an IPv6 stanza.

Trusted DNS is the VLAN gateway, and the Trusted search domain is `home.arpa`. VyOS forwards to AdGuard. This guest is static, so it does not learn that domain from DHCP. Write the resolver file directly:

```bash
printf 'nameserver 10.10.10.1\nsearch home.arpa\n' | sudo tee /etc/resolv.conf
```

Apply the address from this console. `ifdown` drops the DHCP address, which would kill an SSH session.

```bash
sudo ifdown ens18
```

```bash
sudo ifup ens18
```

`ens18` should show `10.10.10.10/24`, and the default route should be `10.10.10.1`.

```bash
ip -4 addr show ens18
```

```bash
ip route
```

```bash
ping -c 3 10.10.10.1
```

```bash
ping -c 3 1.1.1.1
```

A link-local IPv6 address may exist. Leave it. This VLAN has no IPv6 router.

---

# Step 4 — SSH key

On the Mac. The Mac must be on Trusted. `ssh-copy-id` installs the Mac's default key and will ask for the `sumit` password this once.

```bash
ssh-copy-id sumit@10.10.10.10
```

Confirm a second login succeeds and does not ask for a password. Leave that session open until Step 5 is done.

```bash
ssh sumit@10.10.10.10
```

Copy the sshd drop-in from `homelab/ssh` in this repository. It is the same file Proxmox and the other Debian guests use.

```bash
scp disable-passwords.conf sumit@10.10.10.10:/tmp/disable-passwords.conf
```

On the VM:

```bash
sudo cp /tmp/disable-passwords.conf /etc/ssh/sshd_config.d/disable-passwords.conf
```

`reload` tells sshd to reread that directory. Existing sessions stay up. New sessions can no longer use a password. Console login on the Proxmox UI still can.

```bash
sudo systemctl reload ssh
```

From the Mac, this must fail:

```bash
ssh -o PreferredAuthentications=password -o PubkeyAuthentication=no sumit@10.10.10.10
```

The `~/.ssh/config` alias is Step 10, after `dev.home.arpa` resolves. Until then, keep using `sumit@10.10.10.10`.

---

# Step 5 — Packages

On the VM, as `sumit`.

Confirm this is Debian 13 before adding Docker’s repository. The codename must be `trixie`.

```bash
grep VERSION_CODENAME /etc/os-release
```

`apt update` refreshes the Debian package lists. The following install is the host tooling only. Git is the client. `qemu-guest-agent` reports the VM to Proxmox. `ripgrep` provides `rg`. No Go, Node, or other language SDK.

```bash
sudo apt update
```

```bash
sudo apt install git curl ca-certificates htop tmux ripgrep jq unzip qemu-guest-agent
```

The agent package enables its service. From the Proxmox host, `ping` here is the guest agent answering, not ICMP. No output and a zero exit means the channel works.

```bash
qm agent 101 ping
```

---

# Step 6 — Docker

On the VM. Docker’s own apt repository, not Debian’s `docker.io` package. Dev Containers expects a current engine, Buildx, and Compose.

`/etc/apt/keyrings` is where apt looks for repository signing keys. `install -d` creates the directory. Mode `0755` makes the directory traversable. The key file itself is made world-readable so apt (which drops privileges) can read it.

```bash
sudo install -m 0755 -d /etc/apt/keyrings
```

```bash
sudo curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc
```

```bash
sudo chmod a+r /etc/apt/keyrings/docker.asc
```

This adds Docker’s Debian stable repo for `trixie` only.

```bash
echo 'deb [arch=amd64 signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/debian trixie stable' | sudo tee /etc/apt/sources.list.d/docker.list
```

```bash
sudo apt update
```

`docker-ce` is the engine. `docker-ce-cli` is the client. `containerd.io` is the runtime. The two plugins are what Dev Containers uses to build and to run Compose.

```bash
sudo apt install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
```

`usermod -aG docker` lets `sumit` talk to the Docker socket without sudo. Group membership is read at login, so this session does not have it yet.

```bash
sudo usermod -aG docker sumit
```

Log out and back in as `sumit` (`exit`, then `ssh dev`).

`docker run --rm hello-world` pulls a tiny image, runs it, and deletes the container. The image stays cached. Success is a printed message that the install works.

```bash
docker run --rm hello-world
```

---

# Step 7 — Project directory

On the VM, as `sumit`.

```bash
mkdir ~/code
```

Projects live in `~/code/<project>/`. A project that needs a toolchain keeps it in `~/code/<project>/.devcontainer/`. Do not create a project here, and do not install a language runtime on the VM for one.

Set `git config --global user.name` and `user.email` yourself before the first commit. This runbook does not choose them.

---

# Step 8 — Cursor and VS Code

On the Mac. Install the Remote SSH extension in whichever app you will use. Connect to the host `dev`.

The first connection installs that app’s remote component under `sumit`’s home directory: `~/.cursor-server` for Cursor, `~/.vscode-server` for VS Code. Leave both in place. Do not install code-server.

Install the Dev Containers extension in the app you are using. Open a project only once that project has a `.devcontainer/` of its own. Use one of the two apps at a time on a given project.

---

# Step 9 — Checks

From the Mac:

```bash
ssh dev
```

On the VM:

```bash
hostname
```

```bash
ip -4 addr show ens18
```

```bash
cat /etc/resolv.conf
```

```bash
git --version
```

```bash
docker info
```

Expect hostname `dev`, address `10.10.10.10/24`, nameserver `10.10.10.1`, search `home.arpa`, and `docker info` running as `sumit` without sudo.

---

# Step 10 — DNS name

In the AdGuard UI (`http://10.10.0.4/`), add a DNS rewrite `dev.home.arpa` → `10.10.10.10`. That row is in the table in `../adguard/setup.md`.

Trusted DHCP `domain-name` is `home.arpa` (Step 6 in `../adguard/setup.md`). Renew the Mac's lease so its search domain matches. Then:

```bash
dig @10.10.10.1 dev.home.arpa +short
```

Expect `10.10.10.10`.

Add a host alias on the Mac, in `~/.ssh/config`. `Host` is the name you type. `HostName` is the DNS name.

```text
Host dev
    HostName dev.home.arpa
    User sumit
```

That uses the same default key `ssh-copy-id` installed.

```bash
ssh dev
```

---

# After it works

In this repository, mark the guest CURRENT instead of PLANNED:

- the heading and table in `README.md` (this directory)
- the guest row in `../proxmox.md`
- the `10.10.10.10` row in `../README.md`
