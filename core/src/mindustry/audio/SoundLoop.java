package mindustry.audio;

import arc.*;
import arc.audio.*;
import arc.math.*;
import arc.util.*;

/**
 * A simple class for playing a looping sound at a position.
 */
public class SoundLoop {
    private static final float FADE_SPEED = 0.05f;

    private final Sound sound;
    private int id = -1;
    private float volume, baseVolume;

    public SoundLoop(Sound sound, float baseVolume) {
        this.sound = sound;
        this.baseVolume = baseVolume;
    }

    public void update(float x, float y, boolean play) {
        update(x, y, play, 1f);
    }

    public void update(float x, float y, boolean play, float volumeScale) {
        if (baseVolume <= 0) return;

        if (id < 0) {
            if (play) {
                id = sound.loop(sound.calcVolume(x, y) * volume * baseVolume * volumeScale, 1f, sound.calcPan(x, y));
            }
        } else {
            // Fade the sound in or out
            if (play) {
                volume = Mathf.clamp(volume + FADE_SPEED * Time.delta);
            } else {
                volume = Mathf.clamp(volume - FADE_SPEED * Time.delta);
                if (volume <= 0.001f) {
                    Core.audio.stop(id);
                    id = -1;
                    return;
                }
            }

            Core.audio.set(id, sound.calcPan(x, y), sound.calcVolume(x, y) * volume * baseVolume * volumeScale);
        }
    }

    public void stop() {
        if (id != -1) {
            Core.audio.stop(id);
            id = -1;
            volume = baseVolume = -1;
        }
    }
}
