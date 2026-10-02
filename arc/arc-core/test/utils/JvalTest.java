package utils;

import arc.util.serialization.*;
import arc.util.serialization.Jval.*;
import org.junit.*;

import static org.junit.Assert.*;

public class JvalTest{

    @Test
    public void parseExponential(){
        Jval val = Jval.read("{a: 1e-4, b: 1e92}");
        assertEquals(1e-4f, val.getDouble("a", 0f), 0.00001f);
        assertEquals(1e92, val.getDouble("b", 0f), 0.00001f);
    }

    @Test
    public void parseUnquotedStringArray(){
        Jval val = Jval.read("{\nkey: [result, result2]\n}");
        assertEquals("result", val.get("key").asArray().get(0).asString());
        assertEquals("result2", val.get("key").asArray().get(1).asString());
    }

    @Test
    public void parseUnquotedComma(){
        //unlike the official spec, this JVal implementation discards commas in unquoted strings
        Jval val = Jval.read("[\n" +
        "O, T,\n" +
        "]");

        assertEquals("O", val.asArray().get(0).asString());
        assertEquals("T", val.asArray().get(1).asString());

        Jval comma = Jval.read("it: it,");

        assertEquals("it", comma.getString("it"));
    }

    @Test
    public void parseUnquotedString(){
        Jval val = Jval.read("name: Molten Silver\n" +
        "description: Imagine silver, but not solid at all.\n" +
        "temperature: 0.9,//comment\n" +
        "viscosity: 0.8\n" +
        "effect: melting\n" +
        "color: a4a2bd\n" +
        "empty: \"\"");

        assertEquals("Imagine silver, but not solid at all.", val.getString("description"));
        assertEquals(0.8f, val.getFloat("viscosity", 0f), 0.0001f);
        assertEquals("", val.getString("empty", "not empty"));
    }

    @Test
    public void parseWithComma(){
        Jval val = Jval.read("name: always,");

        assertEquals(Jtype.string, val.get("name").getType());
        assertEquals("always", val.getString("name"));
    }

    @Test
    public void parseJson(){
        //taken from hjson site
        Jval val = Jval.read("\n" +
        "\n" +
        "  // Live demo.\n" +
        "  // Type either into the Hjson or JSON input\n" +
        "  // to convert to the other format.\n" +
        "  // Reload to reset.\n" +
        "\n" +
        "  # comments are useful\n" +
        "  # specify rate in requests/second\n" +
        "  \"rate\": 1000\n" +
        "\n" +
        "  // maybe you prefer js style comments\n" +
        "  /* or if you feel old fashioned */\n" +
        "\n" +
        "  # key names do not need to be placed in quotes\n" +
        "  key: \"value\"\n" +
        "\n" +
        "  # you don't need quotes for strings\n" +
        "  text: comma, no quotes!\n" +
        "\n" +
        "  # note that for quoteless strings everything up\n" +
        "  # to the next line is part of the string!\n" +
        "\n" +
        "  # commas are optional\n" +
        "  commas:\n" +
        "  {\n" +
        "    one: 1//comment\n" +
        "    two: 2\n" +
        "  }\n" +
        "\n" +
        "  # trailing commas are allowed\n" +
        "  trailing:\n" +
        "  {\n" +
        "    one: 1,//comment\n" +
        "    two: 2,\n" +
        "  }\n" +
        "\n" +
        "  # multiline string\n" +
        "  haiku:\n" +
        "    '''\n" +
        "    aba1111111111111111111111111111111111basb\n" +
        "    ddddddddddddd1111111dyfh3289gh2goui3ho3hgohjgo3hghddddddddddddddddddddddddddddddddddddddddd\n" +
        "    eeeeeeeeeeeeeeesehshhhsher5hysghyr5yherd5yhg5edhy5syhedyhedyhedyhesdyhgeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeekkkkkllllbvbbbcbZzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee\n" +
        "    '''\n" +
        "\n" +
        "  # Obviously you can always use standard JSON syntax as well:\n" +
        "  favNumbers: [ 1, 2, 3, 6, 42 ]\n" +
        "\n" +
        "  ");

        String expected = "{\"rate\":1000,\"key\":\"value\",\"text\":\"comma, no quotes!\",\"commas\":{\"one\":1,\"two\":2},\"trailing\":{\"one\":1,\"two\":2},\"haiku\":\"aba1111111111111111111111111111111111basb\\nddddddddddddd1111111dyfh3289gh2goui3ho3hgohjgo3hghddddddddddddddddddddddddddddddddddddddddd\\neeeeeeeeeeeeeeesehshhhsher5hysghyr5yherd5yhg5edhy5syhedyhedyhedyhesdyhgeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeekkkkkllllbvbbbcbZzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzzeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee\",\"favNumbers\":[1,2,3,6,42]}";

        assertEquals(expected, val.toString(Jformat.plain));
    }

