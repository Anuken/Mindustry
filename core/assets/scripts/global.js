//Generated class. Do not modify.

"use strict";

let scriptName = "base.js"
let modName = "none"

const log = (context, obj) => Vars.mods.scripts.log(context, String(obj))
const print = text => log(modName + "/" + scriptName, text)

const newFloats = cap => Vars.mods.getScripts().newFloats(cap);

//these are not strictly necessary, but are kept for edge cases
const run = method => new java.lang.Runnable(){run: method}
const boolf = method => new Boolf(){get: method}
const boolp = method => new Boolp(){get: method}
const floatf = method => new Floatf(){get: method}
const floatp = method => new Floatp(){get: method}
const cons = method => new Cons(){get: method}
const prov = method => new Prov(){get: method}
const func = method => new Func(){get: method}

const newEffect = (lifetime, renderer) => new Effect.Effect(lifetime, new Effect.EffectRenderer({render: renderer}))
Call = Packages.mindustry.gen.Call

//js 'extend(Base, ..., {})' = java 'new Base(...) {}'
function extend(/*Base, ..., def*/){
    const Base = arguments[0]
    const def = arguments[arguments.length - 1]
    //swap order from Base, def, ... to Base, ..., def
    const args = [Base, def].concat(Array.from(arguments).splice(1, arguments.length - 2))

    //forward constructor arguments to new JavaAdapter
    const instance = JavaAdapter.apply(null, args)
    //JavaAdapter only overrides functions; set fields too
    for(var i in def){
        if(typeof(def[i]) != "function"){
            instance[i] = def[i]
        }
    }
    return instance
}


