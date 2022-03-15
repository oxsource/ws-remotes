const { Server } = require("socket.io")
const server = new Server()
const port = 9000

// clients  ---->  server ----> devices  
// clients  <----  server <---- devices

const devices = server.of('devices')
const clients = server.of('clients')

//broadcat online device list
const broadcastDeviceList = () => {
    devices.fetchSockets().then(s => {
        const names = s.map(e => e.id || '').filter(e => e.length > 0)
        clients.emit("devices", names)
    }).catch(e => console.error(`fetch devices sockets failed: ${e}.`))
}

//devices connect callback
devices.on("connection", (socket) => {
    console.log(`device ${socket.id} connected.`)
    broadcastDeviceList()
    socket.on("disconnect", (reason) => {
        console.log(`device ${socket.id} disconnect via ${reason}`)
        broadcastDeviceList()
    })
    socket.on("source", (response) => {
        const { id = '', content = '' } = response || {}
        if (id.length <= 0 || content.length <= 0) return
        console.log(`device source ${id} ${content}`)
        clients.fetchSockets().then(s => {
            s.filter(e => e.id == id).forEach(e => e.emit('source', { id: socket.id, content }))
        }).catch(e => console.error(`device ${socket.id} dispatch source failed: ${e}.`))
    })
})

//clients connect callback
clients.on("connection", (socket) => {
    console.log(`client ${socket.id} connected.`)
    broadcastDeviceList()
    socket.on("disconnect", (reason) => {
        console.log(`client ${socket.id} disconnect via ${reason}`)
    })
    socket.on("sink", (request) => {
        const { id = '', content = '' } = request || {}
        if (id.length <= 0 || content.length <= 0) return
        devices.fetchSockets().then(s => {
            s.filter(e => e.id == id).forEach(e => e.emit('sink', { id: socket.id, content }))
        }).catch(e => console.error(`client ${socket.id} dispatch sink failed: ${e}.`))
    })
})

//server start and listen
server.listen(port)
console.log(`websocket server listen at port ${port}.`)
