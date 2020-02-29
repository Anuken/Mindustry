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
    private static final String data = "eJzNWI1y0zgQfoh7AeGZm5GhmLT8tGDgpn/cZQ4K1wCdG8IwsqwkAtvySXLTEvLS9wS3KymJ0yZHuBluyLSxvVrt76fddX76u2b8ExsKUsoqb4zVl0kusmaYyrJW2hKmeXKz/TCQhTBXSE11halkdrRMAckNt8u0xspiBcUILVkhPzMrVbVYXxjIlRar6FKtolbCJvuanwj7SqtzmQu9jusVhELYlnMf2TlbkuoIBauGiRaDQrQdckvVNWbn0WdZA5kXzBjSrT7CPqUndZMVkhNjwU9OzpXMiZQ0npwGyUZY6i3qhYCA5U7GFonGWlqhoy1CD1Vldh6vcmSLHFxacdAMBkI/jWm9RbKY3H5KJnJAayIr0FxxoQYERFSg0e+KJ0uPhJMnTkmLo04zEPzuPdHCNIXt5sBSiTFx1M77FMxAHS97iTT7Va7BNZDqvOfbwOtuk4HSJ6wUNGKeJVEmOWhkkUdx2rNaVkP3JDTJwtVrWVqjcfpMiiIHYwZ4NcDEt5OhsI5sYB30UPfgOcijwBmjje4W2V9f1oLG5MmTIN9HOp6gL0F/wupaVPliD62aoojjdMqZ5SP6eqTVmGWFIBDE6XQagoQX047Q3vsU70/BbzpiZkRn8q3yumkcx5DHC4v5M9QJiNNWsB0lnYrCCDIL9BkgQI3BYtxUg15AA7NiKJ3yCfWp9wpIISsBVNgoLgSn0bgECHJTa5XDKSXgG3nzpnsUJaYupKURicAi2FrSODFNZryZkecpRDW0IzAaDkVdMA4p7VcAzShqUYgnpGBuISxg11A0IiaPyX3y5YuzaC4JiNudOdWq52os9CEzkCE4/JVlAF4aCa2VjuI5n/irYYXpDisoD4456tztdDo7nc7tzj38uo9fnc4D/NqF/z34f9gSsE4RlLxC5BswWgVxIhZx4NLdhX1DVuzrYVOKyh5fcFFjUaNRxhDmWthGVwRBAInxAUmnWxvk6qjb+/3otPv22OXKF4iTpsygJnwlZ8u8P0rufrDQP4MmR7DTQeDxPuFawGF6LcoaH2kExXn4CcOSnGcmwiJUiDNXk8lgHA77gkZRFFSiceLqNibBEpV9PHvRhXScS456fhX2ZYa9gfajsazKYWnNoz58kn5fK2X7fS7L851+FEOGbhEng6uia0WJJ3xJXHIMUPmjEfqSfCCenRACgnsCewu5SQZalQRqxt2dDwcQywPFdA6iA/Mzpckx4yOUigqgWyx0eZYzw7WsbXLMR2rGlrTBFfjEhbQEanB4PIGyhrcuGrxQmMZ0DdK5V4FhvnPnBBI/VHCPscTqu58ZVTRWvGIewAHq13Ccug05OG5RF4L4Cl73vgmua0/A/4hhsEBL6DRrgTydxbSCFgsx7V0azM/QDUG10PaSRg0MWgmug5RMqUKwirDqErgHUEeFa5yyskQCpZPC5XGrp4T4AfnWLd8lW/1pwfaOSnLLGZGghYcqB5dj8vN1Se9dR41TbwFMiyLN4NB9WtNdIY03gDWGo0pyibPBIU6FfjplARo0OoQTBB4PNSuPmGX9fgTmdCvQ7Prtb+IitNwXgKJBoqEnq7InRE6/ErGWO1uks0Uehg+0boTYDbAJUCyNhREknuBT+Qm+cSAJcwHT+spUgBMBQRMC3U0I8ao8bHdC4JG7NSqAzDhFZXwEIwWNznp/9jbxmAbbgTVKjvZfg3+uUrWkTqcQaTu0YMSGCnZ3d2Fl8ziGyIGOVuQ2GaJWDEsopOVBoE5bGEUOwFfuGdA/HKcmi9N32lRWlqJ16vCgTFfBEeHvcehG+0tfxLBG1TXC7mi2QL8j9DB+cwsWaAO7QiNbLPrkrbLlm7Pliuu1dH1PbH/Vh+vAxthcB/R/hJbz+N+x5ViWwHUNNDvxbL5rGpnPyheMFRZoBhMQvIlwfTGA4RNM+8dlDalZBNwr38QLzxmnQe3ijYriHPDgHmYXSlfFMcuBOU6XrasbO7cLL1fXDTvHVtuKiL/1ryztTtHWmQunc4NA4DGcZmgG5J++hXaMvxS416g4xTe57kufobA12yI8OfdsuLyeCWG+frUx6KtTTHlSqgwP1i/EQSDehjfLTlhFszh09kLpGf/MaYjV6eHdHcI1D+F3zxAujj+A1Dm0xRaz2/xcgQW4DNF4y4oGj18Q63WjMTnEXX4WK89Uaz0crnUuOj7sxDKe1URSh7E1Awr8hV8sAvJmg0drhtusartXX8e+8mXXTTXhRTdoRKdm41dQB++7EGschQExQZMnrC4uZj73hTjAtoTlOTVQUJjeD34H9bjoYjazoIBEOI/n+t0lJhO/Aoq2t3fu70HJ6TzYu7dz/+Hu89SbXfm5CH9amJuAY/FVC2HRm0YmKO7u9s0RdNGwc27j3MRROgUBQg9g5nW/99Sz4Ltxyk+ThswrD+T0H7j81+Q=";

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
