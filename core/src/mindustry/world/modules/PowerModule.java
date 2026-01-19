package mindustry.world.modules;

import arc.struct.*;
import arc.util.io.*;
import mindustry.world.blocks.power.*;

public class PowerModule extends BlockModule{
    /**
     * In case of unbuffered consumers, this is the percentage (1.0f = 100%) of the demanded power which can be supplied.
     * Blocks will work at a reduced efficiency if this is not equal to 1.0f.
     * In case of buffered consumers, this is the percentage of power stored in relation to the maximum capacity.
     */
    public float status = 0.0f;
    public boolean init;
    public PowerGraph graph = new PowerGraph();
    public IntSeq links = new IntSeq();
    private IntSet linksSet = new IntSet();

    @Override
    public void write(Writes write){
        write.s(links.size);
        for(int i = 0; i < links.size; i++){
            write.i(links.get(i));
        }
        write.f(status);
    }

    @Override
    public void read(Reads read){
        links.clear();
        linksSet.clear();
        short amount = read.s();
        for(int i = 0; i < amount; i++){
            int link = read.i();
            links.add(link);
            linksSet.add(link);
        }
        status = read.f();
        if(Float.isNaN(status) || Float.isInfinite(status)) status = 0f;
    }

    public boolean containsLink(int pos){
        return linksSet.contains(pos);
    }

    public void addLink(int pos){
        if(linksSet.add(pos)){
            links.add(pos);
        }
    }

    public void addLinkUnique(int pos){
        if(linksSet.add(pos)){
            links.add(pos);
        }
    }

    public boolean removeLink(int pos){
        if(linksSet.remove(pos)){
            links.removeValue(pos);
            return true;
        }
        return false;
    }

    public void removeLinkIndex(int index){
        if(index >= 0 && index < links.size){
            linksSet.remove(links.get(index));
            links.removeIndex(index);
        }
    }

    public void clearLinks(){
        links.clear();
        linksSet.clear();
    }
}