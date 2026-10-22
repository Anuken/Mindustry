import arc.Files.*;
import arc.files.*;
import arc.struct.*;
import arc.util.*;

public class SkinGenerator{

    public static void main(String[] args){
        if(OS.env("destination.dir") == null) return;

        Fi path = Fi.get(OS.env("destination.dir"));
        Fi dest = path.child("core").child("src").list()[0].child("ui");
        Fi ui = path.child("core").child("assets-raw").child("sprites").child("ui");
        Fi icons = path.child("core").child("assets-raw").child("sprites").child("icons");
        String pack = path.child("core").child("src").list()[0].name() + ".ui";

        {
            Seq<String> regions = ui.findAll(f -> f.extEquals("png") && !f.name().endsWith(".9.png")).map(Fi::nameWithoutExtension).map(Strings::kebabToCamel);
            Seq<String> patches = ui.findAll(f -> f.extEquals("png") && f.name().endsWith(".9.png")).map(f -> f.name().replace(".9.png", "")).map(Strings::kebabToCamel);

            StringBuilder loading = new StringBuilder();
            for(Fi fi : ui.findAll(f -> f.extEquals("png"))){
                String name = fi.nameWithoutExtension().replace(".9", "");
                loading.append("        ").append(Strings.kebabToCamel(name)).append(" = ").append("Core.atlas.getDrawable(\"").append(name).append("\");\n");
            }

            String template = new Fi("textemplate", FileType.internal).readString();
            template = template
                .replace("%package%", pack)
                .replace("%patches%", patches.toString(", "))
                .replace("%regions%", regions.toString(", "))
                .replace("%load%", loading.toString());

            dest.child("Tex.java").writeString(template);
            Log.info("Done with base textures.");
        }

        {
            Seq<String> iconfs = icons.findAll(f -> f.extEquals("png") && f.name().startsWith("icon-")).map(f -> Strings.kebabToCamel(f.nameWithoutExtension().replace("icon-", "")));

            StringBuilder loading = new StringBuilder();
            for(Fi fi : icons.findAll(f -> f.extEquals("png"))){
                String name = fi.nameWithoutExtension();
                loading.append("        ").append(Strings.kebabToCamel(name.replace("icon-", ""))).append(" = ").append("Core.atlas.getDrawable(\"").append(name).append("\");\n");
            }

            String template = new Fi("icontemplate", FileType.internal).readString();
            template = template
                .replace("%package%", pack)
                .replace("%regions%", iconfs.toString(", "))
                .replace("%load%", loading.toString());

            dest.child("Icon.java").writeString(template);
            Log.info("Done with base icons.");
        }


    }
}
