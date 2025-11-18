package mindustry.mod.mixin;

import arc.util.Log;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.service.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MindustryMixinService extends MixinServiceAbstract implements IGlobalPropertyService{
    private MindustryClassProvider classProvider;
    private MindustryBytecodeProvider bytecodeProvider;
    private MindustryContainerHandle containerHandle;
    private MindustryTransformerService transformerProvider;
    private static final Map<String, Object> globalProperties = new ConcurrentHashMap<>();

    @Override
    public String getName(){
        return "Mindustry";
    }

    @Override
    public boolean isValid(){
        try{
            Class.forName("mindustry.Vars");
            return true;
        }catch(Throwable t){
            return false;
        }
    }

    @Override
    public void prepare(){
        System.out.println("[Mixin] Preparing Mindustry Mixin service");
        try{
            containerHandle = new MindustryContainerHandle();
        }catch(Exception e){
            System.err.println("[Mixin] Failed to create container handle");
            e.printStackTrace();
        }

    }

    @Override
    public MixinEnvironment.Phase getInitialPhase(){
        return MixinEnvironment.Phase.DEFAULT;
    }

    @Override
    public void init(){
        System.out.println("[Mixin] Initializing Mindustry Mixin service - creating providers");
        classProvider = new MindustryClassProvider();
        bytecodeProvider = new MindustryBytecodeProvider();
        transformerProvider = new MindustryTransformerService();
        System.out.println("[Mixin] MindustryMixinService init complete");
    }

    @Override
    public void beginPhase(){
    }

    @Override
    public void checkEnv(Object bootSource){
    }

    @Override
    public InputStream getResourceAsStream(String name){
        return classProvider.getResourceAsStream(name);
    }

    public void registerInvalidClass(String className){
    }

    public boolean isClassLoaded(String className){
        return classProvider.isClassLoaded(className);
    }

    public String getClassRestrictions(String className){
        return "";
    }

    @Override
    public IClassProvider getClassProvider(){
        return classProvider;
    }

    @Override
    public IClassBytecodeProvider getBytecodeProvider(){
        return bytecodeProvider;
    }

    @Override
    public ITransformerProvider getTransformerProvider(){
        return transformerProvider;
    }

    /**
     * Get the transformer service for manual class transformation
     */
    public MindustryTransformerService getMindustryTransformerProvider(){
        return transformerProvider;
    }

    @Override
    public IClassTracker getClassTracker(){
        return null;
    }

    @Override
    public IMixinAuditTrail getAuditTrail(){
        return null;
    }

    @Override
    public Collection<String> getPlatformAgents(){
        return Collections.emptyList();
    }

    @Override
    public IContainerHandle getPrimaryContainer(){
        return containerHandle;
    }

    @Override
    public Collection<IContainerHandle> getMixinContainers(){
        return Collections.emptyList();
    }

    @Override
    protected ILogger createLogger(String name){
        return new MindustryMixinLogger(name);
    }

    @Override
    public IPropertyKey resolveKey(String name){
        return new PropertyKey(name);
    }

    @Override
    public <T> T getProperty(IPropertyKey key){
        return (T)globalProperties.get(((PropertyKey)key).name);
    }

    @Override
    public void setProperty(IPropertyKey key, Object value){
        globalProperties.put(((PropertyKey)key).name, value);
    }

    @Override
    public <T> T getProperty(IPropertyKey key, T defaultValue){
        T value = getProperty(key);
        return value != null ? value : defaultValue;
    }

    @Override
    public String getPropertyString(IPropertyKey key, String defaultValue){
        Object value = getProperty(key);
        return value != null ? value.toString() : defaultValue;
    }

    private static class PropertyKey implements IPropertyKey{
        final String name;

        PropertyKey(String name){
            this.name = name;
        }

        @Override
        public String toString(){
            return name;
        }
    }

    private static class MindustryMixinLogger implements ILogger{
        private final String name;

        MindustryMixinLogger(String name){
            this.name = name;
        }

        @Override
        public String getType(){
            return name;
        }

        @Override
        public String getId(){
            return name;
        }

        @Override
        public void catching(Throwable t){
            Log.err("[Mixin:" + name + "]", t);
        }

        @Override
        public void catching(org.spongepowered.asm.logging.Level level, Throwable t){
            log(level, "Caught exception", t);
        }

        @Override
        public void debug(String message, Object... params){
            Log.debug("[Mixin:" + name + "] " + format(message, params));
        }

        @Override
        public void error(String message, Object... params){
            Log.err("[Mixin:" + name + "] " + format(message, params));
        }

        @Override
        public void fatal(String message, Object... params){
            Log.err("[Mixin:" + name + "] FATAL: " + format(message, params));
        }

        @Override
        public void info(String message, Object... params){
            Log.info("[Mixin:" + name + "] " + format(message, params));
        }

        @Override
        public void info(String message, Throwable throwable){
            Log.info("[Mixin:" + name + "] " + message);
        }

        @Override
        public void debug(String message, Throwable throwable){
            Log.debug("[Mixin:" + name + "] " + message, throwable);
        }

        @Override
        public void error(String message, Throwable throwable){
            Log.err("[Mixin:" + name + "] " + message, throwable);
        }

        @Override
        public void fatal(String message, Throwable throwable){
            Log.err("[Mixin:" + name + "] FATAL: " + message, throwable);
        }

        @Override
        public void log(org.spongepowered.asm.logging.Level level, String message, Object... params){
            String formatted = format(message, params);
            switch(level){
                case FATAL:
                case ERROR:
                    Log.err("[Mixin:" + name + "] " + formatted);
                    break;
                case WARN:
                    Log.warn("[Mixin:" + name + "] " + formatted);
                    break;
                case INFO:
                    Log.info("[Mixin:" + name + "] " + formatted);
                    break;
                case DEBUG:
                case TRACE:
                    Log.debug("[Mixin:" + name + "] " + formatted);
                    break;
            }
        }

        @Override
        public void log(org.spongepowered.asm.logging.Level level, String message, Throwable throwable){
            switch(level){
                case FATAL:
                case ERROR:
                    Log.err("[Mixin:" + name + "] " + message, throwable);
                    break;
                case WARN:
                    Log.warn("[Mixin:" + name + "] " + message, throwable);
                    break;
                case INFO:
                    Log.info("[Mixin:" + name + "] " + message);
                    break;
                case DEBUG:
                case TRACE:
                    Log.debug("[Mixin:" + name + "] " + message);
                    break;
            }
        }

        @Override
        public void trace(String message, Object... params){
            Log.debug("[Mixin:" + name + "] " + format(message, params));
        }

        @Override
        public void trace(String message, Throwable throwable){
            Log.debug("[Mixin:" + name + "] " + message, throwable);
        }

        @Override
        public void warn(String message, Object... params){
            Log.warn("[Mixin:" + name + "] " + format(message, params));
        }

        @Override
        public void warn(String message, Throwable throwable){
            Log.warn("[Mixin:" + name + "] " + message, throwable);
        }

        @Override
        public <T extends Throwable> T throwing(T t){
            Log.err("[Mixin:" + name + "]", t);
            return t;
        }

        private String format(String message, Object... params){
            if(params == null || params.length == 0) return message;
            return String.format(message.replace("{}", "%s"), params);
        }
    }
}
