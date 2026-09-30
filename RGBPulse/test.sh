#!/bin/bash
set -euo pipefail
cd "$(dirname "$0")"
mkdir -p work/tests
javac -encoding UTF-8 -d work/tests src/dev/rgbpulse/gboard/BodyGeometry.java src/dev/rgbpulse/gboard/PanelPolicy.java tests/GeometryTest.java tests/PanelPolicyTest.java
java -cp work/tests dev.rgbpulse.gboard.GeometryTest
java -cp work/tests dev.rgbpulse.gboard.PanelPolicyTest
python3 tests/source_contract_test.py
javac -encoding UTF-8 -d work/tests src/dev/rgbpulse/gboard/LegendPolicy.java tests/LegendPolicyTest.java
java -cp work/tests dev.rgbpulse.gboard.LegendPolicyTest
python3 tests/material_contract_test.py
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
