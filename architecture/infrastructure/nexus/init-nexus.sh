#!/bin/sh

set -e

USER="admin"
INITIAL_PASS=$(tr -d '\r' < /nexus-data/admin.password)
NEW_PASSWORD=${NEXUS_PASSWORD}

echo "Changing Nexus admin password..."

curl -sS -u "$USER:$INITIAL_PASS" \
  -X PUT "http://nexus:8081/service/rest/v1/security/users/admin/change-password" \
  -H "Content-Type: text/plain" \
  -d "$NEW_PASSWORD"

echo "Password changed!"

# Accept the EULA
echo "Accepting Nexus EULA..."
curl -sS -u "$USER:$NEW_PASSWORD" \
  -X POST "http://nexus:8081/service/rest/v1/system/eula" \
  -H "Content-Type: application/json" \
  -d @- <<EOF
  {
    "accepted": true,
    "disclaimer": "Use of Sonatype Nexus Repository - Community Edition is governed by the End User License Agreement at https://links.sonatype.com/products/nxrm/ce-eula. By returning the value from ‘accepted:false’ to ‘accepted:true’, you acknowledge that you have read and agree to the End User License Agreement at https://links.sonatype.com/products/nxrm/ce-eula."
  }
EOF

# Disable anonymous access

echo "Disabling anonymous access..."

curl -sS -u "$USER:$NEW_PASSWORD" \
  -X PUT "http://nexus:8081/service/rest/v1/security/anonymous" \
  -H "Content-Type: application/json" \
  -d @- <<EOF
{
  "enabled": false,
  "userId": "anonymous",
  "realmName": "NexusAuthorizingRealm"
}
EOF

echo "Anonymous access disabled!"

# Create a Maven hosted repository

echo "Creating Maven hosted repository..."

curl -sS -u "$USER:$NEW_PASSWORD" \
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
