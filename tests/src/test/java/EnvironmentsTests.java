import arc.util.serialization.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.*;
import mindustry.io.*;
import mindustry.maps.*;
import mindustry.world.meta.*;
import org.junit.jupiter.api.*;

import static org.junit.jupiter.api.Assertions.*;

public class EnvironmentsTests{
    static final String[] legacyNames = {"terrestrial", "space", "underwater", "spores", "scorching", "groundOil", "groundWater", "oxygen"};
    static Env[] extra;

    @BeforeAll
    static void init(){
        ApplicationTests.launchApplication(false);

        //Env.all is static, so these persist for the whole JVM
        extra = new Env[70];
        for(int i = 0; i < extra.length; i++){
            String name = "test_env_" + i;
            extra[i] = Env.get(name) != null ? Env.get(name) : new Env(name);
        }
    }

    static Environments set(Env... envs){
        return Environments.of(envs);
    }

    static Rules read(String json){
        return JsonIO.read(Rules.class, json);
    }

    @Test
    void legacyIdsMatchBitPositions(){
        for(int i = 0; i < legacyNames.length; i++){
            assertEquals(i, Env.all.get(i).id);
            assertEquals(legacyNames[i], Env.all.get(i).name);
            assertSame(Env.all.get(i), Env.get(legacyNames[i]));
        }
    }

    @Test
    void envRegistration(){
        assertThrows(IllegalArgumentException.class, () -> new Env("space"));
        assertThrows(IllegalArgumentException.class, () -> new Env("any"));
        assertThrows(IllegalArgumentException.class, () -> new Env(""));
        assertThrows(IllegalArgumentException.class, () -> new Env(null));
        assertNull(Env.get("nope"));
    }

    @Test
    void basicSetOperations(){
        Environments a = set(Env.terrestrial, Env.space);

        assertTrue(a.has(Env.terrestrial));
        assertFalse(a.has(Env.underwater));
        assertFalse(a.isEmpty());
        assertTrue(Environments.none.isEmpty());

        assertSame(a, a.with(Env.space));
        assertSame(a, a.with(Environments.none));
        assertSame(a, a.without(Env.oxygen));
        assertSame(a, a.without(set(Env.oxygen)));

        Environments b = a.with(Env.underwater);
        assertNotSame(a, b);
        assertTrue(b.has(Env.underwater));
        assertFalse(a.has(Env.underwater));

        assertEquals(set(Env.terrestrial), a.without(Env.space));
        assertEquals(set(Env.terrestrial), a.without(set(Env.space, Env.oxygen)));
        assertEquals(set(Env.terrestrial, Env.space, Env.oxygen), a.with(set(Env.space, Env.oxygen)));
        assertEquals(Environments.none, a.without(a));
        assertEquals(a, Environments.of(set(Env.terrestrial), set(Env.space)));
    }

    @Test
    void containsQueries(){
        Environments a = set(Env.terrestrial, Env.space);

        assertTrue(a.containsAny(set(Env.space, Env.oxygen)));
        assertFalse(a.containsAny(set(Env.oxygen)));
        assertFalse(a.containsAny(Environments.none));

        assertTrue(a.containsAll(set(Env.space)));
        assertTrue(a.containsAll(Environments.none));
        assertFalse(a.containsAll(set(Env.space, Env.oxygen)));

        //multi-word
        Environments big = set(Env.terrestrial, extra[60]);
        assertTrue(big.containsAny(set(extra[60])));
        assertTrue(big.containsAll(set(Env.terrestrial, extra[60])));
        assertFalse(a.containsAll(big));
        assertFalse(a.containsAny(set(extra[60])));
        assertTrue(big.containsAll(a.without(Env.space)));
    }

    @Test
    void equalityAcrossWordCounts(){
        Environments small = set(Env.terrestrial);
        Environments trimmed = set(Env.terrestrial, extra[60]).without(extra[60]);

        assertEquals(small, trimmed);
        assertEquals(small.hashCode(), trimmed.hashCode());
        assertNotEquals(small, set(Env.terrestrial, extra[60]));
        assertNotEquals(Environments.none, Environments.any);
    }

