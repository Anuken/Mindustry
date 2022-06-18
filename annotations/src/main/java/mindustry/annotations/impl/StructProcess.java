package mindustry.annotations.impl;

import arc.struct.*;
import arc.util.*;
import com.squareup.javapoet.*;
import mindustry.annotations.Annotations.*;
import mindustry.annotations.*;
import mindustry.annotations.util.*;

import javax.annotation.processing.*;
import javax.lang.model.element.*;
import javax.lang.model.type.*;

/**
 * Generates ""value types"" classes that are packed into integer primitives of the most aproppriate size.
 * It would be nice if Java didn't make crazy hacks like this necessary.
 */
@SupportedAnnotationTypes({
"mindustry.annotations.Annotations.Struct"
})
public class StructProcess extends BaseProcessor{

    @Override
    public void process(RoundEnvironment env) throws Exception{
        Seq<Stype> elements = types(Struct.class);

        for(Stype elem : elements){

            if(!elem.name().endsWith("Struct")){
                err("All classes annotated with @Struct must have their class names end in 'Struct'.", elem);
                continue;
            }

            String structName = elem.name().substring(0, elem.name().length() - "Struct".length());
            String structParam = structName.toLowerCase();

            TypeSpec.Builder classBuilder = TypeSpec.classBuilder(structName)
            .addModifiers(Modifier.FINAL, Modifier.PUBLIC);

            try{
                Seq<Svar> variables = elem.fields();
                int structSize = variables.mapInt(StructProcess::varSize).sum();
                int structTotalSize = (structSize <= 8 ? 8 : structSize <= 16 ? 16 : structSize <= 32 ? 32 : 64);

                if(variables.size == 0){
                    err("making a struct with no fields is utterly pointles.", elem);
                    continue;
                }

                //obtain type which will be stored
                Class<?> structType = typeForSize(structSize);

                //[constructor] get(fields...) : structType
                MethodSpec.Builder constructor = MethodSpec.methodBuilder("get")
                .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                .returns(structType);

                StringBuilder cons = new StringBuilder();
                StringBuilder doc = new StringBuilder();
                doc.append("Bits used: ").append(structSize).append(" / ").append(structTotalSize).append("\n");

                int offset = 0;
                for(Svar var : variables){
                    int size = varSize(var);
                    TypeName varType = var.tname();
                    String varName = var.name();
                    boolean isBool = varType == TypeName.BOOLEAN;

                    //add val param to constructor
                    constructor.addParameter(varType, varName);

                    //[get] field(structType) : fieldType
                    MethodSpec.Builder getter = MethodSpec.methodBuilder(var.name())
                    .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                    .returns(varType)
                    .addParameter(structType, structParam);
                    //[set] field(structType, fieldType) : structType
                    MethodSpec.Builder setter = MethodSpec.methodBuilder(var.name())
                    .addModifiers(Modifier.STATIC, Modifier.PUBLIC)
                    .returns(structType)
                    .addParameter(structType, structParam).addParameter(varType, "value");

                    //field for offset
                    classBuilder.addField(FieldSpec.builder(structType, "bitMask" + Strings.capitalize(varName), Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                        .initializer(!isBool ? "($T)($L)" : "($T)(1L << $L)", structType, isBool ? offset : bitString(offset, size, structTotalSize)).build());

                    //[getter]
                    if(isBool){
                        //bools: single bit, is simplified
                        getter.addStatement("return ($L & (1L << $L)) != 0", structParam, offset);
                    }else if(varType == TypeName.FLOAT){
                        //floats: need conversion
                        getter.addStatement("return Float.intBitsToFloat((int)(($L >>> $L) & $L))", structParam, offset, bitString(size, structTotalSize));
                    }else{
                        //bytes, shorts, chars, ints
                        getter.addStatement("return ($T)(($L >>> $L) & $L)", varType, structParam, offset, bitString(size, structTotalSize));
                    }

                    //[setter] + [constructor building]
                    if(varType == TypeName.BOOLEAN){
                        cons.append(" | (").append(varName).append(" ? ").append("1L << ").append(offset).append("L : 0)");

                        //bools: single bit, needs special case to clear things
                        setter.beginControlFlow("if(value)");
                        setter.addStatement("return ($T)(($L & ~(1L << $LL)))", structType, structParam, offset);
                        setter.nextControlFlow("else");
                        setter.addStatement("return ($T)(($L & ~(1L << $LL)) | (1L << $LL))", structType, structParam, offset, offset);
                        setter.endControlFlow();
                    }else if(varType == TypeName.FLOAT){
                        cons.append(" | (").append("(").append(structType).append(")").append("Float.floatToIntBits(").append(varName).append(") << ").append(offset).append("L)");

                        //floats: need conversion
                        setter.addStatement("return ($T)(($L & (~$L)) | (($T)Float.floatToIntBits(value) << $LL))", structType, structParam, bitString(offset, size, structTotalSize), structType, offset);
                    }else{
                        cons.append(" | (((").append(structType).append(")").append(varName).append(" << ").append(offset).append("L)").append(" & ").append(bitString(offset, size, structTotalSize)).append(")");

                        //bytes, shorts, chars, ints
                        setter.addStatement("return ($T)(($L & (~$L)) | (($T)value << $LL))", structType, structParam, bitString(offset, size, structTotalSize), structType, offset);
                    }

                    doc.append("<br>  ").append(varName).append(" [").append(offset).append("..").append(size + offset).append("]\n");

                    //add finished methods
                    classBuilder.addMethod(getter.build());
                    classBuilder.addMethod(setter.build());

                    offset += size;
                }

                classBuilder.addJavadoc(doc.toString());

                //add constructor final statement + add to class and build
                constructor.addStatement("return ($T)($L)", structType, cons.substring(3));
                classBuilder.addMethod(constructor.build());

                JavaFile.builder(packageName, classBuilder.build()).build().writeTo(BaseProcessor.filer);
            }catch(IllegalArgumentException e){
                e.printStackTrace();
                err(e.getMessage(), elem);
            }
        }

    }

    static String bitString(int offset, int size, int totalSize){
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < offset; i++) builder.append('0');
        for(int i = 0; i < size; i++) builder.append('1');
        for(int i = 0; i < totalSize - size - offset; i++) builder.append('0');
        return "0b" + builder.reverse().toString() + "L";
    }

