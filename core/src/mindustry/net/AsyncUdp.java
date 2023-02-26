package mindustry.net;

import arc.func.*;
import arc.util.*;

import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.*;

public class AsyncUdp implements Runnable{
    Selector selector;
    DelayQueue<Request> removals = new DelayQueue<>();
    TaskQueue tasks = new TaskQueue();
    int emptySelects;

    public AsyncUdp(){
        try{
            selector = Selector.open();

            Threads.daemon("AsyncUDP", this);
            Threads.daemon("AsyncUDP-Delay", () -> {
                while(true){
                    try{
                        var request = removals.take();
                        synchronized(request){
                            request.cancel(new TimeoutException());
                        }
                    }catch(InterruptedException ignored){
                    }
                }
            });
        }catch(IOException e){
            throw new ArcRuntimeException(e);
        }
    }

    public void send(InetSocketAddress address, int timeout, int bufferSize, ByteBuffer data, Cons<ByteBuffer> received, Cons<Exception> failed){
        //TODO is it worth posting to the task queue when you can just run it here? shouldn't be very expensive
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
                Log.err(e);
                failed.get(e);
            }
        });
        selector.wakeup();
    }

    @Override
    public void run(){
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

                    if(key.isReadable()){
                        var request = (Request)key.attachment();

                        synchronized(request){
                            try{
                                var channel = (DatagramChannel)key.channel();
                                var buffer = ByteBuffer.allocate(request.bufferSize);
                                channel.receive(buffer);
                                buffer.flip();

                                request.received.get(buffer);
                                request.close();
                            }catch(IOException error){
                                request.cancel(error);
                                Log.err(error);
                            }
                        }
                    }

                }
            }catch(IOException e){
                Log.err(e);
            }
        }
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
            if(!key.isValid()) return;
            try{
                key.cancel();
                channel.close();
            }catch(Exception close){
                close.printStackTrace();
            }
        }

        void cancel(Exception error){
            if(!key.isValid()) return;
            failed.get(error);
            close();
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
