/**
 * GetA1Q01NewCustomers.groovy
 * ===========================
 * Implements SQL Assignment 1, Query 1 — New Customers Acquired in a Date Range.
 *
 * The service uses the Moqui entity engine (ec.entity.find()) exclusively.
 * No raw SQL execution (ec.entity.sqlFind) is used.
 *
 * Part 4 — Rendering SQL BEFORE execution
 * ----------------------------------------
 * When renderSqlOnly == true, this script builds the exact same EntityFindBuilder
 * that EntityFindImpl.iteratorExtended() would build, calls every makeSql* method
 * in the same order, then READS the sqlTopLevel StringBuilder — all before calling
 * makeConnection() or makePreparedStatement().
 *
 * Source references (see Part 3 & Part 5 write-up for full details):
 *   - EntityFindBase.groovy  — the query object (EntityFind + EntityFindBase)
 *   - EntityFindImpl.java    — iteratorExtended() / listExtended() / oneExtended()
 *   - EntityFindBuilder.java — makeSqlSelectFields, makeSqlFromClause,
 *                              makeWhereClause, makeGroupByClause,
 *                              makeOrderByClause, addLimitOffset
 *   - EntityQueryBuilder.java— sqlTopLevel StringBuilder, parameters List
 *   - condition/             — FieldValueCondition, ListCondition, etc.
 */


import org.moqui.entity.EntityCondition
import org.moqui.entity.EntityList
import org.moqui.impl.entity.EntityDefinition
import org.moqui.impl.entity.EntityFindBase
import org.moqui.impl.entity.EntityFindBuilder
import org.moqui.impl.entity.FieldInfo
import org.moqui.impl.entity.condition.EntityConditionImplBase

import java.sql.Timestamp
// ── 0. Input Binding ────────────────────────────────────────────────────────
// These are injected into the Groovy binding by Moqui's script runner:
//   ec           — ExecutionContext
//   context      — the service in-parameters Map
//   result       — the service out-parameters Map
// All we need to do is populate `result`.

Timestamp fromDateTs = (Timestamp) context.fromDate
Timestamp thruDateTs = (Timestamp) context.thruDate
boolean renderOnly   = (context.renderSqlOnly == true)

// ── 1. Build the EntityFind for Party ──────────────────────────────────────
// ec.entity.find("mantle.party.Party") returns EntityFindImpl which implements
// EntityFind (the public interface) and extends EntityFindBase (the abstract class
// that holds all state: conditions, fieldsToSelect, orderBy, cache, distinct, etc.)
//
// Casting to EntityFindBase gives us access to the protected / package-visible
// fields needed to drive EntityFindBuilder manually in Part 4.
//
// NOTE: We need a view-entity or secondary lookups for contact info.
// The base Party entity does NOT join Person / PartyRole / ContactMech.
// We use mantle.party.Party for the date-filter + role filter, then
// join Person inline, and do a separate lookup for contact-mech.

def partyFind = ec.entity.find("assignment.party.AssignmentCustomerView")
        .condition("lastUpdatedStamp", EntityCondition.GREATER_THAN_EQUAL_TO, fromDateTs)
        .condition("lastUpdatedStamp", EntityCondition.LESS_THAN, thruDateTs)
        .condition("roleTypeId", "Customer")
        .useCache(false)

