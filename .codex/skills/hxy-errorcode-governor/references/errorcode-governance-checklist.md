# Errorcode Governance Checklist

1. Canonical register exists and is current.
2. No target document contains `TBD_*` error markers.
3. Product docs branch by code, not message.
4. Failure mode and retry class align with contract docs.
5. Reserved-disabled and degraded markers are preserved across UI and runbook layers.
6. New codes are added to the canonical register before downstream use.
