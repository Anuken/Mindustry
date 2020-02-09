package mindustry.annotations.util;

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
import com.sun.tools.javac.util.*;
import sun.reflect.annotation.*;

import javax.lang.model.type.*;
import java.io.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.Map.*;
import java.lang.Class;

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
        LinkedHashMap var1 = new LinkedHashMap();
        ClassSymbol var2 = (ClassSymbol)this.anno.type.tsym;

        for(com.sun.tools.javac.code.Scope.Entry var3 = var2.members().elems; var3 != null; var3 = var3.sibling){
            if(var3.sym.kind == 16){
                MethodSymbol var4 = (MethodSymbol)var3.sym;
                Attribute var5 = var4.getDefaultValue();
                if(var5 != null){
                    var1.put(var4, var5);
                }
            }
        }

        Iterator var6 = this.anno.values.iterator();

        while(var6.hasNext()){
            Pair var7 = (Pair)var6.next();
            var1.put(var7.fst, var7.snd);
        }

        return var1;
    }

    private Object generateValue(MethodSymbol var1, Attribute var2){
        AnnotationProxyMaker.ValueVisitor var3 = new AnnotationProxyMaker.ValueVisitor(var1);
        return var3.getValue(var2);
    }

    private static final class MirroredTypesExceptionProxy extends ExceptionProxy{
        static final long serialVersionUID = 269L;
        private transient List<TypeMirror> types;
        private final String typeStrings;

        MirroredTypesExceptionProxy(List<TypeMirror> var1){
            this.types = var1;
            this.typeStrings = var1.toString();
        }

        public String toString(){
            return this.typeStrings;
        }

        public int hashCode(){
            return (this.types != null ? this.types : this.typeStrings).hashCode();
        }

        public boolean equals(Object var1){
            return this.types != null && var1 instanceof AnnotationProxyMaker.MirroredTypesExceptionProxy && this.types.equals(((AnnotationProxyMaker.MirroredTypesExceptionProxy)var1).types);
        }

        protected RuntimeException generateException(){
            return new MirroredTypesException(this.types);
        }

        private void readObject(ObjectInputStream var1) throws IOException, ClassNotFoundException{
            var1.defaultReadObject();
            this.types = null;
        }
    }

    private static final class MirroredTypeExceptionProxy extends ExceptionProxy{
        static final long serialVersionUID = 269L;
        private transient TypeMirror type;
        private final String typeString;

        MirroredTypeExceptionProxy(TypeMirror var1){
            this.type = var1;
            this.typeString = var1.toString();
        }

        public String toString(){
            return this.typeString;
        }

        public int hashCode(){
            return (this.type != null ? this.type : this.typeString).hashCode();
        }

        public boolean equals(Object var1){
            return this.type != null && var1 instanceof AnnotationProxyMaker.MirroredTypeExceptionProxy && this.type.equals(((AnnotationProxyMaker.MirroredTypeExceptionProxy)var1).type);
        }

        protected RuntimeException generateException(){
            return new MirroredTypeException(this.type);
        }

        private void readObject(ObjectInputStream var1) throws IOException, ClassNotFoundException{
            var1.defaultReadObject();
            this.type = null;
        }
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
            this.value = new AnnotationProxyMaker.MirroredTypeExceptionProxy(var1.classType);
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

                this.value = new AnnotationProxyMaker.MirroredTypesExceptionProxy(var14.toList());
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
                    this.value = new EnumConstantNotPresentExceptionProxy((Class)this.returnClass, var2);
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
                this.value = new AnnotationProxyMaker.MirroredTypeExceptionProxy(((UnresolvedClass)var1).classType);
            }else{
                this.value = null;
            }

        }

        private void typeMismatch(Method var1, final Attribute var2){
            class AnnotationTypeMismatchExceptionProxy extends ExceptionProxy{
                static final long serialVersionUID = 269L;
                final transient Method method;

                AnnotationTypeMismatchExceptionProxy(Method var2x){
                    this.method = var2x;
                }

                public String toString(){
                    return "<error>";
                }

                protected RuntimeException generateException(){
                    return new AnnotationTypeMismatchException(this.method, var2.type.toString());
                }
            }

            this.value = new AnnotationTypeMismatchExceptionProxy(var1);
        }
    }
}
