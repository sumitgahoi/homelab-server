# Install VM 100

Run when VM 100 is absent. Desired config is `config.boot`. Later edits: `README.md`.

MACs are pinned on create (`02:00:00:00:00:00`–`:02`): `eth0` LAN / `eth1` WAN / `eth2` services.

## Mac

From this directory:

```bash
mkdir -p /tmp/vyoscfg
```

```bash
cp config.boot /tmp/vyoscfg/
```

```bash
hdiutil makehybrid -iso -joliet -default-volume-name VYOSCFG -o /tmp/vyos-config.iso /tmp/vyoscfg
```

```bash
scp /tmp/vyos-config.iso root@192.168.50.200:/var/lib/vz/template/iso/vyos-config.iso
```

## pve

```bash
ssh root@192.168.50.200
```

```bash
test ! -f /etc/pve/qemu-server/100.conf
```

```bash
wget -O /var/lib/vz/template/iso/vyos.iso https://github.com/vyos/vyos-nightly-build/releases/download/2026.09.16-0028-rolling/vyos-2026.09.16-0028-rolling-generic-amd64.iso
```

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
  --ide3 local:iso/vyos-config.iso,media=cdrom \
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

On the live ISO, put Git `config.boot` where the installer will offer it. Do not `load` it on the live system. If `/dev/sr1` is empty, try `sr0`:

```bash
sudo mkdir -p /mnt/cdrom
```

```bash
sudo mount /dev/sr1 /mnt/cdrom
```

Read-only / write-protected is expected. Then:

```bash
sudo cp /mnt/cdrom/config.boot /opt/vyatta/etc/config/config.boot
```

Then install to the blank disk. The installer still lists only two paths.

**IMPORTANT:** Choose:

    1: /opt/vyatta/etc/config/config.boot

This is the Git `config.boot` copied from the VYOSCFG ISO above. Do not choose `config.boot.default`.

When it asks to reboot, say no, then halt so QEMU actually powers off (a reboot would keep the CDs attached):

```bash
install image
```

```bash
shutdown -h now
```

`qm terminal` ends when the VM stops. Drop both CDs (the ISO files on disk stay), boot from disk, start:

```bash
qm set 100 --delete ide2
```

```bash
qm set 100 --delete ide3
```

```bash
qm set 100 --boot order=virtio0
```

```bash
qm start 100
```

## Mac

Plug into CBS350 **GE12**. Static `10.10.10.99/24`, no gateway. Pin the new host key:

```bash
ssh-keygen -R 10.10.10.1
```

```bash
ssh-keyscan -H 10.10.10.1 >> ~/.ssh/known_hosts
```

