import org.moqui.impl.entity.EntityDefinition
import org.moqui.impl.entity.EntityFindBase
import org.moqui.impl.entity.EntityFindBuilder
import org.moqui.impl.entity.FieldInfo
import org.moqui.impl.entity.condition.EntityConditionImplBase

boolean renderOnly = (context.renderSqlOnly == true);

def activeProducts = ec.entity.find("assignment.demo.ProductDemo") // change to product
                  .condition("isVirtual","N")
                  .condition("productTypeId","FINISHED_GOOD")
                  .selectField("productId")
                  .selectField("productTypeId")
                  .selectField("internalName")
                  .useCache(false)

if(renderOnly){
    // Typecast to EntityFindBase to bypass the public interface
    // and access protected internal state variables (like distinct, limit, and fieldsToSelect)
    EntityFindBase efb = (EntityFindBase) activeProducts // add a log for object

    // Get the EntityDefinition to access
    // the XML metadata (translates Moqui field names like 'lastUpdatedStamp'
    // into SQL column names like 'LAST_UPDATED_STAMP').
    EntityDefinition ed = efb.getEntityDef() //add a log for object

    // Compiles all scattered conditions (maps, single fields, explicit conditions)
    // into one unified master condition tree for the WHERE clause.
    // Typecast to EntityConditionImplBase because this internal class contains the logic
    // to actually generate the raw SQL string.
    EntityConditionImplBase whereCondition = (EntityConditionImplBase) efb.getWhereEntityConditionInternal(ed) //add a log for object

    // Grab the list of field names the user explicitly requested (e.g., ['partyId', 'firstName']).
    List<String> selectFields = efb.fieldsToSelect

    def fieldInfoList = []
    if (selectFields) {
        selectFields.each { fname ->
            // Convert the string name into a FieldInfo object which holds the SQL column name and metadata.
            FieldInfo fi = ed.getFieldInfo(fname)
            if (fi) fieldInfoList << fi
        }
    } else {
        // If no fields were selected, behave like 'SELECT *' by grabbing the metadata for every field on the entity.
        fieldInfoList.addAll(ed.entityInfo.allFieldInfoArray.findAll { it != null })
    }

    // Convert the Groovy list into a strict Java array, because EntityFindBuilder requires an array to generate the SELECT clause.
    FieldInfo[] fieldInfoArray = fieldInfoList.toArray(new FieldInfo[0])

    EntityFindBuilder efBuilder = new EntityFindBuilder(ed,efb,whereCondition,fieldInfoArray)


// 3. Build the SELECT clause. The 3rd argument checks Moqui's database config
// to see if we need to add 'AS alias' to prevent column name collisions (useful for large joins).
    efBuilder.makeSqlSelectFields(fieldInfoArray,null,
    "true" == efBuilder.efi.getDatabaseNode(ed.groupName)?.attribute("add-unique-as"))
    
    efBuilder.makeSqlFromClause()
    efBuilder.makeWhereClause()
    efBuilder.makeGroupByClause()
    
    EntityConditionImplBase havingCondition = (EntityConditionImplBase) efb.havingEntityCondition
    if (havingCondition) {
        efBuilder.makeHavingClause(havingCondition)
    }

    List<String> orderBy = efb.orderByFields?:[]
    boolean hasLimit = (efb.limit != null || efb.offset != null)
    efBuilder.makeOrderByClause(orderBy, hasLimit)
    if(hasLimit) efBuilder.addLimitOffset(efb.limit, efb.offset)

    String sqlText = efBuilder.sqlTopLevel.toString()

    // Extract parameter values (the ? bindings) from the parameters list
    List paramValues = efBuilder.parameters.collect { p ->
        p.getValue()   // EntityConditionParameter.getValue() returns the raw Java value
    }


    ec.logger.info("=== A1Q02 Rendered SQL (BEFORE execution) ===\n${sqlText}")
    ec.logger.info("=== A1Q02 Parameters: ${paramValues}")

    result.renderedSql = sqlText
    result.renderedParams = paramValues
    result.productList = []
    return
}

// Execution Block (When renderSqlOnly is false)
def productListRaw = activeProducts.list()
List<Map> productList = []

for (def prod in productListRaw) {
    productList << [
        productId: prod.productId,
        productTypeId: prod.productTypeId,
        internalName: prod.internalName
    ]
}

result.renderedSql = null
result.renderedParams = []
result.productList = productList
