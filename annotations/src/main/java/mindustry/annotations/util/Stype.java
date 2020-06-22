package mindustry.annotations.util;

import arc.struct.*;
import mindustry.annotations.*;

import javax.lang.model.element.*;
import javax.lang.model.type.*;

public class Stype extends Selement<TypeElement>{

    public Stype(TypeElement typeElement){
        super(typeElement);
    }

    public static Stype of(TypeMirror mirror){
        return new Stype((TypeElement)BaseProcessor.typeu.asElement(mirror));
    }

    public String fullName(){
        return mirror().toString();
    }

    public Seq<Stype> interfaces(){
        return Seq.with(e.getInterfaces()).map(Stype::of);
    }

    public Seq<Stype> allInterfaces(){
        return interfaces().flatMap(s -> s.allInterfaces().and(s)).distinct();
    }

    public Seq<Stype> superclasses(){
        return Seq.with(BaseProcessor.typeu.directSupertypes(mirror())).map(Stype::of);
    }

    public Seq<Stype> allSuperclasses(){
        return superclasses().flatMap(s -> s.allSuperclasses().and(s)).distinct();
    }

    public Stype superclass(){
        return new Stype((TypeElement)BaseProcessor.typeu.asElement(BaseProcessor.typeu.directSupertypes(mirror()).get(0)));
    }

    public Seq<Svar> fields(){
        return Seq.with(e.getEnclosedElements()).select(e -> e instanceof VariableElement).map(e -> new Svar((VariableElement)e));
    }

    public Seq<Smethod> methods(){
        return Seq.with(e.getEnclosedElements()).select(e -> e instanceof ExecutableElement
        && !e.getSimpleName().toString().contains("<")).map(e -> new Smethod((ExecutableElement)e));
    }

    public Seq<Smethod> constructors(){
        return Seq.with(e.getEnclosedElements()).select(e -> e instanceof ExecutableElement
        && e.getSimpleName().toString().contains("<")).map(e -> new Smethod((ExecutableElement)e));
    }

    @Override
    public TypeMirror mirror(){
        return e.asType();
    }
}
