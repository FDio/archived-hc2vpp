#!/usr/bin/env bash
echo "VPPA - configuring routing table for SIDs and policy via HC"
curl --insecure --request PUT \
  --url https://172.17.0.2:8445/restconf/config/vpp-fib-table-management:fib-table-management/vpp-fib-table-management:fib-tables/table/0/vpp-fib-table-management:ipv6 \
  --header 'accept: application/json' \
  --header 'authorization: Basic YWRtaW46YWRtaW4=' \
  --header 'content-type: application/json' \
  --data '{
    "vpp-fib-table-management:table": [
        {
            "table-id": 0,
            "address-family": "vpp-fib-table-management:ipv6",
            "name": "ipv6-VRF:0"
        }
    ]
}'
sleep 1

echo "VPPA - configuring local sid A::201 via HC"
curl --insecure --request PUT \
  --url https://172.17.0.2:8445/restconf/config/hc2vpp-ietf-routing:routing/hc2vpp-ietf-srv6-base:srv6/locators/locator/a::/ \
  --header 'accept: application/json' \
  --header 'authorization: Basic YWRtaW46YWRtaW4=' \
  --header 'content-type: application/json' \
  --data '{
    "hc2vpp-ietf-srv6-base:locator": [
        {
            "name": "a::",
            "is-default": false,
            "prefix": {
                "address": "a::",
                "length": 64
            },
            "enable": true,
            "vpp-ietf-srv6-base:fib-table" : {
                "table-id": 0,
                "address-family": "vpp-fib-table-management:ipv6"
            }
        }
    ]
}'
sleep 1

curl --insecure --request PUT \
  --url https://172.17.0.2:8445/restconf/config/hc2vpp-ietf-routing:routing/hc2vpp-ietf-srv6-base:srv6/locators/locator/a::/static/local-sids/sid/513/ \
  --header 'accept: application/json' \
  --header 'authorization: Basic YWRtaW46YWRtaW4=' \
  --header 'content-type: application/json' \
  --data '{
    "hc2vpp-ietf-srv6-static:sid": [
        {
            "opcode": 513,
            "end-behavior-type": "ietf-srv6-types:End.DX6",
            "end-dx6": {
                "paths": {
                    "path": [
                        {
                            "path-index": 1,
                            "interface": "host-veth0",
                            "next-hop": "a::1",
                            "weight": 1,
                            "role": "PRIMARY"
                        }
                    ]
                }
            }
        }
    ]
}'
sleep 1

echo "VPPA - configuring encapsulation source via HC"
curl --insecure --request PUT \
  --url https://172.17.0.2:8445/restconf/config/hc2vpp-ietf-routing:routing/hc2vpp-ietf-srv6-base:srv6/hc2vpp-ietf-srv6-base:encapsulation/ \
  --header 'accept: application/json' \
  --header 'authorization: Basic YWRtaW46YWRtaW4=' \
  --header 'content-type: application/json' \
  --data '{
    "hc2vpp-ietf-srv6-base:encapsulation": {
        "ip-ttl-propagation": false,
        "source-address": "a::1"
    }
}'
sleep 1

echo "VPPA - configuring SRv6 policy via HC"
curl --insecure --request PUT \
  --url https://172.17.0.2:8445/restconf/config/hc2vpp-oc-srte-policy:segment-routing \
  --header 'accept: application/json' \
  --header 'authorization: Basic YWRtaW46YWRtaW4=' \
  --header 'content-type: application/json' \
  --data '{
    "hc2vpp-oc-srte-policy:segment-routing": {
        "traffic-engineering": {
            "named-segment-lists": {
                "named-segment-list": [{
                    "name": "a::e-1",
                    "config": {
                        "name": "a::e-1"
                    },
                    "segments": {
                        "segment": [{
                            "index": 1,
                            "config": {
                                "index": 1,
                                "type": "type-2",
                                "sid-value": "c::100"
                            }
                        },
                        {
                            "index": 2,
                            "config": {
                                "index": 2,
                                "type": "type-2",
                                "sid-value": "d::100"
                            }
                        },
                        {
                            "index": 3,
                            "config": {
                                "index": 3,
                                "type": "type-2",
                                "sid-value": "e::201"
                            }
                        }]
                    }
                }]
            },
            "policies": {
                "policy": [{
                    "name": "a::e",
                    "config": {
                        "name": "a::e",
                        "color": 1,
                        "endpoint": "e::1",
                        "admin-state": "UP"
                    },
                    "color": 1,
                    "endpoint": "e::1",
                    "candidate-paths": {
                        "candidate-path": [{
                            "name": "candidatePath1",
                            "provisioning-method": "provisioning-method-config",
                            "preference": 100,
                            "distinguisher": 0,
                            "config": {
                                "name": "candidatePath1",
                                "provisioning-method": "provisioning-method-config",
                                "computation-method": "path-explicitly-defined",
                                "preference": 100,
                                "distinguisher": 0
                            },
                            "binding-sid": {
                                "config": {
                                    "alloc-mode": "explicit",
                                    "type": "srv6",
                                    "value": "a::e"
                                }
                            },
                            "segment-lists": {
                                "segment-list": [{
                                    "name": "a::e-1",
                                    "config": {
                                        "name": "a::e-1",
                                        "weight": 1
                                    }
                                }]
                            }
                        }]
                    },
                    "autoroute-include": {
                        "config": {
                            "metric-type": "constant",
                            "metric-constant": 0
                        },
                        "prefixes": {
                            "config": {
                                "prefixes-all": false
                            },
                            "prefix": [{
                                "ip-prefix": "e::/64",
                                "config": {
                                    "ip-prefix": "e::/64"
                                }
                            }]
                        }
                    },
                    "binding-sid": {
                        "config": {
                            "alloc-mode": "explicit",
                            "type": "srv6",
                            "value": "a::e"
                        }
                    },
                    "vpp-oc-srte-policy:vpp-sr-policy": {
                        "config": {
                            "policy-type": "Default",
                            "policy-behavior": "Encapsulation",
                            "table-id": 0,
                            "address-family": "vpp-fib-table-management:ipv6"
                        }
                    }
                }]
            }
        }
    }
}'
sleep 1
