package mindustry.mod;

import arc.struct.*;

public class ModClassLoader extends ClassLoader{
    private Seq<ClassLoader> children = new Seq<>();
    private volatile boolean inChild = false;

    public void addChild(ClassLoader child){
        children.add(child);
    }

    @Override
    protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException{
        //always try the superclass first
        try{
            return super.loadClass(name, resolve);
        }catch(ClassNotFoundException error){
            //a child may try to delegate class loading to its parent, which is *this class loader* - do not let that happen
            if(inChild) throw error;
            int size = children.size;
            //if it doesn't exist in the main class loader, try all the children
            for(int i = 0; i < size; i++){
                try{
                    inChild = true;
                    var out = children.get(i).loadClass(name);
                    inChild = false;
                    return out;
                }catch(ClassNotFoundException ignored){
                }
            }
            throw error;
        }
    }
}
