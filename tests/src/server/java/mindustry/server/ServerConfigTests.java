package mindustry.server;

import mindustry.io.*;
import mindustry.net.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

public class ServerConfigTests extends ServerTestBase{
    /** These open sockets or change the global log level, which reset() does not undo. */
    static final Set<String> skipped = Set.of("socketInput", "socketInputPort", "socketInputAddress", "debug");

    static Stream<String> configFields(){
        return Arrays.stream(ServerConfig.class.getFields()).map(Field::getName).filter(n -> !skipped.contains(n));
    }

    @ParameterizedTest
    @MethodSource("configFields")
    void fieldCanBeChangedAndIsPersisted(String name) throws Exception{
        Field field = ServerConfig.class.getField(name);
        Object before = ServerHarness.call(() -> field.get(netServer.config));
        Object next = differentValue(field.getType(), before);
        String arg = next instanceof Boolean b ? (b ? "on" : "off") : next.toString();

        ServerHarness.command("config " + name + " " + arg).assertHas("set to");

        assertEquals(next, ServerHarness.call(() -> field.get(netServer.config)), "live value of " + name);
        ServerConfig saved = JsonIO.read(ServerConfig.class, ServerControl.instance.configFile.readString());
        assertEquals(next, field.get(saved), "value of " + name + " in config.hjson");
    }

    @Test
    void listingShowsEveryField(){
        CommandResult result = ServerHarness.command("config");
        for(Field field : ServerConfig.class.getFields()){
            result.assertHas(field.getName());
        }
    }

    @Test
    void readsCurrentValue(){
        ServerHarness.command("config name").assertHas("'name' is currently Server");
    }

    @Test
    void defaultRestoresValue(){
        ServerHarness.command("config name Changed");
        ServerHarness.command("config name default");
        assertEquals("Server", ServerHarness.call(() -> netServer.config.name));
    }

    @Test
    void invalidNumberIsRejected(){
        int before = ServerHarness.call(() -> netServer.config.playerLimit);
        ServerHarness.command("config playerLimit abc").expectError("Not a valid number");
        assertEquals(before, ServerHarness.call(() -> netServer.config.playerLimit));
    }

    @Test
    void unknownFieldIsRejected(){
        ServerHarness.command("config notAThing 1").expectError("Unknown config");
    }

    @Test
    void reloadReadsTheFile(){
        ServerControl.instance.configFile.writeString("port: " + ServerHarness.port() + "\nname: FromFile\nplayerLimit: 7");
        ServerHarness.command("config reload").assertHas("Config reloaded");

        assertEquals("FromFile", ServerHarness.call(() -> netServer.config.name));
        assertEquals(7, ServerHarness.call(() -> netServer.config.playerLimit));
    }

    @Test
    void debugChangesLogLevel(){
        try{
            ServerHarness.command("config debug on");
            assertEquals(arc.util.Log.LogLevel.debug, arc.util.Log.level);
        }finally{
            ServerHarness.command("config debug off");
        }
        assertEquals(arc.util.Log.LogLevel.info, arc.util.Log.level);
    }

    static Object differentValue(Class<?> type, Object before){
        if(type == boolean.class) return !(Boolean)before;
        if(type == int.class) return (Integer)before + 1;
        if(type == float.class) return (Float)before + 1f;
        if(type == String.class) return "test_value".equals(before) ? "other_value" : "test_value";
        if(type.isEnum()){
            Object[] values = type.getEnumConstants();
            return values[(((Enum<?>)before).ordinal() + 1) % values.length];
        }
        throw new AssertionError("No test value for config type " + type + ", add one.");
    }
}
