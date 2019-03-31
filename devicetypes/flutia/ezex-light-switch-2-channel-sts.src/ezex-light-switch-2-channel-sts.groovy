/**
 * eZEX C2O Light Switch (2 Channel, E220-KR2N0Z0-HA) - v1.0.3
 *
 *  github: Euiho Lee (flutia)
 *  email: flutia@naver.com
 *  Date: 2019-03-31
 *  Copyright flutia and stsmarthome (cafe.naver.com/stsmarthome/)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with the License. You may obtain a copy
 *  of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 *  License for the specific language governing permissions and limitations
 *  under the License.
 */
metadata {
    definition(name: "eZEX Light Switch-2 Channel (STS)", namespace: "flutia", author: "flutia", vid: "generic-switch", ocfDeviceType: "oic.d.light") {
        capability "Actuator"
        capability "Switch"
        capability "Configuration"
        capability "Refresh"
        capability "Sensor"

        attribute "allstates", "string"

        command "onAll"
        command "offAll"

        fingerprint profileId: "0104", deviceId: "0100", inClusters: "0000, 0003, 0004, 0006", model: "E220-KR2N0Z0-HA"
    }

    simulator {}

    preferences {
        (1..switchNumbers()).each {
            input name: "nameOfSwitch${it}", type: "text", title: "${it}번 스위치 이름"
        }
    }

    // UI tile definitions
    tiles(scale: 2) {
        standardTile("tileOnAll", "device.onAll", decoration: "flat", width: 2, height: 2) {
            state "default", label: '모두 켜기', action: "onAll", icon: "st.lights.multi-light-bulb-on", backgroundColor: "#ffffff"
        }
        standardTile("tileOffAll", "device.offAll", decoration: "flat", width: 2, height: 2) {
            state "default", label: '모두 끄기', action: "offAll", icon: "st.lights.multi-light-bulb-off", backgroundColor: "#ffffff"
        }
        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label: "", action: "refresh.refresh", icon: "st.secondary.refresh"
        }
        standardTile("tileAllOnOff", "device.allstates", width: 2, height: 2, canChangeIcon: true, buildState())

        def switchTiles = []
        (1..switchNumbers()).each {
            childDeviceTile("tileSW${it}", "switch${it}", width: 2, height: 2)
            switchTiles << "tileSW${it}"
        }

        main(["tileAllOnOff"])
        if (switchNumbers() == 4) {
            details(["tileSW3", "tileOnAll", "tileSW1", "tileSW4", "tileOffAll", "tileSW2", "refresh"])
        } else {
            details(switchTiles + ["tileOnAll", "tileOffAll", "refresh"])
        }
    }
}

def switchNumbers() {
    2
}

def makeSwitchPrem(mm, str, step) {
    for (int i = 0; i < 2; i++) {
        def myStr = str + ((i == 0) ? "◉" : "○")
        if (step == 0) {
            if (!mm.contains(myStr)) {
                mm << myStr
            }
        } else {
            makeSwitchPrem(mm, myStr, step - 1)
        }
    }
}

def buildState() {
    def stateAllOn = "◉" * switchNumbers()
    def stateAllOff = "○" * switchNumbers()

    Set switchStates = []
    makeSwitchPrem(switchStates, "", switchNumbers() - 1)
    switchStates.removeAll([stateAllOff])

    def states = {
        state "turningOn", label: '모두 켜는 중', action: "offAll", icon: "st.switches.light.on", backgroundColor: "#ffffff", nextState: "turningOff"
        state "turningOff", label: '모두 끄는 중', action: "onAll", icon: "st.switches.light.off", backgroundColor: "#00a0dc", nextState: "turningOn"

        switchStates.each { item ->
            state item, label: '${name}', action: "offAll", icon: "st.switches.light.on", backgroundColor: "#00a0dc", nextState: "turningOff"
        }

        state stateAllOff, label: '${name}', action: "onAll", icon: "st.switches.light.off", backgroundColor: "#ffffff", nextState: "turningOn"
    }
    return states
}

def installed() {
    createChildDevices()
}

