# CBS350

`running-config` is the known-good CBS350 running configuration exported from
the switch.

This directory is a restore runbook plus that snapshot. A human uses the CBS350
web UI. Do not wrap the restore in Ansible or a deploy script.

It is kept as close as possible to the switch-generated backup. The only line
removed before committing is:

    username cisco password encrypted ...

The administrator account/password must therefore be created separately during
factory setup.

## Backup / update Git

In the CBS350 web UI:

1. Administration → File Management → File Operations
2. Operation Type: **Backup File**
3. Source File Type: **Running Configuration**
4. Copy Method: **HTTP/HTTPS**
5. Sensitive Data Handling: **Exclude**
6. Click **Apply**

Save the downloaded file as:

    running-config.raw

The CBS350 export may still contain the encrypted local-user password even when
"Sensitive Data Handling: Exclude" is selected. Remove only that line:

    sed '/^username cisco password encrypted /d' running-config.raw > running-config

Do not otherwise normalize, reformat, or manually reconstruct `running-config`.

Review the diff before committing:

    git diff -- running-config

Do not commit `running-config.raw`.

## Restore from factory reset

### 1. Factory setup

Connect a laptop directly to an access port on the factory-reset switch.

Configure the laptop temporarily as:

    IP:      192.168.1.100
    Mask:    255.255.255.0
    Gateway: none

Open:

    https://192.168.1.254

Log in with the factory credentials and create the administrator password when
prompted.

The username/password are intentionally not stored in `running-config`.

### 2. Restore the configuration

In the CBS350 web UI:

Administration → File Management → File Operations

Choose:

    Operation Type:        Update File
    Destination File Type: Running Configuration
    Copy Method:           HTTP/HTTPS
    File:                  running-config

Apply the file.

The management session may become unavailable because the restored
configuration moves switch management to VLAN 10 at:

    10.10.10.2/24

### 3. Reconnect through the admin port

Connect the laptop to **GE12**.

GE12 is deliberately reserved as:

    admin OOB - do not repurpose

Use either DHCP from VyOS or temporarily configure:

    IP:      10.10.10.99
    Mask:    255.255.255.0
    Gateway: none

Verify:

    ping 10.10.10.1
    ping 10.10.10.2

Then open:

    https://10.10.10.2

### 4. Persist the restored configuration

Uploading `running-config` changes the running configuration only.

Once connectivity and configuration have been verified:

Administration → File Management → File Operations

Choose:

    Operation Type:        Duplicate
    Source File Type:      Running Configuration
    Destination File Type: Startup Configuration

Click **Apply**.

Do not reboot the switch before this step.

## Important ports

    GE1       Proxmox nic0 LAN trunk
    GE2       UniFi U6+
    GE3-12    Trusted / VLAN 10
    GE12      Reserved admin recovery port
    GE13-22   IoT / VLAN 30
    GE23-24   India / VLAN 40
    GE25-28   SFP, administratively disabled

GE2 uses VLAN 10 as its native VLAN. VLANs 20, 30 and 40 are carried tagged.

VyOS is the router, DHCP server, DNS forwarder and firewall. The CBS350 performs
Layer-2 switching only.