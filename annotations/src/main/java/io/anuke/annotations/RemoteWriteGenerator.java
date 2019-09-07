package io.anuke.annotations;

import com.squareup.javapoet.*;
import io.anuke.annotations.Annotations.Loc;
import io.anuke.annotations.IOFinder.ClassSerializer;

import javax.lang.model.element.*;
import javax.tools.Diagnostic.Kind;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;

/** Generates code for writing remote invoke packets on the client and server. */
public class RemoteWriteGenerator{
    private final HashMap<String, ClassSerializer> serializers;

    /** Creates a write generator that uses the supplied serializer setup. */
    public RemoteWriteGenerator(HashMap<String, ClassSerializer> serializers){
        this.serializers = serializers;
    }

    /** Generates all classes in this list. */
    public void generateFor(List<ClassEntry> entries, String packageName) throws IOException{

        for(ClassEntry entry : entries){
            //create builder
            TypeSpec.Builder classBuilder = TypeSpec.classBuilder(entry.name).addModifiers(Modifier.PUBLIC);
            classBuilder.addJavadoc(RemoteMethodAnnotationProcessor.autogenWarning);

            //add temporary write buffer
            classBuilder.addField(FieldSpec.builder(ByteBuffer.class, "TEMP_BUFFER", Modifier.STATIC, Modifier.PRIVATE, Modifier.FINAL)
            .initializer("ByteBuffer.allocate($1L)", RemoteMethodAnnotationProcessor.maxPacketSize).build());

            //go through each method entry in this class
            for(MethodEntry methodEntry : entry.methods){
                //write the 'send event to all players' variant: always happens for clients, but only happens if 'all' is enabled on the server method
                if(methodEntry.where.isClient || methodEntry.target.isAll){
                    writeMethodVariant(classBuilder, methodEntry, true, false);
                }

                //write the 'send event to one player' variant, which is only applicable on the server
                if(methodEntry.where.isServer && methodEntry.target.isOne){
                    writeMethodVariant(classBuilder, methodEntry, false, false);
                }

                //write the forwarded method version
                if(methodEntry.where.isServer && methodEntry.forward){
                    writeMethodVariant(classBuilder, methodEntry, true, true);
                }
            }

            //build and write resulting class
            TypeSpec spec = classBuilder.build();
            JavaFile.builder(packageName, spec).build().writeTo(Utils.filer);
        }
    }