    static String bitString(int size, int totalSize){
        StringBuilder builder = new StringBuilder();
        for(int i = 0; i < size; i++) builder.append('1');
        for(int i = 0; i < totalSize - size; i++) builder.append('0');
        return "0b" + builder.reverse().toString() + "L";
    }

    static int varSize(Svar var) throws IllegalArgumentException{
        if(!var.mirror().getKind().isPrimitive()){
            throw new IllegalArgumentException("All struct fields must be primitives: " + var);
        }

        StructField an = var.annotation(StructField.class);
        if(var.mirror().getKind() == TypeKind.BOOLEAN && an != null && an.value() != 1){
            throw new IllegalArgumentException("Booleans can only be one bit long... why would you do this?");
        }

        if(var.mirror().getKind() == TypeKind.FLOAT && an != null && an.value() != 32){
            throw new IllegalArgumentException("Float size can't be changed. Very sad.");
        }

        return an == null ? typeSize(var.mirror().getKind()) : an.value();
    }

    static Class<?> typeForSize(int size) throws IllegalArgumentException{
        if(size <= 8){
            return byte.class;
        }else if(size <= 16){
            return short.class;
        }else if(size <= 32){
            return int.class;
        }else if(size <= 64){
            return long.class;
        }
        throw new IllegalArgumentException("Too many fields, must fit in 64 bits. Curent size: " + size);
    }

    /** returns a type's element size in bits. */
    static int typeSize(TypeKind kind) throws IllegalArgumentException{
        switch(kind){
            case BOOLEAN:
                return 1;
            case BYTE:
                return 8;
            case SHORT:
                return 16;
            case FLOAT:
            case CHAR:
            case INT:
                return 32;
            default:
                throw new IllegalArgumentException("Invalid type kind: " + kind + ". Note that doubles and longs are not supported.");
        }
    }
}
