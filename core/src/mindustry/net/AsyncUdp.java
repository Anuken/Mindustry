package mindustry.net;

import arc.func.*;
import arc.util.*;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;

public class AsyncUdp{
    static Selector selector;
    static DelayQueue<Request> removals = new DelayQueue<>();
    static TaskQueue tasks = new TaskQueue();
    static int emptySelects;

    static{
        try{
            selector = Selector.open();

            //handle requests and tasks
            Threads.daemon("AsyncUDP", () -> {
                while(true){
                    try{
                        long startTime = Time.millis();
                        int selected = selector.select(0);

                        tasks.run();

                        if(selected == 0){
                            //prevent hogging the CPU due to empty selects as per Kryonet implementation
                            if(emptySelects++ >= 100){
                                emptySelects = 0;
                                long elapsedTime = System.currentTimeMillis() - startTime;
                                if(elapsedTime < 25) Threads.sleep(25 - elapsedTime);
                            }
                            continue;
                        }

                        var keys = selector.selectedKeys();
                        for(Iterator<SelectionKey> iter = keys.iterator(); iter.hasNext(); ){
                            var key = iter.next();
                            iter.remove();

                            if(key.isReadable() && key.isValid()){
                                var request = (Request)key.attachment();
                                try{
                                    var channel = (DatagramChannel)key.channel();
                                    var buffer = ByteBuffer.allocate(request.bufferSize);
                                    channel.receive(buffer);
                                    buffer.flip();

                                    request.received.get(buffer);
                                    request.close();
                                }catch(Exception error){
                                    request.fail(error);
                                }
                            }

                        }
                    }catch(Throwable e){
                        //should not happen
                        Log.err(e);
                    }
                }
            });

            //remove requests with the delay queue
            Threads.daemon("AsyncUDP-Delay", () -> {
                while(true){
                    try{
                        var request = removals.take();
                        tasks.post(() -> request.fail(new TimeoutException()));
                        selector.wakeup();
                    }catch(InterruptedException ignored){}
                }
            });
        }catch(IOException e){
            throw new ArcRuntimeException(e);
        }
    }

    public static void send(InetSocketAddress address, int timeout, int bufferSize, ByteBuffer data, Cons<ByteBuffer> received, Cons<Exception> failed){
        tasks.post(() -> {
            try{
                DatagramChannel channel = selector.provider().openDatagramChannel();
                channel.configureBlocking(false);
                channel.connect(address);
                channel.send(data, address);

                SelectionKey key = channel.register(selector, SelectionKey.OP_READ);

                Request req = new Request(address, timeout, bufferSize, data, channel, key, received, failed);
                key.attach(req);

                removals.offer(req);
            }catch(Exception e){
                failed.get(e);
            }
        });
        selector.wakeup();
    }

    static class Request implements Delayed{
        final InetSocketAddress address;
        final long timeout, connectStartMs;
        final int bufferSize;
        final ByteBuffer data;
        final Cons<ByteBuffer> received;
        final Cons<Exception> failed;
        final DatagramChannel channel;
        final SelectionKey key;

        boolean closed = false;

        public Request(InetSocketAddress address, long timeout, int bufferSize, ByteBuffer data, DatagramChannel channel, SelectionKey key, Cons<ByteBuffer> received, Cons<Exception> failed){
            this.address = address;
            this.timeout = timeout;
            this.bufferSize = bufferSize;
            this.data = data;
            this.received = received;
            this.failed = failed;
            this.channel = channel;
            this.key = key;
            this.connectStartMs = Time.millis();
        }

        void close(){
            try{
                closed = true;
                key.cancel();
                channel.close();
            }catch(Exception close){
                close.printStackTrace();
            }
        }

        void fail(Exception error){
            if(!closed){
                failed.get(error);
                close();
            }
        }

        @Override
        public long getDelay(TimeUnit unit){
            return unit.convert(timeout - Time.timeSinceMillis(connectStartMs), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed o){
            return Long.compare(getDelay(TimeUnit.MILLISECONDS), o.getDelay(TimeUnit.MILLISECONDS));
        }
    }
}
