package mindustry.annotations.remote;

import arc.struct.*;
import arc.util.io.*;
import com.squareup.javapoet.*;
import mindustry.annotations.Annotations.*;
import mindustry.annotations.*;
import mindustry.annotations.util.*;
import mindustry.annotations.util.TypeIOResolver.*;

import javax.lang.model.element.*;
import java.io.*;

import static mindustry.annotations.BaseProcessor.*;

/** Generates code for writing remote invoke packets on the client and server. */
public class CallGenerator{

    /** Generates all classes in this list. */
    public static void generate(ClassSerializer serializer, Seq<MethodEntry> methods) throws IOException{
        //create builder
        TypeSpec.Builder callBuilder = TypeSpec.classBuilder(RemoteProcess.callLocation).addModifiers(Modifier.PUBLIC);

        MethodSpec.Builder register = MethodSpec.methodBuilder("registerPackets")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC);

        //go through each method entry in this class
        for(MethodEntry ent : methods){
            //builder for the packet type
            TypeSpec.Builder packet = TypeSpec.classBuilder(ent.packetClassName)
            .addModifiers(Modifier.PUBLIC);

            //temporary data to deserialize later
            packet.addField(FieldSpec.builder(byte[].class, "DATA", Modifier.PRIVATE).initializer("NODATA").build());

            packet.superclass(tname("mindustry.net.Packet"));

            //return the correct priority
            if(ent.priority != PacketPriority.normal){
                packet.addMethod(MethodSpec.methodBuilder("getPriority")
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(Override.class).returns(int.class).addStatement("return $L", ent.priority.ordinal())
                .build());
            }

            //implement read & write methods
            makeWriter(packet, ent, serializer);
            makeReader(packet, ent, serializer);

            //generate handlers
            if(ent.where.isClient){
                packet.addMethod(writeHandleMethod(ent, false));
            }

            if(ent.where.isServer){
                packet.addMethod(writeHandleMethod(ent, true));
            }

            //register packet
            register.addStatement("mindustry.net.Net.registerPacket($L.$L::new)", packageName, ent.packetClassName);

            //add fields to the type
            for(Svar param : ent.element.params()){
                packet.addField(param.tname(), param.name(), Modifier.PUBLIC);
            }

            //write the 'send event to all players' variant: always happens for clients, but only happens if 'all' is enabled on the server method
            if(ent.where.isClient || ent.target.isAll){
                writeCallMethod(callBuilder, ent, true, false);
            }

            //write the 'send event to one player' variant, which is only applicable on the server
            if(ent.where.isServer && ent.target.isOne){
                writeCallMethod(callBuilder, ent, false, false);
            }

            //write the forwarded method version
            if(ent.where.isServer && ent.forward){
                writeCallMethod(callBuilder, ent, true, true);
            }

            //write the completed packet class
            JavaFile.builder(packageName, packet.build()).build().writeTo(BaseProcessor.filer);
        }

        callBuilder.addMethod(register.build());

