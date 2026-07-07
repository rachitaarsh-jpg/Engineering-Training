import org.moqui.impl.entity.EntityDefinition
import org.moqui.impl.entity.EntityFindBase
import org.moqui.impl.entity.EntityFindBuilder
import org.moqui.impl.entity.FieldInfo
import org.moqui.impl.entity.condition.EntityConditionImplBase
import org.moqui.entity.EntityDynamicView


boolean renderSqlOnly = (context.renderSqlOnly==true)

// Create a Dynamic View Entity equivalent to the SQL query
def ef = ec.entity.find("mantle.order.OrderHeader")
EntityDynamicView entityDynamicView = ef.makeEntityDynamicView()

// FROM OrderHeader
entityDynamicView.addMemberEntity("OH", "mantle.order.OrderHeader", null, null, null)

// JOIN OrderItem OI ON OH.orderId = OI.orderId
entityDynamicView.addMemberEntity("OI", "mantle.order.OrderItem", "OH", false, ["orderId": "orderId"])

// JOIN Product p ON oi.productId = p.productId
entityDynamicView.addMemberEntity("PRD", "assignment.demo.ProductDemo", "OI", false, ["productId": "productId"])

// JOIN GoodIdentification gi ON p.productId = gi.productId
entityDynamicView.addMemberEntity("GI", "assignment.demo.GoodIdentification", "PRD", false, ["productId": "productId"])

// SELECT fields
entityDynamicView.addAlias("PRD", "PRODUCT_ID", "productId", null)
entityDynamicView.addAlias("OH", "SHOPIFY_ID", "externalId", null) // externalId is on OrderHeader
entityDynamicView.addAlias("OH", "HOTWAX_ID", "orderId", null)
entityDynamicView.addAlias("GI", "NETSUITE_ID", "idValue", null)

// We need an alias for the field we want to filter on
entityDynamicView.addAlias("GI", "goodIdentificationTypeId", "goodIdentificationTypeId", null)

def productAcrossSystemList = ef.condition("goodIdentificationTypeId", "in", ["ERP_ID", "NETSUITE_PRODUCT_ID"])
                                .selectFields(["PRODUCT_ID", "SHOPIFY_ID", "HOTWAX_ID", "NETSUITE_ID"])

if(renderSqlOnly){
    EntityFindBase efb = (EntityFindBase) productAcrossSystemList
    ec.logger.info("DownCasted to EntityFindBase -> ${efb}")

    EntityDefinition ed = efb.getEntityDef()
    ec.logger.info("EntityDefinition Object -> ${ed}")

    EntityConditionImplBase whereCondition = (EntityConditionImplBase) efb.getWhereEntityConditionInternal(ed)
    ec.logger.info("whereCondition via EntityConditionImplBase -> ${whereCondition}")

    List<String> selectedFields = efb.fieldsToSelect

    def fieldInfoList = []

    if(selectedFields) {
        selectedFields.each { fname ->
            ec.logger.info("FieldName -> ${fname.toString()}")
            FieldInfo fi = ed.getFieldInfo(fname)
            ec.logger.info("FieldInfo -> ${fi}")
            if(fi) fieldInfoList << fi
        }
    } else {
        fieldInfoList.addAll(ed.entityInfo.allFieldInfoArray.findAll{it!=null})
    }

    FieldInfo[] fieldInfoArray = fieldInfoList.toArray(new FieldInfo[0])

    EntityFindBuilder efBuilder = new EntityFindBuilder(ed,efb,whereCondition,fieldInfoArray)

    efBuilder.makeSqlSelectFields(fieldInfoArray,null,
            "true" == efBuilder.efi.getDatabaseNode(ed.groupName)?.attribute("add-unique-as"))

    efBuilder.makeSqlFromClause()
    efBuilder.makeWhereClause()
    efBuilder.makeGroupByClause()

    EntityConditionImplBase havingCondition = (EntityConditionImplBase) efb.havingEntityCondition
    if(havingCondition){
        efBuilder.makeHavingClause(havingCondition)
    }

    List<String> orderBy = efb.orderByFields?:[]
    boolean hasLimit=(efb.limit!=null || efb.offset!=null)
    efBuilder.makeOrderByClause(orderBy,hasLimit)
    if(hasLimit) efBuilder.addLimitOffset(efb.limit,efb.offset)

    String sqlText = efBuilder.sqlTopLevel.toString()
    List paramValues = efBuilder.parameters.collect{p->
        p.getValue()
    }

    ec.logger.info("=== A1Q04 Rendered SQL (BEFORE execution) ===\n${sqlText}")
    ec.logger.info("=== A1Q04 Parameters: ${paramValues}")

    result.renderedSql = sqlText
    result.renderedParams = paramValues
    result.productList=[]
    return

}

List productList = productAcrossSystemList.list()

result.productList    = productList
result.renderedSql     = null
result.renderedParams  = []

ec.logger.info("A1Q04 found ${productList.size()}")