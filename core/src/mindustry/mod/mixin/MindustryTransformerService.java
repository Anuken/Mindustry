package mindustry.mod.mixin;

import arc.util.Log;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.transformer.IMixinTransformer;
import org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory;
import org.spongepowered.asm.service.ITransformer;
import org.spongepowered.asm.service.ITransformerProvider;

import java.util.Collection;
import java.util.Collections;

public class MindustryTransformerService implements ITransformerProvider{
    private IMixinTransformer transformer;

    @Override
    public Collection<ITransformer> getTransformers(){
        return Collections.emptyList();
    }

    @Override
    public Collection<ITransformer> getDelegatedTransformers(){
        return Collections.emptyList();
    }

    @Override
    public void addTransformerExclusion(String name){
        // No exclusions needed for now
    }

    /**
     * Get the Mixin transformer instance
     */
    public IMixinTransformer getMixinTransformer(){
        if(transformer == null){
            try{
                Class<?> factoryClass = Class.forName("org.spongepowered.asm.mixin.transformer.IMixinTransformerFactory");
                java.lang.reflect.Method factoryMethod = factoryClass.getMethod("createTransformer");
                transformer = (IMixinTransformer)factoryMethod.invoke(null);
                Log.info("[Mixin] Created Mixin transformer via factory");
            }catch(Exception e){
                try{
                    java.lang.reflect.Constructor<?> constructor = Class
                        .forName("org.spongepowered.asm.mixin.transformer.MixinTransformer")
                        .getDeclaredConstructor();
                    constructor.setAccessible(true);
                    transformer = (IMixinTransformer)constructor.newInstance();
                    Log.info("[Mixin] Created Mixin transformer via reflection");
                }catch(Exception e2){
                    Log.err("[Mixin] Failed to create Mixin transformer", e2);
                }
            }
        }
        return transformer;
    }

    /**
     * Transform a class using Mixin
     */
    public byte[] transformClass(MixinEnvironment environment, String name, byte[] classBytes){
        if(transformer == null){
            getMixinTransformer();
        }

        if(transformer != null){
            try{
                return transformer.transformClassBytes(name, name, classBytes);
            }catch(Exception e){
                Log.err("[Mixin] Failed to transform class @", name, e);
            }
        }

        return classBytes;
    }
}
