/*
 * eZEX C2O Power Outlet (2 sockets, E210-KR210Z1-HA) - ALPHA
 *  
 *  github: Euiho Lee (flutia)
 *  email: flutia@naver.com
 *  Date: 2017-09-11
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
	definition(name: "eZEX Smart Socket", namespace: "flutia", author: "flutia", ocfDeviceType: "oic.d.smartplug") {
		capability "Actuator"
		capability "Switch"
		capability "Power Meter"
		capability "Configuration"
		capability "Refresh"
		capability "Sensor"
		capability "Health Check"
		capability "Outlet"
        capability "Voltage Measurement"
        
        attribute "currentCurrent", "string"
        attribute "accumulateWh", "string"
        attribute "powerFactor", "number"
        attribute "lock", "string"
        attribute "powerCutOff", "string"
        
        command "lock"
        command "unlock"
        
        fingerprint profileId: "0104", deviceId: "0051", inClusters: "0000, 0003, 0004, 0006, 0B04, 0702", outClusters: "0019", model: "E210-KR210Z1-HA"
	}

	// simulator metadata
	simulator {
		// status messages
		status "on": "on/off: 1"
		status "off": "on/off: 0"

		// reply messages
		reply "zcl on-off on": "on/off: 1"
		reply "zcl on-off off": "on/off: 0"
	}

	preferences {
    /*
		section {
			image(name: 'educationalcontent', multiple: true, images: [
					"http://cdn.device-gse.smartthings.com/Outlet/US/OutletUS1.jpg",
					"http://cdn.device-gse.smartthings.com/Outlet/US/OutletUS2.jpg"
			])
		}
    */    
	}

	// UI tile definitions
	tiles(scale: 2) {
		multiAttributeTile(name: "switch", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
			tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label: '${currentValue}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00A0DC", nextState: "turningOff"
				attributeState "off", label: '${currentValue}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "turningOn"
				attributeState "turningOn", label: 'Turning On', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00A0DC", nextState: "turningOff"
				attributeState "turningOff", label: 'Turning Off', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "turningOn"
			}
			tileAttribute("power", key: "SECONDARY_CONTROL") {
				attributeState "power", label: '${currentValue}W'
			}
			tileAttribute("device.powerCutOff", key: "MARQUEE") {
				attributeState "enabled", label: '', icon: 'st.Outdoor.outdoor3', backgroundColor: "#44b621"
            	attributeState "disabled", label: '', icon: 'st.Outdoor.outdoor3', backgroundColor: "#FFFFFF"
			}
		}

		standardTile("refresh", "device.power", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "default", label: '', action: "refresh.refresh", icon: "st.secondary.refresh"
		}
        
        valueTile("CurrentVolt", "device.voltage", width:2, height:2 ) {
        	state "val", label: '전압\n${currentValue}V', defaultState: true
		}
        
        valueTile("CurrentCurrent", "device.currentCurrent", width:2, height:2 ) {
        	state "val", label: '전류\n${currentValue}A', defaultState: true
		}
        
        valueTile("AccumulateKWH", "device.accumulateWh", width:2, height:2 ) {
        	state "val", label: '누적전력\n${currentValue}Wh', defaultState: true
		}
        
        valueTile("powerFactor", "device.powerFactor", width:2, height:2 ) {
        	state "val", label: '역률\n${currentValue}%', defaultState: true
		}
        
        standardTile("tileLock", "lock", width:2, height:2) {
        	state "locked", label: 'locked', icon: 'st.tesla.tesla-locked', backgroundColor: "#00A0DC", action: "unlock"
            state "unlocked", label: 'unlocked', icon: 'st.tesla.tesla-unlocked', backgroundColor: "#FFFFFF", action: "lock"
        }
        
        standardTile("tilePowerCutOff", "device.powerCutOff", width:2, height:2) {
        	state "enabled", label: '', icon: 'st.Outdoor.outdoor3', backgroundColor: "#44b621"
            state "disabled", label: '', icon: 'st.Outdoor.outdoor3', backgroundColor: "#FFFFFF"
        }
		main "switch"
        details(["switch", "CurrentVolt", "CurrentCurrent", "tilePowerCutOff", "AccumulateKWH", "powerFactor", "refresh",  "tileLock"])
	}
}

