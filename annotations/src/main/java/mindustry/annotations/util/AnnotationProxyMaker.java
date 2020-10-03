package mindustry.annotations.util;

import arc.func.*;
import com.sun.tools.javac.code.*;
import com.sun.tools.javac.code.Attribute.Array;
import com.sun.tools.javac.code.Attribute.Enum;
import com.sun.tools.javac.code.Attribute.Error;
import com.sun.tools.javac.code.Attribute.Visitor;
import com.sun.tools.javac.code.Attribute.*;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Type.ArrayType;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.*;
import sun.reflect.annotation.*;

import javax.lang.model.element.*;
import javax.lang.model.type.*;
import java.lang.Class;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.Map.*;

//replaces the standard Java AnnotationProxyMaker with one that doesn't crash
//thanks, oracle.
@SuppressWarnings({"sunapi", "unchecked"})
public class AnnotationProxyMaker{
    private final Compound anno;
    private final Class<? extends Annotation> annoType;

    private AnnotationProxyMaker(Compound var1, Class<? extends Annotation> var2){
        this.anno = var1;
        this.annoType = var2;
    }

    public static <A extends Annotation> A generateAnnotation(Compound var0, Class<A> var1){
        AnnotationProxyMaker var2 = new AnnotationProxyMaker(var0, var1);
        return (A)var1.cast(var2.generateAnnotation());
    }

    private Annotation generateAnnotation(){
        return AnnotationParser.annotationForMap(this.annoType, this.getAllReflectedValues());
    }

    private Map<String, Object> getAllReflectedValues(){
        LinkedHashMap var1 = new LinkedHashMap();
        Iterator var2 = this.getAllValues().entrySet().iterator();

        while(var2.hasNext()){
            Entry var3 = (Entry)var2.next();
            MethodSymbol var4 = (MethodSymbol)var3.getKey();
            Object var5 = this.generateValue(var4, (Attribute)var3.getValue());
            if(var5 != null){
                var1.put(var4.name.toString(), var5);
            }
        }

        return var1;
    }

    private Map<MethodSymbol, Attribute> getAllValues(){
        LinkedHashMap map = new LinkedHashMap();
        ClassSymbol cl = (ClassSymbol)this.anno.type.tsym;

        //try to use Java 8 API for this if possible
        try{
            Class entryClass = Class.forName("com.sun.tools.javac.code.Scope$Entry");
            Object members = cl.members();
            Field field = members.getClass().getField("elems");
            Object elems = field.get(members);
            Field siblingField = entryClass.getField("sibling");
            Field symField = entryClass.getField("sym");
            for(Object currEntry = elems; currEntry != null; currEntry = siblingField.get(currEntry)){
                handleSymbol((Symbol)symField.get(currEntry), map);
            }

        }catch(Throwable e){
            //otherwise try other API

            try{
                Class lookupClass = Class.forName("com.sun.tools.javac.code.Scope$LookupKind");
                Field nonRecField = lookupClass.getField("NON_RECURSIVE");
                Object nonRec = nonRecField.get(null);
                Scope scope = cl.members();
                Method getSyms = scope.getClass().getMethod("getSymbols", lookupClass);
                Iterable<Symbol> it = (Iterable<Symbol>)getSyms.invoke(scope, nonRec);
                Iterator<Symbol> i = it.iterator();
                while(i.hasNext()){
                    handleSymbol(i.next(), map);
                }

            }catch(Throwable death){
                //I tried
                throw new RuntimeException(death);
            }
        }

        for(Pair var7 : this.anno.values){
            map.put(var7.fst, var7.snd);
        }

        return map;
    }

    private void handleSymbol(Symbol sym, LinkedHashMap map){

        if(sym.getKind() == ElementKind.METHOD){
            MethodSymbol var4 = (MethodSymbol)sym;
            Attribute var5 = var4.getDefaultValue();
            if(var5 != null){
                map.put(var4, var5);
            }
        }
    }

    private Object generateValue(MethodSymbol var1, Attribute var2){
        AnnotationProxyMaker.ValueVisitor var3 = new AnnotationProxyMaker.ValueVisitor(var1);
        return var3.getValue(var2);
    }

