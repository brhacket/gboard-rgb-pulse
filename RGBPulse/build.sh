#!/usr/bin/env bash
# Reproducible command-line build. Requires JDK 11+, curl, zip, openssl, aapt and libapksig-java.
set -Eeuo pipefail
cd "$(dirname "$0")"

for command in java javac curl zip openssl aapt python3; do
  command -v "$command" >/dev/null || { echo "error: missing required command: $command" >&2; exit 127; }
done
[[ -f /usr/share/java/apksig.jar ]] || { echo "error: install libapksig-java (or provide /usr/share/java/apksig.jar)" >&2; exit 1; }

python3 embed_shader.py
mkdir -p tools signing
rm -rf work
mkdir -p work/classes
fetch() { [[ -s "$2" ]] || curl --fail --location --retry 3 --retry-delay 2 --output "$2" "$1"; }
fetch https://raw.githubusercontent.com/Sable/android-platforms/master/android-34/android.jar tools/android.jar
fetch https://api.xposed.info/de/robv/android/xposed/api/82/api-82.jar tools/xposed.jar
fetch https://storage.googleapis.com/r8-releases/raw/8.3.37/r8.jar tools/r8.jar

javac -encoding UTF-8 -source 8 -target 8 -Xlint:-options -nowarn \
  -cp tools/android.jar:tools/xposed.jar -d work/classes src/dev/rgbpulse/gboard/*.java
java -cp tools/r8.jar com.android.tools.r8.D8 --release --lib tools/android.jar \
  --classpath tools/xposed.jar --min-api 33 --output work $(find work/classes -name '*.class' -print)
aapt package -f -M AndroidManifest.xml -S res -A assets -I tools/android.jar -F work/unsigned.apk
(cd work && zip -q -u unsigned.apk classes.dex)

if [[ ! -s signing/key.pk8 || ! -s signing/cert.pem ]]; then
  openssl req -x509 -newkey rsa:2048 -keyout signing/key.pem -out signing/cert.pem \
    -days 10000 -nodes -subj '/CN=Gboard RGB Pulse local build/'
  openssl pkcs8 -topk8 -inform PEM -outform DER -in signing/key.pem -out signing/key.pk8 -nocrypt
  chmod 600 signing/key.pem signing/key.pk8
fi
javac -encoding UTF-8 -nowarn -cp /usr/share/java/apksig.jar -d work Sign.java
java -cp /usr/share/java/apksig.jar:work Sign signing/key.pk8 signing/cert.pem \
  work/unsigned.apk "${1:-Gboard-RGB-Pulse.apk}"
echo "Built ${1:-Gboard-RGB-Pulse.apk}"
