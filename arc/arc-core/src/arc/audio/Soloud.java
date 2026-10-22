package arc.audio;

/** JNI bindings for the Soloud library. */
public class Soloud{

    /*JNI
    #include "soloud.h"
    #include "soloud_file.h"
    #include "soloud_wav.h"
    #include "soloud_wavstream.h"
    #include "soloud_bus.h"
    #include "soloud_thread.h"
    #include "soloud_filter.h"
    #include "soloud_biquadresonantfilter.h"
    #include "soloud_echofilter.h"
    #include "soloud_lofifilter.h"
    #include "soloud_flangerfilter.h"
    #include "soloud_waveshaperfilter.h"
    #include "soloud_bassboostfilter.h"
    #include "soloud_robotizefilter.h"
    #include "soloud_freeverbfilter.h"
    #include <stdio.h>

    using namespace SoLoud;

    Soloud soloud;

    void throwError(JNIEnv* env, int result){
        jclass excClass = env->FindClass("arc/util/ArcRuntimeException");
        env->ThrowNew(excClass, soloud.getErrorString(result));
    }

    */

    static native void init(); /*
        int result = soloud.init();

        if(result != 0) throwError(env, result);
    */

    static native void deinit(); /*
        soloud.deinit();
    */

    static native String backendString(); /*
        return env->NewStringUTF(soloud.getBackendString());
    */

    static native int backendId(); /*
        return soloud.getBackendId();
    */

    static native int backendChannels(); /*
        return soloud.getBackendChannels();
    */

    static native int backendSamplerate(); /*
        return soloud.getBackendSamplerate();
    */

    static native int backendBufferSize(); /*
        return soloud.getBackendBufferSize();
    */

    static native int version(); /*
        return soloud.getVersion();
    */

    static native int activeVoiceCount(); /*
        return soloud.getActiveVoiceCount();
    */

    static native void stopAll(); /*
        soloud.stopAll();
    */

    static native void pauseAll(boolean paused); /*
        soloud.setPauseAll(paused);
    */

    static native void biquadSet(long handle, int type, float frequency, float resonance); /*
        ((BiquadResonantFilter*)handle)->setParams(type, frequency, resonance);
    */

    static native void echoSet(long handle, float delay, float decay, float filter); /*
        ((EchoFilter*)handle)->setParams(delay, decay, filter);
    */

    static native void lofiSet(long handle, float sampleRate, float bitDepth); /*
        ((LofiFilter*)handle)->setParams(sampleRate, bitDepth);
    */

    static native void flangerSet(long handle, float delay, float frequency); /*
        ((FlangerFilter*)handle)->setParams(delay, frequency);
    */

    static native void waveShaperSet(long handle, float amount); /*
        ((WaveShaperFilter*)handle)->setParams(amount);
    */

    static native void bassBoostSet(long handle, float amount); /*
        ((BassboostFilter*)handle)->setParams( amount);
    */

    static native void robotizeSet(long handle, float freq, int waveform); /*
        ((RobotizeFilter*)handle)->setParams(freq, waveform);
    */

    static native void freeverbSet(long handle, float mode, float roomSize, float damp, float width); /*
        ((FreeverbFilter*)handle)->setParams(mode, roomSize, damp, width);
    */

    static native long filterBiquad(); /* return (jlong)(new BiquadResonantFilter()); */
    static native long filterEcho(); /* return (jlong)(new EchoFilter()); */
    static native long filterLofi(); /* return (jlong)(new LofiFilter()); */
    static native long filterFlanger(); /* return (jlong)(new FlangerFilter()); */
    static native long filterBassBoost(); /* return (jlong)(new BassboostFilter()); */
    static native long filterWaveShaper(); /* return (jlong)(new WaveShaperFilter()); */
    static native long filterRobotize(); /* return (jlong)(new RobotizeFilter()); */
    static native long filterFreeverb(); /* return (jlong)(new FreeverbFilter()); */

    static native void setGlobalFilter(int index, long handle); /*
        soloud.setGlobalFilter(index, ((Filter*)handle));
    */

    static native void filterFade(int voice, int filter, int attribute, float value, float timeSec); /*
        soloud.fadeFilterParameter(voice, filter, attribute, value, timeSec);
    */

    static native void filterSet(int voice, int filter, int attribute, float value); /*
        soloud.setFilterParameter(voice, filter, attribute, value);
    */

    static native long busNew(); /*
        return (jlong)(new Bus());
    */

    static native void idSeek(int id, float seconds); /*
        soloud.seek(id, seconds);
    */

    static native void idVolume(int id, float volume); /*
        soloud.setVolume(id, volume);
    */

    static native float idGetVolume(int id); /*
        return soloud.getVolume(id);
    */

