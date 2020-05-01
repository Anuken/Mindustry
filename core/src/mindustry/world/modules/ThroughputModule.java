package mindustry.world.modules;

import arc.math.*;
import arc.util.io.*;

public class ThroughputModule extends BlockModule{

    @Override
    public void write(Writes write){

    }

    @Override
    public void read(Reads read){

    }

    public WindowedMean window = new WindowedMean(60 * 10);
    public float i;

    public void update(){
        window.addValue(i);
        i = 0;
    }
}
