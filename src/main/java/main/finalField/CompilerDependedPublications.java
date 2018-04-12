package main.finalField;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.IntResult1;

public class CompilerDependedPublications {

    /*
     * All comments from HotSpotSafePublication acceptable to this. This ex publish correct reference on HotSpot.
     *
     * As you now from jls(https://docs.oracle.com/javase/specs/jls/se8/html/jls-17.html)
     * that write to final field hb to read this filed from another thread, am I right?
     *
     * Partially, there is smt more. Find words
     *         This happens-before ordering does not transitively close with other happens-before orderings.
     *
     * That means
     * write(instance.a =1) ---hb---> read(instance.a) but not ---hb---> with read in the same thread read(instance.b)
     *
     * And we can see instance.b == 0 in Actor2
     */
    @JCStressTest
    @Outcome(id = "0", expect = Expect.ACCEPTABLE_INTERESTING, desc = "Can see?")
    @Outcome(id = "1", expect = Expect.ACCEPTABLE, desc = "Fully initialized object")
    @Outcome(id = "-1", expect = Expect.ACCEPTABLE, desc = "Instance hasn't been initialized")
    @State
    public static class VeryTrickyUnSafePublication {

        FinalClass instance;

        @Actor
        void actor1() {
            instance = new FinalClass();
        }

        @Actor
        void actor2(IntResult1 r) {
            final FinalClass tmp = instance; // Prevent NPE
            if (tmp != null) {
                r.r1 = tmp.doAction();
            } else {
                r.r1 = -1;
            }
        }

        class FinalClass {

            final int a;
            int b;

            public FinalClass() {
                a = 1;
                b = 1;
            }

            public synchronized int doAction() {
                return b;
            }
        }
    }
}