    static native void idPan(int id, float pan); /*
        soloud.setPan(id, pan);
    */

    static native void idPitch(int id, float pitch); /*
        soloud.setRelativePlaySpeed(id, pitch);
    */

    static native void idPause(int id, boolean pause); /*
        soloud.setPause(id, pause);
    */

    static native boolean idGetPause(int voice); /*
        return soloud.getPause(voice);
    */

    static native void idProtected(int id, boolean protect); /*
        soloud.setProtectVoice(id, protect);
    */

    static native void idStop(int voice); /*
        soloud.stop(voice);
    */

    static native void idLooping(int voice, boolean looping); /*
        soloud.setLooping(voice, looping);
    */

    static native boolean idGetLooping(int voice); /*
        return soloud.getLooping(voice);
    */

    static native float idPosition(int voice); /*
        return (jfloat)soloud.getStreamPosition(voice);
    */

    static native boolean idValid(int voice); /*
        return soloud.isValidVoiceHandle(voice);
    */

    static native long wavLoadBytes(byte[] bytes, int length); /*
        Wav* wav = new Wav();

        int result = wav->loadMem((unsigned char*)bytes, length, true, true);

        if(result != 0) throwError(env, result);

        return (jlong)wav;
    */

    static native long wavLoadFile(String path); /*
        Wav* wav = new Wav();

        int result = wav->load(path);

        if(result != 0) throwError(env, result);

        return (jlong)wav;
    */

    static native long streamLoadBytes(byte[] bytes, int length); /*
        WavStream* stream = new WavStream();

        int result = stream->loadMem((unsigned char*)bytes, length, true, true);

        if(result != 0) throwError(env, result);

        return (jlong)stream;
    */

    static native long streamLoadFile(String path); /*
        WavStream* stream = new WavStream();

        int result = stream->load(path);

        if(result != 0) throwError(env, result);

        return (jlong)stream;
    */

    static native double streamLength(long handle); /*
        WavStream* source = (WavStream*)handle;
        return (jdouble)source->getLength();
    */

    static native double wavLength(long handle); /*
         Wav* source = (Wav*)handle;
         return (jdouble)source->getLength();
    */

    static native void sourceDestroy(long handle); /*
        AudioSource* source = (AudioSource*)handle;
        delete source;
    */

    static native void sourceInaudible(long handle, boolean tick, boolean play); /*
        AudioSource* wav = (AudioSource*)handle;
        wav->setInaudibleBehavior(tick, play);
    */

    static native int sourcePlay(long handle); /*
        AudioSource* wav = (AudioSource*)handle;
        return soloud.play(*wav);
    */

    static native int sourceCount(long handle); /*
        AudioSource* wav = (AudioSource*)handle;
        return soloud.countAudioSource(*wav);
    */

    static native int sourcePlay(long handle, float volume, float pitch, float pan, boolean loop); /*
        AudioSource* wav = (AudioSource*)handle;

        return soloud.play(*wav, volume, pan, pitch, false, loop);
    */

    static native int sourcePlayBus(long handle, long busHandle, float volume, float pitch, float pan, boolean loop); /*
        AudioSource* wav = (AudioSource*)handle;
        Bus* bus = (Bus*)busHandle;

        return bus->play(*wav, volume, pan, pitch, false, loop);
    */

    static native void sourcePriority(long handle, float priority); /*
        AudioSource* source = (AudioSource*)handle;
        source->setPriority(priority);
    */

    static native void sourceMinConcurrentInterrupt(long handle, float value); /*
        AudioSource* source = (AudioSource*)handle;
        source->setMinConcurrentInterrupt(value);
    */

    static native void sourceMaxConcurrent(long handle, int maxConcurrent); /*
        AudioSource* source = (AudioSource*)handle;
        source->setMaxConcurrent(maxConcurrent);
    */

    static native void sourceConcurrentGroup(long handle, int group); /*
        AudioSource* source = (AudioSource*)handle;
        source->setConcurrentGroup(group);
    */

    static native void sourceLoop(long handle, boolean loop); /*
        AudioSource* source = (AudioSource*)handle;
        source->setLooping(loop);
    */

    static native void sourceSingleInstance(long handle, boolean single); /*
        AudioSource* source = (AudioSource*)handle;
        source->setSingleInstance(single);
    */

    static native void sourceStop(long handle); /*
        AudioSource* source = (AudioSource*)handle;
        source->stop();
    */

    static native void sourceFilter(long handle, int index, long filter); /*
        ((AudioSource*)handle)->setFilter(index, ((Filter*)filter));
    */

    //iOS only functions below:

    static native int pauseDevice(); /*
        return soloud.pause();
    */

    static native int resumeDevice(); /*
        return soloud.resume();
    */
}
