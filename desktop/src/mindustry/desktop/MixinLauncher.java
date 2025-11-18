package mindustry.desktop;

/**
 * Launcher entry point for Mindustry with Mixin support.
 *
 * The Mixin Java Agent (configured via -javaagent) will automatically:
 * 1. Initialize Mixin during JVM startup (premain phase)
 * 2. Load MindustryMixinConnector which scans for mixin configs
 * 3. Register all mixin configurations before any game classes load
 * 4. Install a ClassFileTransformer that applies mixins as classes are loaded
 */
public class MixinLauncher{

    public static void main(String[] args) throws Exception{
        System.out.println("[MixinLauncher] Starting Mindustry with Mixin Java Agent support...");
        System.out.println("[MixinLauncher] Mixins will be applied automatically by the Java agent");
        System.out.println("[MixinLauncher] ===================================");

        DesktopLauncher.main(args);
    }
}
