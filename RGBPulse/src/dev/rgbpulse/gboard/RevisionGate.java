package dev.rgbpulse.gboard;

final class RevisionGate {
    static boolean matches(String current,String received){
        return current!=null&&!current.isEmpty()&&current.equals(received);
    }
}
