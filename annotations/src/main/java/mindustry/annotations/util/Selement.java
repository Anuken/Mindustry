package mindustry.annotations.util;

import arc.struct.*;
import arc.util.ArcAnnotate.*;
import com.squareup.javapoet.*;
import com.sun.tools.javac.code.Attribute.*;
import mindustry.annotations.*;

import javax.lang.model.element.*;
import javax.lang.model.type.*;
import java.lang.Class;
import java.lang.annotation.*;
import java.lang.reflect.*;

public class Selement<T extends Element>{
    public final T e;

    public Selement(T e){
        this.e = e;
    }

    public @Nullable String doc(){
        return BaseProcessor.elementu.getDocComment(e);
    }

    public Seq<Selement<?>> enclosed(){
        return Seq.with(e.getEnclosedElements()).map(Selement::new);
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

    public Seq<? extends AnnotationMirror> annotations(){
        return Seq.with(e.getAnnotationMirrors());
    }

    public <A extends Annotation> A annotation(Class<A> annotation){
        try{
            Method m = com.sun.tools.javac.code.AnnoConstruct.class.getDeclaredMethod("getAttribute", Class.class);
            m.setAccessible(true);
            Compound compound = (Compound)m.invoke(e, annotation);
            return compound == null ? null : AnnotationProxyMaker.generateAnnotation(compound, annotation);
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }

    public <A extends Annotation> boolean has(Class<A> annotation){
        return annotation(annotation) != null;
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
        return o != null && o.getClass() == getClass() && e.equals(((Selement)o).e);
    }
}
