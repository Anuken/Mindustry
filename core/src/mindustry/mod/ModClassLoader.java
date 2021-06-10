package mindustry.mod;

import arc.struct.*;

public class ModClassLoader extends ClassLoader{
    private Seq<ClassLoader> children = new Seq<>();
    private volatile boolean inChild = false;

    public void addChild(ClassLoader child){
        children.add(child);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException{
        //a child may try to delegate class loading to its parent, which is *this class loader* - do not let that happen
        if(inChild) throw new ClassNotFoundException(name);

        ClassNotFoundException last = null;
        int size = children.size;
        //if it doesn't exist in the main class loader, try all the children
        for(int i = 0; i < size; i++){
            try{
                inChild = true;
                var out = children.get(i).loadClass(name);
                inChild = false;
                return out;
            }catch(ClassNotFoundException e){
                last = e;
            }
        }
        throw (last == null ? new ClassNotFoundException(name) : last);
    }
}
