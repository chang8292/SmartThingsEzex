/**
 * eZEX C2O Light Switch Child Device - v1.0.0
 *
 *  github: Euiho Lee (flutia)
 *  email: flutia@naver.com
 *  Date: 2018-01-13
 *  Copyright flutia and stsmarthome (cafe.naver.com/stsmarthome/)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 *
 */
metadata {
    definition(name: "eZEX Light Switch Child Device (STS)", namespace: "flutia", author: "flutia") {
        capability "Switch"
        capability "Actuator"
        capability "Sensor"
        capability "Refresh"
    }

    tiles(scale: 2) {
        standardTile("switch", "device.switch", width: 3, height: 3, canChangeIcon: true) {
                state "off", label: '꺼짐', action: "switch.on", icon: "st.switches.light.off", backgroundColor: "#FFFFFF", nextState: "turningOn"
                state "on", label: '켜짐', action: "switch.off", icon: "st.switches.light.on", backgroundColor: "#00A0DC", nextState: "turningOff"
                state "turningOn", label: '켜는 중', action: "switch.off", icon: "st.switches.light.off", backgroundColor: "#00A0DC", nextState: "turningOff"
                state "turningOff", label: '끄는 중', action: "switch.on", icon: "st.switches.light.off", backgroundColor: "#FFFFFF", nextState: "turningOn"
        }
        standardTile("refresh", "device.switch", inactiveLabel: false, decoration: "flat", width: 3, height: 3) {
            state "default", label: "", action: "refresh.refresh", icon: "st.secondary.refresh"
        }
    }
}

def on() {
    parent.childOn(device.deviceNetworkId)
}

def off() {
    parent.childOff(device.deviceNetworkId)
}

void refresh() {
    parent.childRefresh(device.deviceNetworkId)
}