package mindustry.mod.mixin;

import org.spongepowered.asm.mixin.Mixins;
import org.spongepowered.asm.mixin.connect.IMixinConnector;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * Mixin Connector that loads mixin configurations during the Java agent premain phase.
 */
public class MindustryMixinConnector implements IMixinConnector{

    @Override
    public void connect(){
        System.out.println("[MindustryMixinConnector] Connecting and scanning for mixin configs...");

        try{
            String userHome = System.getProperty("user.home");
            String dataDir = System.getProperty("mindustry.data.dir");

            File modsDir;
            if(dataDir != null){
                modsDir = new File(dataDir, "mods");
            }else{
                String os = System.getProperty("os.name").toLowerCase();
                if(os.contains("win")){
                    modsDir = new File(System.getenv("APPDATA"), "Mindustry/mods");
                }else if(os.contains("mac")){
                    modsDir = new File(userHome, "Library/Application Support/Mindustry/mods");
                }else{
                    modsDir = new File(userHome, ".local/share/Mindustry/mods");
                }
            }

            if(!modsDir.exists() || !modsDir.isDirectory()){
                System.out.println("[MindustryMixinConnector] Mods directory not found: " + modsDir);
                return;
            }

            System.out.println("[MindustryMixinConnector] Scanning mods directory: " + modsDir);

            File[] modFiles = modsDir.listFiles();
            if(modFiles == null) return;

            java.util.List<URL> allModUrls = new java.util.ArrayList<>();
            for(File modFile : modFiles){
                if(modFile.getName().endsWith(".jar") || modFile.getName().endsWith(".zip")){
                    allModUrls.add(modFile.toURI().toURL());
                }
            }

            if(!allModUrls.isEmpty()){
                URLClassLoader allModsLoader = new URLClassLoader(
                    allModUrls.toArray(new URL[0]),
                    getClass().getClassLoader()
                );

                MindustryClassProvider.setModClassLoader(allModsLoader);

                Thread.currentThread().setContextClassLoader(allModsLoader);

                System.out.println("[MindustryMixinConnector] Registered " + allModUrls.size() + " mod(s) with class provider");
            }

            int mixinModsFound = 0;
            for(File modFile : modFiles){
                if(modFile.getName().endsWith(".jar") || modFile.getName().endsWith(".zip")){
                    if(scanModForMixins(modFile)){
                        mixinModsFound++;
                    }
                }
            }

            System.out.println("[MindustryMixinConnector] Found " + mixinModsFound + " mod(s) with mixin configurations");

        }catch(Exception e){
            System.err.println("[MindustryMixinConnector] Error scanning for mixin configs:");
            e.printStackTrace();
        }
    }

    private boolean scanModForMixins(File modFile){
        try{
            URL modUrl = modFile.toURI().toURL();
            URLClassLoader modLoader = new URLClassLoader(new URL[]{modUrl}, getClass().getClassLoader());

            try(InputStream modJsonStream = modLoader.getResourceAsStream("mod.json")){
                if(modJsonStream == null) return false;

                StringBuilder jsonContent = new StringBuilder();
                byte[] buffer = new byte[8192];
                int read;
                while((read = modJsonStream.read(buffer)) != -1){
                    jsonContent.append(new String(buffer, 0, read));
                }

                String json = jsonContent.toString();

                String mixinsKey = "\"mixins\"";
                int mixinsIndex = json.indexOf(mixinsKey);
                if(mixinsIndex == -1) return false;

                int colonIndex = json.indexOf(":", mixinsIndex);
                int startQuote = json.indexOf("\"", colonIndex);
                int endQuote = json.indexOf("\"", startQuote + 1);
                if(startQuote == -1 || endQuote == -1) return false;

                String mixinConfigPath = json.substring(startQuote + 1, endQuote);
                System.out.println("[MindustryMixinConnector] Found mixin config '" + mixinConfigPath + "' in " + modFile.getName());

                Mixins.addConfiguration(mixinConfigPath);
                System.out.println("[MindustryMixinConnector] ✓ Registered mixin config: " + mixinConfigPath);
                return true;
            }

        }catch(Exception e){
            System.err.println("[MindustryMixinConnector] Error scanning mod file: " + modFile.getName());
            e.printStackTrace();
        }

        return false;
    }
}
