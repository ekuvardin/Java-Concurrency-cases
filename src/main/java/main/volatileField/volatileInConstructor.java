package main.volatileField;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.IntResult1;

public class volatileInConstructor {

    /*
     * The main question can r1 == 43?
     * The only possible sequence of actions depictured below
     * 
     * read(v, !=null)
     *     \----po----> vstore(v.a, 42)
     *                    \----sw----> read(v.a, 42)
     *                               \----po----> write(v.a, 43)
     *                                     \----po----> write(v)
     *                                                      \----po---->read(v.a, 43)
     * 
     * from read(v!=null) to write(v.a, 43) we clearly see hb(transitive closure po, sw, po)
     * Then according through doc 17.4.5 If x and y are actions of the same thread and x comes before y in program order, then hb(x, y). 
     * write(v) hb read(v.a, 43)
     * 
     * And finally
     * read(v, !=null)
     *     \----hb----> vstore(v.a, 42)
     *                    \----hb----> read(v.a, 42)
     *                               \----hb----> write(v.a, 43)
     *                                     \----hb----> write(v)
     *                                                      \----hb---->read(v.a, 43)
     *                                                      
     *  But  write(v) must be before  read(v, !=null) and we violate hb consistency                                               
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
     * The main question can r1 == 42?
     * Yes it can for fully answer see http://cs.oswego.edu/pipermail/concurrency-interest/2013-November/011954.html
     * 
     *   read(a, !null)
     *      \--po--> vread(a.f, 0)
     *                    \---so---> vstore(a.f, 42)
     *                                    \---po---> store(a)
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
