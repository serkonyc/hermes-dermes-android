#!/bin/bash
# Dermes release pipeline: one script, no manual steps. Bump = edit AndroidManifest
# (versionCode +1, versionName +0.0.1), then run: ./release.sh vX.Y.Z
# Every gate is checked INSIDE the produced APK; any failure aborts before GitHub sees it.
set -e
TAG="$1"
[ -z "$TAG" ] && { echo "usage: ./release.sh vX.Y.Z"; exit 1; }
REPO="serkonyc/hermes-dermes-android"
BT=$HOME/.hermes/work/android-sdk/build-tools/35.0.0
AJ=$HOME/.hermes/work/android-sdk/platforms/android-35/android.jar
cd ~/.hermes/work/dermes-android

# Gate 0: tag matches manifest versionName exactly (updater compares these two).
VN=$($BT/aapt2 dump xmltree --file AndroidManifest.xml <(echo dummy) 2>/dev/null; grep -o 'versionName="[^"]*"' AndroidManifest.xml | cut -d'"' -f2)
[ "v$VN" != "$TAG" ] && { echo "FAIL: tag $TAG != manifest versionName $VN"; exit 1; }

# Clean build.
rm -rf build && mkdir build
$BT/aapt2 compile --dir res -o build/res.zip
$BT/aapt2 link -o build/base.apk -I "$AJ" --manifest AndroidManifest.xml build/res.zip --auto-add-overlay --java build/gen
javac -nowarn --release 8 -cp "$AJ:ext-src/core.jar" -d build/classes $(find build/gen -name '*.java') src/ai/serge/dermes/*.java
$BT/d8 --release --lib "$AJ" --min-api 26 --output build/ $(find build/classes -name '*.class') ext-src/core.jar
cd build && zip -q base.apk classes.dex && cd ..
$BT/zipalign -f 4 build/base.apk build/aligned.apk
$BT/apksigner sign --ks monitor.keystore --ks-pass pass:dermes --v1-signing-enabled true --out dermes.apk build/aligned.apk

# Gate 1: signature.
$BT/apksigner verify dermes.apk

# Gate 2: launcher class + package (dex and manifest must agree — the original sin).
ACT=$($BT/aapt2 dump badging dermes.apk | sed -n "s/launchable-activity: name='\([^']*\)'.*/\1/p")
[ "ai.serge.dermes.DermesActivity" != "$ACT" ] && { echo "FAIL: launcher is $ACT"; exit 1; }
PKG=$($BT/aapt2 dump badging dermes.apk | sed -n "s/^package: name='\([^']*\)'.*/\1/p")
[ "ai.serge.dermes" != "$PKG" ] && { echo "FAIL: package is $PKG"; exit 1; }
rm -rf /tmp/dz && mkdir /tmp/dz && unzip -qo dermes.apk classes.dex -d /tmp/dz
$BT/dexdump -f /tmp/dz/classes.dex 2>/dev/null | grep -q "Lai/serge/dermes/DermesActivity;" || { echo "FAIL: DermesActivity not in dex"; exit 1; }

# Gate 3: no stray assets (core.jar-in-assets bug of 2026-10-21).
unzip -l dermes.apk | grep -q "assets/" && { echo "FAIL: assets/ found in APK"; exit 1; }

SHA=$(shasum -a 256 dermes.apk | cut -d' ' -f1)

# Ship.
git add -A
git -c user.name=hermes -c user.email=hermes@local commit -q -m "release $TAG" || true
git push -q origin main
gh release create "$TAG" --target main -R "$REPO" \
  --notes "Dermes $TAG. sha256 $SHA" dermes.apk || { echo "FAIL: release create"; exit 1; }

# Gate 4: read back from GitHub exactly like the phone does.
sleep 8
DL="https://github.com/$REPO/releases/download/$TAG/dermes.apk"
curl -sL -m 120 -o /tmp/verify.apk "$DL"
SIZE_ONLINE=$(gh api "repos/$REPO/releases/tags/$TAG" -q '.assets[0].size')
SHA_ONLINE=$(shasum -a 256 /tmp/verify.apk | cut -d' ' -f1)
[ "$SHA_ONLINE" != "$SHA" ] && { echo "FAIL: online sha $SHA_ONLINE != local $SHA"; exit 1; }
echo "OK $TAG: $SIZE_ONLINE bytes, sha $SHA, live URL verified"
