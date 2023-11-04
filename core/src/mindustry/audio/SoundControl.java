package mindustry.audio;

import arc.*;
import arc.audio.*;
import arc.audio.Filters.*;
import arc.files.Fi;
import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.game.EventType.*;
import mindustry.gen.*;

import static mindustry.Vars.*;

/**
 * Controls playback of multiple audio tracks and looping sounds.
 */
public class SoundControl {
    // Configuration parameters
    public float finTime = 120f, foutTime = 120f, musicInterval = 3f * Time.toMinutes, musicChance = 0.6f, musicWaveChance = 0.46f;

    // Music sequences
    public Seq<Music> ambientMusic = Seq.with(); // Normal ambient music
    public Seq<Music> darkMusic = Seq.with(); // Darker music for conflict
    public Seq<Music> bossMusic = Seq.with(); // Music for boss events

    // Internal variables
    protected Music lastRandomPlayed;
    protected Interval timer = new Interval(4);
    protected @Nullable Music current;
    protected float fade;
    protected boolean silenced;
    protected AudioBus uiBus = new AudioBus();
    protected boolean wasPlaying;
    protected AudioFilter filter = new BiquadFilter() {{
        set(0, 500, 1);
    }};
    protected ObjectMap<Sound, SoundData> sounds = new ObjectMap<>();

    public SoundControl() {
        Events.on(ClientLoadEvent.class, e -> reload());
        Events.on(WaveEvent.class, e -> Time.run(Mathf.random(8f, 15f) * 60f, () -> {
            boolean boss = state.rules.spawns.contains(group -> group.getSpawned(state.wave - 2) > 0 && group.effect == StatusEffects.boss);
            if (boss) {
                playOnce(bossMusic.random(lastRandomPlayed));
            } else if (Mathf.chance(musicWaveChance)) {
                playRandom();
            }
        }));
        setupFilters();
    }

    protected void setupFilters() {
        Core.audio.soundBus.setFilter(0, filter);
        Core.audio.soundBus.setFilterParam(0, Filters.paramWet, 0f);
    }

    protected void reload() {
        current = null;
        fade = 0f;
        ambientMusic = Seq.with(Musics.game1, Musics.game3, Musics.game6, Musics.game8, Musics.game9, Musics.fine);
        darkMusic = Seq.with(Musics.game2, Musics.game5, Musics.game7, Musics.game4);
        bossMusic = Seq.with(Musics.boss1, Musics.boss2, Musics.game2, Musics.game5);

        for (var sound : Core.assets.getAll(Sound.class, new Seq<>())) {
            var file = Fi.get(Core.assets.getAssetFileName(sound));
            if (file.parent().name().equals("ui")) {
                sound.setBus(uiBus);
            }
        }

        Events.fire(new MusicRegisterEvent());
    }

    public void loop(Sound sound, float volume) {
        if (Vars.headless) return;

        loop(sound, Core.camera.position, volume);
    }

    public void loop(Sound sound, Position pos, float volume) {
        if (Vars.headless) return;

        float baseVol = sound.calcFalloff(pos.getX(), pos.getY());
        float vol = baseVol * volume;

        SoundData data = sounds.get(sound, SoundData::new);
        data.volume += vol;
        data.volume = Mathf.clamp(data.volume, 0f, 1f);
        data.total += baseVol;
        data.sum.add(pos.getX() * baseVol, pos.getY() * baseVol);
    }

    public void stop() {
        silenced = true;
        if (current != null) {
            current.stop();
            current = null;
            fade = 0f;
        }
    }

