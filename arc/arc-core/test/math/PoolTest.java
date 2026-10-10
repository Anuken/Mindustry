package math;

import arc.func.*;
import arc.struct.*;
import arc.util.*;
import arc.util.pooling.*;
import org.junit.*;

public class PoolTest{

    @Test
    public void allocation(){
        long start = 0;

        int objects = 100000;
        Seq<Object> list = new Seq<>(objects);
        Pools.get(Object.class, Object::new, objects);

        for(int i = 0; i < objects; i++){
            list.add(Pools.obtain(Object.class, Object::new));
        }

        Pools.freeAll(list, true);
        list.clear();

        long pre = memory();

        for(int i = 0; i < objects; i++){
            list.add(Pools.get(Object.class, Object::new).obtain());
        }

        Pools.freeAll(list, true);
        list.clear();

        for(int i = 0; i < objects; i++){
            list.add(Pools.get(Object.class, Object::new).obtain());
        }

        long post = memory();

        Prov a = Object::new;
        Prov b = Object::new;

        Log.info("a == b: @; codes: @ @; equality: @", a == b, a.hashCode(), b.hashCode(), a.equals(b));
        Log.info("Memory delta: @ b", (post - pre));
        Log.info("Total memory allocated: @ mb", Strings.fixed((post - start)/1024f/1024f, 1));
    }

    long memory(){
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
    }
}
