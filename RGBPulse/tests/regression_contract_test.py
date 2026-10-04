"""Source guardrails, not a replacement for Android/device tests."""
from pathlib import Path
root=Path(__file__).resolve().parents[1]
src=root/'src/dev/rgbpulse/gboard'
ui=(src/'SettingsActivity.java').read_text()
side=(src/'SideSweep.java').read_text()
cfg=(src/'Config.java').read_text()
key=(src/'KeyStyle.java').read_text()
assert 'w.rowTop!=0' not in side and 'w.originX!=0' not in side
assert 'WavePolicy.glow' in side and 'boolean body=top!=null && cx!=null' in side
assert 'SideSweep.glowAt' in ui and 'fx.side.active(now,cfg.duration)' in ui
assert 'lastAuto' not in ui and 'postDelayed(tick' not in ui
assert 'ProcessBuilder' not in ui  # root work is isolated from the UI thread
assert 'Save & restart Gboard' in ui and 'saveRestart.setOnClickListener(v->persist())' in ui
assert 'Apply changes' not in ui and 'Save only' not in ui  # one button, no separate apply step
assert 'if(saved)' in ui and 'stopped=GboardRestart.stop()' in ui
assert 'Browse animations' not in ui and 'Import TTF' not in ui
assert 'Ripple on tap' in ui and 'Setup & troubleshooting' in ui
assert ui.count('slider(settings,')==2
assert 'SettingsStore.save(snapshot,applied)' in ui
assert 'getSharedPreferences("settings_draft",Context.MODE_PRIVATE)' in ui
assert 'Discard draft changes?' in ui
assert 'enabled = false' in cfg and 'tapEffects = false' in cfg
assert 'ClassCastException' in cfg and 'value==null?fallback:value' in cfg
assert 'cfg.refined)return' in key and 'if(!c.refined){caps.apply' in key
assert 'implements Drawable.Callback' in key and 'onStateChange' in key
assert 'ripple.draw' in ui and 'ripple.draw' in key
paint=(src/'RipplePaint.java').read_text().split('void draw(',1)[1]
assert 'new Paint' not in paint and 'new RectF' not in paint
assert 'canvas.clipRect(bounds)' in paint
assert 'RGBPulse/signing/' in (root.parent/'.gitignore').read_text()
print('PASS: simple UI, one save-and-restart button, stock text, drawable state, shared clipped renderer, safe preferences')

assert 'Test your keyboard' in ui and 'EditText test=new EditText(this)' in ui
assert 'test.setSaveEnabled(false)' in ui
assert 'fx.drawFields(canvas,play,now)' in ui and 'fx.tap(rect.centerX(),rect.centerY(),play,rect,now)' in ui
assert 'Key animations' in ui and '.setSingleChoiceItems(keyEffects' in ui
assert 'putBoolean("refined37",true).putBoolean("tapEffects36",false)' not in ui
module=(src/'PulseModule.java').read_text()
assert 'now-lastDraw>=16' not in module
frame=module.split('@Override public void run() {',1)[1].split('void render(',1)[0]
assert frame.index('fx.side.advance(config,play,now)') < frame.index('body.invalidate()')
assert 'for(View k:keys) k.invalidate();' in frame and 'root.postOnAnimation(this)' in module
assert 'c.sideStyle=1;c.tapEffects=false' not in cfg
print('PASS: real keyboard input restored, optional background pipeline, refresh-rate redraw and pre-invalidation advancement')
assert 'safeScan(layoutDirty||body==null||!body.isShown())' in module
assert 'onGlobalLayout(){layoutDirty=true;}' in module
assert 'removeOnGlobalLayoutListener(this)' in module
assert 'keyStyle.refreshRipples()' in module
assert 'key.getOverlay().add(layer)' in key and 'private void clearOverlays()' in key
fx=(src/'Fx.java').read_text()
assert 'cfg.enabled&&(cfg.tapEffects||cfg.background>0)&&fields!=null' in fx

restart=(src/'GboardRestart.java').read_text()
assert 'waitFor(25,TimeUnit.SECONDS)' in restart and 'destroyForcibly()' in restart
assert 'am force-stop --user current com.google.android.inputmethod.latin' in restart
assert 'Open app settings' in ui and 'if(!ok)' in ui
refined=key.split('private void applyRefined(',1)[1].split('private void clearOverlays',1)[0]
assert 'cfg.tiles &&' in refined and 'if(!refinedApplied)' in refined and '.getOverlay().remove' in refined
print('PASS: confirmed save-before-restart, bounded root process, refined overlay lifecycle')

assert 'c.enabled=c.ripple||c.tapEffects||c.background>0||c.opening>0||c.closing>0' in cfg
assert 'clamp(number(p,"pulse47", c.effect), 0, 7)' in cfg and 'clamp(number(p,"background51",0),0,2)' in cfg
assert 'c.glass=c.ripple;c.sideStyle=c.ripple?1:0' in cfg
assert 'enabled.setChecked(cfg.ripple)' in ui
assert 'putBoolean("ripple40",value)' in ui and 'if(cfg.ripple)fx.side.tap' in ui
assert 'main Enable ripple switch must be on' not in ui
assert 'cfg=Config.from(draft);preview.fx.cfg=cfg' in ui
assert 'boolean fading=keyStyle.advanceRipples(now)' in module
assert 'if(active||sideActive||fading) kick()' in module
assert 'layer.fade.advance' in key and 'fade.value()' in key
assert 'amount=fade.advance(amount,now);active|=fade.active()' in ui
print('PASS: independent applied toggles and shared per-key fade lifecycle')

