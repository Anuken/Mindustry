package mindustry.entities.abilities;

import mindustry.gen.*;

public abstract class Ability implements Cloneable{
    public void update(Unit unit){}
    public void draw(Unit unit){}

    public Ability copy(){
        try{
            return (Ability)clone();
        }catch(CloneNotSupportedException e){
            //I am disgusted
            throw new RuntimeException("java sucks", e);
        }
    }
}
