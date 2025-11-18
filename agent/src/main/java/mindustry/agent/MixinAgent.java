package mindustry.agent;

import java.lang.instrument.Instrumentation;

/**
 * Java Agent that initializes Mixin before the application starts.
 */
public class MixinAgent {

    private static Instrumentation instrumentation;

    /**
     * Called by JVM when agent is loaded via -javaagent
     */
    public static void premain(String agentArgs, Instrumentation inst) {
        instrumentation = inst;

        System.out.println("[MixinAgent] Premain starting...");
        System.out.println("[MixinAgent] Agent args: " + agentArgs);

        try {
            Class<?> mixinBootstrapClass = Class.forName("org.spongepowered.asm.launch.MixinBootstrap");
            mixinBootstrapClass.getMethod("init").invoke(null);
            System.out.println("[MixinAgent] Mixin bootstrap initialized");

            Class<?> mixinServiceClass = Class.forName("org.spongepowered.asm.service.MixinService");
            Object service = mixinServiceClass.getMethod("getService").invoke(null);
            System.out.println("[MixinAgent] Got service: " + service.getClass().getName());

            if(service.getClass().getName().equals("mindustry.mod.mixin.MindustryMixinService")){
                service.getClass().getMethod("init").invoke(service);
                System.out.println("[MixinAgent] MindustryMixinService initialized");
            }

            try {
                Class<?> connectorClass = Class.forName("mindustry.mod.mixin.MindustryMixinConnector");
                Object connector = connectorClass.getDeclaredConstructor().newInstance();
                connectorClass.getMethod("connect").invoke(connector);
                System.out.println("[MixinAgent] MindustryMixinConnector invoked");
            } catch (ClassNotFoundException e) {
                System.out.println("[MixinAgent] MindustryMixinConnector not found (this is normal during agent build)");
            }

            try {
                Class<?> iMixinServiceClass = Class.forName("org.spongepowered.asm.service.IMixinService");
                Object transformerProvider = iMixinServiceClass.getMethod("getTransformerProvider").invoke(service);

                if(transformerProvider != null){
                    System.out.println("[MixinAgent] Got transformer provider: " + transformerProvider.getClass().getName());

                    Class<?> mindustryTransformerClass = Class.forName("mindustry.mod.mixin.MindustryTransformerService");
                    java.lang.reflect.Method getMixinTransformer = mindustryTransformerClass.getMethod("getMixinTransformer");
                    Object mixinTransformer = getMixinTransformer.invoke(transformerProvider);

                    if(mixinTransformer != null){
                        System.out.println("[MixinAgent] Got MixinTransformer: " + mixinTransformer.getClass().getName());

                        Class<?> mixinEnvironmentClass = Class.forName("org.spongepowered.asm.mixin.MixinEnvironment");
                        Object defaultEnv = mixinEnvironmentClass.getMethod("getDefaultEnvironment").invoke(null);

                        java.lang.instrument.ClassFileTransformer transformer = new java.lang.instrument.ClassFileTransformer(){
                            @Override
                            public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                                                  java.security.ProtectionDomain protectionDomain, byte[] classfileBuffer){
                                try{
                                    String name = className.replace('/', '.');
                                    java.lang.reflect.Method transformMethod = mindustryTransformerClass.getMethod(
                                        "transformClass",
                                        mixinEnvironmentClass,
                                        String.class,
                                        byte[].class
                                    );
                                    byte[] result = (byte[])transformMethod.invoke(transformerProvider, defaultEnv, name, classfileBuffer);
                                    return result != null ? result : classfileBuffer;
                                }catch(Exception e){
                                }
                                return classfileBuffer;
                            }
                        };

                        inst.addTransformer(transformer);
                        System.out.println("[MixinAgent] ✓ Installed Mixin ClassFileTransformer");
                    }else{
                        System.err.println("[MixinAgent] WARNING: getMixinTransformer() returned null");
                    }
                }else{
                    System.err.println("[MixinAgent] WARNING: getTransformerProvider() returned null");
                }
            } catch (Exception e) {
                System.err.println("[MixinAgent] Failed to install ClassFileTransformer:");
                e.printStackTrace();
            }

        } catch (Exception e) {
            System.err.println("[MixinAgent] Failed to initialize Mixin:");
            e.printStackTrace();
        }

        System.out.println("[MixinAgent] Premain complete");
    }

    public static Instrumentation getInstrumentation() {
        return instrumentation;
    }
}
