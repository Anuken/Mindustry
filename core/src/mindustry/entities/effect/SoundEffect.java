package mindustry.entities.effect;

import arc.*;
import arc.audio.*;
import arc.func.*;
import arc.graphics.*;
import arc.math.*;
import arc.struct.*;
import mindustry.entities.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;

/** Plays a sound effect when created and simultaneously renders an effect. */
public class SoundEffect extends Effect{
    private static final Seq<Runnable> loadQueue = new Seq<>();

    public Sound sound = Sounds.none;
    public float minPitch = 0.8f;
    public float maxPitch = 1.2f;
    public float minVolume = 1f;
    public float maxVolume = 1f;
    public Effect effect;

    static{
        Events.on(FileTreeInitEvent.class, e -> {
            loadQueue.each(Runnable::run);
            loadQueue.clear();
        });
    }

    public SoundEffect(){
    }

    public SoundEffect(Sound sound, Effect effect){
        this.sound = sound;
        this.effect = effect;
    }

    public SoundEffect(Prov<Sound> soundLoader, Effect effect){
        loadQueue.add(() -> this.sound = soundLoader.get());
        this.effect = effect;
    }

    @Override
    public void create(float x, float y, float rotation, Color color, Object data){
        if(!shouldCreate()) return;

        sound.at(x, y, Mathf.range(minPitch, maxPitch), Mathf.range(minVolume, maxVolume));
        effect.create(x, y, rotation, color, data);
    }
}
