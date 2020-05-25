package mindustry.annotations.util;

import arc.struct.*;
import com.squareup.javapoet.*;
import com.sun.source.tree.*;
import mindustry.annotations.*;

import javax.lang.model.element.*;
import javax.lang.model.type.*;

public class Smethod extends Selement<ExecutableElement>{

    public Smethod(ExecutableElement executableElement){
        super(executableElement);
    }

    public boolean isAny(Modifier... mod){
        for(Modifier m : mod){
            if(is(m)) return true;
        }
        return false;
    }

    public boolean is(Modifier mod){
        return e.getModifiers().contains(mod);
    }

    public Stype type(){
        return new Stype((TypeElement)up());
    }

    public Array<TypeMirror> thrown(){
        return Array.with(e.getThrownTypes()).as();
    }

    public Array<TypeName> thrownt(){
        return Array.with(e.getThrownTypes()).map(TypeName::get);
    }

    public Array<TypeParameterElement> typeVariables(){
        return Array.with(e.getTypeParameters()).as();
    }

    public Array<Svar> params(){
        return Array.with(e.getParameters()).map(Svar::new);
    }

    public boolean isVoid(){
        return ret().toString().equals("void");
    }

    public TypeMirror ret(){
        return e.getReturnType();
    }

    public TypeName retn(){
        return TypeName.get(ret());
    }

    public MethodTree tree(){
        return BaseProcessor.trees.getTree(e);
    }

    public String simpleString(){
        return name() + "(" + params().toString(", ", p -> BaseProcessor.simpleName(p.mirror().toString())) + ")";
    }
}
