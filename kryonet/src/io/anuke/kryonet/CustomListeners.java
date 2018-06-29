package io.anuke.kryonet;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Listener.QueuedListener;

import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CustomListeners {

    static public class LagListener extends QueuedListener {
        protected final ScheduledExecutorService threadPool;
        private final int lagMillisMin, lagMillisMax;
        final LinkedList<Runnable> runnables = new LinkedList();

        public LagListener (int lagMillisMin, int lagMillisMax, Listener listener) {
            super(listener);
            this.lagMillisMin = lagMillisMin;
            this.lagMillisMax = lagMillisMax;
            threadPool = Executors.newScheduledThreadPool(1, r -> {
                Thread t = Executors.defaultThreadFactory().newThread(r);
                t.setDaemon(true);
                return t;
            });
        }

        protected int calculateLag() {
            return lagMillisMin + (int)(Math.random() * (lagMillisMax - lagMillisMin));
        }

        @Override
        public void queue (Runnable runnable) {

            synchronized (runnables) {
                runnables.addFirst(runnable);
            }
            threadPool.schedule(() -> {
                Runnable runnable1;
                synchronized (runnables) {
                    runnable1 = runnables.removeLast();
                }
                runnable1.run();
            }, calculateLag(), TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Delays, reorders and does not make guarantees to the delivery of incoming objects
     * to the wrapped listener (in order to simulate lag, jitter, package loss and
     * package duplication).
     * Notification events are likely processed on a separate thread after a delay.
     * Note that only the delivery of incoming objects is modified. To modify the delivery
     * of outgoing objects, use a UnreliableListener at the other end of the connection.
     */
    static public class UnreliableListener extends LagListener {
        private final float lossPercentage;
        private final float duplicationPercentage;
        private final CustomListeners.LagListener tcpListener;

        public UnreliableListener (int lagMillisMin, int lagMillisMax, float lossPercentage,
                                   float duplicationPercentage, Listener listener) {
            super(lagMillisMin, lagMillisMax, listener);
            this.tcpListener = new CustomListeners.LagListener(lagMillisMin, lagMillisMax, listener);
            this.lossPercentage = lossPercentage;
            this.duplicationPercentage = duplicationPercentage;
        }

        @Override
        public void received(Connection connection, Object object) {
            if(KryoCore.lastUDP) {
                super.received(connection, object);
            }else{
                tcpListener.received(connection, object);
            }
        }

        @Override
        public void queue (Runnable runnable) {
            do {
                if (Math.random() >= lossPercentage) {
                    threadPool.schedule(runnable, calculateLag(), TimeUnit.MILLISECONDS);
                }
            } while (Math.random() < duplicationPercentage);
        }
    }
}
