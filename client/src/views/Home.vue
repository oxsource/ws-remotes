<template>
  <div class="home">
    <div class="header">
      <p>Websocket Shell</p>
    </div>
    <div class="body">
      <div class="devices">
        <p>online devices</p>
        <el-radio-group v-model="device" class="device-groups">
          <el-radio :label="e" v-for="e in devices" :key="e">{{e}}</el-radio>
        </el-radio-group>
      </div>
      <div class="terminal">
        <p>{{`console ${connected ? 'connected' : 'disconnect'}`}}</p>
        <div id="console">
           <p ref="refConsole"><i v-for="(e, index) in lines" :key="index">{{e}}</i></p>
        </div>
        <div class="inputs">
           <el-input
            size="small"
            placeholder="please input content"
            v-model="command"
            rows="1"
            @keyup.enter.native="onSendCommand"
            clearable>
          </el-input>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { io } from 'socket.io-client'

export default {
  name: 'Home',
  data: () => {
    return {
      command: '',
      lines: [],
      devices: [],
      device: '',
      connected: false,
      socket: null,
      hostSymbol: '$local: '
    }
  },
  mounted () {
    const socket = io('ws://localhost:9000/clients', {
      transports: ['websocket']
    })
    this.socket = socket
    socket.on('connect', () => {
      this.connected = true
      this.println(`socket connected: ${socket.id}.`)
    })
    socket.on('disconnect', (reason) => {
      this.connected = false
      this.println(`socket disconnect via ${reason}.`)
    })
    socket.on('devices', value => {
      if (!value) return
      this.devices = [...value]
    })
    socket.on('source', response => {
      const { id = '', content = '' } = response || {}
      if (id.length <= 0 || content.length <= 0) return
      this.println(content, id)
    })
  },

  methods: {
    onSendCommand () {
      const tx = this.command || ''
      this.command = ''
      const id = this.device || ''
      if (tx.length <= 0) return
      const line = id.length <= 0 ? 'please choose device firstly.' : tx
      this.println(line)
      tx === 'clear()' ? this.lines = [] : this.emitToSocket(tx)
      this.scrollToBottom()
    },

    scrollToBottom () {
      const console = this.$refs.refConsole
      if (!console) return
      this.$nextTick(() => { console.scrollTop = console.scrollHeight })
    },

    println (s, who) {
      const host = who && who.length > 0 ? `$${who}: ` : '$local: '
      this.lines.push(`${host}${s || ''}`)
      this.scrollToBottom()
    },

    emitToSocket (s) {
      if (!s || s.length <= 0) return
      const id = this.device || ''
      if (id.length <= 0) return
      const socket = this.socket
      if (!socket || !this.connected) return
      socket.emit('sink', { id, content: s })
    }
  }
}

</script>

<style lang="less" scoped>
.home {
  width: 100%;
  height: 100%;
}
.header {
  height: 48px;
  display: flex;
  flex-flow: row nowrap;
  justify-content: center;
  align-items: center;
  border-bottom: #333 dashed 0.5px;
  font-weight: bold;
}
.body {
  display: flex;
  flex-flow: row nowrap;
  height: calc(100% - 48px);

  .devices {
    width: 25%;
    height: 100%;
    border-right: #333 dashed 0.5px;
    overflow-y: scroll;
    position: relative;

    & > p {
      color: #303133;
      font-size: 18px;
      width: 25%;
      position: fixed;
      background: #fff;
      z-index: 10;
      padding: 5px 0;
      border-bottom: #333 dashed 0.5px;
      font-weight: 600;
    }

    /deep/ .el-radio-group{
      display: flex;
      flex-flow: column;
      margin-top: 60px;
    }

    /deep/ .el-radio{
      box-sizing: border-box;
      padding: 10px 10px;
      display: flex;
      flex-flow: row nowrap;
      overflow: hidden;
      justify-content: flex-start;
      align-items: center;
    }

    /deep/ .el-radio__label{
      text-overflow: ellipsis;
      color: #333;
      font-size: 13px;
    }
  }
  .terminal {
    width: 75%;
    height: 100%;
    display: flex;
    flex-flow: column nowrap;

    & > p {
      color: #303133;
      font-size: 18px;
      padding: 5px 0;
      border-bottom: #333 dashed 0.5px;
      border-left: #333 dashed 0.5px;
      background: #fff;
      font-weight: 600;
    }

    #console {
      height: calc(100% - 60px);
      overflow: hidden;
      box-sizing: border-box;
      background: #000;
      scrollbar-color: #f00;
      scrollbar-base-color: #67C23A;
      padding: 2px 6px;

      & > p {
        height: 100%;
        overflow-y: scroll;
        text-align: start;
        display: flex;
        flex-flow: column nowrap;
        align-content: flex-start;

        i {
          color: #67C23A;
          font-size: 13px;
        }
      }
    }

    .inputs {
      box-sizing: border-box;
      max-height: 60px;
      height: 60px;
      border-top: #333 dashed 0.5px;
      display: inline-flex;
      flex-flow: row nowrap;
      justify-content: flex-start;
      align-items: center;
      padding: 20px 20px;

      .submit {
        width: 90px;
        margin-left: 20px;
      }
    }
  }
}
</style>
