package dev.rgbpulse.gboard;
/** Main-letter guard keeps secondary hints and unsupported labels readable. */
final class CutoutPolicy {
 static boolean mainLegend(float size,float height){
  return Float.isFinite(size)&&Float.isFinite(height)&&height>0&&size>=height*.22f&&size<=height*.72f;
 }
}
