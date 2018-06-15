#!/usr/bin/env bash
echo "VPPD - configuring routing table for SIDs and policy via HC"
curl --insecure --request PUT \
  --url https://172.17.0.5:8445/restconf/config/vpp-fib-table-management:fib-table-management/vpp-fib-table-management:fib-tables/table/0/vpp-fib-table-management:ipv6 \
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

echo "VPPD - configuring local sid D::100 via HC"
curl --insecure --request PUT \
  --url https://172.17.0.5:8445/restconf/config/hc2vpp-ietf-routing:routing/hc2vpp-ietf-srv6-base:srv6/locators/locator/d::/ \
  --header 'accept: application/json' \
  --header 'authorization: Basic YWRtaW46YWRtaW4=' \
  --header 'content-type: application/json' \
  --data '{
    "hc2vpp-ietf-srv6-base:locator": [
        {
            "name": "d::",
            "is-default": false,
            "prefix": {
                "address": "d::",
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
  --url https://172.17.0.5:8445/restconf/config/hc2vpp-ietf-routing:routing/hc2vpp-ietf-srv6-base:srv6/locators/locator/d::/static/local-sids/sid/256/ \
  --header 'accept: application/json' \
  --header 'authorization: Basic YWRtaW46YWRtaW4=' \
  --header 'content-type: application/json' \
  --data '{
    "hc2vpp-ietf-srv6-static:sid": [
        {
            "opcode": 256,
            "end-behavior-type": "ietf-srv6-types:End",
            "end": {
            }
        }
    ]
}'
sleep 1