    public void update() {
        boolean paused = state.isGame() && Core.scene.hasDialog();
        boolean playing = state.isGame();

        if (current != null && !current.isPlaying()) {
            current = null;
            fade = 0f;
        }

        if (timer.get(1, 30f)) {
            Core.audio.soundBus.fadeFilterParam(0, Filters.paramWet, paused ? 1f : 0f, 0.4f);
        }

        if (playing != wasPlaying) {
            wasPlaying = playing;

            if (playing) {
                Core.audio.soundBus.play();
                setupFilters();
            } else {
                Core.audio.soundBus.stop();
                Core.audio.musicBus.play();
                Core.audio.soundBus.play();
            }
        }

        Core.audio.setPaused(Core.audio.soundBus.id, state.isPaused());

        if (state.isMenu()) {
            silenced = false;
            if (ui.planet.isShown()) {
                play(Musics.launch);
            } else if (ui.editor.isShown()) {
                play(Musics.editor);
            } else {
                play(Musics.menu);
            }
        } else if (state.rules.editor) {
            silenced = false;
            play(Musics.editor);
        } else {
            silence();

            if (timer.get(musicInterval)) {
                if (Mathf.chance(musicChance)) {
                    playRandom();
                }
            }
        }

        updateLoops();
    }

    protected void updateLoops() {
        if (!state.isGame()) {
            sounds.clear();
            return;
        }

        float avol = Core.settings.getInt("ambientvol", 100) / 100f;

        sounds.each((sound, data) -> {
            data.curVolume = Mathf.lerpDelta(data.curVolume, data.volume * avol, 0.11f);

            boolean play = data.curVolume > 0.01f;
            float pan = Mathf.zero(data.total, 0.0001f) ? 0f : sound.calcPan(data.sum.x / data.total, data.sum.y / data.total);
            if (data.soundID <= 0 || !Core.audio.isPlaying(data.soundID)) {
                if (play) {
                    data.soundID = sound.loop(data.curVolume, 1f, pan);
                    Core.audio.protect(data.soundID, true);
                }
            } else {
                if (data.curVolume <= 0.001f) {
                    sound.stop();
                    data.soundID = -1;
                    return;
                }
                Core.audio.set(data.soundID, pan, data.curVolume);
            }

            data.volume = 0f;
            data.total = 0f;
            data.sum.setZero();
        });
    }

    public void playRandom() {
        if (isDark()) {
            playOnce(darkMusic.random(lastRandomPlayed));
        } else {
            playOnce(ambientMusic.random(lastRandomPlayed));
        }
    }

    protected boolean isDark() {
        if (player.team().data().hasCore() && player.team().data().core().healthf() < 0.85f) {
            return true;
        }

        if (Mathf.chance((float) (Math.log10((state.wave - 17f) / 19f) + 1) / 4f)) {
            return true;
        }

        return Mathf.chance(state.enemies / 70f + 0.1f);
    }

    protected void play(@Nullable Music music) {
        if (shouldPlay()) {
            if (current != null) {
                current.setVolume(0);
            }

            fade = 0f;
            return;
        }

        if (current != null) {
            current.setVolume(fade * Core.settings.getInt("musicvol") / 100f);
        }

        if (silenced) {
            return;
        }

        if (current == null && music != null) {
            current = music;
            current.setLooping(true);
            current.setVolume(fade = 0f);
            current.play();
            silenced = false;
        } else if (current == music && music != null) {
            fade = Mathf.clamp(fade + Time.delta / finTime);
        } else if (current != null) {
            fade = Mathf.clamp(fade - Time.delta / foutTime);

            if (fade <= 0.01f) {
                current.stop();
                current = null;
                silenced = true;
                if (music != null) {
                    current = music;
                    current.setVolume(fade = 0f);
                    current.setLooping(true);
                    current.play();
                    silenced = false;
                }
            }
        }
    }

    protected void playOnce(Music music) {
        if (current != null || music == null || shouldPlay()) return;

        lastRandomPlayed = music;
        fade = 1f;
        current = music;
        current.setVolume(1f);
        current.setLooping(false);
        current.play();
    }

    protected boolean shouldPlay() {
        return Core.settings.getInt("musicvol") <= 0;
    }

    protected void silence() {
        play(null);
    }

    protected static class SoundData {
        float volume;
        float total;
        Vec2 sum = new Vec2();
        int soundID;
        float curVolume;
    }
}
