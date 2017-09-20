package main.volatileField;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.IntResult1;

public class volatileInConstructor {

    /*
     * W: Can r1 == 43?
     * A: No
     *
     * The only possible sequence of actions are depictured below
     *
     * Actor1
     *      vread(v, !=null)
     *          \---po---> vstore(v.a, 42)
     * Actor2               \
     *                      \---sw--->  vread(v.a, 42)
     *                                  \---po---> vwrite(v.a, 43)
     *                                              \---po---> vstore(v)
     *                                                          \---po--->vread(v.a, 43)
     * 
     * And finally
     * Actor1
     *      vread(v, !=null)
     *          \---hb---> vstore(v.a, 42)
     * Actor2               \
     *                      \---hb--->  vread(v.a, 42)
     *                                  \---hb---> vwrite(v.a, 43)
     *                                              \---hb---> vstore(v)
     *                                                          \---hb--->vread(v.a, 43)
     *
     *  But we can not commit read(v, !=null) before store(v), according JLS 17.4.5.
     *  (For simple replace store(v) as v=T)
     *  We say that a read <T> of a variable <v> is allowed to observe
     *  a write <T> to <v> if, in the happens-before partial order of the execution trace:
     *  read<T> is not ordered before write <T> and according to your trace we violate this.
     *
     *  So we can't see 43.
     */
    @JCStressTest
    @Outcome(id = "1", expect = Expect.ACCEPTABLE, desc = "Works only actor1")
    @Outcome(id = "42", expect = Expect.ACCEPTABLE, desc = "Data races. Actor2 writes field v.a right after actor1 create object")
    @Outcome(id = "43", expect = Expect.FORBIDDEN, desc = "You can't see initialized variable 42 in constructor")
    @State
    public static class SafePublicationExample {

        volatile SafeVol v;

        @Actor
        void actor1(IntResult1 r) {
            v = new SafeVol();
            r.r1 = v.a;
        }

        @Actor
        void actor2() {
            if (v != null) {
                v.a = 42;
            }
        }

        class SafeVol {
            volatile int a = 0;

            public SafeVol() {
                a = a + 1;
            }
        }
    }

    /*
     * Q: Can we see r1 == 42?
     * W: Yes
     *
     * Yes we can, for fully answer see http://cs.oswego.edu/pipermail/concurrency-interest/2013-November/011954.html
     * 
     *   read(v, !null)
     *      \--po--> vread(v.a, 0)
     *                    \---so---> vstore(v.a, 42)
     *                                    \---po---> store(v)
     * JVM accept this execution
     */
    @JCStressTest
    @Outcome(id = "-1", expect = Expect.ACCEPTABLE, desc = "Actor1 doesn't see reference")
    @Outcome(id = "0", expect = Expect.ACCEPTABLE_INTERESTING, desc = "Object publishing before constructor executing")
    @Outcome(id = "42", expect = Expect.ACCEPTABLE, desc = "Works only actor2 then sequentially actor1")
    @State
    public static class UnSafePublicationExample {

        SafeVol v;

        @Actor
        void actor1(IntResult1 r) {
            if (v != null) {
                r.r1 = v.a;
            } else {
                r.r1 = -1;
            }
        }

        @Actor
        void actor2() {
            v = new SafeVol();
        }

        class SafeVol {
            volatile int a = 0;

            public SafeVol() {
                a = 42;
            }
        }
    }
}
