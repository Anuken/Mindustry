package io.anuke.mindustry;

import io.anuke.arc.core.Timers;
import io.anuke.arc.util.Log;

import java.io.IOException;

public class PackerLauncher {

    public static void main(String[] args) throws IOException {
        Vars.headless = true;
        ImageContext context = new ImageContext();
        context.load();
        Time.mark();
        Generators.generate(context);
        Log.info("&ly[Generator]&lc Total time to generate: &lg{0}&lcms", Time.elapsed());
        Log.info("&ly[Generator]&lc Total images created: &lg{0}", Image.total());
        Image.dispose();
    }

}
