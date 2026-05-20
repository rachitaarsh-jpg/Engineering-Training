package org.apache.ofbiz.relationshipmgr

import org.apache.ofbiz.entity.GenericValue
import org.apache.ofbiz.base.util.UtilHttp
import org.apache.ofbiz.base.util.UtilDateTime

def createRmPerson() {
    Map parameters = UtilHttp.getParameterMap(request)
    String partyId = parameters.partyId
    
    if (!partyId) {
        partyId = delegator.getNextSeqId("RmParty")
    }
    
    try {
        // Create RmParty if it doesn't exist
        GenericValue rmParty = delegator.findOne("RmParty", [partyId: partyId], false)
        if (!rmParty) {
            rmParty = delegator.makeValue("RmParty", [partyId: partyId, partyTypeId: "PERSON"])
            rmParty.create()
        }
        
        // Create RmPerson
        GenericValue rmPerson = delegator.makeValue("RmPerson", [partyId: partyId])
        rmPerson.setNonPKFields(parameters)
        rmPerson.create()
        
        return "success"
    } catch (Exception e) {
        request.setAttribute("_ERROR_MESSAGE_", e.getMessage())
        return "error"
    }
}

def createRmPartyRole() {
    Map parameters = UtilHttp.getParameterMap(request)
    GenericValue userLogin = (GenericValue) session.getAttribute("userLogin")
    
    String partyId = parameters.partyId
    String roleTypeId = parameters.roleTypeId
    
    if (!partyId || !roleTypeId) {
        request.setAttribute("_ERROR_MESSAGE_", "Missing partyId or roleTypeId")
        return "error"
    }

    try {
        // Check if role already exists
        GenericValue existingRole = delegator.findOne("RmPartyRole", [partyId: partyId, roleTypeId: roleTypeId], false)
        if (existingRole) {
            org.apache.ofbiz.base.util.Debug.logInfo("Role ${roleTypeId} already exists for party ${partyId}, skipping creation", "RmEvents")
            return "success"
        }

        org.apache.ofbiz.base.util.Debug.logInfo("Creating Role with parameters: " + parameters, "RmEvents")
        dispatcher.runSync("createRmPartyRole", parameters + [userLogin: userLogin])
        return "success"
    } catch (Exception e) {
        request.setAttribute("_ERROR_MESSAGE_", e.getMessage())
        return "error"
    }
}

def deleteRmPartyRole() {
    Map parameters = UtilHttp.getParameterMap(request)
    GenericValue userLogin = (GenericValue) session.getAttribute("userLogin")
    try {
        dispatcher.runSync("deleteRmPartyRole", parameters + [userLogin: userLogin])
        return "success"
    } catch (Exception e) {
        request.setAttribute("_ERROR_MESSAGE_", e.getMessage())
        return "error"
    }
}

def createRmContactMechEmail() {
    Map parameters = UtilHttp.getParameterMap(request)
    GenericValue userLogin = (GenericValue) session.getAttribute("userLogin")
    try {
        // Create RmContactMech
        Map contactMechResult = dispatcher.runSync("createRmContactMech", [contactMechTypeId: "EMAIL_ADDRESS", infoString: parameters.infoString, userLogin: userLogin])
        String contactMechId = contactMechResult.contactMechId
        
        // Link to Party
        dispatcher.runSync("createRmPartyContactMech", [partyId: parameters.partyId, contactMechId: contactMechId, fromDate: UtilDateTime.nowTimestamp(), userLogin: userLogin])
        
        return "success"
    } catch (Exception e) {
        request.setAttribute("_ERROR_MESSAGE_", e.getMessage())
        return "error"
    }
}

def createRmContactMechPhone() {
    Map parameters = UtilHttp.getParameterMap(request)
    GenericValue userLogin = (GenericValue) session.getAttribute("userLogin")
    try {
        // Create RmContactMech
        Map contactMechResult = dispatcher.runSync("createRmContactMech", [contactMechTypeId: parameters.contactMechTypeId, infoString: parameters.infoString, userLogin: userLogin])
        String contactMechId = contactMechResult.contactMechId
        
        // Link to Party
        dispatcher.runSync("createRmPartyContactMech", [partyId: parameters.partyId, contactMechId: contactMechId, fromDate: UtilDateTime.nowTimestamp(), userLogin: userLogin])
        
        return "success"
    } catch (Exception e) {
        request.setAttribute("_ERROR_MESSAGE_", e.getMessage())
        return "error"
    }
}

def createRmPostalAddress() {
    Map parameters = UtilHttp.getParameterMap(request)
    GenericValue userLogin = (GenericValue) session.getAttribute("userLogin")
    try {
        // Create RmContactMech
        Map contactMechResult = dispatcher.runSync("createRmContactMech", [contactMechTypeId: "POSTAL_ADDRESS", userLogin: userLogin])
        String contactMechId = contactMechResult.contactMechId
        
        // Create RmPostalAddress
        dispatcher.runSync("createRmPostalAddress", [contactMechId: contactMechId, address1: parameters.address1, city: parameters.city, postalCode: parameters.postalCode, userLogin: userLogin])
        
        // Link to Party
        dispatcher.runSync("createRmPartyContactMech", [partyId: parameters.partyId, contactMechId: contactMechId, fromDate: UtilDateTime.nowTimestamp(), userLogin: userLogin])
        
        return "success"
    } catch (Exception e) {
        request.setAttribute("_ERROR_MESSAGE_", e.getMessage())
        return "error"
    }
}
