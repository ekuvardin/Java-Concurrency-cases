package main.finalField;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.IntResult1;

public class NestedFinal {

    /*
     * Test case describes, that final initialization in constructor spread over only the class with final field.
     *
     * Q: Can we see v.b == 0?
     * Psedocode:
     *  write(v.b, 1)
     *     \----po---->freeze
     *                 \----so---->read(v!=null)
     *                        \----po---->read(v.a,0)
     *                               \----so---->write(v.a, 1)
     *                                         \----po---->publish(v)
     *
     * transforms to
     *  write(v.b, 1)
     *     \----hb---->freeze
     *                 \----so---->read(v!=null)
     *                        \----hb---->read(v.a,0)
     *                               \----so---->write(v.a, 1)
     *                                         \----hb---->publish(v)
     *
     * So we can find execution when no rules is violated.
     * Note1 read(v.a,0) and write(v.a, 1) linked only so not sw
     * Note2 freeze linked with read(v!=null). Seriously. I don't know should it be HB.
     *
     *
     * !!!!!!!!!!!WARRANT!!!!!!!!!!
     * Some speculation. Be very attentive and don't understand phrases below. It's just a theory.
     * Thrust only JVM because JVM gives some more than JSR-133 Compiler Cookbook.
     *
     * You can't see id = 0 on x86 processors because it(according JSR-133 Compiler Cookbook)
     * has only StoreLoad barrier and can reorder only Normal Store following Normal Load operation.
     *
     * This code ( v = new OrdinaryClass()) can be represented as
     *
     * FinalClass temp = <new>  // system init
     * temp.a = 1               // initialize final field
     * [LoadStore|StoreStore]   // on x86 we can remove this on
     * temp.b = 1               // initialize ordinary field
     * v = temp;                // publish
     *
     */
    @JCStressTest
    @Outcome(id = "0", expect = Expect.ACCEPTABLE_INTERESTING, desc = "Despite that we have final filed, save initialization doesn't cover OrdinaryClass, only FinalClass")
    @Outcome(id = "1", expect = Expect.ACCEPTABLE, desc = "Fully initialized object")
    @Outcome(id = "-1", expect = Expect.ACCEPTABLE, desc = "Actor2 doesn't see reference")
    @State
    public static class UnsafePublication {

        OrdinaryClass v;

        @Actor
        void actor1() {
            v = new OrdinaryClass();
        }

        @Actor
        void actor2(IntResult1 r) {
            if (v != null) {
                r.r1 = v.b;
            } else {
                r.r1 = -1;
            }
        }

        class FinalClass {
            final int a;

            public FinalClass() {
                a = 1;
            }
        }

        class OrdinaryClass extends FinalClass {
            volatile int b;

            public OrdinaryClass() {
                super();
                b = 1;
            }
        }
    }

    /*
     * Test case describes, that final initialization in constructor spread over only the class with final field.
     *
     * Q: Can we see v.b == 0?
     *
     * At least in actor2 we need this
     * read(v, !=null)
     *     \---po--read(v.b, 0)
     *
     * Trace A
     * write(v.b = 1)
     *    \----po---->write(v)
     *            \----sw---->read(v, !=null)
     *                              \----po---->read(v.b, 0)
     *
     * (po, sw, po) transforms to hb
     *
     * write(v.b = 1)
     *    \----hb---->write(v)
     *            \----hb---->read(v, !=null)
     *                              \----hb---->read(v.b, 0)
     *
     * and read(v.b, 0) inconsistent with previous write(v.b = 1) according to hb rules
     * We can't see 0
     */
    @JCStressTest
    @Outcome(id = "0", expect = Expect.FORBIDDEN, desc = "Can't see according to hb rules")
    @Outcome(id = "1", expect = Expect.ACCEPTABLE, desc = "Fully initialized object")
    @Outcome(id = "-1", expect = Expect.ACCEPTABLE, desc = "Actor2 doesn't see reference")
    @State
    public static class SafePublication {

        volatile OrdinaryClass v;

        @Actor
        void actor1() {
            v = new OrdinaryClass();
        }

        @Actor
        void actor2(IntResult1 r) {
            if (v != null) {
                r.r1 = v.b;
            } else {
                r.r1 = -1;
            }
        }

        class FinalClass {
            final int a;

            public FinalClass() {
                a = 1;
            }
        }

        class OrdinaryClass extends FinalClass {
            int b;

            public OrdinaryClass() {
                super();
                b = 1;
            }
        }
    }
}
