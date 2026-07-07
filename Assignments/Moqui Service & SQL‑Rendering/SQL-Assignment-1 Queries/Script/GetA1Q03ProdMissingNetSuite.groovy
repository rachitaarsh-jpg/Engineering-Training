import org.moqui.impl.entity.EntityDefinition
import org.moqui.impl.entity.EntityFindBase
import org.moqui.impl.entity.EntityFindBuilder
import org.moqui.impl.entity.FieldInfo
import org.moqui.impl.entity.condition.EntityConditionImplBase

boolean renderOnly = (context.renderSqlOnly == true);

def missingProducts = ec.entity.find("assignment.demo.ProductWithGoodView")
                    .condition("netsuiteId", null)
                    .selectField("productId")
                    .selectField("productTypeId")
                    .selectField("internalName")
                    .selectField("netsuiteId")
                    .useCache(false)

if(renderOnly){
    EntityFindBase efBase = (EntityFindBase) missingProducts
    EntityDefinition ed = efBase.getEntityDef()
    EntityConditionImplBase whereCondition = (EntityConditionImplBase) efBase.getWhereEntityConditionInternal(ed)

    List<String> selectFields = efBase.fieldsToSelect

    def fieldInfoList = []

    if(selectFields){
        selectFields.each {fname->
            FieldInfo fi = ed.getFieldInfo(fname)
            if(fi) fieldInfoList << fi
        }
    } else{
        fieldInfoList.addAll(ed.entityInfo.allFieldInfoArray.findAll{it!=null})
    }
    FieldInfo[] fieldInfoArray = fieldInfoList.toArray(new FieldInfo[0])

    EntityFindBuilder efb = new EntityFindBuilder(ed,efBase,whereCondition,fieldInfoArray)
    efb.makeSqlSelectFields(fieldInfoArray,null,
    "true" == efb.efi.getDatabaseNode(ed.groupName)?.attribute("add-unique-as"))

    efb.makeSqlFromClause()
    efb.makeWhereClause()
    efb.makeGroupByClause()

    EntityConditionImplBase havingCondition = (EntityConditionImplBase) efBase.havingEntityCondition
    if(havingCondition){
        efb.makeHavingClause(havingCondition)
    }

    List<String> orderBy = efBase.orderByFields?:[]
    boolean hasLimit=(efBase.limit!=null || efBase.offset!=null)
    efb.makeOrderByClause(orderBy,hasLimit)
    if(hasLimit) efb.addLimitOffset(efBase.limit,efBase.offset)

    String sqlText = efb.sqlTopLevel.toString()
    List paramValues = efb.parameters.collect{p->
        p.getValue()
    }

    ec.logger.info("=== A1Q03 Rendered SQL (BEFORE execution) ===\n${sqlText}")
    ec.logger.info("=== A1Q03 Parameters: ${paramValues}")

    result.renderedSql = sqlText
    result.renderedParams = paramValues
    result.productList=[]
    return
}

List productList = missingProducts.list()


result.productList    = productList
result.renderedSql     = null
result.renderedParams  = []

ec.logger.info("A1Q03 found ${productList.size()}")
