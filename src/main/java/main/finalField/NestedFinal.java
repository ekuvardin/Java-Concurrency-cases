package main.finalField;

import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.IntResult1;

public class NestedFinal {

    /*
     * Test case describes, that final initialization in constructor spread over only the class with final field.
     *
     * Q: Can we see v.b == 0?
     * A: Yes
     *
     * Pseudocode:
     * Actor1
     *  write(v.b, 1)
     *     \----po---->freeze
     *          \----po---->write(v.a, 1)
     *                 \----po---->publish(v)
     *                 \
     *                 \
     * Actor2          \
     *                 \----mc---->read(tmp, v)
     *                                 \----po---->read(tmp!=null)
     *                                                \----po---->read(tmp.a,0)
     *
     *
     * transforms to
     * Actor1
     *  write(v.b, 1)
     *     \----hb---->freeze
     *          \----hb---->write(v.a, 1)
     *                 \----hb---->publish(v)
     *                 \
     *                 \
     * Actor2          \
     *                 \----mc---->read(tmp, v)
     *                                 \----hb---->read(tmp!=null)
     *                                                \----hb---->read(tmp.a,0)
     *
     * So we can find execution when no rules is violated.
     * Note: mc - is memory change means write v and read v to tmp
     *
     * Thus, JVM accepts this execution
     *
     * !!!!!!!!!!!WARNING!!!!!!!!!!
     * Some speculation. This topic covers JSR-133 Compiler Cookbook, JLS, Compilers.
     * Be very attentive and don't understand phrases below. It's just a theory.
     *
     * Can we see id = 0 on x86 processors? So there are two players that can influence(may be some more) on it:
     * compilers and processors.
     *
     *  Processors. Just imagine that we see instructions like below
     *  This code ( v = new OrdinaryClass()) can be represented as
     *
     *  FinalClass temp = <new>  // system init
     *  temp.a = 1               // initialize final field
     *  [LoadStore|StoreStore]   // on x86 we can remove this
     *  temp.b = 1               // initialize ordinary field
     *  v = temp;                // publish
     *
     *  We can't see b = 0 on x86 processors because it(according JSR-133 Compiler Cookbook)
     *  has only StoreLoad barrier and can reorder only Normal Store following Normal Load operation.
     *  (for ex. On ARM & PPC we need StoreStore barrier)
     *
     *  on x86 you see
     *
     *   0x0000000002b963e9: mov     dword ptr [rdx+0ch],1h  ;*putfield a
     *                                           ; - main.FinalClass::<init>@7 (line 14)
     *                                           ; - main.OrdinaryClass::<init>@1 (line 22)
     *
     *   0x0000000002b963f0: mov     dword ptr [rdx+10h],1h  ;*putfield b
     *                                          ; - main.OrdinaryClass::<init>@7 (line 23)
     *   0x0000000002b963f7: add     rsp,40h
     *   0x0000000002b963fb: pop     rbp
     *   0x0000000002b963fc: test    dword ptr [130100h],eax  ;   {poll_return}
     *
     *
     *  And what about Compilers?
     *  It depends. For ex. HotSpot makes membar on the end of constructor if exists at least one final-field
     *  (See CompilerDependedPublication.HotSpotSafePublication)
     *  May be in Future these guaranties cover and extends classes. Who knows. If you don't know trust only JVM.
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
            final OrdinaryClass tmp = v; // Safe us from NPE
            if (tmp != null) {
                r.r1 = tmp.b;
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
     * A: No
     *
     * At least in actor2 we need this
     *
     * Actor1
     * read(v, !=null)
     *     \---po--read(v.b, 0)
     *
     * Actor2
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
     *
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
