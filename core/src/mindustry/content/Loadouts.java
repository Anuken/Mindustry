package mindustry.content;

import mindustry.game.*;

public class Loadouts{
    public static Schematic
    basicShard,
    basicFoundation,
    basicNucleus,
    basicBastion;

    public static void load(){
        basicShard = Schematics.readBase64("bXNjaAF4nGNgZmBmZmDJS8xNZZDJKCkpKLbS16/MLy0p1UtK1XcNi/Q3cKwwyqkyYOBOSS1OLsosKMnMz2NgYGDLSUxKzSlmYIqOZWTgSs4vStUtzkgsSgFKMYIQkAAAhSEXTA==");
        basicFoundation = Schematics.readBase64("bXNjaAF4nGNgYWBhZmDJS8xNZWBNSk3MK2bgTkktTi7KLCjJzM9jYGBgy0lMSs0pZmCKjmVk4E/OL0rVTcsvzUtJhMozghCQAACx6RHB");
        basicNucleus = Schematics.readBase64("bXNjaAF4nA3CwQ2AIBAEwAXFjxRBA1ZkfCDcgwh3BiTG7iUzMDATZvaFYGOK7pPuLpYXa6QWarqfJAxVsGR/Um7Q+6Fgg1TauIdMvQFQgB7wAza8E4M=");
        basicBastion = Schematics.readBase64("bXNjaAF4nGNgYWBhZmDJS8xNZWBNzMsEUtwpqcXJRZkFJZn5eQyClfmlCin5Cnn5JQqpFZnFJVwMbDmJSak5xQxM0bGMDDzJ+UWpukmJxWDVDAyMIAQkACMdFqE=");
    }
}
