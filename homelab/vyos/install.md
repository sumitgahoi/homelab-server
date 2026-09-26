# Install VM 100

Run when VM 100 is absent. After the guest OS exists, configure the router from `setup.md` using `commands.txt`. Do not seed the installer with a `config.boot` from Git.

MACs are pinned on create (`02:00:00:00:00:00`–`:02`): `eth0` LAN / `eth1` WAN / `eth2` services. Do not delete and re-add the guest NICs; that renumbers them.

## Purpose

This section only creates the virtual machine and installs VyOS onto disk.

It does **not** apply the house routing policy. That happens afterwards, command by command, in `setup.md`.

## pve — create the VM

The host is `root@10.10.10.3`.

```bash
ssh root@10.10.10.3
```

Abort if VM 100 already exists.

```bash
# Do not overwrite an existing router VM.
test ! -f /etc/pve/qemu-server/100.conf
```

Download the VyOS installer ISO onto `local` when `vyos.iso` is missing. The filename in this repository is the known-good image used for this homelab:

```bash
wget -O /var/lib/vz/template/iso/vyos.iso https://github.com/vyos/vyos-nightly-build/releases/download/2026.09.16-0028-rolling/vyos-2026.09.16-0028-rolling-generic-amd64.iso
```

Create the VM. The three virtio NICs must keep these MAC addresses so `eth0` / `eth1` / `eth2` stay LAN / WAN / Services.

```bash
qm create 100 \
  --name vyos \
  --cores 2 \
  --sockets 1 \
  --memory 4096 \
  --balloon 0 \
  --cpu host \
  --ostype l26 \
  --onboot 1 \
  --agent 0 \
  --serial0 socket \
  --net0 virtio,bridge=vmbr0,macaddr=02:00:00:00:00:00 \
  --net1 virtio,bridge=vmbr1,macaddr=02:00:00:00:00:01 \
  --net2 virtio,bridge=vmbr-svc,macaddr=02:00:00:00:00:02 \
  --virtio0 local-lvm:8 \
  --ide2 local:iso/vyos.iso,media=cdrom \
  --boot order='ide2;virtio0'
```

```bash
qm start 100
```

## Install VyOS

Serial console (easier than iKVM for paste):

```bash
qm terminal 100
```

On the live ISO, install to the blank disk using the installer default configuration. Do **not** `load` a Git `config.boot` here. The house policy is applied later from `commands.txt` in `setup.md`.

When it asks to reboot, say no, then halt so QEMU actually powers off (a reboot would keep the CD attached):

```bash
install image
```

```bash
shutdown -h now
```

`qm terminal` ends when the VM stops. Drop the installer CD (the ISO file on disk stays), boot from disk, start:

```bash
qm set 100 --delete ide2
```

```bash
qm set 100 --boot order=virtio0
```

```bash
qm start 100
```

## After first boot

Confirm the three interfaces exist and the MACs match LAN / WAN / Services:

```
show interfaces
```

Set or reset the `vyos` password locally if the installer did not leave a usable login. The Git `commands.txt` does not contain `encrypted-password`.

Then leave this file and continue in `setup.md`.

## Mac — first SSH after the Trusted interface exists

Plug into CBS350 **GE12**. Static `10.10.10.99/24`, no gateway. Pin the new host key only after `eth0.10` is configured and committed in `setup.md`:

```bash
ssh-keygen -R 10.10.10.1
```

```bash
ssh-keyscan -H 10.10.10.1 >> ~/.ssh/known_hosts
```
