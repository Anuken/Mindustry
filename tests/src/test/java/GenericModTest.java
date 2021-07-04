import arc.*;
import arc.Net.*;
import arc.util.io.*;
import mindustry.*;
import org.junit.jupiter.api.*;

import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

public class GenericModTest{

    /** grabs a mod and puts it in the mod folder */
    static void grabMod(String url){
        //clear older mods
        ApplicationTests.testDataFolder.deleteDirectory();
        new Net().http(new HttpRequest().block(true).url(url).method(HttpMethod.GET), httpResponse -> {
            try{
                ApplicationTests.testDataFolder.child("mods").child("test_mod." + (url.endsWith("jar") ? "jar" : "zip")).writeBytes(Streams.copyBytes(httpResponse.getResultAsStream()));
            }catch(IOException e){
                Assertions.fail(e);
            }
        }, Assertions::fail);

        ApplicationTests.launchApplication(false);
    }

    static void checkExistence(String modName){
        assertNotEquals(Vars.mods, null);
        assertNotEquals(Vars.mods.list().size, 0, "At least one mod must be loaded.");
        assertEquals(Vars.mods.list().first().name, modName, modName + " must be loaded.");
    }
}
