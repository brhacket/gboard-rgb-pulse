"""Transport security/lifecycle source checks. Actual cross-process Android testing is still needed."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
src=root/'src/dev/rgbpulse/gboard'
module=(src/'PulseModule.java').read_text()
provider=(src/'SettingsProvider.java').read_text()
client=(src/'SettingsClient.java').read_text()
ui=(src/'SettingsActivity.java').read_text()
assert 'XSharedPreferences' not in module and 'prefs.reload()' not in module
assert 'MODE_WORLD_READABLE' not in ui
assert 'Binder.getCallingUid()' in provider and 'getPackagesForUid' in provider
assert 'Only Gboard may acknowledge settings' in provider
assert 'RevisionGate.matches(current,received)' in provider
assert 'SettingsContract.PERMISSION' in client and 'Context.RECEIVER_EXPORTED' in client
assert 'Config next=new Config()' in client and 'request!=generation.get()' in client
assert client.index('listener.apply(next)') < client.index('"ack",null,receipt')
assert 'Config.from(new BundlePreferences(data))' in client
assert 'Turn everything off & apply' in ui and 'Saved · waiting for Gboard' in ui
cleanup=module.split('void applyCurrentSettings(){',1)[1].split('void safeScan(',1)[0]
assert cleanup.index('keyStyle.restore()') < cleanup.index('if(!config.enabled||!visible){root.invalidate();return;}')
for callback in ['this','scanLater','settleScan']:
    assert 'root.removeCallbacks('+callback+')' in cleanup
assert 'if (body == next && next != null) return;' in module
assert 'synchronized(SettingsStore.LOCK)' in provider
assert 'SettingsStore.save(snapshot,applied)' in ui
print('PASS: UID-restricted settings IPC, permission-gated notification, revision receipt, fail-closed read and unconditional off cleanup')