    @Test
    public void parseTrickyKeyNames(){
        Jval val = Jval.read(
        "\"key with spaces\": spaced key\n" +
        "\"key.with.dots\": dotted key\n" +
        "\"123numericKey\": value\n" +
        "unquoted_key: regular value"
        );

        assertEquals("spaced key", val.getString("key with spaces"));
        assertEquals("dotted key", val.getString("key.with.dots"));
        assertEquals("value", val.getString("123numericKey"));
        assertEquals("regular value", val.getString("unquoted_key"));
    }

    @Test
    public void parseCommentsInTrickyPlaces(){
        Jval val = Jval.read(
        "// Header comment\n" +
        "# Hash comment\n" +
        "/* Block comment */\n" +
        "item: value // inline JS comment\n" +
        "item2: value2 # inline Hash comment\n" +
        "array: [\n" +
        "  /* block in array */ 1,\n" +
        "  2 // inline in array\n" +
        "]"
        );

        assertEquals("value", val.getString("item"));
        assertEquals("value2", val.getString("item2"));
        assertEquals(1, val.get("array").asArray().get(0).asInt());
        assertEquals(2, val.get("array").asArray().get(1).asInt());
    }

    @Test
    public void parseEdgeCasesAndLiteralTypes(){
        Jval val = Jval.read(
        "boolTrue: true\n" +
        "boolFalse: false\n" +
        "nullVal: null\n" +
        "stringTrue: \"true\"\n" +
        "stringNumber: \"12345\"\n" +
        "negativeNum: -987.654\n" +
        "zeroNum: 0"
        );

        assertTrue(val.get("boolTrue").asBool());
        assertFalse(val.get("boolFalse").asBool());
        assertTrue(val.get("nullVal").isNull());
        assertEquals(Jtype.string, val.get("stringTrue").getType());
        assertEquals(Jtype.string, val.get("stringNumber").getType());
        assertEquals(-987.654f, val.getFloat("negativeNum", 0f), 0.001f);
        assertEquals(0, val.getInt("zeroNum", -1));
    }

    @Test
    public void parseEmptyContainersAndWhitespace(){
        Jval val = Jval.read(
        "emptyObj: {}\n" +
        "emptyArr: []\n" +
        "commentObj: {\n" +
        "  // empty\n" +
        "}\n" +
        "commentArr: [\n" +
        "  # empty\n" +
        "]"
        );

        assertEquals(0, val.get("emptyObj").asObject().size);
        assertEquals(0, val.get("emptyArr").asArray().size);
        assertEquals(0, val.get("commentObj").asObject().size);
        assertEquals(0, val.get("commentArr").asArray().size);
    }

    @Test
    public void parseUnicodeEscapes(){
        Jval val = Jval.read(
        "smiley: \"\\u263A\"\n" +
        "combined: \"caf\\u00e9\"\n" +
        "emoji: \"\\ud83d\\ude00\"\n"
        );

        assertEquals("\u263A", val.getString("smiley"));
        assertEquals("café", val.getString("combined"));
        assertEquals("😀", val.getString("emoji"));
    }

    @Test
    public void parseExponentialAndLargeNumbers(){
        Jval val = Jval.read(
        "sci1: 1.5e10\n" +
        "sci2: 2E-5\n" +
        "sci3: 1e+3\n" +
        "maxLong: 9223372036854775807\n" +
        "minLong: -9223372036854775808\n" +
        "overflow: 99999999999999999999\n" +
        "tiny: 0.0000001\n"
        );

        assertEquals(1.5e10, val.getDouble("sci1", 0), 0.001);
        assertEquals(2E-5, val.getDouble("sci2", 0), 1e-10);
        assertEquals(1000.0, val.getDouble("sci3", 0), 0.001);
        assertEquals(9223372036854775807L, val.get("maxLong").asLong());
        assertEquals(Long.MIN_VALUE, val.get("minLong").asLong());
        assertEquals(1.0E20, val.getDouble("overflow", 0), 1e15); // too big for long, falls back to double
        assertEquals(0.0000001, val.getDouble("tiny", 0), 1e-12);
    }

