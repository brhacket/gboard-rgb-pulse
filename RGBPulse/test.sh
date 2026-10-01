#!/usr/bin/env bash
set -Eeuo pipefail
cd "$(dirname "$0")"

if ! command -v javac >/dev/null || ! command -v java >/dev/null; then
  echo "error: JDK 11+ is required to run Java tests (javac/java not found)." >&2
  exit 127
fi
mkdir -p work/tests
rm -rf work/tests/*
# Keep each test's production dependency list explicit: this suite works without Android SDK.
javac -encoding UTF-8 -d work/tests src/dev/rgbpulse/gboard/BodyGeometry.java src/dev/rgbpulse/gboard/PanelPolicy.java tests/GeometryTest.java tests/PanelPolicyTest.java
java -cp work/tests dev.rgbpulse.gboard.GeometryTest
java -cp work/tests dev.rgbpulse.gboard.PanelPolicyTest
javac -encoding UTF-8 -d work/tests src/dev/rgbpulse/gboard/LegendPolicy.java tests/LegendPolicyTest.java
java -cp work/tests dev.rgbpulse.gboard.LegendPolicyTest
javac -encoding UTF-8 -d work/tests src/dev/rgbpulse/gboard/CapPolicy.java tests/CapPolicyTest.java
java -cp work/tests dev.rgbpulse.gboard.CapPolicyTest
javac -encoding UTF-8 -d work/tests src/dev/rgbpulse/gboard/LegendWave.java tests/LegendWaveTest.java
java -cp work/tests dev.rgbpulse.gboard.LegendWaveTest
javac -encoding UTF-8 -d work/tests src/dev/rgbpulse/gboard/CapPolicy.java src/dev/rgbpulse/gboard/CutoutPolicy.java tests/CutoutPolicyTest.java
java -cp work/tests dev.rgbpulse.gboard.CutoutPolicyTest
javac -encoding UTF-8 -d work/tests src/dev/rgbpulse/gboard/TileGeometry.java tests/TileGeometryTest.java
java -cp work/tests dev.rgbpulse.gboard.TileGeometryTest
javac -encoding UTF-8 -d work/tests src/dev/rgbpulse/gboard/TrailPoints.java tests/TrailPointsTest.java
java -cp work/tests dev.rgbpulse.gboard.TrailPointsTest
javac -encoding UTF-8 -d work/tests src/dev/rgbpulse/gboard/WavePolicy.java tests/WavePolicyTest.java
java -cp work/tests dev.rgbpulse.gboard.WavePolicyTest
python3 tests/regression_contract_test.py
python3 tests/source_contract_test.py
python3 tests/material_contract_test.py
printf 'PASS: Java policy and source contract tests\n'
