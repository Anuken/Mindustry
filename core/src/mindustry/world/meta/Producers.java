package mindustry.world.meta;

import arc.struct.*;
import mindustry.gen.*;

public class Producers{
    private Array<Produce> producers = new Array<>();

    public void add(Produce prod){
        producers.add(prod);
    }

    interface Produce{
        void add(Tilec entity);
    }
}
