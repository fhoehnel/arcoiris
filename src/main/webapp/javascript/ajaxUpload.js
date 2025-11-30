function checkMultiUploadTargetExists(targetFileName, callbackRejected, callbackOk) {
    const parameters = {
        method: "existFile",
        param1: encodeURIComponent(targetFileName)
    }
    xmlGetRequest("ajaxRPC", parameters, responseXml => {
        const resultItem = responseXml.getElementsByTagName("result")[0];
        const result = resultItem.firstChild.nodeValue;
        let uploadRejected = false;
        if (result && result === "true") {
            if (!confirm(targetFileName + " - " + resourceBundle["upload.file.exists"])) {
                uploadRejected = true;
                callbackRejected();
            }
        }
        if (!uploadRejected) {
            callbackOk();
        }
    });
}

