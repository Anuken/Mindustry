package io.anuke.mindustry.io;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Array;
import io.anuke.mindustry.Vars;
import io.anuke.mindustry.core.Tutorial;
import io.anuke.mindustry.core.Tutorial.Stage;
import io.anuke.mindustry.resource.Item;
import io.anuke.mindustry.resource.Liquid;
import io.anuke.mindustry.resource.Weapon;
import io.anuke.mindustry.world.Block;
import io.anuke.mindustry.world.GameMode;
import io.anuke.mindustry.world.Map;
import io.anuke.ucore.core.Inputs.DeviceType;
import io.anuke.ucore.core.KeyBinds;
import io.anuke.ucore.core.KeyBinds.Keybind;
import io.anuke.ucore.scene.ui.SettingsDialog.SettingsTable.Setting;
import io.anuke.ucore.util.Bundles;
import io.anuke.ucore.util.Mathf;

public class BundleGen {
    private static FileHandle file;

    public static void buildBundle(FileHandle file){
        BundleGen.file = file;

        file.writeString("", false);
        write("about.text=" + join(Vars.aboutText));
        write("discord.text=Join the mindustry discord!\n[orange]");

        Mathf.each(table -> {
            for(Setting setting : table.getSettings()){
                write("setting." + setting.name + ".name=" + setting.title);
            }
        }, Vars.ui.getPrefs().game, Vars.ui.getPrefs().graphics, Vars.ui.getPrefs().sound);

        for(Map map : Vars.world.maps().list()){
            write("map." + map.name + ".name=" + map.name);
        }
        for(Tutorial.Stage stage : Stage.values()){
            write("tutorial." + stage.name() + ".text=" + stage.text);
        }
        for(Keybind bind : KeyBinds.getSection("default").keybinds.get(DeviceType.keyboard)){
            write("keybind." + bind.name + ".name=" + bind.name);
        }
        for(GameMode mode : GameMode.values()){
            write("mode." + mode.name() + ".name=" + mode.name());
        }
        for(Weapon weapon : Weapon.values()){
            write("weapon." + weapon.name() + ".name=" + weapon.name());
            write("weapon." + weapon.name() + ".description=" + weapon.description);
        }
        for(Item item : Item.values()){
            write("item." + item.name() + ".name=" + item.name());
        }
        for(Liquid liquid : Liquid.values()){
            write("liquid." + liquid.name() + ".name=" + liquid.name());
        }
        for(Block block : Block.getAllBlocks()){
            write("block." + block.name + ".name=" + block.formalName);
            if(block.fullDescription != null) write("block." + block.name + ".fulldescription=" + block.fullDescription);
            if(block.description != null) write("block." + block.name + ".description=" + block.description);

            Array<String> a = new Array<>();
            block.getStats(a);
            for(String s : a){
                if(s.contains(":")) {
                    String color = s.substring(0, s.indexOf("]")+1);
                    String first = s.substring(color.length(), s.indexOf(":")).replace("/", "").replace(" ", "").toLowerCase();
                    String last = s.substring(s.indexOf(":"), s.length());
                    s = color + Bundles.getNotNull("text.blocks." + first) + last;
                }
            }
        }
    }

    private static void write(String string){
        file.writeString(string.replaceAll("\\n", "\\\\n") + "\n", true);
    }

    public static String join(String[] strings){
        String s = "";
        for(String string : strings){
            s += string + "\n";
        }
        return s;
    }

}
