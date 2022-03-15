const { io } = require('socket.io-client')
const socket = io('ws://localhost:9000/devices', {
    transports: ['websocket']
})
socket.on('connect', () => {
    console.log(`socket connected: ${socket.id}.`)
})
socket.on('disconnect', (reason) => {
    console.log(`socket disconnect via ${reason}.`)
})
socket.on('sink', (request) => {
    const {id = '', content = ''} = request || {}
    if(id.length <= 0 || content.length <= 0) return
    console.log(`socket deal some thing for ${id} that content is ${content}.`)
    socket.emit('source', {id, content : `replay for ${content}`})
})