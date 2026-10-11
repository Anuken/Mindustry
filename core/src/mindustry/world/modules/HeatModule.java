package mindustry.world.modules;

import mindustry.io.*;

public class HeatModule extends BlockModule{
    /** Heat emitted to neighbors. Only this value is saved. */
    public float output;
    /** Total heat received from neighbors. Recomputed every tick. */
    public float input;
    /** Heat received per side. Recomputed every tick. */
    public float[] sides = new float[4];

    @Override
    public void write(Writes write){
        write.f(output);
    }

    @Override
    public void read(Reads read, boolean legacy){
        output = read.f();
    }
}