private getSafeNumberValueFromState(stateKey) {
	def val = state[stateKey]
    def calVal = val ?: 0
    calVal = calVal instanceof Number ? calVal : zigbee.convertHexToInt(calVal)

	return calVal
}

// 현재 전압
private getVoltage(value) {
	def voltage = 0
	if( value ) {
		voltage = zigbee.convertHexToInt(value)
	}
	voltage = voltage / 1000
    return voltage
}

// 현재 전류
private getCurrent(value) {
	def current = 0
	if( value ) {
		current = zigbee.convertHexToInt(value)
	}
	current = current / 1000
    return current
}

// 누적전력
private getAccumlate(currentSummationDelivered) {
    def calCrrentSummationDelivered = currentSummationDelivered ?: 0
    calCrrentSummationDelivered = calCrrentSummationDelivered instanceof Number ? calCrrentSummationDelivered : zigbee.convertHexToInt(calCrrentSummationDelivered)
    def calMeteringMultiplier = getSafeNumberValueFromState('meteringMultiplier')
    def calMeteringDivisor = getSafeNumberValueFromState('meteringDivisor')
    if( calMeteringDivisor == 0 ) {
        calMeteringDivisor = 1
    }
    def accumulateWh = calCrrentSummationDelivered * calMeteringMultiplier / calMeteringDivisor * 1000
    
    def pattern = "##,###.###"
    def df = new java.text.DecimalFormat(pattern)
    def formatted = df.format(accumulateWh)
	return formatted
}

// 순시 전력
private getInstantDemend(instantDemend) {
	def calInstantDemend = instantDemend ?: 0
    calInstantDemend = calInstantDemend instanceof Number ? calInstantDemend : zigbee.convertHexToInt(calInstantDemend)
    def calMeteringMultiplier = getSafeNumberValueFromState('meteringMultiplier')
    def calMeteringDivisor = getSafeNumberValueFromState('meteringDivisor')
    if( calMeteringDivisor == 0 ) {
        calMeteringDivisor = 1
    }
    def momentaryW = calInstantDemend * calMeteringMultiplier / calMeteringDivisor * 1000
    return momentaryW
}

private long getTIme() {
    (new GregorianCalendar().time.time / 1000l).toLong()
}

private Map parseOnOffDesc(desc, clusterId, attrId, command, event) {
	def retMap = [:]
	if( desc == null ) {
    	return null
    }
    if( clusterId == "0006" && (command == "11" || command == "10")) {
    	log.debug "It's lock event, ignored, clusterId:${clusterId}"
    	return null
    }
    if( event != null && event.name == "switch") {
    	retMap["name"] = "switch"
        retMap["value"] = event.value
        return retMap
    }
    if( desc.startsWith("on/off") ) {
    	def tryEvent = zigbee.getEvent(desc)
        if(tryEvent != null && tryEvent.name == "switch") {
        	retMap.name = "switch"
            retMap.value = tryEvent.value
            return retMap
        }
    }
    if( clusterId == "0B" && (commandData == "00" || commandData == "01")) {
    	def onOffValue = commandData == "01" ? "on" : "off"
        retMap.name = "switch"
        retMap.value = onOffValue
        return retMap
    }

	return null
}

