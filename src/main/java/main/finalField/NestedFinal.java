package main.finalField;

import main.volatileInConstructorNested;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.IntResult1;
import org.openjdk.jcstress.infra.results.IntResult2;

public class NestedFinal {

    /*
     * Test case describes, that final initialization in constructor spread over only the class with final field.
     *
     * Psedocode:
     *
     * FinalClass temp = <new>  // system init
     * temp.a = 1               // initialize final field
     * [LoadStore|StoreStore]
     * temp.b = 1               // initialize ordinary field
     * v = temp;                // publish
     *
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
            int b;

            public OrdinaryClass() {
                super();
                b = 1;
            }
        }
    }

    /*
     * Test case describes, that final initialization in constructor spread over only the class with final field.
     *
     * Psedocode:
     *
     * FinalClass temp = <new>  // system init
     * temp.a = 1               // initialize final field
     * [LoadStore|StoreStore]
     * temp.b = 1               // initialize ordinary field
     * [StoreStore]             // According to JSR-133 Cookbook we need barries between [Normal Store] and [Volatile Store]
     * v = temp;                // publish
     *
     * Just imagine, that we can see v.b=0
     *
     *
     * write(v.b = 1)
     *    \----po---->write(v)
     *            \----sw----read(v, !=null)
     *                              \---po--read(v.b, 0)
     *
     * transforms to hb
     *
     * write(v.b = 1)
     *    \----hb---->write(v)
     *            \----hb----read(v, !=null)
     *                              \---hb--read(v.b, 0)
     *
     * and read(v.b, 0) inconsistent with previous write(v.b = 1) according to hb rules
     * thus we can't see v.b = 0
     *
     */
    @JCStressTest
    @Outcome(id = "0", expect = Expect.FORBIDDEN, desc = "Safe publication")
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
