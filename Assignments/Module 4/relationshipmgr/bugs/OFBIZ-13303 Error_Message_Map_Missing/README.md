# OFBIZ-13303 – errorMessageMap not returned

## Problem

OFBiz services were returning `errorMessageMap`, but it was missing in the final response.

## Root Cause

`errorMessageMap` was not defined as an OUT parameter in ModelService, so it was filtered out.

## Fix

Added `errorMessageMap` in ModelServiceReader as a default OUT parameter.

## Key Learning

OFBiz only returns parameters defined in ModelService.

## Reproduction Steps

1. Create a service returning errorMessageMap
2. Call service using dispatcher.runSync()
3. Observe missing errorMessageMap
4. Apply fix and test again


// errorMessageList
def = new ModelParam();
def.name = ModelService.ERROR_MESSAGE_LIST;
def.type = "java.util.List";
def.mode = ModelService.OUT_PARAM;
def.optional = true;
def.internal = true;
service.addParam(def);

+// FIX: Add errorMessageMap
+def = new ModelParam();
+def.name = ModelService.ERROR_MESSAGE_MAP;
+def.type = "java.util.Map";
+def.mode = ModelService.OUT_PARAM;
+def.optional = true;
+def.internal = true;
+service.addParam(def);