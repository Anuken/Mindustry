const log = function(context, obj){
    Vars.mods.getScripts().log(context, obj ? String(obj) : "null")
}

const extendContent = function(classType, name, params){
    return new JavaAdapter(classType, params, modName + "-" + name)
}

const extend = function(classType, params){
    return new JavaAdapter(classType, params)
}