// ── PART 4: Render SQL BEFORE Execution ────────────────────────────────────
if (renderOnly) {
    /*
     * The path in EntityFindImpl.iteratorExtended() (which list() calls):
     *
     *   1.  new EntityFindBuilder(ed, this, whereCondition, fieldInfoArray)
     *        → constructor sets sqlTopLevel = "SELECT "
     *   2.  efb.makeDistinct()          [if distinct]
     *   3.  efb.makeSqlSelectFields(…)  → appends col names after SELECT
     *   4.  efb.makeSqlFromClause()     → appends FROM table/join
     *   5.  efb.makeWhereClause()       → appends WHERE …
     *   6.  efb.makeGroupByClause()     → appends GROUP BY … (view-entity only)
     *   7.  efb.makeHavingClause(…)     → appends HAVING … (view-entity only)
     *   8.  efb.makeOrderByClause(…)    → appends ORDER BY …
     *   9.  efb.addLimitOffset(…)       → appends LIMIT/OFFSET
     *  10.  efb.makeForUpdate()         [if forUpdate]
     *
     *  THEN (and only then) does it touch the database:
     *  11.  efb.makeConnection()        ← first DB contact — we STOP before this
     *  12.  efb.makePreparedStatement() ← finalizes finalSql = sqlTopLevel.toString()
     *  13.  efb.setPreparedStatementValues()
     *  14.  efb.executeQuery()          ← actual JDBC execution
     *
     * getQueryTextList() is populated at step 14 (queryTextList.add(efb.finalSql))
     * which is AFTER execution.  That is why it is banned for "before execution".
     */

    EntityFindBase efBase = (EntityFindBase) partyFind
    EntityDefinition ed    = efBase.getEntityDef()

    // Resolve the combined where condition (merges singleCondField + simpleAndMap + whereEntityCondition)
    EntityConditionImplBase whereCondition =
            (EntityConditionImplBase) efBase.getWhereEntityConditionInternal(ed)

    // Resolve the FieldInfo array for selected fields
    def fieldInfoList = []
    List<String> selectFields = efBase.fieldsToSelect
    if (selectFields) {
        selectFields.each { fname ->
            FieldInfo fi = ed.getFieldInfo(fname)
            if (fi) fieldInfoList << fientityfindbuild
        }
    } else {
        // no specific fields selected → use all fields
        fieldInfoList.addAll(ed.entityInfo.allFieldInfoArray.findAll { it != null })
    }
    FieldInfo[] fieldInfoArray = fieldInfoList.toArray(new FieldInfo[0])

    // Build the EntityFindBuilder — same constructor EntityFindImpl uses
    EntityFindBuilder efb = new EntityFindBuilder(ed, efBase, whereCondition, fieldInfoArray)

    // Replay the same sequence of makeSql* calls (no connection, no execute)
    efb.makeSqlSelectFields(fieldInfoArray, null,
            "true" == efBase.efi.getDatabaseNode(ed.groupName)?.attribute("add-unique-as"))
    efb.makeSqlFromClause()
    efb.makeWhereClause()
    efb.makeGroupByClause()
    // no having for this simple entity
    List<String> orderByExpanded = efBase.orderByFields ?: []
    boolean hasLimitOffset = (efBase.limit != null || efBase.offset != null)
    efb.makeOrderByClause(orderByExpanded, hasLimitOffset)
    if (hasLimitOffset) efb.addLimitOffset(efBase.limit, efBase.offset)

    // ← STOP HERE.  sqlTopLevel now contains the full SQL string.
    // We never called makeConnection() so the database was NOT touched.
    String sqlText = efb.sqlTopLevel.toString()

    // Extract parameter values (the ? bindings) from the parameters list
    List paramValues = efb.parameters.collect { p ->
        p.getValue()   // EntityConditionParameter.getValue() returns the raw Java value
    }

    ec.logger.info("=== A1Q01 Rendered SQL (BEFORE execution) ===\n${sqlText}")
    ec.logger.info("=== A1Q01 Parameters: ${paramValues}")

    result.renderedSql    = sqlText
    result.renderedParams = paramValues
    result.customerList   = []
    return
}

// ── 2. Execute the View-Entity Query ─────────────────────────────────────────
EntityList partyList = partyFind.list()
List<Map> customerList = []

partyList.each { partyVal ->
    String partyId = partyVal.partyId

    // (Name and Role are already joined natively by AssignmentCustomerView! We only need to lookup contact info)

    // 2a. Look up email contact mech (ContactMechTypeEnumId = CmtEmailAddress)
    String email = null
    def emailPcmList = ec.entity.find("mantle.party.contact.PartyContactMech")
            .condition("partyId", partyId)
            .useCache(false)
            .list()
    for (def pcm : emailPcmList) {
        def cm = ec.entity.find("mantle.party.contact.ContactMech")
                .condition("contactMechId", pcm.contactMechId)
                .condition("contactMechTypeEnumId", "CmtEmailAddress")
                .useCache(false)
                .one()
        if (cm) { email = cm.infoString; break }
    }

    // 2b. Look up phone (TelecomNumber joined via PartyContactMech)
    String phone = null
    for (def pcm : emailPcmList) {          // reuse the PCM list
        def tn = ec.entity.find("mantle.party.contact.TelecomNumber")
                .condition("contactMechId", pcm.contactMechId)
                .useCache(false)
                .one()
        if (tn) { phone = tn.contactNumber; break }
    }

    customerList << [
            partyId   : partyId,
            firstName : partyVal.firstName,
            lastName  : partyVal.lastName,
            entryDate : partyVal.lastUpdatedStamp,
            email     : email,
            phone     : phone
    ]
}

result.customerList    = customerList
result.renderedSql     = null
result.renderedParams  = []

ec.logger.info("A1Q01 found ${customerList.size()} customer(s) between ${fromDateTs} and ${thruDateTs}")
