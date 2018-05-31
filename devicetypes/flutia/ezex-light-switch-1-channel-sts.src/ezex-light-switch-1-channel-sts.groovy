/**
 * eZEX C2O Light Switch (1 Channel, E220-KR3N0Z0-HA) - v1.0.0
 *
 *  github: Euiho Lee (flutia)
 *  email: flutia@naver.com
 *  Date: 2018-01-13
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
    definition(name: "eZEX Light Switch-1 Channel (STS)", namespace: "flutia", author: "flutia") {
        capability "Actuator"
        capability "Switch"
        capability "Refresh"
        capability "Sensor"
        
        fingerprint profileId: "0104", deviceId: "0100", inClusters: "0000, 0003, 0004, 0006", model: "E220-KR1N0Z0-HA"
    }

    simulator {}
    preferences {}

    // UI tile definitions
    tiles(scale: 2) {
        multiAttributeTile(name: "tileSwitch", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
            tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
                attributeState "off", label: '꺼짐', action: "switch.on", icon: "st.switches.light.off", backgroundColor: "#FFFFFF", nextState: "turningOn"
                attributeState "on", label: '켜짐', action: "switch.off", icon: "st.switches.light.on", backgroundColor: "#00A0DC", nextState: "turningOff"
                attributeState "turningOn", label: '켜는 중', action: "switch.off", icon: "st.switches.light.off", backgroundColor: "#00A0DC", nextState: "turningOff"
                attributeState "turningOff", label: '끄는 중', action: "switch.on", icon: "st.switches.light.off", backgroundColor: "#FFFFFF", nextState: "turningOn"
            }
        }
        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
            state "default", label: "", action: "refresh.refresh", icon: "st.secondary.refresh"
        }

        main(["tileSwitch"])
        details(["tileSwitch", "refresh"])
    }
}

// Parse incoming device messages to generate events
def parse(String description) {
    def event = zigbee.getEvent(description)
    if( event != null && event.name == "switch") {
        return createEvent(name: "switch", value: event.value)
    }
    
    log.debug "Unhandled Event - description: ${description}, parseMap: ${parseMap}, event: ${event}"
}

def off() {
    def cmd = "st cmd 0x${device.deviceNetworkId} 1 0x0006 0x00 {}"
    return cmd
}

def on() {
    def cmd = "st cmd 0x${device.deviceNetworkId} 1 0x0006 0x01 {}"
    return cmd
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
    return refresh()
}

def refresh() {
    log.debug "Executing 'refresh' for 0x${device.deviceNetworkId}"
    def cmds = []
    cmds << "st rattr 0x${device.deviceNetworkId} 1 0x0006 0x0000"
    cmds << "delay 50"
    return cmds
}

def configure() {
    // Device-Watch allows 2 check-in misses from device + ping (plus 1 min lag time)
    // enrolls with default periodic reporting until newer 5 min interval is confirmed
    // sendEvent(name: "checkInterval", value: 2 * 10 * 60 + 1 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
    return refresh()
}