assert 'putInt("opacity3",55)' not in ui and 'if(!draft.contains("duration3"))' in ui
assert 'Ripple strength' in ui and 'Background brightness' in ui
assert 'Hide key fills' in ui and 'putBoolean("hideTiles45",value)' in ui
assert 'cfg.refined?cfg.backgroundOpacity:cfg.opacity' in (src/'FieldFx.java').read_text()
assert 'cfg.rippleOpacity/100f' in ui and 'cfg.rippleOpacity/100f' in key
assert 'LegendTint.color' in ui and 'LegendTint.color(original.getColor(),layer.fade.value(),cfg.letterInactive,cfg.letterActive)' in key
assert 'original.setColor' not in key
assert 'refinedOriginalPaint' in key and 'refinedPreviousScope' in key
assert 'if(stable)return;' in key
assert 'Paint.Style.FILL' not in (src/'RipplePaint.java').read_text()
print('PASS: independent brightness, stable tiles and scoped native text tint without face wash')

assert 'Border width' in ui and 'colorControl(appearancePanel,"Letters · inactive"' in ui
assert 'swatch(select,candidate[0])' in ui and 'updatePalette()' in ui
assert 'm.getName().equals("drawGlyphs")' in key and 'layer.labels.apply' in key
assert 'width*dp' in (src/'RipplePaint.java').read_text()
assert 'return p*travel' in (src/'WavePolicy.java').read_text()
assert 'void restore()' in (src/'NativeLegends.java').read_text()
print('PASS: linear movement, custom state colors, swatch previews and native legend fallbacks')

assert 'type.getDeclaredMethod("onDraw",Canvas.class)' in key
assert 'layer.bindDrawingScopes()' in key and 'trackDrawingViews(group.getChildAt(i),depth+1)' in key
assert 'REFINED_CHILD' in key and 'clearDrawingScopes()' in key
assert '((KeyStyle)owner).overlays.get(layer.key)!=layer' in key
assert 'RGBPulse legend bind keys=' in key and 'RGBPulse legend tint=' in key
assert 'refinedScope.remove()' in key and 'refinedScope.set(previous)' in key
print('PASS: child/onDraw text scopes, recycled-child cleanup and bounded legend diagnostics')

assert 'syncHiddenBackgrounds(keys)' in key
assert 'restoreHiddenBackgrounds();refinedApplied=false;' in key
assert 'collectTileViews(g.getChildAt(i),live)' in key
assert 'hiddenBackgrounds.put((View)p.thisObject,(Drawable)p.args[0])' in key
assert 'if(!cfg.enabled||!cfg.refined||!cfg.ripple)return;' in key
assert 'if(!cfg.enabled||!cfg.refined||!cfg.ripple||cfg.hideTiles)return;' not in key
assert 'if(cfg.ripple)ripple.draw' in ui
assert 'invalidateTree(v)' in key and 'setBackground(v,original)' in key
print('PASS: verified key subtree backgrounds, theme replacement, recycling/off restoration and no tile outlines')

light=(src/'LifecycleLight.java').read_text()
assert 'shell.addView(pinned)' in ui and 'demo=card(pinned)' in ui
assert 'shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1))' in ui
assert 'private void selectPage(int index)' in ui and 'previewCollapsed' in ui
assert 'actions.addView(saveRestart,new LinearLayout.LayoutParams(-1,px(56)))' in ui
assert 'transitionControl(transitions,"Opening light"' in ui and 'transitionControl(transitions,"Closing light"' in ui
assert 'onFinishInputView' in module and 'closingLight()' in module
assert 'host.getOverlay().remove' in light and 'animator.cancel()' in light
assert 'setTranslation' not in light and 'setScale' not in light
assert '.putInt("opening46",0).putInt("closing46",0)' in ui
print('PASS: sticky responsive preview, independent borders, grouped controls and cancellable light-only transitions')

assert 'Try everyday setup' in ui and 'quietBackground48' not in ui and 'Quiet background' not in ui
assert 'Key animation · ' in ui and 'Background · ' in ui and 'putInt("background51",index)' in ui
assert 'restart=button("Restart…",false)' not in ui and 'Gboard was not stopped' in ui
assert 'glide.points.clear()' in fx and 'QuietBackground' not in fx
print('PASS: single save-and-restart button replaces apply/restart pair')

# The magnetic fluid was removed: no code, shader, UI or preference leftovers.
for gone in ['FluidSim.java','FluidFx.java','FluidMotion.java']:
    assert not (src/gone).exists()
assert not (root/'shaders/fluid.agsl').exists()
assert not (root/'tests/fluid_shader_test.py').exists() and not (root/'tests/FluidSimTest.java').exists()
assert 'fluid' not in cfg and 'fluid' not in ui and 'fluid' not in fx and 'fluid' not in module
assert 'FLUID' not in (src/'ShaderCode.java').read_text() and 'fluid.agsl' not in (root/'embed_shader.py').read_text()
print('PASS: magnetic fluid fully removed from config, UI, touch wiring, shader and sources')

assert 'root.postDelayed(resync,RESYNC_MS)' in module and 'SettingsClient.request()' in module
assert 'root.removeCallbacks(resync)' in module
client=(src/'SettingsClient.java').read_text()
assert 'if(pullRetries<3)' in client and 'keeping last applied settings' in client
assert 'schedulePull(900)' in client and 'MAIN.postDelayed(pull,delayMs)' in client
print('PASS: long-session resync keeps effects alive without blanking applied settings')