    @Test
    public void parseNumberFormatsThatLookTricky(){
        Jval val = Jval.read(
        "leadingZero: 0123\n" +
        "justDot: .5\n" +
        "trailingDot: 5.\n" +
        "plusSign: +5\n" +
        "notANumber: 5-5\n" +
        "versionString: 1.2.3\n" +
        "negZero: -0\n" +
        "onlyMinus: -\n"
        );

        assertEquals(Jtype.string, val.get("leadingZero").getType());
        assertEquals("0123", val.getString("leadingZero"));
        assertEquals(".5", val.getString("justDot"));
        assertEquals("5.", val.getString("trailingDot"));
        assertEquals("+5", val.getString("plusSign"));
        assertEquals("5-5", val.getString("notANumber"));
        assertEquals("1.2.3", val.getString("versionString"));
        assertEquals(0, val.get("negZero").asInt());
        assertEquals("-", val.getString("onlyMinus"));
    }

    @Test
    public void parseBooleanAndNullLookalikes(){
        Jval val = Jval.read(
        "truthy: trueish\n" +
        "falsey: falsely\n" +
        "nully: nullable\n" +
        "exactTrue: true\n" +
        "exactFalse: false\n" +
        "exactNull: null\n"
        );

        assertEquals(Jtype.string, val.get("truthy").getType());
        assertEquals("trueish", val.getString("truthy"));
        assertEquals("falsely", val.getString("falsey"));
        assertEquals("nullable", val.getString("nully"));
        assertTrue(val.get("exactTrue").asBool());
        assertFalse(val.get("exactFalse").asBool());
        assertTrue(val.get("exactNull").isNull());
    }

    @Test
    public void parseEscapedCharactersInStrings(){
        Jval val = Jval.read(
        "escaped: \"line1\\nline2\\ttabbed\"\n" +
        "backslash: \"C:\\\\Users\\\\test\"\n" +
        "quotes: \"It said \\\"hi\\\"\"\n"
        );

        assertEquals("line1\nline2\ttabbed", val.getString("escaped"));
        assertEquals("C:\\Users\\test", val.getString("backslash"));
        assertEquals("It said \"hi\"", val.getString("quotes"));
    }

    @Test
    public void parseSingleLineTripleQuoteString(){
        Jval val = Jval.read(
        "note: '''single line note'''\n"
        );

        assertEquals("single line note", val.getString("note"));
    }

    @Test
    public void parseArraysWithMixedQuotelessAndQuoted(){
        Jval val = Jval.read(
        "items: [\n" +
        "  bare word,\n" +
        "  \"quoted string\",\n" +
        "  42,\n" +
        "  true,\n" +
        "  null\n" +
        "]\n"
        );

        assertEquals("bare word", val.get("items").asArray().get(0).asString());
        assertEquals("quoted string", val.get("items").asArray().get(1).asString());
        assertEquals(42, val.get("items").asArray().get(2).asInt());
        assertTrue(val.get("items").asArray().get(3).asBool());
        assertTrue(val.get("items").asArray().get(4).isNull());
    }

    @Test
    public void parseNestedObjectsAndArrays(){
        Jval val = Jval.read(
        "outer: {\n" +
        "  inner: {\n" +
        "    deep: value\n" +
        "  }\n" +
        "  list: [1, 2, [3, 4]]\n" +
        "}\n"
        );

        assertEquals("value", val.get("outer").get("inner").getString("deep"));
        assertEquals(4, val.get("outer").get("list").asArray().get(2).asArray().get(1).asInt());
    }

    @Test
    public void parseEmptyContainersAndStrings(){
        Jval val = Jval.read(
        "emptyObj: {}\n" +
        "emptyArr: []\n" +
        "emptyString: \"\"\n"
        );

        assertEquals(Jtype.object, val.get("emptyObj").getType());
        assertEquals(Jtype.array, val.get("emptyArr").getType());
        assertEquals(0, val.get("emptyArr").asArray().size);
        assertEquals("", val.getString("emptyString"));
    }

    @Test
    public void parseKeyNamesEdgeCases(){
        Jval val = Jval.read(
        "\"\": emptyKeyName\n" +
        "_underscore_key: value1\n" +
        "key-with-dashes: value2\n" +
        "key123: value3\n" +
        "café: value4\n" +
        "日本語: japanese value\n"
        );

        assertEquals("emptyKeyName", val.getString(""));
        assertEquals("value1", val.getString("_underscore_key"));
        assertEquals("value2", val.getString("key-with-dashes"));
        assertEquals("value3", val.getString("key123"));
        assertEquals("value4", val.getString("café"));
        assertEquals("japanese value", val.getString("日本語"));
    }

