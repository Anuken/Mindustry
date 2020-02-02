package mindustry.annotations.util;

import com.squareup.javapoet.*;
import mindustry.annotations.*;

import javax.lang.model.element.*;
import javax.lang.model.type.*;

public class Selement<T extends Element>{
    public final T e;

    public Selement(T e){
        this.e = e;
    }

    public Element up(){
        return e.getEnclosingElement();
    }

    public TypeMirror mirror(){
        return e.asType();
    }

    public TypeName tname(){
        return TypeName.get(mirror());
    }

    public ClassName cname(){
        return ClassName.get((TypeElement)BaseProcessor.typeu.asElement(mirror()));
    }

    public String name(){
        return e.getSimpleName().toString();
    }

    @Override
    public String toString(){
        return e.toString();
    }

    @Override
    public int hashCode(){
        return e.hashCode();
    }

    @Override
    public boolean equals(Object o){
        return o != null && o.getClass() == getClass() && ((Selement)o).e.equals(e);
    }
}
