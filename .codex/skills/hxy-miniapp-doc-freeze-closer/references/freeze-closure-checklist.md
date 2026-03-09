# Freeze Closure Checklist

Before marking any batch `Frozen`:
- Confirm capability truth has no unresolved `ACTIVE` drift.
- Confirm canonical API list uses exact method + path, not wildcard.
- Confirm errorcode register has no `TBD_*` placeholders.
- Confirm `RESERVED_DISABLED` abilities are still excluded from active scope.
- Confirm index totals do not regress prior Frozen counts.
- Confirm handoff names blockers and next owners.

Keep `Ready` if any of the following still change:
- route truth
- method/path truth
- status machine meaning
- error code meaning
- release batch / priority
