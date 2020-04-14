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
