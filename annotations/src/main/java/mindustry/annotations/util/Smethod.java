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

    public String descString(){
        return up().asType().toString() + "#" + super.toString().replace("mindustry.gen.", "");
    }

    public boolean is(Modifier mod){
        return e.getModifiers().contains(mod);
    }

    public Stype type(){
        return new Stype((TypeElement)up());
    }

    public Seq<TypeMirror> thrown(){
        return Seq.with(e.getThrownTypes()).as();
    }

    public Seq<TypeName> thrownt(){
        return Seq.with(e.getThrownTypes()).map(TypeName::get);
    }

    public Seq<TypeParameterElement> typeVariables(){
        return Seq.with(e.getTypeParameters()).as();
    }

    public Seq<Svar> params(){
        return Seq.with(e.getParameters()).map(Svar::new);
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