def determineChildDeviceName(ep) {
    def preferencedName = null
    // preference 값은 이벤트 컨텍스트에서만 가져올 수 있다. 
    try {
        switch (ep) {
            case 1: preferencedName = nameOfSwitch1; break;
            case 2: preferencedName = nameOfSwitch2; break;
            case 3: preferencedName = nameOfSwitch3; break;
            case 4: preferencedName = nameOfSwitch4; break;
            case 5: preferencedName = nameOfSwitch5; break;
            case 6: preferencedName = nameOfSwitch6; break;
        }
    } catch (ignore) {
        log.warn ignore
    }

    if (preferencedName != null) {
        preferencedName = preferencedName.trim()
        if (preferencedName.length() > 0) {
            return preferencedName
        }
    }
    return "${device.displayName} - ${ep}번 스위치"
}

def updated() {
    if (!childDevices || childDevices.size() != switchNumbers()) {
        createChildDevices()
    }

    childDevices.each {
        def ep = getEPFromChildDNID(it.deviceNetworkId)
        def prevLabel = it.getLabel()
        def newLabel = determineChildDeviceName(ep)
        if (prevLabel != newLabel) {
            it.setLabel(newLabel)
        }
    }

    if (device.label != state.oldLabel) {
        state.oldLabel = device.label
    }
}

def createChildDevices() {
    for (i in 1..switchNumbers()) {
        if (childDevices != null) {
            def exists = false
            childDevices.each {
                def ep = getEPFromChildDNID(it.deviceNetworkId)
                if (ep == i) {
                    exists = true
                }
            }
            if (exists) {
                continue; // skip creation
            }

            log.debug "create child - ${i}, ${device.deviceNetworkId}-${i}, ${device.displayName} (CH${i})"

            def preferencedName = determineChildDeviceName(i)
            addChildDevice(
                    "eZEX Light Switch Child Device (STS)", "${device.deviceNetworkId}-ep${i}", null,
                    [completedSetup: true, label: "${preferencedName}", isComponent: true, componentName: "switch${i}", componentLabel: "${i}번 스위치"]
            )
        }
    }
}

def checkOnState(int options, int check) {
    return (options & check) != 0
}

def processRefresh(currentValue) {
    final int SWITCH_NUMBERS = switchNumbers()

    def value = zigbee.convertHexToInt(currentValue)

    final def opCheck = [1: 0b1, 2: 0b10, 3: 0b100, 4: 0b1000, 5: 0b10000, 6: 0b100000] // 비트 연산용 배열
    def onOffMapByEP = [:]; // [1: "off", 2:"off", 3:"off", 4:"off", ...];
    (1..SWITCH_NUMBERS).each {
        onOffMapByEP[it] = "off"
    }

    def allState = ""
    def isOnAny = false;
    for (ep in 1..SWITCH_NUMBERS) {
        def isOn = checkOnState(value, opCheck[ep])
        onOffMapByEP[ep] = isOn ? "on" : "off"
        allState = allState + (isOn ? "◉" : "○")
        if (isOn) {
            isOnAny = true;
        }
    }

    def eventStack = []
    childDevices.each { childDevice ->
        def ep = getEPFromChildDNID(childDevice.deviceNetworkId)
        childDevice.sendEvent(name: "switch", value: onOffMapByEP[ep], displayed: true)

        if (state.switchOp) {
            def displayName = childDevice.label ? childDevice.label : childDevice.name + ep
            def desc = "${displayName} Is ${onOffMapByEP[ep]}"
            eventStack.push(createEvent(name: displayName, value: onOffMapByEP[ep], descriptionText: desc, displayed: true))
        }
    }

    eventStack.push(createEvent(name: "allstates", value: allState, displayed: false))

    // 전등이 하나라도 켜져있거나 다 꺼져있으면 parent switch에도 켜짐 상태를 반영해준다. 그렇지 않으면 off
    eventStack.push(createEvent(name: "switch", value: isOnAny ? "on" : "off", displayed: false));

    state.switchOp = false
    return eventStack
}

