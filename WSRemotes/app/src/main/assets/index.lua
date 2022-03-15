function main()
    local defaults = luajava.bindClass("pizzk.ws.remotes.app.Defaults")
    --defaults.INSTANCE:echo()
    return defaults.INSTANCE:version()
end