// Parse incoming device messages to generate events
def parse(String description) {
	def parseMap = zigbee.parseDescriptionAsMap(description)
    def event = zigbee.getEvent(description)
    
    def clusterId
    def attrId
    def commandData
    if( parseMap != null ) {
    	clusterId = parseMap.cluster ? parseMap.cluster : parseMap.clusterId
        clusterId = (clusterId != null) ? clusterId.toUpperCase() : null
        attrId = parseMap.attrId != null ? parseMap.attrId.toUpperCase() : null
        if( parseMap.data != null ) {
        	commandData = String.valueOf(parseMap.data[0])
        }
    }
    
    def onOffMap = parseOnOffDesc(description, clusterId, attrId, commandData, event)
    if( onOffMap ) {
    	log.info "Switch is ${onOffMap.value}"
    	def switchEvent = createEvent(name: onOffMap.name, value: onOffMap.value)
        return switchEvent
    }
    
    
    def eventStack = []
    if( parseMap != null ) {
    	def forceReturn = false

		if( clusterId == "0006" ) {
        	if( parseMap.command == "0B" ) {	
                if( commandData == "10" || commandData == "11") {	// 커맨드로 들어온 lock/unlock 처리
                	def strValue = String.valueOf(commandData) == "10" ? "locked" : "unlocked"
                	eventStack.push(createEvent( name: "lock", value: strValue))
                }
            } else if( attrId == "0010" ) { // refresh로 들어온 lock/unlock
            	def value = zigbee.convertHexToInt(parseMap.value)
                def strValue = value == 1 ? "locked" : "unlocked"
                eventStack.push(createEvent( name: "lock", value: strValue))
            } else {
            	log.warn "UNKNOWN for 0006: ${description}, ${parseMap}"
            }
            
        } else if( clusterId == "0B04" ) {
        	if( attrId == "0510" ) {
            	def powerFactor = zigbee.convertHexToInt(parseMap.value)
                eventStack.push(createEvent( name: "powerFactor", value: powerFactor))
            }
            
		} else if( clusterId == "0702" ) {
			def renewWatt = false
            def attrProcessor = { theAttrId, value ->
            	if( theAttrId == "0301") {
                	state.meteringMultiplier = value
                    forceReturn = true
                } else if( theAttrId == "0302") {
                	state.meteringDivisor = value
                    forceReturn = true
                } else if( theAttrId == "0000" ) {
                    def accumulateWh = getAccumlate(value)
                    eventStack.push(createEvent(name: "accumulateWh", value: accumulateWh))
                } else if( theAttrId == "0400") {
                	def instantDemend = getInstantDemend(value)
                    eventStack.push(createEvent(name: "power", value: instantDemend))
                } else if(theAttrId == "0901") {
                	def current = getCurrent(value)
                    eventStack.push(createEvent( name: "currentCurrent", value: current))
				} else if(theAttrId == "0902") {
                	def voltage = getVoltage(value)
                    eventStack.push(createEvent( name: "voltage", value: voltage))
                } else if(theAttrId == "0905" ) {
                	log.debug "${clusterId}, ${theAttrId}, ${value}, description is ${description}"
                	def powerCutOffEnabled = value == "01" ? "enabled" : "disabled"
                	eventStack.push(createEvent( name: "powerCutOff", value: powerCutOffEnabled))
                } else {
                	log.warn "Unhandle cluster: ${clusterId}, ${theAttrId}, ${value}, description is ${description}"
                }
            };
            
            def attrs = parseMap.additionalAttrs // 이젝스 확장
            if( attrs == null ) {	// 개별 리포팅: Refresh 호출시 개별 attribute는 이 속성으로 읽는다 
                attrProcessor(attrId, parseMap.value)
            } else { // 자동 리포팅: 디바이스에서 리포팅하는 정보는 attrs 배열 형태이다.
                attrs.each { attr -> 
                    attrProcessor(attr.attrId, attr.value)
                }
			}
		}
        
        // log.debug "eventStack: ${eventStack}, ${clusterId}, ${attrId}"
        if(!eventStack.isEmpty()) {
			return eventStack
		}
        if( forceReturn ) {
        	return
        }
    }

	if (event) {
        log.warn "Unhandled Event - description: ${description}, event: ${event}"
	} else {
		def cluster = zigbee.parse(description)
		if (cluster && cluster.clusterId == 0x0006 && cluster.command == 0x07) {
			if (cluster.data[0] == 0x00) {
				log.debug "ON/OFF REPORTING CONFIG RESPONSE: " + cluster
				return createEvent(name: "checkInterval", value: 60 * 12, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])
			} else {
				log.warn "ON/OFF REPORTING CONFIG FAILED- error code:${cluster.data[0]}"
				event = null
			}
		} else  {
			log.warn "DID NOT PARSE MESSAGE for description : $description"
		}
	}
    
    return event // may be null
}

