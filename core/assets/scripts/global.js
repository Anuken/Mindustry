//Generated class. Do not modify.

"use strict";

const log = function(context, obj){
    Vars.mods.getScripts().log(context, String(obj))
}

const onEvent = function(event, handler){
    Vars.mods.getScripts().onEvent(event, handler)
}

const readString = path => Vars.mods.getScripts().readString(path)

const readBytes = path => Vars.mods.getScripts().readBytes(path)

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

importPackage(Packages.mindustry.maps)
importPackage(Packages.mindustry.world.blocks.production)
importPackage(Packages.mindustry.graphics.g3d)
importPackage(Packages.mindustry.ai.types)
importPackage(Packages.mindustry.world.draw)
importPackage(Packages.mindustry.game)
importPackage(Packages.mindustry.logic)
importPackage(Packages.mindustry.entities.comp)
importPackage(Packages.mindustry.world.blocks.sandbox)
importPackage(Packages.mindustry.world.consumers)
importPackage(Packages.mindustry.type)
importPackage(Packages.mindustry.maps.filters)
importPackage(Packages.arc.struct)
importPackage(Packages.mindustry.world.meta.values)
importPackage(Packages.mindustry.world.blocks.distribution)
importPackage(Packages.mindustry.ui.dialogs)
importPackage(Packages.mindustry.gen)
importPackage(Packages.mindustry.graphics)
importPackage(Packages.arc.scene.ui)
importPackage(Packages.mindustry.ai)
importPackage(Packages.mindustry.content)
importPackage(Packages.mindustry.ctype)
importPackage(Packages.arc.scene.event)
importPackage(Packages.mindustry.entities.bullet)
importPackage(Packages.arc.math.geom)
importPackage(Packages.mindustry.ui)
importPackage(Packages.mindustry.ai.formations)
importPackage(Packages.arc.func)
importPackage(Packages.mindustry.ui.fragments)
importPackage(Packages.mindustry.world.blocks.units)
importPackage(Packages.mindustry.world.modules)
importPackage(Packages.mindustry.world.blocks.power)
importPackage(Packages.arc.scene.utils)
importPackage(Packages.mindustry.entities)
importPackage(Packages.mindustry.audio)
importPackage(Packages.arc.math)
importPackage(Packages.arc.scene.ui.layout)
importPackage(Packages.arc.scene.actions)
importPackage(Packages.arc.scene.style)
importPackage(Packages.mindustry.world.blocks.defense.turrets)
importPackage(Packages.mindustry.core)
importPackage(Packages.arc.scene)
importPackage(Packages.mindustry.entities.units)
importPackage(Packages.mindustry.world.blocks.defense)
importPackage(Packages.mindustry.world)
importPackage(Packages.mindustry.world.blocks)
importPackage(Packages.mindustry.editor)
importPackage(Packages.mindustry.world.blocks.storage)
importPackage(Packages.mindustry.world.blocks.environment)
importPackage(Packages.mindustry.async)
importPackage(Packages.mindustry.maps.planet)
importPackage(Packages.mindustry.world.blocks.liquid)
importPackage(Packages.mindustry.ai.formations.patterns)
importPackage(Packages.mindustry.ui.layout)
importPackage(Packages.mindustry.input)
importPackage(Packages.mindustry.world.blocks.legacy)
importPackage(Packages.mindustry.entities.abilities)
importPackage(Packages.mindustry)
importPackage(Packages.mindustry.world.meta)
importPackage(Packages.mindustry.world.blocks.experimental)
importPackage(Packages.mindustry.world.blocks.payloads)
importPackage(Packages.mindustry.maps.generators)
importPackage(Packages.arc.graphics.g2d)
importPackage(Packages.arc.util)
importPackage(Packages.mindustry.world.blocks.campaign)
importPackage(Packages.arc.graphics)
importPackage(Packages.mindustry.world.producers)
importPackage(Packages.arc)
const PlayerIpUnbanEvent = Packages.mindustry.game.EventType.PlayerIpUnbanEvent
const PlayerIpBanEvent = Packages.mindustry.game.EventType.PlayerIpBanEvent
const PlayerUnbanEvent = Packages.mindustry.game.EventType.PlayerUnbanEvent
const PlayerBanEvent = Packages.mindustry.game.EventType.PlayerBanEvent
const PlayerLeave = Packages.mindustry.game.EventType.PlayerLeave
const PlayerConnect = Packages.mindustry.game.EventType.PlayerConnect
const PlayerJoin = Packages.mindustry.game.EventType.PlayerJoin
const UnitChangeEvent = Packages.mindustry.game.EventType.UnitChangeEvent
const UnitCreateEvent = Packages.mindustry.game.EventType.UnitCreateEvent
const UnitDestroyEvent = Packages.mindustry.game.EventType.UnitDestroyEvent
const BlockDestroyEvent = Packages.mindustry.game.EventType.BlockDestroyEvent
const BuildSelectEvent = Packages.mindustry.game.EventType.BuildSelectEvent
const BlockBuildEndEvent = Packages.mindustry.game.EventType.BlockBuildEndEvent
const BlockBuildBeginEvent = Packages.mindustry.game.EventType.BlockBuildBeginEvent
const ResearchEvent = Packages.mindustry.game.EventType.ResearchEvent
const UnlockEvent = Packages.mindustry.game.EventType.UnlockEvent
const StateChangeEvent = Packages.mindustry.game.EventType.StateChangeEvent
const BuildinghangeEvent = Packages.mindustry.game.EventType.BuildinghangeEvent
const GameOverEvent = Packages.mindustry.game.EventType.GameOverEvent
const TapConfigEvent = Packages.mindustry.game.EventType.TapConfigEvent
const TapEvent = Packages.mindustry.game.EventType.TapEvent
const DepositEvent = Packages.mindustry.game.EventType.DepositEvent
const WithdrawEvent = Packages.mindustry.game.EventType.WithdrawEvent
const SectorCaptureEvent = Packages.mindustry.game.EventType.SectorCaptureEvent
const ZoneConfigureCompleteEvent = Packages.mindustry.game.EventType.ZoneConfigureCompleteEvent
const ZoneRequireCompleteEvent = Packages.mindustry.game.EventType.ZoneRequireCompleteEvent
const PlayerChatEvent = Packages.mindustry.game.EventType.PlayerChatEvent
const CommandIssueEvent = Packages.mindustry.game.EventType.CommandIssueEvent
const LaunchItemEvent = Packages.mindustry.game.EventType.LaunchItemEvent
const SectorLoseEvent = Packages.mindustry.game.EventType.SectorLoseEvent
const WorldLoadEvent = Packages.mindustry.game.EventType.WorldLoadEvent
const ClientLoadEvent = Packages.mindustry.game.EventType.ClientLoadEvent
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
const MapPublishEvent = Packages.mindustry.game.EventType.MapPublishEvent
const MapMakeEvent = Packages.mindustry.game.EventType.MapMakeEvent
const ResizeEvent = Packages.mindustry.game.EventType.ResizeEvent
const LaunchEvent = Packages.mindustry.game.EventType.LaunchEvent
const LoseEvent = Packages.mindustry.game.EventType.LoseEvent
const WinEvent = Packages.mindustry.game.EventType.WinEvent
const Trigger = Packages.mindustry.game.EventType.Trigger