def getChildDeviceWithEP(strEP) {
    def ep = strEP as int
    def selectedChild
    childDevices.each { childDevice ->
        def childEP = getEPFromChildDNID(childDevice.deviceNetworkId)
        if (childEP == ep) {
            selectedChild = childDevice;
        }
    }
    return selectedChild;
}

def getCommandType(description, parseMap, event) {
    def isParseMapValid = (parseMap != null && !parseMap.isEmpty())
    def isEventValid = (event != null && !event.isEmpty())

    // Refresh
    // ----------------------------------------------
    // - event 값은 오류가 있으므로 사용하면 안된다.
    // parseMap:[profileId:0104, clusterId:0006, sourceEndpoint:01, destinationEndpoint:01, options:0140, messageType:00, command:01, direction:01, attrId:0011, resultCode:00, encoding:18, value:01, isValidForDataType:true, data:[11, 00, 00, 18, 01], clusterInt:6, attrInt:17, commandInt:1], event:[name:switch, value:on]
    // Refresh (전부 꺼져있을 때)
    // parseMap:[profileId:0104, clusterId:0006, sourceEndpoint:01, destinationEndpoint:01, options:0140, messageType:00, command:01, direction:01, attrId:0011, resultCode:00, encoding:18, value:00, isValidForDataType:true, data:[11, 00, 00, 18, 00], clusterInt:6, attrInt:17, commandInt:1], event:[name:switch, value:off]
    // Refresh (전부 켜져있을 때)
    // parseMap:[profileId:0104, clusterId:0006, sourceEndpoint:01, destinationEndpoint:01, options:0140, messageType:00, command:01, direction:01, attrId:0011, resultCode:00, encoding:18, value:07, isValidForDataType:true, data:[11, 00, 00, 18, 07], clusterInt:6, attrInt:17, commandInt:1], event:[name:switch, value:off]
    if (isParseMapValid && parseMap.command == "01" && parseMap.attrId == "0011") {
        return "REFRESH"
    }

    // 전체 켜기
    // ----------------------------------------------
    // parseMap: [profileId:0104, clusterId:0006, sourceEndpoint:01, destinationEndpoint:01, options:0140, messageType:00, command:04, direction:01, data:[00], clusterInt:6, commandInt:4], event: [:]
    // 이후 n번의 switch on 이벤트가 날아온다. 이 이벤트 형식은 물리조작시 날아오는 이벤트와 동일하다.
    // 전체 끄기
    // parseMap: [profileId:0104, clusterId:0006, sourceEndpoint:01, destinationEndpoint:01, options:0140, messageType:00, command:04, direction:01, data:[00], clusterInt:6, commandInt:4], event: [:]
    // 구성상으로는 전체 켜기와 동일하다. 이후 n번 switch  off 이벤트가 날아오고, 이 switch off 이벤트 형식은 물리조작시 날아오는 이벤트와 동일하다.
    if (isParseMapValid && parseMap.command == "04" && parseMap.sourceEndpoint == "01" && parseMap.destinationEndpoint == "01") {
        return "ALLONOFF"
    }

    // 물리조작 OR 개별스위치 상태 변경됨
    // ----------------------------------------------
    // 스위치 켜기
    // description:on/off: 1, parseMap:[:], event:[name:switch, value:on]
    // 스위치 끄기
    // description: on/off: 0, parseMap: [:], event: [name:switch, value:off]
    // 
    // * EP가 날아오지 않는다. 추후 읽어들일 필요가있다.
    if (!isParseMapValid && description.startsWith("on/off") && event.name == "switch") {
        return "SWITCH_OP"
    }

    // APP 에서의 개별 스위치 끄고 켜기
    // ---------------------------------------------------------
    // parseMap: [profileId:0104, clusterId:0006, sourceEndpoint:02, destinationEndpoint:01, options:0140, messageType:00, command:0B, direction:01, data:[00, 00], clusterInt:6, commandInt:11], event: [name:switch, value:off]
    // parseMap: [profileId:0104, clusterId:0006, sourceEndpoint:02, destinationEndpoint:01, options:0140, messageType:00, command:0B, direction:01, data:[01, 00], clusterInt:6, commandInt:11], event: [name:switch, value:on]
    //
    if (isParseMapValid && parseMap.command == "0B") {
        return "SWITCH_OP_APP"
    }

}

