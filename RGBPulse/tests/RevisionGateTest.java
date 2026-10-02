package dev.rgbpulse.gboard;
public final class RevisionGateTest {
    static void check(boolean condition){if(!condition)throw new AssertionError();}
    public static void main(String[] args){
        check(RevisionGate.matches("revision-new","revision-new"));
        check(!RevisionGate.matches("revision-new","revision-old"));
        check(!RevisionGate.matches("",""));check(!RevisionGate.matches(null,null));
        check(!RevisionGate.matches("revision-new",null));check(!RevisionGate.matches(null,"revision-new"));
        System.out.println("PASS: only nonempty current revision receipts are accepted");
    }
}