def off() {
	// device.endpointId = 1
	// zigbee.off() + refresh()
    zigbee.off()
}

def on() {
    // device.endpointId = 1
    // zigbee.on() + refresh()
    zigbee.on()
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
	device.endpointId = 1
	return zigbee.onOffRefresh()
}

def refresh() {
    def endpointId = 1
    def delay = 50
    def cmds = []
    
    // on/off
    cmds << "st rattr 0x${device.deviceNetworkId} ${endpointId} 0x0006 0x0000"
    cmds << "delay ${delay}"
    // 잠금
    cmds << "st rattr 0x${device.deviceNetworkId} ${endpointId} 0x0006 0x0010"
    cmds << "delay ${delay}"
    
    // multiplier & divisor
    cmds << "st rattr 0x${device.deviceNetworkId} ${endpointId} 0x0702 0x0301"
    cmds << "delay ${delay}"
    cmds << "st rattr 0x${device.deviceNetworkId} ${endpointId} 0x0702 0x0302"
    cmds << "delay ${delay}"
    
    // 누적전력
    cmds << "st rattr 0x${device.deviceNetworkId} ${endpointId} 0x0702 0x0000"
    cmds << "delay ${delay}"
    
    // 순시전력
    cmds << "st rattr 0x${device.deviceNetworkId} ${endpointId} 0x0702 0x0400"
    cmds << "delay ${delay}"
    
    // 전류
    cmds << "st rattr 0x${device.deviceNetworkId} ${endpointId} 0x0702 0x0901"
    cmds << "delay ${delay}"
    
    // 전압
    cmds << "st rattr 0x${device.deviceNetworkId} ${endpointId} 0x0702 0x0902"
    cmds << "delay ${delay}"
    
    // 역률
    cmds << "st rattr 0x${device.deviceNetworkId} ${endpointId} 0x0B04 0x0510"
    cmds << "delay ${delay}"
    
    // 대기전력 차단
    cmds << "st rattr 0x${device.deviceNetworkId} ${endpointId} 0x0702 0x0905"
    cmds << "delay ${delay}"
    
    // configure reporting 
    // power factor
    cmds <<  "zdo bind 0x${device.deviceNetworkId} 1 1 0x0B04 {${device.zigbeeId}} {}"
    cmds << "delay 200"
    cmds << "zcl global send-me-a-report 0x0B04 0x0510 0x28 20 60 {0x3F800000}"
    cmds << "delay 200"
    cmds << "send 0x${device.deviceNetworkId} 1 1"
    cmds << "delay 200"

	// lock
    cmds <<  "zdo bind 0x${device.deviceNetworkId} 1 1 0x0006 {${device.zigbeeId}} {}"
    cmds << "delay 200"
    cmds << "zcl global send-me-a-report 0x0006 0x0010 0x20 5 30 {0x3F800000}"
    cmds << "delay 200"
    cmds << "send 0x${device.deviceNetworkId} 1 1"
    cmds << "delay 200"

	log.info "refresh operaion requested"
    return cmds
}

def configure() {
	// Device-Watch allows 2 check-in misses from device + ping (plus 1 min lag time)
	// enrolls with default periodic reporting until newer 5 min interval is confirmed
	sendEvent(name: "checkInterval", value: 2 * 10 * 60 + 1 * 60, displayed: false, data: [protocol: "zigbee", hubHardwareId: device.hub.hardwareID])

	device.endpointId = 1
    return refresh()
}

def lock() {
	log.info "LOCK"
    def endpointId = 1
    def cmds = []
    cmds << "st cmd 0x${device.deviceNetworkId} ${endpointId} 0x0006 0x10 {}"
    cmds << "delay 100"
    return cmds
}

def unlock() {
	log.info "UNLOCK"
    def endpointId = 1
    def cmds = []
    cmds << "st cmd 0x${device.deviceNetworkId} ${endpointId} 0x0006 0x11 {}"
    cmds << "delay 100"
    return cmds
}
