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
     * [StoreStore]             // According to JSR-133 Cookbook we need barries between [Normal Store] and [Volatile Store]
     * v = temp;                // publish
     *
     *
     */
    @JCStressTest
    @Outcome(id = "0", expect = Expect.ACCEPTABLE_INTERESTING, desc = "Despite that we have final filed, save initialization doesn't cover OrdinaryClass, only FinalClass")
    @Outcome(id = "1", expect = Expect.ACCEPTABLE, desc = "Fully initialized object")
    @State
    public static class UnsafePublication {

        volatile OrdinaryClass v;

        @Actor
        void actor1() {
            v = new OrdinaryClass();
        }

        @Actor
        void actor2(IntResult1 r) {
            if (v != null) {
                r.r1 = v.b;
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
