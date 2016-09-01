/**
 *  Whole House Fan
 *
 *  Copyright 2014 Brian Steere
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
 *  make thermostat optional -jlm 2016-08-27
 *  update namespace, author, description -jlm 2016-08-27
 *  create input/variable for minimum temperature difference -jlm 2016-08-31
 *
 */
definition(
    name: "Whole House Fan",
    namespace: "jlm-kkf",
    author: "Brian Steere & John Mason",
    description: "Toggle a whole house fan (switch) when: Outside is more than x degrees cooler than inside, Inside is above x temp, Thermostat is off (optional), window/door open (optional)",
    category: "Green Living",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Developers/whole-house-fan.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Developers/whole-house-fan%402x.png"
)


preferences {
	section("Outdoor") {
		input "outTemp", "capability.temperatureMeasurement", title: "Outdoor Thermometer", required: true
	}
    
    section("Indoor") {
    	input "inTemp", "capability.temperatureMeasurement", title: "Indoor Thermometer", required: true
        input "minTemp", "number", title: "Minimum Indoor Temperature", required: true
        input "minDiff", "numner", title: "Minimum Degrees warmer Inside", required: false
        input "fans", "capability.switch", title: "Vent Fan", multiple: true, required: true
    }
    
    section("Thermostat") {
    	input "thermostat", "capability.thermostat", title: "Thermostat", required: false
    }
    
    section("Windows/Doors") {
    	paragraph "[Optional] Only turn on the fan if at least one of these is open"
        input "checkContacts", "enum", title: "Check windows/doors", options: ['Yes', 'No'], required: true 
    	input "contacts", "capability.contactSensor", title: "Windows/Doors", multiple: true, required: false
    }
}

def installed() {
	log.debug "Installed with settings: ${settings}"

	initialize()
}

def updated() {
	log.debug "Updated with settings: ${settings}"

	unsubscribe()
	initialize()
}

def initialize() {
	state.fanRunning = false;
    
    subscribe(outTemp, "temperature", "checkThings");
    subscribe(inTemp, "temperature", "checkThings");
    if (thermostat) {
    	subscribe(thermostat, "thermostatMode", "checkThings");
    }
    subscribe(contacts, "contact", "checkThings");
}

def checkThings(evt) {
	def outsideTemp = settings.outTemp.currentTemperature
    def insideTemp = settings.inTemp.currentTemperature
    if (thermostat) {
    	def thermostatMode = settings.thermostat.currentThermostatMode
    }
    def somethingOpen = settings.checkContacts == 'No' || settings.contacts?.find { it.currentContact == 'open' }
    
    log.debug "Inside: $insideTemp, Outside: $outsideTemp, Thermostat: $thermostatMode, Something Open: $somethingOpen, Minimum Difference: $minDiff"
    
    def shouldRun = true;
    
    // if(thermostatMode != 'off') {
    if (themostat) {
    	if (thermostatMode != 'off') {
    		log.debug "Not running due to thermostat mode"
    		shouldRun = false;
    	}
    }

    if (minDiff) {
    	insideTemp -= minDiff
    }

    if(insideTemp < outsideTemp) {
    	log.debug "Not running due to insideTemp > outdoorTemp"
    	shouldRun = false;
    }
    
    if(insideTemp < settings.minTemp) {
    	log.debug "Not running due to insideTemp < minTemp"
    	shouldRun = false;
    }
    
    if(!somethingOpen) {
    	log.debug "Not running due to nothing open"
        shouldRun = false
    }
    
    if(shouldRun && !state.fanRunning) {
    	fans.on();
        state.fanRunning = true;
    } else if(!shouldRun && state.fanRunning) {
    	fans.off();
        state.fanRunning = false;
    }
}