package mindustry.annotations;

import arc.files.*;
import arc.struct.*;
import arc.util.Log;
import arc.util.Log.*;
import arc.util.*;
import com.squareup.javapoet.*;
import com.sun.source.util.*;
import com.sun.tools.javac.model.*;
import com.sun.tools.javac.processing.*;
import com.sun.tools.javac.tree.*;
import com.sun.tools.javac.util.*;
import mindustry.annotations.util.*;

import javax.annotation.processing.*;
import javax.lang.model.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.*;
import javax.tools.Diagnostic.*;
import javax.tools.*;
import java.io.*;
import java.lang.annotation.*;
import java.util.List;
import java.util.*;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
public abstract class BaseProcessor extends AbstractProcessor{
    /** Name of the base package to put all the generated classes. */
    public static final String packageName = "mindustry.gen";

    public static Types typeu;
    public static JavacElements elementu;
    public static Filer filer;
    public static Messager messager;
    public static Trees trees;
    public static TreeMaker maker;

    protected int round;
    protected int rounds = 1;
    protected RoundEnvironment env;
    protected Fi rootDirectory;

    protected Context context;

    public static String getMethodName(Element element){
        return ((TypeElement)element.getEnclosingElement()).getQualifiedName().toString() + "." + element.getSimpleName();
    }

    public static boolean isPrimitive(String type){
        return type.equals("boolean") || type.equals("byte") || type.equals("short") || type.equals("int")
        || type.equals("long") || type.equals("float") || type.equals("double") || type.equals("char");
    }

    public static boolean instanceOf(String type, String other){
        TypeElement a = elementu.getTypeElement(type);
        TypeElement b = elementu.getTypeElement(other);
        return a != null && b != null && typeu.isSubtype(a.asType(), b.asType());
    }

    public static String getDefault(String value){
        switch(value){
            case "float":
            case "double":
            case "int":
            case "long":
            case "short":
            case "char":
            case "byte":
                return "0";
            case "boolean":
                return "false";
            default:
                return "null";
        }
    }

    //in bytes
    public static int typeSize(String kind){
        switch(kind){
            case "boolean":
            case "byte":
                return 1;
            case "short":
                return 2;
            case "float":
            case "char":
            case "int":
                return 4;
            case "long":
                return 8;
            default:
                throw new IllegalArgumentException("Invalid primitive type: " + kind + "");
        }
    }

    public static String simpleName(String str){
        return str.contains(".") ? str.substring(str.lastIndexOf('.') + 1) : str;
    }

    public static TypeName tname(String pack, String simple){
        return ClassName.get(pack, simple );
    }

    public static TypeName tname(String name){
        if(!name.contains(".")) return ClassName.get(packageName, name);

        String pack = name.substring(0, name.lastIndexOf("."));
        String simple = name.substring(name.lastIndexOf(".") + 1);
        return ClassName.get(pack, simple);
    }

    public static TypeName tname(Class<?> c){
        return ClassName.get(c).box();
    }

    public static TypeVariableName getTVN(TypeParameterElement element) {
        String name = element.getSimpleName().toString();
        List<? extends TypeMirror> boundsMirrors = element.getBounds();

        List<TypeName> boundsTypeNames = new ArrayList<>();
        for (TypeMirror typeMirror : boundsMirrors) {
            boundsTypeNames.add(TypeName.get(typeMirror));
        }

        return TypeVariableName.get(name, boundsTypeNames.toArray(new TypeName[0]));
    }

    public static void write(TypeSpec.Builder builder) throws Exception{
        write(builder, null);
    }

    public static void write(TypeSpec.Builder builder, Seq<String> imports) throws Exception{
        JavaFile file = JavaFile.builder(packageName, builder.build()).skipJavaLangImports(true).build();

        if(imports != null){
            String rawSource = file.toString();
            Seq<String> result = new Seq<>();
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

    public Seq<Selement> elements(Class<? extends Annotation> type){
        return Seq.with(env.getElementsAnnotatedWith(type)).map(Selement::new);
    }

    public Seq<Stype> types(Class<? extends Annotation> type){
        return Seq.with(env.getElementsAnnotatedWith(type)).select(e -> e instanceof TypeElement)
            .map(e -> new Stype((TypeElement)e));
    }

    public Seq<Svar> fields(Class<? extends Annotation> type){
        return Seq.with(env.getElementsAnnotatedWith(type)).select(e -> e instanceof VariableElement)
        .map(e -> new Svar((VariableElement)e));
    }

    public Seq<Smethod> methods(Class<? extends Annotation> type){
        return Seq.with(env.getElementsAnnotatedWith(type)).select(e -> e instanceof ExecutableElement)
        .map(e -> new Smethod((ExecutableElement)e));
    }

    public static void err(String message){
        messager.printMessage(Kind.ERROR, message);
        Log.err("[CODEGEN ERROR] " +message);
    }

    public static void err(String message, Element elem){
        messager.printMessage(Kind.ERROR, message, elem);
        Log.err("[CODEGEN ERROR] " + message + ": " + elem);
    }

    public void err(String message, Selement elem){
        err(message, elem.e);
    }

    @Override
    public synchronized void init(ProcessingEnvironment env){
        super.init(env);

        JavacProcessingEnvironment javacProcessingEnv = (JavacProcessingEnvironment)env;

        trees = Trees.instance(env);
        typeu = env.getTypeUtils();
        elementu = javacProcessingEnv.getElementUtils();
        filer = env.getFiler();
        messager = env.getMessager();
        context = ((JavacProcessingEnvironment)env).getContext();
        maker = TreeMaker.instance(javacProcessingEnv.getContext());

        Log.setLogLevel(LogLevel.info);

        if(System.getProperty("debug") != null){
            Log.setLogLevel(LogLevel.debug);
        }
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv){
        if(round++ >= rounds) return false; //only process 1 round
        if(rootDirectory == null){
            try{
                String path = Fi.get(filer.getResource(StandardLocation.CLASS_OUTPUT, "no", "no")
                .toUri().toURL().toString().substring(OS.isWindows ? 6 : "file:".length()))
                .parent().parent().parent().parent().parent().parent().parent().toString().replace("%20", " ");
                rootDirectory = Fi.get(path);
            }catch(IOException e){
                throw new RuntimeException(e);
            }
        }

        this.env = roundEnv;
        try{
            process(roundEnv);
        }catch(Throwable e){
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
