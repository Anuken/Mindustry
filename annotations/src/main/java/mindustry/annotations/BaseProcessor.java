package mindustry.annotations;

import arc.struct.*;
import arc.util.*;
import com.squareup.javapoet.*;
import com.sun.source.util.*;
import mindustry.annotations.util.*;

import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.lang.model.util.*;
import javax.tools.Diagnostic.*;
import javax.tools.*;
import java.io.*;
import java.lang.annotation.*;
import java.util.*;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
public abstract class BaseProcessor extends AbstractProcessor{
    /** Name of the base package to put all the generated classes. */
    public static final String packageName = "mindustry.gen";

    public static Types typeu;
    public static Elements elementu;
    public static Filer filer;
    public static Messager messager;
    public static Trees trees;

    protected int round;
    protected int rounds = 1;
    protected RoundEnvironment env;

    public static String getMethodName(Element element){
        return ((TypeElement)element.getEnclosingElement()).getQualifiedName().toString() + "." + element.getSimpleName();
    }

    public static boolean isPrimitive(String type){
        return type.equals("boolean") || type.equals("byte") || type.equals("short") || type.equals("int")
        || type.equals("long") || type.equals("float") || type.equals("double") || type.equals("char");
    }

    public static void write(TypeSpec.Builder builder) throws Exception{
        write(builder, null);
    }

    public static void write(TypeSpec.Builder builder, Array<String> imports) throws Exception{
        JavaFile file = JavaFile.builder(packageName, builder.build()).skipJavaLangImports(true).build();

        if(imports != null){
            String rawSource = file.toString();
            Array<String> result = new Array<>();
            for (String s : rawSource.split("\n", -1)) {
                result.add(s);
                if (s.startsWith("package ")) {
                    result.add("");
                    for (String i : imports) {
                        result.add(i);
                    }
                }
            }

            String out = result.toString("\n");
            JavaFileObject object = filer.createSourceFile(file.packageName + "." + file.typeSpec.name, file.typeSpec.originatingElements.toArray(new Element[0]));
            OutputStream stream = object.openOutputStream();
            stream.write(out.getBytes());
            stream.close();
        }else{
            file.writeTo(filer);
        }
    }

    public Array<Stype> types(Class<? extends Annotation> type){
        return Array.with(env.getElementsAnnotatedWith(type)).select(e -> e instanceof TypeElement)
            .map(e -> new Stype((TypeElement)e));
    }

    public Array<Svar> fields(Class<? extends Annotation> type){
        return Array.with(env.getElementsAnnotatedWith(type)).select(e -> e instanceof VariableElement)
        .map(e -> new Svar((VariableElement)e));
    }

    public Array<Smethod> methods(Class<? extends Annotation> type){
        return Array.with(env.getElementsAnnotatedWith(type)).select(e -> e instanceof ExecutableElement)
        .map(e -> new Smethod((ExecutableElement)e));
    }

    public void err(String message){
        messager.printMessage(Kind.ERROR, message);
        Log.err("[CODEGEN ERROR] " +message);
    }

    public void err(String message, Element elem){
        messager.printMessage(Kind.ERROR, message, elem);
        Log.err("[CODEGEN ERROR] " + message + ": " + elem);
    }

    public void err(String message, Selement elem){
        err(message, elem.e);
    }

    @Override
    public synchronized void init(ProcessingEnvironment env){
        super.init(env);

        trees = Trees.instance(env);
        typeu = env.getTypeUtils();
        elementu = env.getElementUtils();
        filer = env.getFiler();
        messager = env.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv){
        if(round++ >= rounds) return false; //only process 1 round
        this.env = roundEnv;
        try{
            process(roundEnv);
        }catch(Exception e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return true;
    }

    @Override
    public SourceVersion getSupportedSourceVersion(){
        return SourceVersion.RELEASE_8;
    }

    public void process(RoundEnvironment env) throws Exception{

    }
}
