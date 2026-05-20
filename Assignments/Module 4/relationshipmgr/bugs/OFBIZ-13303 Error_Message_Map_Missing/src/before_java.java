public class before_java {
    //Inside ModelServiceReader -> createAttrDefs(...)
// Add the default optional parameters
    ModelParam def;

// responseMessage
    def = new ModelParam();
    def.name = ModelService.RESPONSE_MESSAGE;
    def.type = "String";
    def.mode = ModelService.OUT_PARAM;
    def.optional = true;
    def.internal = true;
        service.addParam(def);

// errorMessage
    def = new ModelParam();
    def.name = ModelService.ERROR_MESSAGE;
    def.type = "String";
    def.mode = ModelService.OUT_PARAM;
    def.optional = true;
    def.internal = true;
        service.addParam(def);

// errorMessageList
    def = new ModelParam();
    def.name = ModelService.ERROR_MESSAGE_LIST;
    def.type = "java.util.List";
    def.mode = ModelService.OUT_PARAM;
    def.optional = true;
    def.internal = true;
        service.addParam(def);

//  MISSING: errorMessageMap

// successMessage
    def = new ModelParam();
    def.name = ModelService.SUCCESS_MESSAGE;
    def.type = "String";
    def.mode = ModelService.OUT_PARAM;
    def.optional = true;
    def.internal = true;
        service.addParam(def);
}
