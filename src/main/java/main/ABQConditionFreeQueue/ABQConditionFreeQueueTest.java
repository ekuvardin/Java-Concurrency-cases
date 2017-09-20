package main.ABQConditionFreeQueue;

import org.openjdk.jcstress.annotations.*;

public class ABQConditionFreeQueueTest {

    /*
    *  Q: Can actor1 hangs?
    *  W: Yes
    *
    *  For example Thread A put value, Thread B read value
    *
    *  Thread A
    *   load(count, 0)
    *       \--hb--> load(items.length, threshold)
    *                  \--hb--> lock(v)
    *                           \--hb--> load(count, 0)
    *                                      \--hb--> load(items.length, threshold)
    *                                            \--hb--> write(count, 1)
    *                                                \--hb--> unlock(v)
    *  Thread B                                                 \
    *                                                           \--race-->load(count, 0)
    *
    *  As unlock(v) does't linked with load(count, 0) even with so
    *  An we can see count == 0
     */

    @JCStressTest(Mode.Termination)
    @Outcome(id = "TERMINATED", expect = Expect.ACCEPTABLE, desc = "Gracefully finished.")
    @Outcome(id = "STALE", expect = Expect.ACCEPTABLE_INTERESTING, desc = "Test hung up.")
    @State
    public static class hungRun {

        ABQConditionFreeQueue<Integer> queue = new ABQConditionFreeQueue<>(1);

        @Actor
        void actor1() throws InterruptedException {
            queue.dequeue(false);
        }

        @Signal
        public void signal() throws InterruptedException {
            queue.enqueue(1);
        }

    }

    /*
      On x86 situation is ridiculous: if we have Thread.yield() then test doesn't hung up.
      I don't know why because Thread.yield() is not a synchronization action.
     */
    @JCStressTest(Mode.Termination)
    @Outcome(id = "TERMINATED", expect = Expect.ACCEPTABLE, desc = "Gracefully finished.")
    @Outcome(id = "STALE", expect = Expect.FORBIDDEN, desc = "Test hung up.")
    @State
    public static class NormalRun {

        ABQConditionFreeQueue<Integer> queue = new ABQConditionFreeQueue<>(1);

        @Actor
        void actor1() throws InterruptedException {
            queue.dequeue(true);
        }

        @Signal
        public void signal() throws InterruptedException {
            queue.enqueue(1);
        }

    }
}
