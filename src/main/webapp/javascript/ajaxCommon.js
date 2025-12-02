function xmlRequest(url, callBackFunction) {
    const req = new XMLHttpRequest();
    if (req) {
        req.onreadystatechange = () => callBackFunction(req);
	    req.open("GET", url, true);
	    req.send("");
    } 
}

function xmlGetRequest(command, parameters, successCallBack, failureCallBack, skipWaitIndicator) {
    fetchGet(command, parameters, successCallBack, failureCallBack, skipWaitIndicator, true)
}

function fetchGet(command, parameters, successCallBack, failureCallBack, skipWaitIndicator, responseIsXML) {
    if (!skipWaitIndicator) {
        showHourGlass();
    }

    let url = getContextRoot() + "/servlet?command=" + command;
    for (const key in parameters) {
        url = url + "&" + key + "=" + parameters[key];
    }

    fetch(url)
        .then((response) => {
            if (!skipWaitIndicator) {
                hideHourGlass();
            }
            if (response.ok) {
                return response.text();
            }
            if (typeof failureCallBack !== 'undefined') {
                failureCallBack();
                successCallBack = undefined;
            } else {
                throw new Error('fetch communication error');
            }
        })
        .then((data) => {
            if (successCallBack) {
                if (responseIsXML) {
                    const parser = new DOMParser();
                    const xmlDoc = parser.parseFromString(data, 'text/xml');
                    successCallBack(xmlDoc);
                } else {
                    successCallBack(data);
                }
            }
        })
        .catch(error => {
            if (!skipWaitIndicator) {
                hideHourGlass();
            }
            customAlert(resourceBundle["alert.communicationFailure"]);
            console.error("communication error:", error);
        });
}

function xmlPostRequest(command, parameters, successCallBack, failureCallBack) {
    showHourGlass();

    let postData = "";
    if (command) {
        postData = "command=" + command;
    }
    for (const key in parameters) {
        postData = postData + (postData.length > 0 ? "&" : "") + key + "=" + parameters[key];
    }

    const requestOptions = {
        method: 'POST',
        headers: {"Content-Type": "application/x-www-form-urlencoded"},
        body: postData
    };

    fetch(getContextRoot() + "/servlet", requestOptions)
        .then((response) => {
            hideHourGlass();
            if (response.ok) {
                return response.text();
            }
            if (typeof failureCallBack !== 'undefined') {
                failureCallBack();
            } else {
                throw new Error('fetch communication error');
            }
        })
        .then((data) => {
            if (successCallBack) {
                const parser = new DOMParser();
                const xmlDoc = parser.parseFromString(data, 'text/xml');
                successCallBack(xmlDoc);
            }
        })
        .catch(error => {
            hideHourGlass();
            customAlert(resourceBundle["alert.communicationFailure"]);
            console.error("communication error:", error);
        });
}

function htmlFragmentByXslt(xmlUrl, xslUrl, fragmentCont, callback) {
    // native XSLT processing will be removed in 2026 from all major browsers:
    // https://developer.chrome.com/docs/web-platform/deprecating-xslt?hl=de

    // XSLT with Javascript (google ajaxslt)
    htmlFragmentByXsltJavascript(xmlUrl, xslUrl, fragmentCont, callback);
}

function htmlFragmentByXsltJavascript(xmlUrl, xslUrl, fragmentCont, callback) {
	xmlRequest(xslUrl, function(req) {
        if (req.readyState === 4) {
            if (req.status === 200) {
			    const xslStyleSheet = req.responseXML;

	            xmlRequest(xmlUrl, function(req) {
                    if (req.readyState === 4) {
                        if (req.status === 200) {
			                const xmlDoc = req.responseXML;
                            // browser-independend client-side XSL transformation with google ajaxslt
                            fragmentCont.innerHTML = xsltProcess(xmlDoc, xslStyleSheet);
                            
                            if (callback) {
                                callback();
                            }
                        } else {
                            alert('cannot load xml from ' + xmlUrl);
                        }
                    }
                });
            } else {
                alert('cannot load xsl stylesheet from ' + xslUrl);
            }
        }
    });
}

function getFormData(formObj) {
    let buff= '';
    const elemNum = formObj.elements.length;
	
    for (let i = 0; i < elemNum; i++) {
	    formElem = formObj.elements[i];
	    switch (formElem.type) {
	        case 'checkbox' :
	            if (formElem.checked) {
	                buff += formElem.name + '=' + encodeURIComponent(formElem.value) + '&'
	            }
	            break;
	        case 'text':
	        case 'select-one':
	        case 'hidden':
	        case 'password':
	        case 'email':
	        case 'textarea':
	            buff += formElem.name + '=' + encodeURIComponent(formElem.value) + '&'
	            break;
	    }
    }
    return(buff);
}

function getFormDataAsProps(formObj) {
    const formParams = {};
    const elemNum = formObj.elements.length;
	
    for (let i = 0; i < elemNum; i++) {
	    let formElem = formObj.elements[i];
	    switch (formElem.type) {
	        case 'checkbox' :
	            if (formElem.checked) {
                    formParams[formElem.name] = encodeURIComponent(formElem.value);
	            }
	      
	            break;
	      
	        case 'text':
	        case 'select-one':
	        case 'hidden':
	        case 'password':
	        case 'email':
	        case 'textarea':
                formParams[formElem.name] = encodeURIComponent(formElem.value);
	            break;
	    }
    }
    return formParams;
}

function getPageYScrolled()
{
    if (!browserFirefox) 
    {
        return document.body.scrollTop;
    }
    
    return window.pageYOffset;
}

function getPageXScrolled()
{
    if (!browserFirefox) 
    {
        return document.body.scrollLeft;
    }
    
    return window.pageXOffset;
}

function showHourGlass() {
    const waitDivElem = document.createElement('div');
    
    waitDivElem.setAttribute("id", "waitDiv");
    
    const hourGlassElem = document.createElement('img');
    
    hourGlassElem.setAttribute("src", getContextRoot() + "/images/hourglass.gif");
    hourGlassElem.setAttribute("width", "32");
    hourGlassElem.setAttribute("height", "32");
    hourGlassElem.setAttribute("border", "0");
    
    waitDivElem.appendChild(hourGlassElem);
    
    const divWidth = 60;
    const divHeight = 30;
	
    waitDivElem.style.width = divWidth + "px";
    waitDivElem.style.height = divHeight + "px";

    document.getElementsByTagName('body')[0].appendChild(waitDivElem);    
	
	centerBox(waitDivElem);
	
    waitDivElem.style.visibility = "visible";
}

function hideHourGlass() {
    const waitDiv = document.getElementById("waitDiv");
    if (waitDiv) {
        document.getElementsByTagName('body')[0].removeChild(waitDiv);
    }
}