    @Test
    void anySemantics(){
        Environments any = Environments.any;
        Environments x = set(Env.space);

        assertTrue(any.isAny());
        assertFalse(any.isEmpty());
        assertTrue(any.has(Env.oxygen));
        assertTrue(any.has(extra[69]));

        assertTrue(any.containsAny(x));
        assertFalse(any.containsAny(Environments.none));
        assertTrue(x.containsAny(any));
        assertFalse(Environments.none.containsAny(any));

        assertTrue(any.containsAll(x));
        assertTrue(any.containsAll(any));
        assertFalse(x.containsAll(any));

        assertSame(any, any.with(Env.space));
        assertSame(any, any.with(x));
        assertSame(any, x.with(any));

        assertThrows(UnsupportedOperationException.class, () -> any.without(Env.space));
        assertThrows(UnsupportedOperationException.class, () -> any.without(x));
        assertEquals("any", any.toString());
    }

    @Test
    void legacyBitmask(){
        assertEquals(set(Env.terrestrial, Env.underwater, Env.groundWater), read("{env:69}").env);
        assertEquals(set(Env.terrestrial, Env.underwater, Env.groundWater), JsonIO.read(new Rules(), "{env:69}").env);
        assertEquals(Environments.any, read("{env:-1}").env);
        assertEquals(Environments.none, read("{env:0}").env);

        Environments all = Environments.none;
        for(int i = 0; i < legacyNames.length; i++) all = all.with(Env.all.get(i));
        assertEquals(all, read("{env:255}").env);

        //the old default: terrestrial | spores | groundOil | groundWater | oxygen
        assertEquals(Vars.defaultEnv, read("{env:" + (1 | 1 << 3 | 1 << 5 | 1 << 6 | 1 << 7) + "}").env);
    }

    @Test
    void roundTrip(){
        Environments[] cases = {
        Environments.any,
        Environments.none,
        set(Env.space),
        set(Env.terrestrial, extra[0], extra[63], extra[64], extra[69])
        };

        for(Environments env : cases){
            Rules rules = new Rules();
            rules.env = env;
            assertEquals(env, read(JsonIO.write(rules)).env, "Round trip failed for " + env);
        }
    }

    @Test
    void writesArray(){
        Rules rules = new Rules();
        rules.env = Planets.erekir.defaultEnv;

        var written = Jval.read(JsonIO.write(rules)).get("env");
        assertTrue(written.isArray());
        assertEquals(2, written.asArray().size);
        assertEquals("terrestrial", written.asArray().get(0).asString());
        assertEquals("scorching", written.asArray().get(1).asString());

        rules.env = Environments.any;
        written = Jval.read(JsonIO.write(rules)).get("env");
        assertEquals(1, written.asArray().size);
        assertEquals("any", written.asArray().get(0).asString());
    }

    @Test
    void arrayReading(){
        assertEquals(set(Env.terrestrial), read("{env:[terrestrial, bogus]}").env);
        assertEquals(Environments.any, read("{env:[any, space]}").env);
        assertEquals(Environments.none, read("{env:[]}").env);
        assertEquals(set(Env.space), read("{env:space}").env);
    }

    @Test
    void invalidReading(){
        assertThrows(SerializationException.class, () -> read("{env:{}}"));
        assertThrows(SerializationException.class, () -> read("{env:true}"));
        assertThrows(SerializationException.class, () -> read("{env:[1]}"));
    }

    @Test
    void legacyMapRulesDetectErekir(){
        var tags = new arc.struct.StringMap();
        tags.put("rules", "{env:" + (1 | 1 << 4) + "}");

        Rules rules = new Map(tags).rules();
        assertTrue(rules.hasEnv(Env.scorching));
        assertEquals(Planets.erekir, rules.planet);

        tags.put("rules", "{env:" + (1 | 1 << 3) + "}");
        assertEquals(Planets.serpulo, new Map(tags).rules().planet);
    }
}