    @Test
    public void parseTrailingCommasAndNoTrailingNewline(){
        Jval val = Jval.read(
        "a: 1,\n" +
        "b: two,\n" +
        "noNewlineAtEnd: last value without newline"
        );

        assertEquals(1, val.getInt("a", -1));
        assertEquals("two", val.getString("b"));
        assertEquals("last value without newline", val.getString("noNewlineAtEnd"));
    }

    @Test
    public void parseQuotelessStringsWithInternalWhitespace(){
        Jval val = Jval.read(
        "spaced:   value   with   spaces   \n" +
        "single: word\n"
        );

        assertEquals("value   with   spaces", val.getString("spaced"));
        assertEquals("word", val.getString("single"));
    }

    @Test
    public void parseCommaKey(){
        Jval val = Jval.read("[{item: copper, amount: 10}, {item: lead, amount: 30}]");
        assertEquals(10, val.get(0).getInt("amount"));
        assertEquals("copper", val.get(0).getString("item"));
        assertEquals(30, val.get(1).getInt("amount"));
        assertEquals("lead", val.get(1).getString("item"));
    }

    @Test
    public void parseComma2(){
        Jval val = Jval.read("{item: copper, amount: 10}");
        assertEquals("copper", val.getString("item"));
        assertEquals(10, val.getInt("amount"));
    }

    @Test
    public void parseComma3(){
        Jval val = Jval.read("{\nitem: very cool, this is not copper\n}");
        assertEquals("very cool, this is not copper", val.getString("item"));
    }

