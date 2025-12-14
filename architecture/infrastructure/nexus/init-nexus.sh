#!/bin/sh

USER="admin"
PASS=$(tr -d '\r' < /nexus-data/admin.password)

echo "Creating Maven hosted repository..."

curl -u "$USER:$PASS" \
  -X POST "http://nexus:8081/service/rest/v1/repositories/maven/hosted" \
  -H "Content-Type: application/json" \
  -d @- <<EOF
{
  "name": "iot-libs-release-local",
  "online": true,
  "storage": {
    "blobStoreName": "default",
    "strictContentTypeValidation": true,
    "writePolicy": "ALLOW_ONCE"
  },
  "cleanup": {
    "policyNames": []
  },
  "component": {
    "proprietaryComponents": true
  },
  "maven": {
    "versionPolicy": "MIXED",
    "layoutPolicy": "STRICT",
    "contentDisposition": "ATTACHMENT"
  }
}
EOF

echo "Repository created!"
