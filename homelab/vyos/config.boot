firewall {
    group {
        interface-group IF-DHCP-SERVER {
            interface "eth0.10"
            interface "eth0.20"
            interface "eth0.30"
            interface "eth0.40"
        }
        interface-group IF-INTERNAL {
            interface "eth0.10"
            interface "eth0.20"
            interface "eth0.30"
            interface "eth0.40"
            interface "eth2"
        }
        network-group NET-CLIENT {
            network "10.10.10.0/24"
            network "10.10.20.0/24"
            network "10.10.30.0/24"
        }
        network-group NET-GUEST {
            network "10.10.20.0/24"
        }
        network-group NET-INDIA {
            network "10.10.40.0/24"
        }
        network-group NET-PRIVATE {
            network "10.10.10.0/24"
        }
        network-group NET-RFC1918 {
            network "10.0.0.0/8"
            network "172.16.0.0/12"
            network "192.168.0.0/16"
        }
        network-group NET-SERVICES {
            network "10.10.0.0/24"
        }
    }
    ipv4 {
        forward {
            filter {
                default-action "drop"
                rule 10 {
                    action "accept"
                    state "established"
                    state "related"
                }
                rule 100 {
                    action "accept"
                    description "trusted unrestricted"
                    source {
                        group {
                            network-group "NET-PRIVATE"
                        }
                    }
                }
                rule 200 {
                    action "accept"
                    description "guest to WAN"
                    outbound-interface {
                        name "eth1"
                    }
                    source {
                        group {
                            network-group "NET-GUEST"
                        }
                    }
                }
                rule 300 {
                    action "accept"
                    description "Services to WAN"
                    outbound-interface {
                        name "eth1"
                    }
                    source {
                        group {
                            network-group "NET-SERVICES"
                        }
                    }
                }
                rule 390 {
                    action "drop"
                    description "India must not pivot to RFC1918 via India-GW"
                    destination {
                        group {
                            network-group "NET-RFC1918"
                        }
                    }
                    source {
                        group {
                            network-group "NET-INDIA"
                        }
                    }
                }
                rule 400 {
                    action "accept"
                    description "India Internet via India-GW"
                    outbound-interface {
                        name "eth2"
                    }
                    source {
                        group {
                            network-group "NET-INDIA"
                        }
                    }
                }
            }
        }
        input {
            filter {
                default-action "drop"
                rule 10 {
                    action "accept"
                    state "established"
                    state "related"
                }
                rule 20 {
                    action "accept"
                    description "WAN DHCP client"
                    destination {
                        port "68"
                    }
                    inbound-interface {
                        name "eth1"
                    }
                    protocol "udp"
                }
                rule 30 {
                    action "accept"
                    description "ICMP from internal"
                    inbound-interface {
                        group "IF-INTERNAL"
                    }
                    protocol "icmp"
                }
                rule 40 {
                    action "accept"
                    description "DHCP server"
                    destination {
                        port "67"
                    }
                    inbound-interface {
                        group "IF-DHCP-SERVER"
                    }
                    protocol "udp"
                }
                rule 100 {
                    action "accept"
                    description "DNS from Trusted/Guest/IoT"
                    destination {
                        port "53"
                    }
                    protocol "tcp_udp"
                    source {
                        group {
                            network-group "NET-CLIENT"
                        }
                    }
                }
                rule 1000 {
                    action "accept"
                    description "trusted to VyOS"
                    source {
                        group {
                            network-group "NET-PRIVATE"
                        }
                    }
                }
            }
        }
    }
    ipv6 {
        forward {
            filter {
                default-action "drop"
            }
        }
        input {
            filter {
                default-action "drop"
                rule 10 {
                    action "accept"
                    state "established"
                    state "related"
                }
                rule 15 {
                    action "drop"
                    state "invalid"
                }
                rule 20 {
                    action "accept"
                    description "WAN DHCPv6 client"
                    destination {
                        port "546"
                    }
                    inbound-interface {
                        name "eth1"
                    }
                    protocol "udp"
                    source {
                        port "547"
                    }
                }
                rule 30 {
                    action "accept"
                    description "ICMPv6 on WAN"
                    inbound-interface {
                        name "eth1"
                    }
                    protocol "icmpv6"
                }
            }
        }
    }
}
/* Authoritative VyOS configuration. Dialect: rolling 2026.09.16-0028-rolling.
   Apply: see README.md */
