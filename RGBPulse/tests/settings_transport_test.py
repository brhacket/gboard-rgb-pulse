"""Source contracts for Android delivery/security. Not a Binder/IME device test."""
from pathlib import Path
root=Path(__file__).resolve().parents[1];src=root/'src/dev/rgbpulse/gboard'
client=(src/'SettingsClient.java').read_text();sender=(src/'SettingsTransport.java').read_text()
relay=(src/'SettingsRelay.java').read_text();receipt=(src/'SettingsReceipt.java').read_text()
ui=(src/'SettingsActivity.java').read_text();module=(src/'PulseModule.java').read_text()
manifest=(root/'AndroidManifest.xml').read_text()
assert 'ContentResolver' not in client and 'XSharedPreferences' not in module
assert 'SettingsContract.PERMISSION' in client and 'Context.RECEIVER_EXPORTED' in client
assert 'PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_ONE_SHOT' in sender
assert 'setPackage(SettingsContract.GBOARD)' in sender
assert 'SettingsReceipt" android:exported="false"' in manifest
assert 'SettingsContract.PULL.equals(intent.getAction())' in relay
assert 'getExtras' not in relay and 'getStringExtra' not in relay # no caller-supplied config or destination
assert 'now-last<500' in relay
assert 'synchronized(SettingsStore.LOCK)' in sender and 'synchronized(SettingsStore.LOCK)' in receipt
assert 'RevisionGate.accepts' in receipt
assert 'stamp<lastStamp' in client and 'SystemClock.elapsedRealtimeNanos()' in sender
assert client.index('listener.apply(next)')<client.index('receipt.send()')
assert 'MAIN.postDelayed(failClosed,2500)' in client and 'listener.apply(new Config())' in client
assert 'if(!revision.equals(currentRevision))' in client
# A failed pull is retried, and applied settings survive timeouts once confirmed.
assert 'if(pullRetries<3)' in client and 'MAIN.postDelayed(pull,900)' in client
assert 'keeping last applied settings' in client
assert client.index('MAIN.removeCallbacks(failClosed);pullRetries=0;')<client.index('stamp<lastStamp')
assert 'moduleResponded48' in sender and 'No live module reply' in ui
assert 'SettingsTransport.push(this)' in ui and 'SettingsStore.save(snapshot,applied)' in ui
assert 'saving=false;' in ui and 'Couldn’t save. Tap Save & restart Gboard' in ui
cleanup=module.split('void applyCurrentSettings(){',1)[1].split('void safeScan(',1)[0]
assert cleanup.index('keyStyle.restore()')<cleanup.index('if(!config.enabled||!visible){root.invalidate();return;}')
for callback in ['this','scanLater','settleScan','resync']:assert 'root.removeCallbacks('+callback+')' in cleanup
assert 'if (body == next && next != null) return;' in module
print('PASS: signed snapshots, immutable non-exported receipt, fixed-destination bootstrap, stale-message rejection, retried timeout, kept-applied settings and live-module status')
