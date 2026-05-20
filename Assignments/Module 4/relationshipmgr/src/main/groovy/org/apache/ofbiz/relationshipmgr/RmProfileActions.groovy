package org.apache.ofbiz.relationshipmgr

import org.apache.ofbiz.entity.condition.EntityCondition
import org.apache.ofbiz.entity.condition.EntityOperator

partyId = parameters.partyId
if (partyId) {
    context.partyRoles = from("RmPartyRole").where("partyId", partyId).queryList()
    
    partyContactMechs = from("RmPartyContactMech").where("partyId", partyId).queryList()
    contactMechIds = partyContactMechs*.contactMechId
    
    if (contactMechIds) {
        context.contactMechs = from("RmContactMech").where(EntityCondition.makeCondition("contactMechId", EntityOperator.IN, contactMechIds)).queryList()
    } else {
        context.contactMechs = []
    }
}
