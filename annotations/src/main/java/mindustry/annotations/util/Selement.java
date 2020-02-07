package mindustry.annotations.util;

import arc.struct.*;
import com.squareup.javapoet.*;
import mindustry.annotations.*;

import javax.lang.model.element.*;
import javax.lang.model.type.*;
import java.lang.annotation.*;

public class Selement<T extends Element>{
    public final T e;

    public Selement(T e){
        this.e = e;
    }

    public String fullName(){
        return e.toString();
    }

    public Stype asType(){
        return new Stype((TypeElement)e);
    }

    public Svar asVar(){
        return new Svar((VariableElement)e);
    }

    public Smethod asMethod(){
        return new Smethod((ExecutableElement)e);
    }

    public boolean isVar(){
        return e instanceof VariableElement;
    }

    public boolean isType(){
        return e instanceof TypeElement;
    }

    public boolean isMethod(){
        return e instanceof ExecutableElement;
    }

    public Array<? extends AnnotationMirror> annotations(){
        return Array.with(e.getAnnotationMirrors());
    }

    public <A extends Annotation> A annotation(Class<A> annotation){
        return e.getAnnotation(annotation);
    }

    public <A extends Annotation> boolean has(Class<A> annotation){
        return e.getAnnotation(annotation) != null;
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
        return o != null && o.getClass() == getClass() && e == ((Selement)o).e;
    }
}
