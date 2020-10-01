package mindustry.annotations.remote;

import arc.struct.*;
import arc.util.io.*;
import com.squareup.javapoet.*;
import mindustry.annotations.*;
import mindustry.annotations.util.TypeIOResolver.*;

import javax.lang.model.element.*;

/** Generates code for reading remote invoke packets on the client and server. */
public class RemoteReadGenerator{
    private final ClassSerializer serializers;

    /** Creates a read generator that uses the supplied serializer setup. */
    public RemoteReadGenerator(ClassSerializer serializers){
        this.serializers = serializers;
    }

    /**
     * Generates a class for reading remote invoke packets.
     * @param entries List of methods to use.
     * @param className Simple target class name.
     * @param packageName Full target package name.
     * @param needsPlayer Whether this read method requires a reference to the player sender.
     */
    public void generateFor(Seq<MethodEntry> entries, String className, String packageName, boolean needsPlayer) throws Exception{

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className).addModifiers(Modifier.PUBLIC);
        classBuilder.addJavadoc(RemoteProcess.autogenWarning);

        //create main method builder
        MethodSpec.Builder readMethod = MethodSpec.methodBuilder("readPacket")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addParameter(Reads.class, "read") //buffer to read form
        .addParameter(int.class, "id") //ID of method type to read
        .returns(void.class);

        if(needsPlayer){
            //add player parameter
            readMethod.addParameter(ClassName.get(packageName, "Player"), "player");
        }

        CodeBlock.Builder readBlock = CodeBlock.builder(); //start building block of code inside read method
        boolean started = false; //whether an if() statement has been written yet

        for(MethodEntry entry : entries){
            //write if check for this entry ID
            if(!started){
                started = true;
                readBlock.beginControlFlow("if(id == " + entry.id + ")");
            }else{
                readBlock.nextControlFlow("else if(id == " + entry.id + ")");
            }

            readBlock.beginControlFlow("try");

            //concatenated list of variable names for method invocation
            StringBuilder varResult = new StringBuilder();

            //go through each parameter
            for(int i = 0; i < entry.element.getParameters().size(); i++){
                VariableElement var = entry.element.getParameters().get(i);

                if(!needsPlayer || i != 0){ //if client, skip first parameter since it's always of type player and doesn't need to be read
                    //full type name of parameter
                    String typeName = var.asType().toString();
                    //name of parameter
                    String varName = var.getSimpleName().toString();
                    //captialized version of type name for reading primitives
                    String pname = typeName.equals("boolean") ? "bool" : typeName.charAt(0) + "";

                    //write primitives automatically
                    if(BaseProcessor.isPrimitive(typeName)){
                        readBlock.addStatement("$L $L = read.$L()", typeName, varName, pname);
                    }else{
                        //else, try and find a serializer
                        String ser = serializers.readers.get(typeName.replace("mindustry.gen.", ""), SerializerResolver.locate(entry.element, var.asType(), false));

                        if(ser == null){ //make sure a serializer exists!
                            BaseProcessor.err("No read method to read class type '" + typeName + "' in method " + entry.targetMethod + "; " + serializers.readers, var);
                            return;
                        }

                        //add statement for reading it
                        readBlock.addStatement(typeName + " " + varName + " = " + ser + "(read)");
                    }

                    //append variable name to string builder
                    varResult.append(var.getSimpleName());
                    if(i != entry.element.getParameters().size() - 1) varResult.append(", ");
                }else{
                    varResult.append("player");
                    if(i != entry.element.getParameters().size() - 1) varResult.append(", ");
                }
            }

            //execute the relevant method before the forward
            //if it throws a ValidateException, the method won't be forwarded
            readBlock.addStatement("$N." + entry.element.getSimpleName() + "(" + varResult.toString() + ")", ((TypeElement)entry.element.getEnclosingElement()).getQualifiedName().toString());

            //call forwarded method, don't forward on the client reader
            if(entry.forward && entry.where.isServer && needsPlayer){
                //call forwarded method
                readBlock.addStatement(packageName + "." + entry.className + "." + entry.element.getSimpleName() +
                "__forward(player.con" + (varResult.length() == 0 ? "" : ", ") + varResult.toString() + ")");
            }

            readBlock.nextControlFlow("catch (java.lang.Exception e)");
            readBlock.addStatement("throw new java.lang.RuntimeException(\"Failed to to read remote method '" + entry.element.getSimpleName() + "'!\", e)");
            readBlock.endControlFlow();
        }

        //end control flow if necessary
        if(started){
            readBlock.nextControlFlow("else");
            readBlock.addStatement("throw new $1N(\"Invalid read method ID: \" + id + \"\")", RuntimeException.class.getName()); //handle invalid method IDs
            readBlock.endControlFlow();
        }

        //add block and method to class
        readMethod.addCode(readBlock.build());
        classBuilder.addMethod(readMethod.build());

        //build and write resulting class
        TypeSpec spec = classBuilder.build();
        JavaFile.builder(packageName, spec).build().writeTo(BaseProcessor.filer);
    }
}
