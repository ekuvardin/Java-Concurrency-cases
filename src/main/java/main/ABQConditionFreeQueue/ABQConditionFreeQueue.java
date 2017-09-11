package main.ABQConditionFreeQueue;

import java.util.concurrent.locks.ReentrantLock;

public class ABQConditionFreeQueue<T> {
    private final T[] items;
    /** items index for next take, poll or remove */
    private int takeIndex = 0;
    /** items index for next put, offer, or add. */
    private int putIndex = 0;
    /** Number of items in the queue */
    private int count = 0;

    private final ReentrantLock lock = new ReentrantLock();

    @SuppressWarnings( "unchecked" )
    public ABQConditionFreeQueue( final int size ) {
        this.items = ( T[] ) new Object[size];
    }

    private int inc( int i ) {
        return ( ++i == items.length ) ? 0 : i;
    }

    public void enqueue( final T item ) throws InterruptedException {
        for ( int i = 0; ; i++ ) {
            if ( count < items.length
                    && tryAcquireLock() ) {
                try {
                    if ( count < items.length ) {
                        items[putIndex] = item;
                        putIndex = inc( putIndex );
                        ++count;
                        return;
                    }
                } finally {
                    releaseLock();
                }
            }
            backoff();
        }
    }

    public T dequeue() throws InterruptedException {
        for ( int i = 0; ; i++ ) {
            if ( count > 0 && tryAcquireLock() ) {
                try {
                    if ( count > 0 ) {
                        final T item = items[takeIndex];
                        items[takeIndex] = null;
                        takeIndex = inc( takeIndex );
                        --count;
                        return item;
                    }
                } finally {
                    releaseLock();
                }
            }
            //backoff();
        }
    }

    private void backoff() throws InterruptedException {
        Thread.yield();
    }

    private void releaseLock() {
        lock.unlock();
    }

    private boolean tryAcquireLock() throws InterruptedException {
        return lock.tryLock();
    }

}
