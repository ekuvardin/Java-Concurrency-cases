package main.ABQConditionFreeQueue;

import main.volatileField.volatileInConstructor;
import org.openjdk.jcstress.annotations.*;
import org.openjdk.jcstress.infra.results.IntResult1;

public class ABQConditionFreeQueueTest {

    @JCStressTest(Mode.Termination)
    @Outcome(id = "TERMINATED", expect = Expect.ACCEPTABLE, desc = "Gracefully finished.")
    @Outcome(id = "STALE", expect = Expect.ACCEPTABLE_INTERESTING, desc = "Test hung up.")
    @State
    public static class SafePublicationExample {

        ABQConditionFreeQueue<Integer> queue = new ABQConditionFreeQueue<>(1);

        @Actor
        void actor1() throws InterruptedException {
            queue.dequeue();
        }

        @Signal
        public void signal() throws InterruptedException {
            queue.enqueue(1);
        }

    }
}
