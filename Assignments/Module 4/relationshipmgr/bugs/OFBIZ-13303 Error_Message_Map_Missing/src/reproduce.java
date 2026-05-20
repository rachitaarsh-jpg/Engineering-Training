public class reproduce {
    Map<String, String> errorMap = new HashMap<>();
errorMap.put("field", "Invalid value");

return ServiceUtil.returnError(null, null, errorMap, null);
}
