function checkMultiUploadTargetExists(targetFileName, callbackRejected, callbackOk) {
    var url = getContextRoot() + "/servlet?command=ajaxRPC&method=existFile&param1=" + encodeURIComponent(targetFileName);
    
    xmlRequest(url, function(req) {
        if (req.readyState == 4) {
            if (req.status == 200) {
            
                var responseXml = req.responseXML;
                var resultItem = responseXml.getElementsByTagName("result")[0];
                var result = resultItem.firstChild.nodeValue;  
                
                var uploadRejected = false;
                if (result && (result == "true")) {
                    if (!confirm(targetFileName + " - " + resourceBundle["upload.file.exists"])) {
                        uploadRejected = true;
                        callbackRejected();
                    }
                }
                if (!uploadRejected) {
                    callbackOk();
                }
            } else {
                alert(resourceBundle["alert.communicationFailure"]);
            }
        }
    });          
}

