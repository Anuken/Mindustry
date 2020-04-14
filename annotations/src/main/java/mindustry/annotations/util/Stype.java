package mindustry.annotations.util;

import arc.struct.*;
import mindustry.annotations.*;

import javax.lang.model.element.*;
import javax.lang.model.type.*;
import java.lang.annotation.*;

public class Stype extends Selement<TypeElement>{

    public Stype(TypeElement typeElement){
        super(typeElement);
    }

    public static Stype of(TypeMirror mirror){
        return new Stype((TypeElement)BaseProcessor.typeu.asElement(mirror));
    }

    public Array<Stype> interfaces(){
        return Array.with(e.getInterfaces()).map(Stype::of);
    }

    public Array<Stype> superclasses(){
        Array<Stype> out = new Array<>();
        Stype sup = superclass();
        while(!sup.name().equals("Object")){
            out.add(sup);
            sup = sup.superclass();
        }
        return out;
    }

    public Stype superclass(){
        return new Stype((TypeElement)BaseProcessor.typeu.asElement(BaseProcessor.typeu.directSupertypes(mirror()).get(0)));
    }

    public <A extends Annotation> A annotation(Class<A> annotation){
        return e.getAnnotation(annotation);
    }

    public Array<Svar> fields(){
        return Array.with(e.getEnclosedElements()).select(e -> e instanceof VariableElement).map(e -> new Svar((VariableElement)e));
    }

    public Array<Smethod> methods(){
        return Array.with(e.getEnclosedElements()).select(e -> e instanceof ExecutableElement
        && !e.getSimpleName().toString().contains("<")).map(e -> new Smethod((ExecutableElement)e));
    }

    public Array<Smethod> constructors(){
        return Array.with(e.getEnclosedElements()).select(e -> e instanceof ExecutableElement
        && e.getSimpleName().toString().contains("<")).map(e -> new Smethod((ExecutableElement)e));
    }

    @Override
    public TypeMirror mirror(){
        return e.asType();
    }
}
