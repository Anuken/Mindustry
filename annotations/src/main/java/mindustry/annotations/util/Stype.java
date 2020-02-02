package mindustry.annotations.util;

import arc.struct.*;

import javax.lang.model.element.*;
import javax.lang.model.type.*;
import java.lang.annotation.*;

public class Stype extends Selement<TypeElement>{

    public Stype(TypeElement typeElement){
        super(typeElement);
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
