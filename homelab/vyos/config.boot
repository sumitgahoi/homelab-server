/* Authoritative VyOS configuration. Dialect: rolling 2026.09.16-0028-rolling.
   Apply: see README.md */

interfaces {
    ethernet eth0 {
        description "LAN trunk"
        hw-id 02:00:00:00:00:00
        vif 10 {
            address 10.10.10.1/24
            description "VLAN 10 trusted"
        }
        vif 20 {
            address 10.10.20.1/24
            description "VLAN 20 guest"
        }
        vif 30 {
            address 10.10.30.1/24
            description "VLAN 30 iot"
        }
        vif 40 {
            address 10.10.40.1/24
            description "VLAN 40 india"
        }
    }
    ethernet eth1 {
        address dhcp
        description "WAN"
        hw-id 02:00:00:00:00:01
    }
    ethernet eth2 {
        address 10.10.0.1/24
        description "services"
        hw-id 02:00:00:00:00:02
    }
}
service {
    ssh {
        disable-host-validation
        disable-password-authentication
    }
    dns {
        forwarding {
            listen-address 10.10.10.1
            listen-address 10.10.20.1
            listen-address 10.10.30.1
            listen-address 10.10.40.1
            allow-from 10.10.10.0/24
            allow-from 10.10.20.0/24
            allow-from 10.10.30.0/24
            allow-from 10.10.40.0/24
            name-server 1.1.1.1
            name-server 1.0.0.1
        }
    }
    dhcp-server {
        shared-network-name PRIVATE {
            authoritative
            subnet 10.10.10.0/24 {
                subnet-id 10
                option {
                    default-router 10.10.10.1
                    name-server 10.10.10.1
                    domain-name homelab.local
                }
                range 0 {
                    start 10.10.10.100
                    stop 10.10.10.250
                }
            }
        }
        shared-network-name GUEST {
            authoritative
            subnet 10.10.20.0/24 {
                subnet-id 20
                option {
                    default-router 10.10.20.1
                    name-server 10.10.20.1
                }
                range 0 {
                    start 10.10.20.100
                    stop 10.10.20.250
                }
            }
        }
        shared-network-name IOT {
            authoritative
            subnet 10.10.30.0/24 {
                subnet-id 30
                option {
                    default-router 10.10.30.1
                    name-server 10.10.30.1
                }
                range 0 {
                    start 10.10.30.100
                    stop 10.10.30.250
                }
            }
        }
        shared-network-name INDIA {
            authoritative
            subnet 10.10.40.0/24 {
                subnet-id 40
                option {
                    default-router 10.10.40.1
                    name-server 10.10.40.1
                }
                range 0 {
                    start 10.10.40.100
                    stop 10.10.40.250
                }
            }
        }
    }
}
nat {
    source {
        rule 30 {
            description "Services to WAN"
            outbound-interface {
                name eth1
            }
            source {
                address 10.10.0.0/24
            }
            translation {
                address masquerade
            }
        }
        rule 100 {
            description "NAT trusted to WAN"
            outbound-interface {
                name eth1
            }
            source {
                address 10.10.10.0/24
            }
            translation {
                address masquerade
            }
        }
        rule 110 {
            description "NAT guest to WAN"
            outbound-interface {
                name eth1
            }
            source {
                address 10.10.20.0/24
            }
            translation {
                address masquerade
            }
        }
    }
}
firewall {
    group {
        network-group NET-PRIVATE {
            network 10.10.10.0/24
        }
        network-group NET-GUEST {
            network 10.10.20.0/24
        }
        network-group NET-SERVICES {
            network 10.10.0.0/24
        }
        network-group NET-CLIENT {
            network 10.10.10.0/24
            network 10.10.20.0/24
            network 10.10.30.0/24
            network 10.10.40.0/24
        }
        interface-group IF-INTERNAL {
            interface eth0.10
            interface eth0.20
            interface eth0.30
            interface eth0.40
            interface eth2
        }
        interface-group IF-DHCP-SERVER {
            interface eth0.10
            interface eth0.20
            interface eth0.30
            interface eth0.40
        }
    }
    ipv4 {
        input {
            filter {
                default-action drop
                rule 10 {
                    action accept
                    state established
                    state related
                }
                rule 20 {
                    action accept
                    description "WAN DHCP client"
                    inbound-interface {
                        name eth1
                    }
                    protocol udp
                    destination {
                        port 68
                    }
                }
                rule 30 {
                    action accept
                    description "ICMP from internal"
                    protocol icmp
                    inbound-interface {
                        group IF-INTERNAL
                    }
                }
                rule 40 {
                    action accept
                    description "DHCP server"
                    protocol udp
                    destination {
                        port 67
                    }
                    inbound-interface {
                        group IF-DHCP-SERVER
                    }
                }
                rule 100 {
                    action accept
                    description "DNS from client VLANs"
                    source {
                        group {
                            network-group NET-CLIENT
                        }
                    }
                    destination {
                        port 53
                    }
                    protocol tcp_udp
                }
                rule 1000 {
                    action accept
                    description "trusted to VyOS"
                    source {
                        group {
                            network-group NET-PRIVATE
                        }
                    }
                }
            }
        }
        forward {
            filter {
                default-action drop
                rule 10 {
                    action accept
                    state established
                    state related
                }
                rule 100 {
                    action accept
                    description "trusted unrestricted"
                    source {
                        group {
                            network-group NET-PRIVATE
                        }
                    }
                }
                rule 200 {
                    action accept
                    description "guest to WAN"
                    source {
                        group {
                            network-group NET-GUEST
                        }
                    }
                    outbound-interface {
                        name eth1
                    }
                }
                rule 300 {
                    action accept
                    description "Services to WAN"
                    source {
                        group {
                            network-group NET-SERVICES
                        }
                    }
                    outbound-interface {
                        name eth1
                    }
                }
            }
        }
    }
}
system {
    config-management {
        commit-confirm {
            action reload
        }
    }
    login {
        user vyos {
            authentication {
                public-keys macbook {
                    type ssh-ed25519
                    key AAAAC3NzaC1lZDI1NTE5AAAAIMnN6+7q3OAtjH3lEeRBW7mz/qCwnt8fCCOtKi1+/80Z
                }
            }
        }
    }
}