importPackage(Packages.arc)
importPackage(Packages.arc.audio)
importPackage(Packages.arc.files)
importPackage(Packages.arc.func)
importPackage(Packages.arc.flabel)
importPackage(Packages.arc.graphics)
importPackage(Packages.arc.graphics.g2d)
importPackage(Packages.arc.graphics.gl)
importPackage(Packages.arc.input)
importPackage(Packages.arc.math)
importPackage(Packages.arc.math.geom)
importPackage(Packages.arc.scene)
importPackage(Packages.arc.scene.actions)
importPackage(Packages.arc.scene.event)
importPackage(Packages.arc.scene.style)
importPackage(Packages.arc.scene.ui)
importPackage(Packages.arc.scene.ui.layout)
importPackage(Packages.arc.scene.utils)
importPackage(Packages.arc.struct)
importPackage(Packages.arc.util)
importPackage(Packages.arc.util.async)
importPackage(Packages.arc.util.io)
importPackage(Packages.arc.util.noise)
importPackage(Packages.arc.util.pooling)
importPackage(Packages.arc.util.serialization)
importPackage(Packages.arc.util.viewport)
importPackage(Packages.mindustry)
importPackage(Packages.mindustry.ai)
importPackage(Packages.mindustry.ai.types)
importPackage(Packages.mindustry.async)
importPackage(Packages.mindustry.audio)
importPackage(Packages.mindustry.content)
importPackage(Packages.mindustry.core)
importPackage(Packages.mindustry.ctype)
importPackage(Packages.mindustry.editor)
importPackage(Packages.mindustry.entities)
importPackage(Packages.mindustry.entities.abilities)
importPackage(Packages.mindustry.entities.bullet)
importPackage(Packages.mindustry.entities.effect)
importPackage(Packages.mindustry.entities.part)
importPackage(Packages.mindustry.entities.pattern)
importPackage(Packages.mindustry.entities.units)
importPackage(Packages.mindustry.game)
importPackage(Packages.mindustry.gen)
importPackage(Packages.mindustry.graphics)
importPackage(Packages.mindustry.graphics.g3d)
importPackage(Packages.mindustry.input)
importPackage(Packages.mindustry.io)
importPackage(Packages.mindustry.logic)
importPackage(Packages.mindustry.maps)
importPackage(Packages.mindustry.maps.filters)
importPackage(Packages.mindustry.maps.generators)
importPackage(Packages.mindustry.maps.planet)
importPackage(Packages.mindustry.net)
importPackage(Packages.mindustry.service)
importPackage(Packages.mindustry.type)
importPackage(Packages.mindustry.type.ammo)
importPackage(Packages.mindustry.type.unit)
importPackage(Packages.mindustry.type.weapons)
importPackage(Packages.mindustry.type.weather)
importPackage(Packages.mindustry.ui)
importPackage(Packages.mindustry.ui.dialogs)
importPackage(Packages.mindustry.ui.fragments)
importPackage(Packages.mindustry.ui.layout)
importPackage(Packages.mindustry.world)
importPackage(Packages.mindustry.world.blocks)
importPackage(Packages.mindustry.world.blocks.campaign)
importPackage(Packages.mindustry.world.blocks.defense)
importPackage(Packages.mindustry.world.blocks.defense.turrets)
importPackage(Packages.mindustry.world.blocks.distribution)
importPackage(Packages.mindustry.world.blocks.environment)
importPackage(Packages.mindustry.world.blocks.heat)
importPackage(Packages.mindustry.world.blocks.legacy)
importPackage(Packages.mindustry.world.blocks.liquid)
importPackage(Packages.mindustry.world.blocks.logic)
importPackage(Packages.mindustry.world.blocks.payloads)
importPackage(Packages.mindustry.world.blocks.power)
importPackage(Packages.mindustry.world.blocks.production)
importPackage(Packages.mindustry.world.blocks.sandbox)
importPackage(Packages.mindustry.world.blocks.storage)
importPackage(Packages.mindustry.world.blocks.units)
importPackage(Packages.mindustry.world.consumers)
importPackage(Packages.mindustry.world.draw)
importPackage(Packages.mindustry.world.meta)
importPackage(Packages.mindustry.world.modules)
const AdminRequestEvent = Packages.mindustry.game.EventType.AdminRequestEvent
const PlayerIpUnbanEvent = Packages.mindustry.game.EventType.PlayerIpUnbanEvent
const PlayerIpBanEvent = Packages.mindustry.game.EventType.PlayerIpBanEvent
const PlayerUnbanEvent = Packages.mindustry.game.EventType.PlayerUnbanEvent
const PlayerBanEvent = Packages.mindustry.game.EventType.PlayerBanEvent
const PlayerLeave = Packages.mindustry.game.EventType.PlayerLeave
const PlayerConnect = Packages.mindustry.game.EventType.PlayerConnect
const PlayerJoin = Packages.mindustry.game.EventType.PlayerJoin
const PlayerConnectionConfirmed = Packages.mindustry.game.EventType.PlayerConnectionConfirmed
const ConnectPacketEvent = Packages.mindustry.game.EventType.ConnectPacketEvent
const ConnectionEvent = Packages.mindustry.game.EventType.ConnectionEvent
const UnitChangeEvent = Packages.mindustry.game.EventType.UnitChangeEvent
const UnitUnloadEvent = Packages.mindustry.game.EventType.UnitUnloadEvent
const UnitSpawnEvent = Packages.mindustry.game.EventType.UnitSpawnEvent
const UnitCreateEvent = Packages.mindustry.game.EventType.UnitCreateEvent
const UnitDrownEvent = Packages.mindustry.game.EventType.UnitDrownEvent
const UnitDestroyEvent = Packages.mindustry.game.EventType.UnitDestroyEvent
const BlockDestroyEvent = Packages.mindustry.game.EventType.BlockDestroyEvent
const BuildSelectEvent = Packages.mindustry.game.EventType.BuildSelectEvent
const BlockBuildEndEvent = Packages.mindustry.game.EventType.BlockBuildEndEvent
const BlockBuildBeginEvent = Packages.mindustry.game.EventType.BlockBuildBeginEvent
const ResearchEvent = Packages.mindustry.game.EventType.ResearchEvent
const UnlockEvent = Packages.mindustry.game.EventType.UnlockEvent
const StateChangeEvent = Packages.mindustry.game.EventType.StateChangeEvent
const CoreChangeEvent = Packages.mindustry.game.EventType.CoreChangeEvent
const BuildTeamChangeEvent = Packages.mindustry.game.EventType.BuildTeamChangeEvent
const TileChangeEvent = Packages.mindustry.game.EventType.TileChangeEvent
const TilePreChangeEvent = Packages.mindustry.game.EventType.TilePreChangeEvent
const BuildDamageEvent = Packages.mindustry.game.EventType.BuildDamageEvent
const GameOverEvent = Packages.mindustry.game.EventType.GameOverEvent
const UnitControlEvent = Packages.mindustry.game.EventType.UnitControlEvent
const PickupEvent = Packages.mindustry.game.EventType.PickupEvent
const TapEvent = Packages.mindustry.game.EventType.TapEvent
const ConfigEvent = Packages.mindustry.game.EventType.ConfigEvent
const DepositEvent = Packages.mindustry.game.EventType.DepositEvent
const WithdrawEvent = Packages.mindustry.game.EventType.WithdrawEvent
const SectorCaptureEvent = Packages.mindustry.game.EventType.SectorCaptureEvent
const PlayerChatEvent = Packages.mindustry.game.EventType.PlayerChatEvent
const MenuOptionChooseEvent = Packages.mindustry.game.EventType.MenuOptionChooseEvent
const ClientPreConnectEvent = Packages.mindustry.game.EventType.ClientPreConnectEvent
const SchematicCreateEvent = Packages.mindustry.game.EventType.SchematicCreateEvent
const SectorLaunchEvent = Packages.mindustry.game.EventType.SectorLaunchEvent
const LaunchItemEvent = Packages.mindustry.game.EventType.LaunchItemEvent
const SectorInvasionEvent = Packages.mindustry.game.EventType.SectorInvasionEvent
const SectorLoseEvent = Packages.mindustry.game.EventType.SectorLoseEvent
const WorldLoadEvent = Packages.mindustry.game.EventType.WorldLoadEvent
const FileTreeInitEvent = Packages.mindustry.game.EventType.FileTreeInitEvent
const MusicRegisterEvent = Packages.mindustry.game.EventType.MusicRegisterEvent
const ClientLoadEvent = Packages.mindustry.game.EventType.ClientLoadEvent
const ContentInitEvent = Packages.mindustry.game.EventType.ContentInitEvent
const BlockInfoEvent = Packages.mindustry.game.EventType.BlockInfoEvent
const CoreItemDeliverEvent = Packages.mindustry.game.EventType.CoreItemDeliverEvent
const TurretAmmoDeliverEvent = Packages.mindustry.game.EventType.TurretAmmoDeliverEvent
const LineConfirmEvent = Packages.mindustry.game.EventType.LineConfirmEvent
const TurnEvent = Packages.mindustry.game.EventType.TurnEvent
const WaveEvent = Packages.mindustry.game.EventType.WaveEvent
const ResetEvent = Packages.mindustry.game.EventType.ResetEvent
const PlayEvent = Packages.mindustry.game.EventType.PlayEvent
const DisposeEvent = Packages.mindustry.game.EventType.DisposeEvent
const ServerLoadEvent = Packages.mindustry.game.EventType.ServerLoadEvent
const ClientCreateEvent = Packages.mindustry.game.EventType.ClientCreateEvent
const SaveLoadEvent = Packages.mindustry.game.EventType.SaveLoadEvent
const SaveWriteEvent = Packages.mindustry.game.EventType.SaveWriteEvent
const MapPublishEvent = Packages.mindustry.game.EventType.MapPublishEvent
const MapMakeEvent = Packages.mindustry.game.EventType.MapMakeEvent
const ResizeEvent = Packages.mindustry.game.EventType.ResizeEvent
const LoseEvent = Packages.mindustry.game.EventType.LoseEvent
const WinEvent = Packages.mindustry.game.EventType.WinEvent
const Trigger = Packages.mindustry.game.EventType.Trigger
