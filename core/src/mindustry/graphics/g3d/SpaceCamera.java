package mindustry.graphics.g3d;

import arc.math.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.type.*;

import static mindustry.Vars.*;

/** Camera state for the campaign space scene. */
public class SpaceCamera{
    /** Name of the planet the camera is attached to, or the body used for the scene's solar system. */
    public @Nullable String planet;
    /** If true, the camera orbits {@link #planet}; otherwise it uses the free position and angles. */
    public boolean attached = true;
    /** If true, the map background keeps advancing this camera after it is saved. */
    public boolean updateInGame = false;
    /** Per-planet cloud visibility. Missing entries are visible. */
    public ObjectSet<String> hiddenClouds = new ObjectSet<>();
    /** Per-planet orbit/rotation speeds in degrees per second. Missing entries are 0. */
    public ObjectFloatMap<String> orbitSpeeds = new ObjectFloatMap<>();
    public ObjectFloatMap<String> rotationSpeeds = new ObjectFloatMap<>();
    /** Per-planet cloud rotation multipliers and accumulated clocks. Missing speeds default to 1. */
    public ObjectFloatMap<String> cloudSpeeds = new ObjectFloatMap<>();
    public ObjectFloatMap<String> cloudTimes = new ObjectFloatMap<>();
    /** Free-camera position, in scene units. */
    public Vec3 position = new Vec3(0f, 0f, 8f);
    /** Free-camera yaw/pitch, in radians. */
    public float yaw, pitch;
    /** Attached-camera orbit angles and zoom. */
    public float lon = 0.55f, lat = 0.28f, zoom = 1f;
    private transient PlanetParams cachedParams;

    /** Saved orbit angles by planet name, in degrees. */
    public ObjectFloatMap<String> orbitAngles = new ObjectFloatMap<>();
    /** Saved rotation angles by planet name, in degrees. */
    public ObjectFloatMap<String> rotationAngles = new ObjectFloatMap<>();

    public @Nullable Planet planet(){
        Planet found = planet == null ? null : content.planet(planet);
        return found == null ? Planets.serpulo : found;
    }

    /** Applies this camera to the supplied render parameters. */
    public void apply(PlanetParams params){
        params.planet = planet();
        params.spaceCamera = this;
        params.otherCamPos = null;

        params.uiAlpha = 1f;

        if(attached){
            params.freeCamera = false;
            params.zoom = zoom;

            float cy = Mathf.cos(lat);
            params.camPos.set(cy * Mathf.cos(lon), Mathf.sin(lat), cy * Mathf.sin(lon));
        }else{
            params.freeCamera = true;
            params.freeCamPos.set(position);
            params.freeCamYaw = yaw;
            params.freeCamPitch = pitch;
        }
    }

    public PlanetParams params(){
        if(cachedParams == null){
            cachedParams = new PlanetParams();
        }
        apply(cachedParams);
        return cachedParams;
    }

    /** Snapshots the current orbit/rotation of every body in the selected solar system. */
    public void captureAngles(Planet body){
        orbitAngles.clear();
        rotationAngles.clear();
        cloudTimes.clear();

        for(Planet other : content.planets()){
            if(other.solarSystem == body.solarSystem){
                if(other.parent != null){
                    orbitAngles.put(other.name, other.getOrbitAngle());
                }
                rotationAngles.put(other.name, other.getRotation());
                cloudTimes.put(other.name, Time.globalTime);
            }
        }
    }

    /** Ensures every body in the scene has a saved angle, without overwriting existing values. */
    public void fillAngles(Planet body){
        for(Planet other : content.planets()){
            if(other.solarSystem == body.solarSystem){
                if(other.parent != null && !orbitAngles.containsKey(other.name)){
                    orbitAngles.put(other.name, other.getOrbitAngle());
                }
                if(!rotationAngles.containsKey(other.name)){
                    rotationAngles.put(other.name, other.getRotation());
                }
                if(!cloudTimes.containsKey(other.name)){
                    cloudTimes.put(other.name, Time.globalTime);
                }
            }
        }
    }

    public float orbitAngle(Planet body){
        if(orbitAngles.containsKey(body.name)) return orbitAngles.get(body.name, 0f);
        return body.parent == null ? 0f : body.getOrbitAngle();
    }

    public float rotationAngle(Planet body){
        return rotationAngles.get(body.name, body.getRotation());
    }

    public boolean clouds(Planet body){
        return !hiddenClouds.contains(body.name);
    }

    public void clouds(Planet body, boolean value){
        if(value){
            hiddenClouds.remove(body.name);
        }else{
            hiddenClouds.add(body.name);
        }
    }

    public float orbitSpeed(Planet body){
        return orbitSpeeds.get(body.name, 0f);
    }

    public void orbitSpeed(Planet body, float value){
        orbitSpeeds.put(body.name, value);
    }

    public float rotationSpeed(Planet body){
        return rotationSpeeds.get(body.name, 0f);
    }

    public void rotationSpeed(Planet body, float value){
        rotationSpeeds.put(body.name, value);
    }

    public float cloudSpeed(Planet body){
        return cloudSpeeds.get(body.name, 1f);
    }

    public void cloudSpeed(Planet body, float value){
        cloudSpeeds.put(body.name, value);
    }

    public float cloudTime(Planet body){
        return cloudTimes.get(body.name, 0f);
    }

    public boolean hasMovement(){
        Planet body = planet();
        for(Planet other : content.planets()){
            if(other.solarSystem != body.solarSystem) continue;
            if(orbitSpeed(other) != 0f || rotationSpeed(other) != 0f) return true;
            if(other.cloudMesh != null && clouds(other) && cloudSpeed(other) != 0f) return true;
        }
        return false;
    }

    /** Advances the saved orbit/rotation angles by delta seconds. */
    public void update(float delta){
        if(delta <= 0f) return;

        Planet body = planet();
        for(Planet other : content.planets()){
            if(other.solarSystem != body.solarSystem) continue;

            float orbit = orbitSpeed(other);
            if(other.parent != null && orbit != 0f){
                orbitAngles.put(other.name, normalize(orbitAngle(other) + orbit * delta));
            }

            float rotation = rotationSpeed(other);
            if(rotation != 0f){
                rotationAngles.put(other.name, normalize(rotationAngle(other) + rotation * delta));
            }

            cloudTimes.put(other.name, cloudTime(other) + delta * 60f * cloudSpeed(other));
        }
    }

    private static float normalize(float angle){
        angle %= 360f;
        return angle < 0f ? angle + 360f : angle;
    }
}