        //build and write resulting class
        TypeSpec spec = callBuilder.build();
        JavaFile.builder(packageName, spec).build().writeTo(BaseProcessor.filer);
    }

    private static void makeWriter(TypeSpec.Builder typespec, MethodEntry ent, ClassSerializer serializer){
        MethodSpec.Builder builder = MethodSpec.methodBuilder("write")
            .addParameter(Writes.class, "WRITE")
            .addModifiers(Modifier.PUBLIC).addAnnotation(Override.class);
        Seq<Svar> params = ent.element.params();

        for(int i = 0; i < params.size; i++){
            //first argument is skipped as it is always the player caller
            if(!ent.where.isServer && i == 0){
                continue;
            }

            Svar var = params.get(i);

            //name of parameter
            String varName = var.name();
            //name of parameter type
            String typeName = var.mirror().toString();
            //special case: method can be called from anywhere to anywhere
            //thus, only write the player when the SERVER is writing data, since the client is the only one who reads the player anyway
            boolean writePlayerSkipCheck = ent.where == Loc.both && i == 0;

            if(writePlayerSkipCheck){ //write begin check
                builder.beginControlFlow("if(mindustry.Vars.net.server())");
            }

            if(BaseProcessor.isPrimitive(typeName)){ //check if it's a primitive, and if so write it
                builder.addStatement("WRITE.$L($L)", typeName.equals("boolean") ? "bool" : typeName.charAt(0) + "", varName);
            }else{
                //else, try and find a serializer
                String ser = serializer.getNetWriter(typeName.replace("mindustry.gen.", ""), SerializerResolver.locate(ent.element.e, var.mirror(), true));

                if(ser == null){ //make sure a serializer exists!
                    BaseProcessor.err("No method to write class type: '" + typeName + "'", var);
                }

                //add statement for writing it
                builder.addStatement(ser + "(WRITE, " + varName + ")");
            }

            if(writePlayerSkipCheck){ //write end check
                builder.endControlFlow();
            }
        }

        typespec.addMethod(builder.build());
    }

    private static void makeReader(TypeSpec.Builder typespec, MethodEntry ent, ClassSerializer serializer){
        MethodSpec.Builder readbuilder = MethodSpec.methodBuilder("read")
            .addParameter(Reads.class, "READ")
            .addParameter(int.class, "LENGTH")
            .addModifiers(Modifier.PUBLIC).addAnnotation(Override.class);

        //read only into temporary data buffer
        readbuilder.addStatement("DATA = READ.b(LENGTH)");

        typespec.addMethod(readbuilder.build());

        MethodSpec.Builder builder = MethodSpec.methodBuilder("handled")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class);

        //make sure data is present, begin reading it if so
        builder.addStatement("BAIS.setBytes(DATA)");

        Seq<Svar> params = ent.element.params();

        //go through each parameter
        for(int i = 0; i < params.size; i++){
            Svar var = params.get(i);

            //first argument is skipped as it is always the player caller
            if(!ent.where.isServer && i == 0){
                continue;
            }

            //special case: method can be called from anywhere to anywhere
            //thus, only read the player when the CLIENT is receiving data, since the client is the only one who cares about the player anyway
            boolean writePlayerSkipCheck = ent.where == Loc.both && i == 0;

            if(writePlayerSkipCheck){ //write begin check
                builder.beginControlFlow("if(mindustry.Vars.net.client())");
            }

            //full type name of parameter
            String typeName = var.mirror().toString();
            //name of parameter
            String varName = var.name();
            //capitalized version of type name for reading primitives
            String pname = typeName.equals("boolean") ? "bool" : typeName.charAt(0) + "";

            //write primitives automatically
            if(BaseProcessor.isPrimitive(typeName)){
                builder.addStatement("$L = READ.$L()", varName, pname);
            }else{
                //else, try and find a serializer
                String ser = serializer.readers.get(typeName.replace("mindustry.gen.", ""), SerializerResolver.locate(ent.element.e, var.mirror(), false));

                if(ser == null){ //make sure a serializer exists!
                    BaseProcessor.err("No read method to read class type '" + typeName + "' in method " + ent.targetMethod + "; " + serializer.readers, var);
                }

                //add statement for reading it
                builder.addStatement("$L = $L(READ)", varName, ser);
            }

            if(writePlayerSkipCheck){ //write end check
                builder.endControlFlow();
            }
        }

        typespec.addMethod(builder.build());
    }

    /** Creates a specific variant for a method entry. */
    private static void writeCallMethod(TypeSpec.Builder classBuilder, MethodEntry ent, boolean toAll, boolean forwarded){
        Smethod elem = ent.element;
        Seq<Svar> params = elem.params();

        //create builder
        MethodSpec.Builder method = MethodSpec.methodBuilder(elem.name() + (forwarded ? "__forward" : "")) //add except suffix when forwarding
        .addModifiers(Modifier.STATIC)
        .returns(void.class);

        //forwarded methods aren't intended for use, and are not public
        if(!forwarded){
            method.addModifiers(Modifier.PUBLIC);
        }

        //validate client methods to make sure
        if(ent.where.isClient){
            if(params.isEmpty()){
                BaseProcessor.err("Client invoke methods must have a first parameter of type Player", elem);
                return;
            }

            if(!params.get(0).mirror().toString().contains("Player")){
                BaseProcessor.err("Client invoke methods should have a first parameter of type Player", elem);
                return;
            }
        }

        //if toAll is false, it's a 'send to one player' variant, so add the player as a parameter
        if(!toAll){
            method.addParameter(ClassName.bestGuess("mindustry.net.NetConnection"), "playerConnection");
        }

        //add sender to ignore
        if(forwarded){
            method.addParameter(ClassName.bestGuess("mindustry.net.NetConnection"), "exceptConnection");
        }

        //call local method if applicable, shouldn't happen when forwarding method as that already happens by default
        if(!forwarded && ent.local != Loc.none){
            //add in local checks
            if(ent.local != Loc.both){
                method.beginControlFlow("if(" + getCheckString(ent.local) + " || !mindustry.Vars.net.active())");
            }

            //concatenate parameters
            int index = 0;
            StringBuilder results = new StringBuilder();
            for(Svar var : params){
                //special case: calling local-only methods uses the local player
                if(index == 0 && ent.where == Loc.client){
                    results.append("mindustry.Vars.player");
                }else{
                    results.append(var.name());
                }
                if(index != params.size - 1) results.append(", ");
                index++;
            }

            //add the statement to call it
            method.addStatement("$N." + elem.name() + "(" + results + ")",
            ((TypeElement)elem.up()).getQualifiedName().toString());

            if(ent.local != Loc.both){
                method.endControlFlow();
            }
        }

        //start control flow to check if it's actually client/server so no netcode is called
        method.beginControlFlow("if(" + getCheckString(ent.where) + ")");

        //add statement to create packet from pool
        method.addStatement("$1T packet = new $1T()", tname("mindustry.gen." + ent.packetClassName));

        method.addTypeVariables(Seq.with(elem.e.getTypeParameters()).map(BaseProcessor::getTVN));

        for(int i = 0; i < params.size; i++){
            //first argument is skipped as it is always the player caller
            if((!ent.where.isServer) && i == 0){
                continue;
            }

            Svar var = params.get(i);

            method.addParameter(var.tname(), var.name());

            //name of parameter
            String varName = var.name();
            //special case: method can be called from anywhere to anywhere
            //thus, only write the player when the SERVER is writing data, since the client is the only one who reads it
            boolean writePlayerSkipCheck = ent.where == Loc.both && i == 0;

            if(writePlayerSkipCheck){ //write begin check
                method.beginControlFlow("if(mindustry.Vars.net.server())");
            }

            method.addStatement("packet.$L = $L", varName, varName);

            if(writePlayerSkipCheck){ //write end check
                method.endControlFlow();
            }
        }

        String sendString;

        if(forwarded){ //forward packet
            if(!ent.local.isClient){ //if the client doesn't get it called locally, forward it back after validation
                sendString = "mindustry.Vars.net.send(";
            }else{
                sendString = "mindustry.Vars.net.sendExcept(exceptConnection, ";
            }
        }else if(toAll){ //send to all players / to server
            sendString = "mindustry.Vars.net.send(";
        }else{ //send to specific client from server
            sendString = "playerConnection.send(";
        }

        //send the actual packet
        method.addStatement(sendString + "packet, " + (!ent.unreliable) + ")");


        //end check for server/client
        method.endControlFlow();

        //add method to class, finally
        classBuilder.addMethod(method.build());
    }

    private static String getCheckString(Loc loc){
        return
            loc.isClient && loc.isServer ? "mindustry.Vars.net.server() || mindustry.Vars.net.client()" :
            loc.isClient ? "mindustry.Vars.net.client()" :
            loc.isServer ? "mindustry.Vars.net.server()" : "false";
    }

    /** Generates handleServer / handleClient methods. */
    public static MethodSpec writeHandleMethod(MethodEntry ent, boolean isClient){

        //create main method builder
        MethodSpec.Builder builder = MethodSpec.methodBuilder(isClient ? "handleClient" : "handleServer")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(void.class);

        Smethod elem = ent.element;
        Seq<Svar> params = elem.params();

        if(!isClient){
            //add player parameter
            builder.addParameter(ClassName.get("mindustry.net", "NetConnection"), "con");

            //skip if player is invalid
            builder.beginControlFlow("if(con.player == null || con.kicked)");
            builder.addStatement("return");
            builder.endControlFlow();

            //make sure to use the actual player who sent the packet
            builder.addStatement("mindustry.gen.Player player = con.player");
        }

        //execute the relevant method before the forward
        //if it throws a ValidateException, the method won't be forwarded
        builder.addStatement("$N." + elem.name() + "(" + params.toString(", ", s -> s.name()) + ")", ((TypeElement)elem.up()).getQualifiedName().toString());

        //call forwarded method, don't forward on the client reader
        if(ent.forward && ent.where.isServer && !isClient){
            //call forwarded method
            builder.addStatement("$L.$L.$L__forward(con, $L)", packageName, ent.className, elem.name(), params.toString(", ", s -> s.name()));
        }

        return builder.build();
    }
}