    /** Creates a specific variant for a method entry. */
    private void writeMethodVariant(TypeSpec.Builder classBuilder, MethodEntry methodEntry, boolean toAll, boolean forwarded){
        ExecutableElement elem = methodEntry.element;

        //create builder
        MethodSpec.Builder method = MethodSpec.methodBuilder(elem.getSimpleName().toString() + (forwarded ? "__forward" : "")) //add except suffix when forwarding
        .addModifiers(Modifier.STATIC, Modifier.SYNCHRONIZED)
        .returns(void.class);

        //forwarded methods aren't intended for use, and are not public
        if(!forwarded){
            method.addModifiers(Modifier.PUBLIC);
        }

        //validate client methods to make sure
        if(methodEntry.where.isClient){
            if(elem.getParameters().isEmpty()){
                Utils.messager.printMessage(Kind.ERROR, "Client invoke methods must have a first parameter of type Player.", elem);
                return;
            }

            if(!elem.getParameters().get(0).asType().toString().equals("io.anuke.mindustry.entities.type.Player")){
                Utils.messager.printMessage(Kind.ERROR, "Client invoke methods should have a first parameter of type Player.", elem);
                return;
            }
        }

        //if toAll is false, it's a 'send to one player' variant, so add the player as a parameter
        if(!toAll){
            method.addParameter(ClassName.bestGuess("io.anuke.mindustry.net.NetConnection"), "playerConnection");
        }

        //add sender to ignore
        if(forwarded){
            method.addParameter(ClassName.bestGuess("io.anuke.mindustry.net.NetConnection"), "exceptConnection");
        }

        //call local method if applicable, shouldn't happen when forwarding method as that already happens by default
        if(!forwarded && methodEntry.local != Loc.none){
            //add in local checks
            if(methodEntry.local != Loc.both){
                method.beginControlFlow("if(" + getCheckString(methodEntry.local) + " || !io.anuke.mindustry.Vars.net.active())");
            }

            //concatenate parameters
            int index = 0;
            StringBuilder results = new StringBuilder();
            for(VariableElement var : elem.getParameters()){
                //special case: calling local-only methods uses the local player
                if(index == 0 && methodEntry.where == Loc.client){
                    results.append("io.anuke.mindustry.Vars.player");
                }else{
                    results.append(var.getSimpleName());
                }
                if(index != elem.getParameters().size() - 1) results.append(", ");
                index++;
            }

            //add the statement to call it
            method.addStatement("$N." + elem.getSimpleName() + "(" + results.toString() + ")",
            ((TypeElement)elem.getEnclosingElement()).getQualifiedName().toString());

            if(methodEntry.local != Loc.both){
                method.endControlFlow();
            }
        }

        //start control flow to check if it's actually client/server so no netcode is called
        method.beginControlFlow("if(" + getCheckString(methodEntry.where) + ")");

        //add statement to create packet from pool
        method.addStatement("$1N packet = $2N.obtain($1N.class, $1N::new)", "io.anuke.mindustry.net.Packets.InvokePacket", "io.anuke.arc.util.pooling.Pools");
        //assign buffer
        method.addStatement("packet.writeBuffer = TEMP_BUFFER");
        //assign priority
        method.addStatement("packet.priority = (byte)" + methodEntry.priority.ordinal());
        //assign method ID
        method.addStatement("packet.type = (byte)" + methodEntry.id);
        //rewind buffer
        method.addStatement("TEMP_BUFFER.position(0)");

        for(int i = 0; i < elem.getParameters().size(); i++){
            //first argument is skipped as it is always the player caller
            if((!methodEntry.where.isServer/* || methodEntry.mode == Loc.both*/) && i == 0){
                continue;
            }

            VariableElement var = elem.getParameters().get(i);

            //add parameter to method
            method.addParameter(TypeName.get(var.asType()), var.getSimpleName().toString());

            //name of parameter
            String varName = var.getSimpleName().toString();
            //name of parameter type
            String typeName = var.asType().toString();
            //captialized version of type name for writing primitives
            String capName = typeName.equals("byte") ? "" : Character.toUpperCase(typeName.charAt(0)) + typeName.substring(1);
            //special case: method can be called from anywhere to anywhere
            //thus, only write the player when the SERVER is writing data, since the client is the only one who reads it
            boolean writePlayerSkipCheck = methodEntry.where == Loc.both && i == 0;

            if(writePlayerSkipCheck){ //write begin check
                method.beginControlFlow("if(io.anuke.mindustry.Vars.net.server())");
            }

            if(Utils.isPrimitive(typeName)){ //check if it's a primitive, and if so write it
                if(typeName.equals("boolean")){ //booleans are special
                    method.addStatement("TEMP_BUFFER.put(" + varName + " ? (byte)1 : 0)");
                }else{
                    method.addStatement("TEMP_BUFFER.put" +
                    capName + "(" + varName + ")");
                }
            }else{
                //else, try and find a serializer
                ClassSerializer ser = serializers.get(typeName);

                if(ser == null){ //make sure a serializer exists!
                    Utils.messager.printMessage(Kind.ERROR, "No @WriteClass method to write class type: '" + typeName + "'", var);
                    return;
                }

                //add statement for writing it
                method.addStatement(ser.writeMethod + "(TEMP_BUFFER, " + varName + ")");
            }

            if(writePlayerSkipCheck){ //write end check
                method.endControlFlow();
            }
        }

        //assign packet length
        method.addStatement("packet.writeLength = TEMP_BUFFER.position()");

        String sendString;

        if(forwarded){ //forward packet
            if(!methodEntry.local.isClient){ //if the client doesn't get it called locally, forward it back after validation
                sendString = "io.anuke.mindustry.Vars.net.send(";
            }else{
                sendString = "io.anuke.mindustry.Vars.net.sendExcept(exceptConnection, ";
            }
        }else if(toAll){ //send to all players / to server
            sendString = "io.anuke.mindustry.Vars.net.send(";
        }else{ //send to specific client from server
            sendString = "playerConnection.send(";
        }

        //send the actual packet
        method.addStatement(sendString + "packet, " +
        (methodEntry.unreliable ? "io.anuke.mindustry.net.Net.SendMode.udp" : "io.anuke.mindustry.net.Net.SendMode.tcp") + ")");


        //end check for server/client
        method.endControlFlow();

        //add method to class, finally
        classBuilder.addMethod(method.build());
    }

    private String getCheckString(Loc loc){
        return loc.isClient && loc.isServer ? "io.anuke.mindustry.Vars.net.server() || io.anuke.mindustry.Vars.net.client()" :
        loc.isClient ? "io.anuke.mindustry.Vars.net.client()" :
        loc.isServer ? "io.anuke.mindustry.Vars.net.server()" : "false";
    }
}
