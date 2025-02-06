package mindustry.type;

import arc.func.*;
import arc.struct.*;
import arc.struct.ObjectIntMap.*;
import arc.util.io.*;
import mindustry.*;
import mindustry.ctype.*;

public class PayloadSeq{
    private ObjectIntMap<UnlockableContent> payloads = new ObjectIntMap<>();
    private int total;

    public boolean isEmpty(){
        return total == 0;
    }

    public boolean any(){
        return total > 0;
    }

    public int total(){
        return total;
    }

    public void add(UnlockableContent block){
        add(block, 1);
    }

    public void add(UnlockableContent block, int amount){
        if(block == null) return;
        payloads.increment(block, amount);
        total += amount;
    }

    public void remove(UnlockableContent block){
        add(block, -1);
    }

    public void remove(UnlockableContent block, int amount){
        add(block, -amount);
    }

    public void remove(Seq<PayloadStack> stacks){
        stacks.each(b -> remove(b.item, b.amount));
    }

    /** @return this object */
    public PayloadSeq removeAll(Boolf<UnlockableContent> pred){
        Entries<UnlockableContent> iter = payloads.iterator();
        while(iter.hasNext()){
            Entry<UnlockableContent> e = iter.next();
            if(pred.get(e.key)){
                iter.remove();
                total -= e.value;
            }
        }
        return this;
    }

    public void clear(){
        payloads.clear();
        total = 0;
    }

    public int get(UnlockableContent block){
        return payloads.get(block);
    }

    public boolean contains(Seq<PayloadStack> stacks){
        return !stacks.contains(b -> get(b.item) < b.amount);
    }

    public boolean contains(UnlockableContent block, int amount){
        return get(block) >= amount;
    }

    public boolean contains(UnlockableContent block){
        return get(block) >= 1;
    }

    public boolean contains(PayloadStack stack){
        return get(stack.item) >= stack.amount;
    }

    public void write(Writes write){
        //IMPORTANT NOTICE: size is negated here because I changed the format of this class at some point
        //negated = new format
        write.s(-payloads.size);
        for(var entry : payloads.entries()){
            write.b(entry.key.getContentType().ordinal());
            write.s(entry.key.id);
            write.i(entry.value);
        }
    }

    public void read(Reads read){
        total = 0;
        payloads.clear();
        short amount = read.s();
        if(amount >= 0){
            //old format, block only - can safely ignore, really
            for(int i = 0; i < amount; i++){
                add(Vars.content.block(read.s()), read.i());
            }
        }else{
            //new format
            for(int i = 0; i < -amount; i++){
                add(Vars.content.getByID(ContentType.all[read.ub()], read.s()), read.i());
            }
        }

    }
}
