#!/bin/bash
# Needs: Java 11+, curl, zip, openssl, aapt, libapksig-java (Debian: apt install aapt libapksig-java)
set -euo pipefail
cd "$(dirname "$0")"
python3 embed_shader.py
mkdir -p tools signing; rm -rf work; mkdir -p work/classes
[ -f tools/android.jar ] || curl -fL -o tools/android.jar https://raw.githubusercontent.com/Sable/android-platforms/master/android-34/android.jar
[ -f tools/xposed.jar ] || curl -fL -o tools/xposed.jar https://api.xposed.info/de/robv/android/xposed/api/82/api-82.jar
[ -f tools/r8.jar ] || curl -fL -o tools/r8.jar https://storage.googleapis.com/r8-releases/raw/8.3.37/r8.jar
javac -encoding UTF-8 -source 8 -target 8 -nowarn -cp tools/android.jar:tools/xposed.jar -d work/classes src/dev/rgbpulse/gboard/*.java
java -cp tools/r8.jar com.android.tools.r8.D8 --release --lib tools/android.jar --classpath tools/xposed.jar --min-api 33 --output work $(find work/classes -name '*.class')
aapt package -f -M AndroidManifest.xml -S res -A assets -I tools/android.jar -F work/unsigned.apk
(cd work && zip -q unsigned.apk classes.dex)
if [ ! -f signing/key.pk8 ]; then
 openssl req -x509 -newkey rsa:2048 -keyout signing/key.pem -out signing/cert.pem -days 10000 -nodes -subj '/CN=Gboard RGB Pulse local build/'
 openssl pkcs8 -topk8 -inform PEM -outform DER -in signing/key.pem -out signing/key.pk8 -nocrypt
fi
javac -nowarn -cp /usr/share/java/apksig.jar -d work Sign.java
java -cp /usr/share/java/apksig.jar:work Sign signing/key.pk8 signing/cert.pem work/unsigned.apk "${1:-Gboard-RGB-Pulse.apk}"