interfaces {
    ethernet eth0 {
        description "LAN trunk"
        hw-id "02:00:00:00:00:00"
        offload {
            gro
            gso
            sg
            tso
        }
        vif 10 {
            address "10.10.10.1/24"
            description "VLAN 10 trusted"
            ipv6 {
                address {
                    no-default-link-local
                }
                disable-forwarding
            }
        }
        vif 20 {
            address "10.10.20.1/24"
            description "VLAN 20 guest"
            ipv6 {
                address {
                    no-default-link-local
                }
                disable-forwarding
            }
        }
        vif 30 {
            address "10.10.30.1/24"
            description "VLAN 30 iot"
            ipv6 {
                address {
                    no-default-link-local
                }
                disable-forwarding
            }
        }
        vif 40 {
            address "10.10.40.1/24"
            description "VLAN 40 india"
            ipv6 {
                address {
                    no-default-link-local
                }
                disable-forwarding
            }
        }
    }
    ethernet eth1 {
        address "dhcp"
        address "dhcpv6"
        description "WAN"
        hw-id "02:00:00:00:00:01"
        ipv6 {
            address {
                autoconf
            }
        }
        offload {
            gro
            gso
            sg
            tso
        }
    }
    ethernet eth2 {
        address "10.10.0.1/24"
        description "services"
        hw-id "02:00:00:00:00:02"
        ipv6 {
            address {
                no-default-link-local
            }
            disable-forwarding
        }
        offload {
            gro
            gso
            sg
            tso
        }
    }
}
nat {
    source {
        rule 30 {
            description "Services to WAN"
            outbound-interface {
                name "eth1"
            }
            source {
                address "10.10.0.0/24"
            }
            translation {
                address "masquerade"
            }
        }
        rule 100 {
            description "NAT trusted to WAN"
            outbound-interface {
                name "eth1"
            }
            source {
                address "10.10.10.0/24"
            }
            translation {
                address "masquerade"
            }
        }
        rule 110 {
            description "NAT guest to WAN"
            outbound-interface {
                name "eth1"
            }
            source {
                address "10.10.20.0/24"
            }
            translation {
                address "masquerade"
            }
        }
    }
}
policy {
    route PBR-INDIA {
        description "VLAN 40 uses India-GW, not the main default route"
        interface "eth0.40"
        rule 10 {
            description "India source lookup table 40"
            set {
                table "40"
            }
            source {
                address "10.10.40.0/24"
            }
        }
    }
}
protocols {
    static {
        table 40 {
            route 0.0.0.0/0 {
                blackhole {
                    distance "254"
                }
                next-hop 10.10.0.5 {
                }
            }
        }
    }
}
service {
    dhcp-server {
        shared-network-name GUEST {
            subnet 10.10.20.0/24 {
                option {
                    default-router "10.10.20.1"
                    name-server "10.10.20.1"
                }
                range 0 {
                    start "10.10.20.100"
                    stop "10.10.20.250"
                }
                subnet-id "1"
            }
        }
        shared-network-name INDIA {
            subnet 10.10.40.0/24 {
                option {
                    default-router "10.10.40.1"
                    name-server "1.1.1.1"
                    name-server "1.0.0.1"
                }
                range 0 {
                    start "10.10.40.100"
                    stop "10.10.40.250"
                }
                subnet-id "2"
            }
        }
        shared-network-name IOT {
            subnet 10.10.30.0/24 {
                option {
                    default-router "10.10.30.1"
                    name-server "10.10.30.1"
                }
                range 0 {
                    start "10.10.30.100"
                    stop "10.10.30.250"
                }
                subnet-id "3"
            }
        }
        shared-network-name PRIVATE {
            subnet 10.10.10.0/24 {
                option {
                    default-router "10.10.10.1"
                    domain-name "home.arpa"
                    name-server "10.10.10.1"
                }
                range 0 {
                    start "10.10.10.100"
                    stop "10.10.10.250"
                }
                subnet-id "4"
            }
        }
    }
    dns {
        forwarding {
            allow-from "10.10.10.0/24"
            allow-from "10.10.20.0/24"
            allow-from "10.10.30.0/24"
            listen-address "10.10.10.1"
            listen-address "10.10.20.1"
            listen-address "10.10.30.1"
            name-server 10.10.0.4 {
            }
        }
    }
    ssh {
        disable-host-validation
        disable-password-authentication
    }
}
system {
    config-management {
        commit-confirm {
            action "reload"
        }
        commit-revisions "200"
    }
    conntrack {
        modules {
            ftp
            h323
            nfs
            pptp
            sip
            sqlnet
            tftp
        }
    }
    console {
        device ttyS0 {
            kernel
            speed "115200"
        }
    }
    login {
        user vyos {
            authentication {
                encrypted-password "redacted"
                public-keys macbook {
                    key "AAAAC3NzaC1lZDI1NTE5AAAAIMnN6+7q3OAtjH3lEeRBW7mz/qCwnt8fCCOtKi1+/80Z"
                    type "ssh-ed25519"
                }
            }
        }
    }
    name-server "eth1"
}


// Warning: Do not remove the following line.
// vyos-config-version: "bgp@8:broadcast-relay@1:cluster@2:config-management@1:conntrack@6:conntrack-sync@2:container@3:dhcp-relay@2:dhcp-server@11:dhcpv6-server@6:dns-dynamic@5:dns-forwarding@4:firewall@20:flow-accounting@3:https@7:ids@2:interfaces@34:ipoe-server@4:ipsec@14:isis@3:l2tp@10:lldp@3:mdns@1:monitoring@2:nat@8:nat66@3:nhrp@1:ntp@3:openconnect@3:openvpn@6:ospf@2:pim@1:pki@1:policy@9:pppoe-server@13:pptp@6:qos@3:quagga@12:reverse-proxy@3:rip@1:rpki@2:snmp@3:ssh@3:sstp@7:system@33:vpp@6:vrf@4:vrrp@4:vyos-accel-ppp@2:wanloadbalance@4:webproxy@2"
// Release version: 2026.09.16-0028-rolling
