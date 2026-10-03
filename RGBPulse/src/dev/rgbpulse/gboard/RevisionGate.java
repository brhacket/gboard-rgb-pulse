package dev.rgbpulse.gboard;

final class RevisionGate {
    static boolean accepts(String current,String received,int expectedBuild,int receivedBuild){
        return matches(current,received)&&expectedBuild==receivedBuild;
    }
    static boolean matches(String current,String received){
        return current!=null&&!current.isEmpty()&&current.equals(received);
    }
}
