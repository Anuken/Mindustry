const log = function(context, obj){
    Vars.mods.getScripts().log(context, obj ? String(obj) : "null")
}

const extendContent = function(classType, name, params){
    return new JavaAdapter(classType, params, name)
}

const extend = function(classType, params){
    return new JavaAdapter(classType, params)
}

const run = method => new java.lang.Runnable(){run: method}
const boolf = method => new Boolf(){get: method}
const boolp = method => new Boolp(){get: method}
const cons = method => new Cons(){get: method}
const prov = method => new Prov(){get: method}
const newEffect = (lifetime, renderer) => new Effects.Effect(lifetime, new Effects.EffectRenderer({render: renderer}))
const Calls = Packages.io.anuke.mindustry.gen.Call