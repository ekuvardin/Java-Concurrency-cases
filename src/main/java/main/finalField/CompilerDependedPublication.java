package main.finalField;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.IntResult1;

public class CompilerDependedPublication {

    /*
    * According to http://hg.openjdk.java.net/jdk7u/jdk7u/hotspot/file/tip/src/share/vm/opto/parse1.cpp (see below full comment)
    * HotSpot writes membar to the end of constructor despite of the fact that we have one final and non-final fields and
    * JVM guarantees only visibility to final fields (https://docs.oracle.com/javase/specs/jls/se8/html/jls-17.html)
    *
    * See originally comment from http://hg.openjdk.java.net/jdk7u/jdk7u/hotspot/file/tip/src/share/vm/opto/parse1.cpp
    *
    * Add MemBarRelease for constructors which write volatile field (PPC64).
    * Intention is to avoid that other threads can observe initial values even though the
    * constructor has set the volatile field. Java programmers wouldn't like it and we wanna be nice.
    * if (wrote_final() PPC64_ONLY(|| (wrote_volatile() && method()->is_initializer()))) {
    *     This method (which must be a constructor by the rules of Java)
    *     wrote a final.  The effects of all initializations must be
    *     committed to memory before any code after the constructor
    *     publishes the reference to the newly constructor object.
    *     Rather than wait for the publication, we simply block the
    *     writes here.  Rather than put a barrier on only those writes
    *     which are required to complete, we force all writes to complete.
    *     "All bets are off" unless the first publication occurs after a
    *     normal return from the constructor.  We do not attempt to detect
    *     such unusual early publications.  But no barrier is needed on
    *     exceptional returns, since they cannot publish normally.
    *
    *    _exits.insert_mem_bar(Op_MemBarRelease);
    *
    *    #ifndef PRODUCT
    *            if (PrintOpto && (Verbose || WizardMode)) {
    *                method()->print_name();
    *                tty->print_cr(" writes finals and needs a memory barrier");
    *            }
    *    #endif
    *  }
    */

    @JCStressTest
    @Outcome(id = "0", expect = Expect.FORBIDDEN, desc = "Can't see according to HotSpot implementation")
    @Outcome(id = "1", expect = Expect.ACCEPTABLE, desc = "Fully initialized object")
    @Outcome(id = "-1", expect = Expect.ACCEPTABLE, desc = "Actor2 doesn't see reference")
    @State
    public static class HotSpotSafePublication {

        FinalClass v;

        @Actor
        void actor1() {
            v = new FinalClass();
        }

        @Actor
        void actor2(IntResult1 r) {
            final FinalClass tmp = v; // Prevent NPE
            if (tmp != null) {
                r.r1 = tmp.b;
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
        }
    }

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
     * write(v.a =1) ---hb---> read(v.a) but not ---hb---> with read in the same thread read(v.b)
     *
     * And we can see v.b == 0 in Actor2
     */
    @JCStressTest
    @Outcome(id = "0", expect = Expect.ACCEPTABLE_INTERESTING, desc = "Can see according to jls")
    @Outcome(id = "1", expect = Expect.ACCEPTABLE, desc = "Fully initialized object")
    @Outcome(id = "-1", expect = Expect.ACCEPTABLE, desc = "Actor2 doesn't see reference")
    @State
    public static class VeryTrickyUnSafePublication {

        FinalClass v;

        @Actor
        void actor1() {
            v = new FinalClass();
        }

        @Actor
        void actor2(IntResult1 r) {

            final FinalClass tmp = v; // Prevent NPE
            if (tmp != null) {
                IntResult1 r2 = new IntResult1();
                r2.r1 = tmp.a;

                r.r1 = tmp.b;
            } else {
                r.r1 = -1;
            }
        }

        class FinalClass {

            final int a;
            int b;

            public FinalClass() {
                b = 1;
                a = 1;
            }
        }
    }

}
