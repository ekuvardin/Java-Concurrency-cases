package main.ABQConditionFreeQueue;

import main.volatileField.volatileInConstructor;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.IntResult1;

public class ABQConditionFreeQueueTest {

    /*
    *   If we don't have Thread.yield() then test hung up.
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
