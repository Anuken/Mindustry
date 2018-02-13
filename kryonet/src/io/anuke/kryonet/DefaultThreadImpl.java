package io.anuke.kryonet;

import io.anuke.mindustry.core.ThreadHandler.ThreadProvider;
import io.anuke.ucore.entities.Entity;
import io.anuke.ucore.entities.EntityGroup;
import io.anuke.ucore.entities.EntityGroup.EntityContainer;
import io.anuke.ucore.util.Log;

import java.util.Iterator;
import java.util.concurrent.CopyOnWriteArrayList;

public class DefaultThreadImpl implements ThreadProvider {
    private Thread thread;

    @Override
    public boolean isOnThread() {
        return Thread.currentThread() == thread;
    }

    @Override
    public void sleep(long ms) throws InterruptedException{
        Thread.sleep(ms);
    }

    @Override
    public void start(Runnable run) {
        if(thread != null){
            thread.interrupt();
            thread = null;
        }

        thread = new Thread(run);
        thread.setDaemon(true);
        thread.setName("Update Thread");
        thread.start();
        Log.info("Starting logic thread.");
    }

    @Override
    public void stop() {
        if(thread != null){
            thread.interrupt();
            thread = null;
        }
    }

    @Override
    public void wait(Object object) throws InterruptedException{
        object.wait();
    }

    @Override
    public void notify(Object object) {
        object.notify();
    }

    @Override
    public <T extends Entity> void switchContainer(EntityGroup<T> group) {
        group.setContainer(new ConcurrentContainer<>());
    }

    static class ConcurrentContainer<T> implements EntityContainer<T>{
        private CopyOnWriteArrayList<T> list = new CopyOnWriteArrayList<>();

        @Override
        public int size() {
            return list.size();
        }

        @Override
        public void add(T item) {
            list.add(item);
        }

        @Override
        public void clear() {
            list.clear();
        }

        @Override
        public void remove(T item) {
            list.remove(item);
        }

        @Override
        public T get(int index) {
            return list.get(index);
        }

        @Override
        public Iterator<T> iterator() {
            return list.iterator();
        }
    }
}
