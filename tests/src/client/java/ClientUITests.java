import arc.*;
import arc.scene.ui.*;
import arc.struct.*;
import mindustry.type.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.*;
import org.junit.jupiter.params.provider.*;

import java.util.function.*;

import static mindustry.Vars.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Builds and renders the scene2d UI.
 * <p>
 * Deliberately not covered here: JoinDialog, HostDialog, DiscordDialog and the mod browser. They talk to the internet or open sockets as soon as they are shown,
 * which a deterministic test must not do. Once networking is under test, they belong with it.
 */
public class ClientUITests extends ClientTestBase{

    static Dialog dialogs(String name){
        return switch(name){
            case "about" -> ui.about;
            case "custom" -> ui.custom;
            case "load" -> ui.load;
            case "settings" -> ui.settings;
            case "controls" -> ui.controls;
            case "language" -> ui.language;
            case "database" -> ui.database;
            case "schematics" -> ui.schematics;
            case "maps" -> ui.maps;
            default -> throw new IllegalArgumentException(name);
        };
    }

    void showAndHide(Supplier<Dialog> get){
        ClientHarness.run(() -> get.get().show());
        ClientHarness.waitUntil("the dialog to open", () -> get.get().isShown(), 300);
        ClientHarness.frames(10);

        ClientHarness.run(() -> get.get().hide());
        //dialog animations run on real time, not game ticks
        ClientHarness.waitUntil("the dialog to close", () -> !get.get().isShown(), 600);
    }

    @ParameterizedTest
    @ValueSource(strings = {"about", "custom", "load", "settings", "controls", "language", "database", "schematics", "maps"})
    void menuDialogs(String name){
        showAndHide(() -> dialogs(name));
    }

    @Test
    void pauseMenu(){
        playMap(groundZero());
        ClientHarness.frames(30);

        showAndHide(() -> ui.paused);
    }

    /** The content info page of everything that has one: the database entry for every block, unit, item, liquid, status effect... */
    @Test
    void contentInfoForEverything(){
        Seq<UnlockableContent> all = ClientHarness.call(() -> {
            Seq<UnlockableContent> result = new Seq<>();
            for(Seq<Content> list : content.getContentMap()){
                for(Content c : list){
                    if(c instanceof UnlockableContent u && !u.isHidden()) result.add(u);
                }
            }
            return result;
        });
        assertTrue(all.size > 100, "suspiciously little content: " + all.size);

        for(UnlockableContent c : all){
            try{
                ClientHarness.run(() -> ui.content.show(c));
                ClientHarness.frames(2);
            }catch(Throwable t){
                throw new AssertionError("Failed to show the info dialog of " + c.getContentType() + " '" + c.name + "'", t);
            }
        }

        closeDialogs();
    }

    /** A plain message dialog, which is what every caught error in the game ends up showing. */
    @Test
    void errorMessageDialog(){
        ClientHarness.run(() -> ui.showErrorMessage("test error message"));
        ClientHarness.frames(30);
        closeDialogs();
    }
}
