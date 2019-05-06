package io.anuke.annotations;

import com.squareup.javapoet.*;
import io.anuke.annotations.Annotations.Serialize;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.util.ElementFilter;
import java.io.*;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes({
"io.anuke.annotations.Annotations.Serialize"
})
public class SerializeAnnotationProcessor extends AbstractProcessor{
    /** Target class name. */
    private static final String className = "Serialization";
    /** Name of the base package to put all the generated classes. */
    private static final String packageName = "io.anuke.mindustry.gen";

    private int round;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv){
        super.init(processingEnv);
        //put all relevant utils into utils class
        Utils.typeUtils = processingEnv.getTypeUtils();
        Utils.elementUtils = processingEnv.getElementUtils();
        Utils.filer = processingEnv.getFiler();
        Utils.messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv){
        if(round++ != 0) return false; //only process 1 round

        try{
            Set<TypeElement> elements = ElementFilter.typesIn(roundEnv.getElementsAnnotatedWith(Serialize.class));

            TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className).addModifiers(Modifier.PUBLIC);
            classBuilder.addAnnotation(AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "\"unchecked\"").build());
            classBuilder.addJavadoc(RemoteMethodAnnotationProcessor.autogenWarning);
            MethodSpec.Builder method = MethodSpec.methodBuilder("init").addModifiers(Modifier.PUBLIC, Modifier.STATIC);

            TypeName jsonType = ClassName.bestGuess("io.anuke.arc.util.serialization.Json");
            TypeName jsonValueType = ClassName.bestGuess("io.anuke.arc.util.serialization.JsonValue");

            classBuilder.addField(jsonType, "bjson", Modifier.STATIC, Modifier.PRIVATE);
            classBuilder.addStaticBlock(CodeBlock.builder()
            .addStatement("bjson = new " + jsonType + "()")
            .build());

            for(TypeElement elem : elements){
                TypeName type = TypeName.get(elem.asType());
                String simpleTypeName = type.toString().substring(type.toString().lastIndexOf('.') + 1);

                TypeSpec.Builder serializer = TypeSpec.anonymousClassBuilder("")
                .addSuperinterface(ParameterizedTypeName.get(
                ClassName.bestGuess("io.anuke.arc.Settings.TypeSerializer"), type));

                MethodSpec.Builder writeMethod = MethodSpec.methodBuilder("write")
                .returns(void.class)
                .addParameter(DataOutput.class, "stream")
                .addParameter(type, "object")
                .addException(IOException.class)
                .addModifiers(Modifier.PUBLIC);

                MethodSpec.Builder readMethod = MethodSpec.methodBuilder("read")
                .returns(type)
                .addParameter(DataInput.class, "stream")
                .addException(IOException.class)
                .addModifiers(Modifier.PUBLIC);

                MethodSpec.Builder jsonWriteMethod = MethodSpec.methodBuilder("write" + simpleTypeName + "Json")
                .returns(void.class)
                .addParameter(jsonType, "json")
                .addParameter(type, "object")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

                MethodSpec.Builder jsonReadMethod = MethodSpec.methodBuilder("read" + simpleTypeName + "Json")
                .returns(type)
                .addParameter(jsonValueType, "value")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

                readMethod.addStatement("$L object = new $L()", type, type);
                jsonReadMethod.addStatement("$L object = new $L()", type, type);

                List<VariableElement> fields = ElementFilter.fieldsIn(Utils.elementUtils.getAllMembers(elem));
                for(VariableElement field : fields){
                    if(field.getModifiers().contains(Modifier.STATIC) || field.getModifiers().contains(Modifier.TRANSIENT) || field.getModifiers().contains(Modifier.PRIVATE))
                        continue;

                    String name = field.getSimpleName().toString();
                    String typeName = Utils.typeUtils.erasure(field.asType()).toString().replace('$', '.');
                    String capName = Character.toUpperCase(typeName.charAt(0)) + typeName.substring(1);

                    if(field.asType().getKind().isPrimitive()){
                        writeMethod.addStatement("stream.write" + capName + "(object." + name + ")");
                        readMethod.addStatement("object." + name + "= stream.read" + capName + "()");

                        jsonWriteMethod.addStatement("json.writeValue(\"" + name + "\", object." + name + ")");
                        jsonReadMethod.addStatement("if(value.has(\"" + name + "\")) object." + name + "= value.get" + capName + "(\"" + name + "\")");
                    }else{
                        writeMethod.addStatement("io.anuke.arc.Core.settings.getSerializer(" + typeName + ".class).write(stream, object." + name + ")");
                        readMethod.addStatement("object." + name + " = (" + typeName + ")io.anuke.arc.Core.settings.getSerializer(" + typeName + ".class).read(stream)");
                    }
                }

                readMethod.addStatement("return object");
                jsonReadMethod.addStatement("return object");

                serializer.addMethod(writeMethod.build());
                serializer.addMethod(readMethod.build());

                method.addStatement("io.anuke.arc.Core.settings.setSerializer($N, $L)", Utils.elementUtils.getBinaryName(elem).toString().replace('$', '.') + ".class", serializer.build());

                name(writeMethod, "write" + simpleTypeName);
                name(readMethod, "read" + simpleTypeName);

                writeMethod.addModifiers(Modifier.STATIC);
                readMethod.addModifiers(Modifier.STATIC);

                classBuilder.addMethod(writeMethod.build());
                classBuilder.addMethod(readMethod.build());

                classBuilder.addMethod(jsonWriteMethod.build());
                classBuilder.addMethod(jsonReadMethod.build());

                MethodSpec.Builder binaryJsonWriteMethod = MethodSpec.methodBuilder("write" + simpleTypeName + "StreamJson")
                .returns(void.class)
                .addParameter(DataOutput.class, "stream")
                .addParameter(type, "object")
                .addException(IOException.class)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addStatement("java.io.StringWriter output = new java.io.StringWriter()")
                .addStatement("bjson.setWriter(output)")
                .addStatement("bjson.writeObjectStart(" + type + ".class, " + type + ".class)")
                .addStatement("write" + simpleTypeName + "Json(bjson, object)")
                .addStatement("bjson.writeObjectEnd()")
                .addStatement("stream.writeUTF(output.toString())");

                MethodSpec.Builder binaryJsonWriteStringMethod = MethodSpec.methodBuilder("write" + simpleTypeName + "StringJson")
                .returns(String.class)
                .addParameter(DataOutput.class, "stream")
                .addParameter(type, "object")
                .addException(IOException.class)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addStatement("java.io.StringWriter output = new java.io.StringWriter()")
                .addStatement("bjson.setWriter(output)")
                .addStatement("bjson.writeObjectStart(" + type + ".class, " + type + ".class)")
                .addStatement("write" + simpleTypeName + "Json(bjson, object)")
                .addStatement("bjson.writeObjectEnd()")
                .addStatement("return output.toString()");

                MethodSpec.Builder binaryJsonReadMethod = MethodSpec.methodBuilder("read" + simpleTypeName + "StreamJson")
                .returns(type)
                .addParameter(DataInput.class, "stream")
                .addException(IOException.class)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addStatement("return read" + simpleTypeName + "Json(bjson.fromJson(null, stream.readUTF()))");

                classBuilder.addMethod(binaryJsonWriteMethod.build());
                classBuilder.addMethod(binaryJsonWriteStringMethod.build());
                classBuilder.addMethod(binaryJsonReadMethod.build());
            }

            classBuilder.addMethod(method.build());

            //write result
            JavaFile.builder(packageName, classBuilder.build()).build().writeTo(Utils.filer);

            return true;
        }catch(Exception e){
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    static void name(MethodSpec.Builder builder, String name){
        try{
            Field field = builder.getClass().getDeclaredField("name");
            field.setAccessible(true);
            field.set(builder, name);
        }catch(Exception e){
            throw new RuntimeException(e);
        }
    }
}
