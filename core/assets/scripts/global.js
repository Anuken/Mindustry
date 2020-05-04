//Generated class. Do not modify.

const log = function(context, obj){
    Vars.mods.getScripts().log(context, String(obj))
}

const onEvent = function(event, handler){
    Vars.mods.getScripts().onEvent(event, handler)
}

var scriptName = "base.js"
var modName = "none"

const print = text => log(modName + "/" + scriptName, text);

const extendContent = function(classType, name, params){
    return new JavaAdapter(classType, params, name)
}

const extend = function(classType, params){
    return new JavaAdapter(classType, params)
}

const newEffect = (lifetime, renderer) => new Effects.Effect(lifetime, new Effects.EffectRenderer({render: renderer}))
Call = Packages.mindustry.gen.Call

importPackage(Packages.mindustry.game)
importPackage(Packages.arc.func)
importPackage(Packages.mindustry.entities)
importPackage(Packages.mindustry.gen)
importPackage(Packages.mindustry.core)
importPackage(Packages.mindustry.world.blocks.storage)
importPackage(Packages.mindustry.ui.dialogs)
importPackage(Packages.arc.scene.ui)
importPackage(Packages.mindustry.world.blocks.defense.turrets)
importPackage(Packages.mindustry.world.blocks.distribution)
importPackage(Packages.mindustry.ui)
importPackage(Packages.mindustry.content)
importPackage(Packages.mindustry.world.blocks.liquid)
importPackage(Packages.arc.struct)
importPackage(Packages.arc.scene.ui.layout)
importPackage(Packages.mindustry.world.modules)
importPackage(Packages.arc.util)
importPackage(Packages.arc.graphics)
importPackage(Packages.mindustry.entities.def)
importPackage(Packages.mindustry.maps.generators)
importPackage(Packages.arc.scene.actions)
importPackage(Packages.mindustry.graphics)
importPackage(Packages.mindustry.entities.bullet)
importPackage(Packages.mindustry.world.blocks.legacy)
importPackage(Packages.mindustry.world.blocks.experimental)
importPackage(Packages.mindustry.editor)
importPackage(Packages.mindustry.world.blocks.power)
importPackage(Packages.mindustry.ui.layout)
importPackage(Packages.mindustry.world.blocks.sandbox)
importPackage(Packages.mindustry.input)
importPackage(Packages.mindustry.world.consumers)
importPackage(Packages.mindustry.ui.fragments)
importPackage(Packages.mindustry.ai.formations)
importPackage(Packages.mindustry.type)
importPackage(Packages.mindustry.world.blocks.production)
importPackage(Packages.arc.scene.event)
importPackage(Packages.arc.math)
importPackage(Packages.arc.scene.utils)
importPackage(Packages.mindustry.world.blocks.defense)
importPackage(Packages.mindustry.graphics.g3d)
importPackage(Packages.mindustry.world.meta)
importPackage(Packages.mindustry.world.blocks.payloads)
importPackage(Packages.mindustry.world)
importPackage(Packages.mindustry.async)
importPackage(Packages.arc.scene.style)
importPackage(Packages.mindustry.world.blocks)
importPackage(Packages.arc.math.geom)
importPackage(Packages.mindustry.ai)
importPackage(Packages.mindustry.maps.filters)
importPackage(Packages.arc.graphics.g2d)
importPackage(Packages.mindustry.ai.formations.patterns)
importPackage(Packages.mindustry.world.blocks.environment)
importPackage(Packages.mindustry)
importPackage(Packages.mindustry.entities.units)
importPackage(Packages.mindustry.ctype)
importPackage(Packages.mindustry.ai.types)
importPackage(Packages.mindustry.maps)
importPackage(Packages.mindustry.world.meta.values)
importPackage(Packages.mindustry.world.producers)
importPackage(Packages.mindustry.world.blocks.units)
importPackage(Packages.arc.scene)
importPackage(Packages.mindustry.maps.planet)
importPackage(Packages.arc)
importPackage(Packages.mindustry.world.blocks.logic)
const PlayerIpUnbanEvent = Packages.mindustry.game.EventType.PlayerIpUnbanEvent
const PlayerIpBanEvent = Packages.mindustry.game.EventType.PlayerIpBanEvent
const PlayerUnbanEvent = Packages.mindustry.game.EventType.PlayerUnbanEvent
const PlayerBanEvent = Packages.mindustry.game.EventType.PlayerBanEvent
const PlayerLeave = Packages.mindustry.game.EventType.PlayerLeave
const PlayerConnect = Packages.mindustry.game.EventType.PlayerConnect
const PlayerJoin = Packages.mindustry.game.EventType.PlayerJoin
const MechChangeEvent = Packages.mindustry.game.EventType.MechChangeEvent
const ResizeEvent = Packages.mindustry.game.EventType.ResizeEvent
const UnitCreateEvent = Packages.mindustry.game.EventType.UnitCreateEvent
const UnitDestroyEvent = Packages.mindustry.game.EventType.UnitDestroyEvent
const BlockDestroyEvent = Packages.mindustry.game.EventType.BlockDestroyEvent
const BuildSelectEvent = Packages.mindustry.game.EventType.BuildSelectEvent
const BlockBuildEndEvent = Packages.mindustry.game.EventType.BlockBuildEndEvent
const BlockBuildBeginEvent = Packages.mindustry.game.EventType.BlockBuildBeginEvent
const ResearchEvent = Packages.mindustry.game.EventType.ResearchEvent
const UnlockEvent = Packages.mindustry.game.EventType.UnlockEvent
const StateChangeEvent = Packages.mindustry.game.EventType.StateChangeEvent
const TileChangeEvent = Packages.mindustry.game.EventType.TileChangeEvent
const WorldLoadEvent = Packages.mindustry.game.EventType.WorldLoadEvent
const GameOverEvent = Packages.mindustry.game.EventType.GameOverEvent
const TapConfigEvent = Packages.mindustry.game.EventType.TapConfigEvent
const TapEvent = Packages.mindustry.game.EventType.TapEvent
const DepositEvent = Packages.mindustry.game.EventType.DepositEvent
const WithdrawEvent = Packages.mindustry.game.EventType.WithdrawEvent
const BlockInfoEvent = Packages.mindustry.game.EventType.BlockInfoEvent
const CoreItemDeliverEvent = Packages.mindustry.game.EventType.CoreItemDeliverEvent
const TurretAmmoDeliverEvent = Packages.mindustry.game.EventType.TurretAmmoDeliverEvent
const LineConfirmEvent = Packages.mindustry.game.EventType.LineConfirmEvent
const WaveEvent = Packages.mindustry.game.EventType.WaveEvent
const ResetEvent = Packages.mindustry.game.EventType.ResetEvent
const PlayEvent = Packages.mindustry.game.EventType.PlayEvent
const DisposeEvent = Packages.mindustry.game.EventType.DisposeEvent
const ContentReloadEvent = Packages.mindustry.game.EventType.ContentReloadEvent
const ServerLoadEvent = Packages.mindustry.game.EventType.ServerLoadEvent
const ClientLoadEvent = Packages.mindustry.game.EventType.ClientLoadEvent
const ClientCreateEvent = Packages.mindustry.game.EventType.ClientCreateEvent
const SaveLoadEvent = Packages.mindustry.game.EventType.SaveLoadEvent
const ZoneConfigureCompleteEvent = Packages.mindustry.game.EventType.ZoneConfigureCompleteEvent
const ZoneRequireCompleteEvent = Packages.mindustry.game.EventType.ZoneRequireCompleteEvent
const PlayerChatEvent = Packages.mindustry.game.EventType.PlayerChatEvent
const CommandIssueEvent = Packages.mindustry.game.EventType.CommandIssueEvent
const MapPublishEvent = Packages.mindustry.game.EventType.MapPublishEvent
const MapMakeEvent = Packages.mindustry.game.EventType.MapMakeEvent
const LaunchItemEvent = Packages.mindustry.game.EventType.LaunchItemEvent
const LaunchEvent = Packages.mindustry.game.EventType.LaunchEvent
const LoseEvent = Packages.mindustry.game.EventType.LoseEvent
const WinEvent = Packages.mindustry.game.EventType.WinEvent
const Trigger = Packages.mindustry.game.EventType.Trigger