// Parse incoming device messages to generate events
def parse(String description) {
    def parseMap = zigbee.parseDescriptionAsMap(description)
    def event = zigbee.getEvent(description)

    def commandType = getCommandType(description, parseMap, event)

    log.debug "Before Handle - commandType: ${commandType}, description: ${description}, parseMap: ${parseMap}, event: ${event}"

    if ("REFRESH" == commandType) {
        return processRefresh(parseMap.value)
    }

    if ("ALLONOFF" == commandType) {
        return null;
    }

    if ("SWITCH_OP_APP" == commandType && parseMap.sourceEndpoint != null) {
        def child = getChildDeviceWithEP(parseMap.sourceEndpoint)
        if (child != null) {
            return child.sendEvent(name: "switch", value: event.value, displayed: true)
        }
    }

    if ("SWITCH_OP" == commandType) {
        state.switchOp = true
        doCommand(getRefreshCommand())
        return null
    }

    log.debug "Unhandled Event - commandType: ${commandType}, description: ${description}, parseMap: ${parseMap}, event: ${event}"
    return null
}

def off() {
    offAll()
}

def on() {
    onAll()
}

private def getEPFromChildDNID(dnId) {
    def epToken = dnId.split("-")[-1]
    def ep = epToken.replace("ep", "")
    return ep as Integer
}

def childOn(String dnId) {
    def ep = getEPFromChildDNID(dnId)
    log.debug "Executing 'on' for 0x${device.deviceNetworkId} endpoint ${ep}, child: ${dnId}"

    def cmd = "st cmd 0x${device.deviceNetworkId} ${ep} 0x0006 0x01 {}"
    doCommand(cmd)
}

def childOff(String dnId) {
    def ep = getEPFromChildDNID(dnId)
    log.debug "Executing 'off' for 0x${device.deviceNetworkId} endpoint ${ep}, child: ${dnId}"

    def cmd = "st cmd 0x${device.deviceNetworkId} ${ep} 0x0006 0x00 {}"
    doCommand(cmd)
}

def childRefresh(String dnId) {
    def ep = getEPFromChildDNID(dnId)
    log.debug "Executing 'refresh' for 0x${device.deviceNetworkId} endpoint ${ep},  child: ${dnId}"
    doCommands(["st rattr 0x${device.deviceNetworkId} ${ep} 0x0006 0x0000"], 100)
}

def onAll() {
    log.debug "Executing 'on all' for 0x${device.deviceNetworkId}"
    def cmds = []
    cmds << "st wattr 0x${device.deviceNetworkId} 1 0x0006 0x0011 0x18 {7}"
    cmds << "delay 100"
    return cmds
}

def offAll() {
    log.debug "Executing 'off all' for 0x${device.deviceNetworkId}"
    def cmds = []
    cmds << "st wattr 0x${device.deviceNetworkId} 1 0x0006 0x0011 0x18 {0}"
    cmds << "delay 100"
    return cmds
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
    refresh()
}

def refresh() {
    log.debug "Executing 'refresh' for 0x${device.deviceNetworkId}"
    return getRefreshCommand()
}

def configure() {
    // Device-Watch allows 2 check-in misses from device + ping (plus 1 min lag time)
    // enrolls with default periodic reporting until newer 5 min interval is confirmed
    // sendEvent(name: "checkInterval", value: 2 * 10 * 60 + 1 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
    return refresh()
}

def getRefreshCommand() {
    "st rattr 0x${device.deviceNetworkId} 1 0x0006 0x0011"
}

private doCommand(cmd) {
    sendHubCommand(new physicalgraph.device.HubAction(cmd))
}

private doCommands(cmds, delay = 100) {
    cmds.each {
        doCommand(it)
        doCommand("delay ${delay}")
    }
}
