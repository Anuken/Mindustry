package mindustry.maps;

import arc.struct.*;
import arc.util.*;
import mindustry.content.*;
import mindustry.type.*;

/** Class for temporarily (?) storing links to map submissions on Discord. */
public class SectorSubmissions{
    private static ObjectMap<Sector, MapSubmission> threadMap = new ObjectMap<>();

    /** @return the link to the Discord discussion thread of the specified hidden sector submission. */
    public static @Nullable String getSectorThread(Sector sector){
        if(sector.generateEnemyBase){
            var sub = threadMap.get(sector);
            if(sub == null){
                return null;
            }
            return sub.mapFileLink.isEmpty() ? sub.threadLink : sub.mapFileLink;
        }
        return null;
    }

    public static void registerSectors(){

        registerSerpuloSector(76, "Skyon", "https://discord.com/channels/391020510269669376/1379926833411391580/1381831690183250056");
        registerSerpuloSector(47, "tinport", "https://discord.com/channels/391020510269669376/1379926802591645820/1397649518203371544");
        registerSerpuloSector(225, "Summi", "https://discord.com/channels/391020510269669376/1379926925719376152/1399286858482978900");
        //111 has an alternate submission https://discord.com/channels/391020510269669376/1379926842659569864/1404825715244793938
        registerSerpuloSector(111, "gausofid", "https://discord.com/channels/391020510269669376/1379926842659569864/1399847451527221301");
        registerSerpuloSector(176, "wpx", "https://discord.com/channels/391020510269669376/1379926887203213353/1390418885081043135");
        registerSerpuloSector(13, "hoijlhj", "https://discord.com/channels/391020510269669376/1379926785164312810/1402569635299065948");
        registerSerpuloSector(259, "tinport", "https://discord.com/channels/391020510269669376/1379928048245280871/1381300770866987049");
        registerSerpuloSector(192, "Skeledragon", "https://discord.com/channels/391020510269669376/1379926914122256449/1380767793989029923");
        registerSerpuloSector(127, "playfree", "https://discord.com/channels/391020510269669376/1379926869465632829/1380253194428354602");
        registerSerpuloSector(207, "cyan", "https://discord.com/channels/391020510269669376/1379926923370827827/1405919087883976837");
        registerSerpuloSector(94, "Wine", "https://discord.com/channels/391020510269669376/1379926838079393802/1406267666976477196");
        registerSerpuloSector(16, "Namero", "https://discord.com/channels/391020510269669376/1379926788280680579/1409970152283312352");
        registerSerpuloSector(116, "Jamespire", "https://discord.com/channels/391020510269669376/1379926845058711734/1404131805074034712");
        registerSerpuloSector(69, "Oct", "https://discord.com/channels/391020510269669376/1379926831326822610/1406230980120940556");
        registerSerpuloSector(92, "Eggypc/Fish", "https://discord.com/channels/391020510269669376/1379926835621527615/1381929336860442731");
        registerSerpuloSector(197, "Hengryton Luck", "https://discord.com/channels/391020510269669376/1379926916911599676/1411358089759817793");
        registerSerpuloSector(67, "Ếch ngồi đáy giếng", "https://discord.com/channels/391020510269669376/1379926828696866898/1389981795386396768");
        //180 has an alternate submission that may be more appropriate in terms of difficulty: https://discord.com/channels/391020510269669376/1379926889648619580/1411534650412892185
        registerSerpuloSector(180, "Locla^Glass", "https://discord.com/channels/391020510269669376/1379926889648619580/1413522098370117765");
        registerSerpuloSector(55, "Namero", "https://discord.com/channels/391020510269669376/1379926823277695189/1412588965256761402");
        registerSerpuloSector(19, "Hengryton Luck", "https://discord.com/channels/391020510269669376/1379926792479183019/1411342610525585468");
        registerSerpuloSector(200, "Axye", "https://discord.com/channels/391020510269669376/1379926918429806755/1398949995041914933");
        //191 has several alternate submissions
        registerSerpuloSector(191, "Skeledragon", "https://discord.com/channels/391020510269669376/1379926912004001914/1380025875806027849");
        //alternate, more difficult submission: https://discord.com/channels/391020510269669376/1379926782966497322/1416145231853781022
        registerSerpuloSector(6, "Namero", "https://discord.com/channels/391020510269669376/1379926782966497322/1415735385828495464");
        registerSerpuloSector(265, "Dem0", "https://discord.com/channels/391020510269669376/1379928052921929891/1416344658904088641");
        registerSerpuloSector(161, "Hengryton Luck", "https://discord.com/channels/391020510269669376/1379926882203730024/1416686287204782217");
        registerSerpuloSector(24, "Stormrider", "https://discord.com/channels/391020510269669376/1379926797042581716/1399404131520876577");
        registerSerpuloSector(263, "ltb12", "https://discord.com/channels/391020510269669376/1379928050010951694/1417750251741249569");
        registerSerpuloSector(66, "quad", "https://discord.com/channels/391020510269669376/1379926825941078128/1417752983889907755");
        registerSerpuloSector(248, "iqtik123", "https://discord.com/channels/391020510269669376/1379926979129774151/1417864622412922890");
        registerSerpuloSector(133, "wpx", "https://discord.com/channels/391020510269669376/1379926871227240770/1417920499761156126");

        /* UNUSED SECTORS:
        registerHiddenSectors(serpulo,
        68, //Winter Forest by wpx: https://discord.com/channels/391020510269669376/1165421701362897000/1235654407006322700
        241,//River Bastion by wpx: https://discord.com/channels/391020510269669376/1165421701362897000/1232658317126402050
        173,//Front Line by stormrider: https://discord.com/channels/391020510269669376/1165421701362897000/1188484967064404061
        12, //Salt Outpost by skeledragon: https://discord.com/channels/391020510269669376/1165421701362897000/1193441915459338361
        106,//Desert Wastes by xaphiro_: https://discord.com/channels/391020510269669376/1165421701362897000/1226498922898264157
        243,//Port 012 by skeledragon: https://discord.com/channels/391020510269669376/1165421701362897000/1174884280242012262
        240 //Cold Grove by wpx: https://discord.com/channels/391020510269669376/1165421701362897000/1230550892718194742
        );*/
    }