    private class ValueVisitor implements Visitor{
        private MethodSymbol meth;
        private Class<?> returnClass;
        private Object value;

        ValueVisitor(MethodSymbol var2){
            this.meth = var2;
        }

        Object getValue(Attribute var1){
            Method var2;
            try{
                var2 = AnnotationProxyMaker.this.annoType.getMethod(this.meth.name.toString());
            }catch(NoSuchMethodException var4){
                return null;
            }

            this.returnClass = var2.getReturnType();
            var1.accept(this);
            if(!(this.value instanceof ExceptionProxy) && !AnnotationType.invocationHandlerReturnType(this.returnClass).isInstance(this.value)){
                this.typeMismatch(var2, var1);
            }

            return this.value;
        }

        public void visitConstant(Constant var1){
            this.value = var1.getValue();
        }

        public void visitClass(com.sun.tools.javac.code.Attribute.Class var1){
            this.value = mirrorProxy(var1.classType);
        }

        public void visitArray(Array var1){
            Name var2 = ((ArrayType)var1.type).elemtype.tsym.getQualifiedName();
            int var6;
            if(var2.equals(var2.table.names.java_lang_Class)){
                ListBuffer var14 = new ListBuffer();
                Attribute[] var15 = var1.values;
                int var16 = var15.length;

                for(var6 = 0; var6 < var16; ++var6){
                    Attribute var7 = var15[var6];
                    Type var8 = var7 instanceof UnresolvedClass ? ((UnresolvedClass)var7).classType : ((com.sun.tools.javac.code.Attribute.Class)var7).classType;
                    var14.append(var8);
                }

                this.value = mirrorProxy(var14.toList());
            }else{
                int var3 = var1.values.length;
                Class var4 = this.returnClass;
                this.returnClass = this.returnClass.getComponentType();

                try{
                    Object var5 = java.lang.reflect.Array.newInstance(this.returnClass, var3);

                    for(var6 = 0; var6 < var3; ++var6){
                        var1.values[var6].accept(this);
                        if(this.value == null || this.value instanceof ExceptionProxy){
                            return;
                        }

                        try{
                            java.lang.reflect.Array.set(var5, var6, this.value);
                        }catch(IllegalArgumentException var12){
                            this.value = null;
                            return;
                        }
                    }

                    this.value = var5;
                }finally{
                    this.returnClass = var4;
                }
            }
        }

        public void visitEnum(Enum var1){
            if(this.returnClass.isEnum()){
                String var2 = var1.value.toString();

                try{
                    this.value = java.lang.Enum.valueOf((Class)this.returnClass, var2);
                }catch(IllegalArgumentException var4){
                    this.value = proxify(() -> new EnumConstantNotPresentException((Class)this.returnClass, var2));
                }
            }else{
                this.value = null;
            }

        }

        public void visitCompound(Compound var1){
            try{
                Class var2 = this.returnClass.asSubclass(Annotation.class);
                this.value = AnnotationProxyMaker.generateAnnotation(var1, var2);
            }catch(ClassCastException var3){
                this.value = null;
            }

        }

        public void visitError(Error var1){
            if(var1 instanceof UnresolvedClass){
                this.value = mirrorProxy(((UnresolvedClass)var1).classType);
            }else{
                this.value = null;
            }

        }

        private void typeMismatch(Method var1, final Attribute var2){
            this.value = proxify(() -> new AnnotationTypeMismatchException(var1, var2.type.toString()));
        }
    }

    private static Object mirrorProxy(Type t){
        return proxify(() -> new MirroredTypeException(t));
    }

    private static Object mirrorProxy(List<Type> t){
        return proxify(() -> new MirroredTypesException(t));
    }

    private static <T extends Throwable> Object proxify(Prov<T> prov){
        try{

            return new ExceptionProxy(){
                @Override
                protected RuntimeException generateException(){
                    return (RuntimeException)prov.get();
                }
            };
        }catch(Throwable t){
            throw new RuntimeException(t);
        }

    }
}
