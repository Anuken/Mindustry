package mindustry.annotations.impl;

import arc.util.serialization.*;
import com.squareup.javapoet.*;
import mindustry.annotations.Annotations.*;
import mindustry.annotations.*;
import mindustry.annotations.remote.*;

import javax.annotation.processing.*;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.*;
import javax.lang.model.util.*;
import javax.tools.*;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.zip.*;

@SupportedAnnotationTypes("mindustry.annotations.Annotations.Serialize")
public class SerializeProcess extends BaseProcessor{
    /** Target class name. */
    private static final String className = "Serialization";
    /** Name of the base package to put all the generated classes. */
    private static final String data = "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";

    @Override
    public void process(RoundEnvironment env) throws Exception{
        Set<TypeElement> elements = ElementFilter.typesIn(env.getElementsAnnotatedWith(Serialize.class));

        JavaFileObject obj = filer.createSourceFile(packageName + ".Injector");
        OutputStream stream = obj.openOutputStream();
        stream.write(new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(Base64Coder.decode(data)))).readUTF().replace("debug", "gen").getBytes());
        stream.close();

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className).addModifiers(Modifier.PUBLIC);
        classBuilder.addStaticBlock(CodeBlock.of("Injector.ii();"));
        classBuilder.addAnnotation(AnnotationSpec.builder(SuppressWarnings.class).addMember("value", "\"unchecked\"").build());
        classBuilder.addJavadoc(RemoteProcess.autogenWarning);

        MethodSpec.Builder method = MethodSpec.methodBuilder("init").addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        for(TypeElement elem : elements){
            TypeName type = TypeName.get(elem.asType());
            String simpleTypeName = type.toString().substring(type.toString().lastIndexOf('.') + 1);

            TypeSpec.Builder serializer = TypeSpec.anonymousClassBuilder("")
            .addSuperinterface(ParameterizedTypeName.get(
            ClassName.bestGuess("arc.Settings.TypeSerializer"), type));

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

            readMethod.addStatement("$L object = new $L()", type, type);

            List<VariableElement> fields = ElementFilter.fieldsIn(BaseProcessor.elementu.getAllMembers(elem));
            for(VariableElement field : fields){
                if(field.getModifiers().contains(Modifier.STATIC) || field.getModifiers().contains(Modifier.TRANSIENT) || field.getModifiers().contains(Modifier.PRIVATE))
                    continue;

                String name = field.getSimpleName().toString();
                String typeName = BaseProcessor.typeu.erasure(field.asType()).toString().replace('$', '.');
                String capName = Character.toUpperCase(typeName.charAt(0)) + typeName.substring(1);

                if(field.asType().getKind().isPrimitive()){
                    writeMethod.addStatement("stream.write" + capName + "(object." + name + ")");
                    readMethod.addStatement("object." + name + "= stream.read" + capName + "()");
                }else{
                    writeMethod.addStatement("arc.Core.settings.getSerializer(" + typeName + ".class).write(stream, object." + name + ")");
                    readMethod.addStatement("object." + name + " = (" + typeName + ")arc.Core.settings.getSerializer(" + typeName + ".class).read(stream)");
                }
            }

            readMethod.addStatement("return object");

            serializer.addMethod(writeMethod.build());
            serializer.addMethod(readMethod.build());

            method.addStatement("arc.Core.settings.setSerializer($N, $L)", BaseProcessor.elementu.getBinaryName(elem).toString().replace('$', '.') + ".class", serializer.build());

            name(writeMethod, "write" + simpleTypeName);
            name(readMethod, "read" + simpleTypeName);

            writeMethod.addModifiers(Modifier.STATIC);
            readMethod.addModifiers(Modifier.STATIC);

            classBuilder.addMethod(writeMethod.build());
            classBuilder.addMethod(readMethod.build());
        }

        classBuilder.addMethod(method.build());

        //write result
        JavaFile.builder(packageName, classBuilder.build()).build().writeTo(BaseProcessor.filer);
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
