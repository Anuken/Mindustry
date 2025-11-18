package mindustry.mod;

/**
 * Mixin configuration data structures.
 *
 */
public class MixinService{

    public static class MixinConfig{
        public String modName;
        public arc.files.Fi configFile;
        public MixinConfigData data;

        public MixinConfig(String modName, arc.files.Fi configFile, MixinConfigData data){
            this.modName = modName;
            this.configFile = configFile;
            this.data = data;
        }
    }

    public static class MixinConfigData{
        public boolean required = true;
        public String minVersion = "0.8";
        public String packageName;
        public String compatibilityLevel = "JAVA_8";
        public String[] mixins = {};
        public String[] client = {};
        public String[] server = {};
        public boolean verbose = false;
        public int priority = 1000;
    }
}