    @Test
    public void parseLongRules(){
        String str = "{teams:{0:{protectCores:false,checkPlacement:false},1:{},2:{infiniteResources:true},3:{infiniteResources:true},4:{},5:{},6:{},7:{},8:{},9:{},10:{},11:{},12:{},13:{},14:{},15:{},16:{},17:{},18:{},19:{},20:{},21:{},22:{},23:{},24:{},25:{},26:{},27:{},28:{},29:{},30:{},31:{},32:{},33:{},34:{},35:{},36:{},37:{},38:{},39:{},40:{},41:{},42:{},43:{},44:{},45:{},46:{},47:{},48:{},49:{},50:{},51:{},52:{},53:{},54:{},55:{},56:{},57:{},58:{},59:{},60:{},61:{},62:{},63:{},64:{},65:{},66:{},67:{},68:{},69:{},70:{},71:{},72:{},73:{},74:{},75:{},76:{},77:{},78:{},79:{},80:{},81:{},82:{},83:{},84:{},85:{},86:{},87:{},88:{},89:{},90:{},91:{},92:{},93:{},94:{},95:{},96:{},97:{},98:{},99:{},100:{},101:{},102:{},103:{},104:{},105:{},106:{},107:{},108:{},109:{},110:{},111:{},112:{},113:{},114:{},115:{},116:{},117:{},118:{},119:{},120:{},121:{},122:{},123:{},124:{},125:{},126:{},127:{},128:{},129:{},130:{},131:{},132:{},133:{},134:{},135:{},136:{},137:{},138:{},139:{},140:{},141:{},142:{},143:{},144:{},145:{},146:{},147:{},148:{},149:{},150:{},151:{},152:{},153:{},154:{},155:{},156:{},157:{},158:{},159:{},160:{},161:{},162:{},163:{},164:{},165:{},166:{},167:{},168:{},169:{},170:{},171:{},172:{},173:{},174:{},175:{},176:{},177:{},178:{},179:{},180:{},181:{},182:{},183:{},184:{},185:{},186:{},187:{},188:{},189:{},190:{},191:{},192:{},193:{},194:{},195:{},196:{},197:{},198:{},199:{},200:{},201:{},202:{},203:{},204:{},205:{},206:{},207:{},208:{},209:{},210:{},211:{},212:{},213:{},214:{},215:{},216:{},217:{},218:{},219:{},220:{},221:{},222:{},223:{},224:{},225:{},226:{},227:{},228:{},229:{},230:{},231:{},232:{},233:{},234:{},235:{},236:{},237:{},238:{},239:{},240:{},241:{},242:{},243:{},244:{},245:{},246:{},247:{},248:{},249:{},250:{},251:{},252:{},253:{},254:{},255:{}},waveSending:false,airUseSpawns:true,wavesSpawnAtCores:false,attackMode:true,reactorExplosions:false,fire:false,logicUnitBuild:false,enemyCoreBuildRadius:80,onlyDepositCore:true,coreDestroyClear:true,hideBannedBlocks:true,dropZoneRadius:64,waveSpacing:18000,initialWaveSpacing:36000,attributes:{},spawns:[{type:arkyid,spacing:4,max:1000,scaling:0.5,shields:1000,shieldScaling:500,amount:2,effect:none,team:3},{type:locus,max:350,scaling:0.2857143,shields:150,shieldScaling:250,amount:20,effect:none,team:3},{type:precept,max:120,scaling:0.5,shields:300,shieldScaling:400,amount:10,effect:none,team:3},{type:vanquish,max:60,scaling:1.25,amount:5,effect:none,team:3},{type:conquer,begin:5,end:19,max:60,scaling:0.8,shields:3000,shieldScaling:800,amount:4,effect:none,team:3},{type:quell,begin:5,max:80,scaling:0.33333334,shields:300,shieldScaling:200,amount:2,effect:overdrive,team:3},{type:conquer,begin:14,max:60,scaling:0.6666667,shields:6000,shieldScaling:1000,amount:10,effect:shielded,team:3},{type:disrupt,begin:17,spacing:3,scaling:1.4285715,shields:1000,shieldScaling:300,amount:6,effect:overclock,team:3},{type:reign,begin:39,end:40,max:1,effect:boss,team:3}],loadout:[{item:graphite,amount:300},{item:silicon,amount:300},{item:beryllium,amount:1000},{item:tungsten,amount:160}],bannedBlocks:{values:[diode,pulverizer,inverted-sorter,mechanical-drill,overflow-gate,pulse-conduit,titanium-wall-large,junction,payload-conveyor,spore-press,meltdown,ripple,core-foundation,overdrive-dome,overdrive-projector,mass-driver,scrap-wall-large,plastanium-wall,repair-point,surge-smelter,multi-press,switch,solar-panel-large,message,hail,tile-logic-display,illuminator,kiln,unloader,liquid-container,thorium-reactor,battery,mechanical-pump,scrap-wall-huge,router,tsunami,underflow-gate,ground-factory,additive-reconstructor,hyper-processor,air-factory,graphite-press,plastanium-wall-large,door,logic-processor,cryofluid-mixer,repair-turret,scrap-wall-gigantic,swarmer,surge-tower,memory-cell,scorch,armored-conveyor,phase-wall-large,bridge-conveyor,blast-drill,blast-mixer,parallax,conduit,impulse-pump,phase-wall,phase-weaver,payload-router,titanium-wall,mend-projector,separator,rotary-pump,logic-display,salvo,titanium-conveyor,advanced-launch-pad,launch-pad,blast-door,segment,battery-large,steam-generator,shock-mine,large-logic-display,thruster,pneumatic-drill,core-shard,liquid-junction,rtg-generator,force-projector,scrap-wall,tetrative-reconstructor,plastanium-compressor,fuse,multiplicative-reconstructor,vault,thorium-wall,naval-factory,plastanium-conveyor,exponential-reconstructor,impact-reactor,copper-wall,scatter,differential-generator,laser-drill,copper-wall-large,memory-bank,cultivator,landing-pad,spectre,thermal-generator,liquid-router,core-nucleus,arc,pyratite-mixer,power-node-large,container,phase-conveyor,oil-extractor,coal-centrifuge,silicon-crucible,wave,distributor,duo,cyclone,combustion-generator,melter,sorter,disassembler,conveyor,surge-wall,plated-conduit,phase-conduit,interplanetary-accelerator,surge-wall-large,mender,bridge-conduit,power-node,solar-panel,micro-processor,water-extractor,liquid-tank,silicon-smelter,thorium-wall-large]},objectives:[],fog:true,lighting:true,ambientLight:6200005b,waveTeam:3,planet:sun}";
        Jval val = Jval.read(str);
        assertEquals("sun", val.getString("planet"));

        assertEquals(str, val.toString(Jformat.minimal));
    }

    @Test
    public void parseLongRules2(){
        Jval val = Jval.read("{fog:true,planet:sun}");
        assertEquals("sun", val.getString("planet"));
    }

}