    static void registerSerpuloSector(int id, String author, String mapFileLink){
        Planet planet = Planets.serpulo;
        Sector sector = planet.sectors.get(id);
        MapSubmission sub = threadMap.get(sector, MapSubmission::new);

        sub.author = author;
        sub.mapFileLink = mapFileLink;

        new SectorPreset("sector-" + planet.name + "-" + id, "hidden-serpulo/" + id, planet, id){{
            requireUnlock = false;
        }};

        sector.generateEnemyBase = true;
    }

    static void registerThread(int id, String link){
        var sub = new MapSubmission();
        sub.threadLink = link;
        threadMap.put(Planets.serpulo.sectors.get(id), sub);
    }

    static{
        //autogenerated
        registerThread(0, "https://discord.com/channels/391020510269669376/1379926780860698784");
        registerThread(6, "https://discord.com/channels/391020510269669376/1379926782966497322");
        registerThread(13, "https://discord.com/channels/391020510269669376/1379926785164312810");
        registerThread(16, "https://discord.com/channels/391020510269669376/1379926788280680579");
        registerThread(19, "https://discord.com/channels/391020510269669376/1379926792479183019");
        registerThread(20, "https://discord.com/channels/391020510269669376/1379926794114961634");
        registerThread(24, "https://discord.com/channels/391020510269669376/1379926797042581716");
        registerThread(27, "https://discord.com/channels/391020510269669376/1379926798833287289");
        registerThread(30, "https://discord.com/channels/391020510269669376/1379926800854945823");
        registerThread(47, "https://discord.com/channels/391020510269669376/1379926802591645820");
        registerThread(55, "https://discord.com/channels/391020510269669376/1379926823277695189");
        registerThread(66, "https://discord.com/channels/391020510269669376/1379926825941078128");
        registerThread(67, "https://discord.com/channels/391020510269669376/1379926828696866898");
        registerThread(69, "https://discord.com/channels/391020510269669376/1379926831326822610");
        registerThread(76, "https://discord.com/channels/391020510269669376/1379926833411391580");
        registerThread(92, "https://discord.com/channels/391020510269669376/1379926835621527615");
        registerThread(94, "https://discord.com/channels/391020510269669376/1379926838079393802");
        registerThread(103, "https://discord.com/channels/391020510269669376/1379926839559979030");
        registerThread(111, "https://discord.com/channels/391020510269669376/1379926842659569864");
        registerThread(116, "https://discord.com/channels/391020510269669376/1379926845058711734");
        registerThread(127, "https://discord.com/channels/391020510269669376/1379926869465632829");
        registerThread(133, "https://discord.com/channels/391020510269669376/1379926871227240770");
        registerThread(138, "https://discord.com/channels/391020510269669376/1379926873152164004");
        registerThread(150, "https://discord.com/channels/391020510269669376/1379926876457537547");
        registerThread(157, "https://discord.com/channels/391020510269669376/1379926879502598155");
        registerThread(161, "https://discord.com/channels/391020510269669376/1379926882203730024");
        registerThread(162, "https://discord.com/channels/391020510269669376/1379926884606808247");
        registerThread(176, "https://discord.com/channels/391020510269669376/1379926887203213353");
        registerThread(180, "https://discord.com/channels/391020510269669376/1379926889648619580");
        registerThread(185, "https://discord.com/channels/391020510269669376/1379926892181983283");
        registerThread(191, "https://discord.com/channels/391020510269669376/1379926912004001914");
        registerThread(192, "https://discord.com/channels/391020510269669376/1379926914122256449");
        registerThread(197, "https://discord.com/channels/391020510269669376/1379926916911599676");
        registerThread(200, "https://discord.com/channels/391020510269669376/1379926918429806755");
        registerThread(204, "https://discord.com/channels/391020510269669376/1379926921130807447");
        registerThread(207, "https://discord.com/channels/391020510269669376/1379926923370827827");
        registerThread(225, "https://discord.com/channels/391020510269669376/1379926925719376152");
        registerThread(230, "https://discord.com/channels/391020510269669376/1379926927585841163");
        registerThread(237, "https://discord.com/channels/391020510269669376/1379926929636851812");
        registerThread(242, "https://discord.com/channels/391020510269669376/1379926931923013843");
        registerThread(243, "https://discord.com/channels/391020510269669376/1379926955423694978");
        registerThread(244, "https://discord.com/channels/391020510269669376/1379926957738954762");
        registerThread(245, "https://discord.com/channels/391020510269669376/1379926971286290584");
        registerThread(246, "https://discord.com/channels/391020510269669376/1379926973454745600");
        registerThread(247, "https://discord.com/channels/391020510269669376/1379926976361533752");
        registerThread(248, "https://discord.com/channels/391020510269669376/1379926979129774151");
        registerThread(251, "https://discord.com/channels/391020510269669376/1379928042637361382");
        registerThread(254, "https://discord.com/channels/391020510269669376/1379928045577703424");
        registerThread(259, "https://discord.com/channels/391020510269669376/1379928048245280871");
        registerThread(263, "https://discord.com/channels/391020510269669376/1379928050010951694");
        registerThread(265, "https://discord.com/channels/391020510269669376/1379928052921929891");
    }

    public static class MapSubmission{
        public String author = "";
        public String threadLink = "";
        public String mapFileLink = "";
    }
}
