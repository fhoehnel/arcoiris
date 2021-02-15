var selectedForUpload = new Array();

var MAX_PICTURE_SIZE_SUM = 40000000;

var MAX_BLOG_TEXT_LENGTH = 4096;

var THUMB_SUBDIR_NAME = "_thumbnails400";
      
var xhr;
      
var lastUploadedFile;
      
var currentFileNum = 1;
	  
var totalSizeSum = 0;
	  
var totalLoaded = 0;
	  
var sizeOfCurrentFile = 0;

var pictureFileSize = 0;

var firefoxDragDrop = existFileReader();

var uploadStartedByButton = false;

var publicUrl = null;

var lastScrollPos = 0;

var daysWithEntries = new Array();

var searchPreviewTimeout = null;

var searchPreviewActive = false;

function existFileReader()
{
    try
    {
        var featureTest = new FileReader();
        if (featureTest != null) 
        {
            return true;
        } 
    }
    catch (Exception)
    {
    }
      
    return false;
}        

function isPictureFile(fileType) {
    lowerCaseFileType = fileType.toLowerCase();
          
    return((lowerCaseFileType.indexOf("jpg") >= 0) ||
           (lowerCaseFileType.indexOf("jpeg") >= 0) ||
           (lowerCaseFileType.indexOf("gif") >= 0) ||
           (lowerCaseFileType.indexOf("png") >= 0));
}

function selectedDuplicate(fileName) {
    for (var i = 0; i < selectedForUpload.length; i++) {
        var selectedFileName;
        if (browserSafari) {
            selectedFileName = selectedForUpload[i].fileName
        } else {
            selectedFileName = selectedForUpload[i].name
        }
      
        if (selectedFileName == fileName) {
            return true;
        }  
    }
          
    return false;
}

function prepareDropZone() {
    if (browserSafari) {
        // in Safari files can be dropped only to the file input component, not to a div
        return;
    }
      
    var dropZone;
    dropZone = document.getElementById("dropZone"); 
    dropZone.addEventListener("mouseover", hideHint, false);      
    dropZone.addEventListener("mouseout", showHint, false);  
    dropZone.addEventListener("dragenter", dragenter, false);  
    dropZone.addEventListener("dragover", dragover, false);  
    dropZone.addEventListener("drop", drop, false);      
}
    
function dragenter(e) {  
    e.stopPropagation();  
    e.preventDefault();  
}  
  
function dragover(e) {  
    e.stopPropagation();  
    e.preventDefault();  
}     
    
function drop(e) { 
    e.stopPropagation();  
    e.preventDefault();  
          
    var dt = e.dataTransfer;  
    var files = dt.files;  

    if (firefoxDragDrop)
    {
        handleFiles(files);  
    }
    else
    {   
        positionStatusDiv();

        var fileNum = files.length;
  
        for (var i = 0; i < fileNum; i++) { 
             selectedForUpload.push(files[i]);
        }

        var file = selectedForUpload.shift();
        if (file) {
            new singleFileBinaryUpload(file); 
        }
    }
}     
  
function showHint() {
    var hintText = document.getElementById("dragDropHint");
    if (hintText != null) {
        hintText.style.visibility = 'visible';  
    }
}
  
function hideHint() {
    var hintText = document.getElementById("dragDropHint");
    if (hintText != null) {
        hintText.style.visibility = 'hidden';  
    }
}

function handleFiles(files) {  
    var dropZone = document.getElementById("dropZone");  
    var uploadFileList = document.getElementById("uploadFiles");

    for (var i = 0; i < files.length; i++) {  
        var file = files[i];  
              
        var fileName;
        var fileSize;
              
        if (browserSafari) {
            fileName = file.fileName;
            file.size = file.fileSize;
        } else {
            fileName = file.name
            fileSize = file.size;
        }
             
        if (!isPictureFile(file.type)) {
        	alert(fileName + ': ' + resourceBundle["blog.noPictureFile"]);
        } else {
            if (file.size > SINGLE_FILE_MAX_SIZE) {
                alert(fileName + ': ' + resourceBundle["blog.uploadFileTooLarge"]);
            } else {
                if (!selectedDuplicate(fileName)) {
                    if (!browserSafari) {
                        var hintText = document.getElementById("dragDropHint");
                        if (hintText) {
                            dropZone.removeChild(hintText);
                        }
                    }

                    if (firefoxDragDrop) {  
                          
                        if (pictureFileSize < MAX_PICTURE_SIZE_SUM) {
                            var img = document.createElement("img");  
                          
                            img.className += (img.className ? " " : "") + "uploadPreview";
                          
                            img.file = file;  
                            dropZone.appendChild(img);  
         
                            var reader = new FileReader();  
                            reader.onload = (function(aImg) { return function(e) { aImg.src = e.target.result; }; })(img);  
                            reader.readAsDataURL(file);  
                                  
                            pictureFileSize += file.size;
                         }
                    } 
                          
                    var listElem = document.createElement("li");
                          
                    listElem.className += (listElem.className ? " " : "") + "selectedForUpload";
                          
                    var listElemText = document.createTextNode(fileName);
                    listElem.appendChild(listElemText);
                    uploadFileList.appendChild(listElem);
                          
                    selectedForUpload.push(file);
                }

                /*
                document.getElementById('selectedForUpload').style.visibility = 'visible';
                document.getElementById('selectedForUpload').style.display = 'block';
                */
            }
        }
    }  
} 

function submitPost() {
    var blogText = document.getElementById("blogText").value;

    if (trim(blogText).length == 0) {
        if ((document.getElementById("blogForm").cmd.value != "changeEntry") && (selectedForUpload.length == 0)) {
            alert(resourceBundle["blog.emptyPost"]);
            return;
        }
    }
    
    if (trim(blogText).length == 0) {
        if (!confirm(resourceBundle["blog.confirmSendEmptyText"])) {
            return;
        }
    } else if (trim(blogText).length > MAX_BLOG_TEXT_LENGTH) {
        alert(resourceBundle["blog.textTooLong"]);
        return;
    }
    
    var geoDataSwitcher = document.getElementById("blogGeoDataSwitcher");
    
    if (geoDataSwitcher && geoDataSwitcher.checked) {
        var latitude = parseFloat(document.getElementById("latitude").value);

        if (isNaN(latitude) || (latitude < (-90.0)) || (latitude > 90.0)) {
            alert(resourceBundle["error.latitudeInvalid"]);
            return;  
        }    

        var longitude = parseFloat(document.getElementById("longitude").value);

        if (isNaN(longitude) || (longitude < (-180.0)) || (longitude > 180.0)) {
            alert(resourceBundle["error.longitudeInvalid"]);
            return;  
        }    
    }
    
    if (selectedForUpload.length > 0) {
        positionStatusDiv();
        sendFiles();
    } else {
        document.getElementById("blogForm").submit();
    }   
}

function positionStatusDiv()
{
    var statusDiv = document.getElementById("uploadStatus");

    var statusDivWidth = statusDiv.offsetWidth;
    var statusDivHeight = statusDiv.offsetHeight; 

    var windowWidth;
    var windowHeight;
    var yScrolled;
    var xScrolled = 0;

    if (browserFirefox) 
    {
        windowWidth = window.innerWidth;
        windowHeight = window.innerHeight;
        yScrolled = window.pageYOffset;
        xScrolled = window.pageXOffset;
    }
    else 
    {
        windowWidth = document.body.clientWidth;
        windowHeight = document.body.clientHeight;
        yScrolled = document.body.scrollTop;
    }

    statusDiv.style.top = ((windowHeight - statusDivHeight) / 2 + yScrolled) + 'px';
    statusDiv.style.left = ((windowWidth - statusDivWidth) / 2 + xScrolled) + 'px';
}

function hideBrowserSpecifics()
{
    document.getElementById('lastUploaded').style.visibility = 'hidden';
    document.getElementById('lastUploaded').style.display = 'none';
    /*
    document.getElementById('selectedForUpload').style.visibility = 'hidden';
    document.getElementById('selectedForUpload').style.display = 'none';
    */
          
    if (browserSafari)
    {
        document.getElementById('dropTarget').style.visibility = 'hidden';
        document.getElementById('dropTarget').style.display = 'none';
    }
}

function sendFiles() {  
    uploadStartedByButton = true;

    var filesToUploadNumCont = document.getElementById("filesToUploadNum");

    filesToUploadNumCont.innerHTML = selectedForUpload.length;  
		  
	for (var i = 0; i < selectedForUpload.length; i++) {
	    if (browserSafari) {
	        totalSizeSum += selectedForUpload[i].fileSize;
	    } else {
		    totalSizeSum += selectedForUpload[i].size;
		}
	}
		  
    var file = selectedForUpload.shift();
          
    if (file) {
        singleFileBinaryUpload(file);
    }
} 
      
function singleFileBinaryUpload(file) {
      
    var fileName;
    var fileSize;
    if (browserSafari) {
        fileName = file.fileName;
        fileSize = file.fileSize;
    } else {
        fileName = file.name
        fileSize = file.size;
    }
      
    sizeOfCurrentFile = fileSize;
	  
	checkMultiUploadTargetExists(fileName, 
	    function() {
            var nextFile = selectedForUpload.shift();
            if (nextFile) {
                new singleFileBinaryUpload(nextFile)
            }
	    }, 
	    function() {
            lastUploadedFile = fileName;
      
            document.getElementById("currentFile").innerHTML = shortText(fileName, 50);
          
            document.getElementById("statusText").innerHTML = "0 " + resourceBundle["label.of"] + " " + formatDecimalNumber(fileSize) + " bytes ( 0%)";

            var statusWin = document.getElementById("uploadStatus");
            statusWin.style.visibility = 'visible';

            var now = new Date();

            var serverFileName = document.getElementById("dateYear").value + "-" +
                                 document.getElementById("dateMonth").value + "-" +
                                 document.getElementById("dateDay").value + "-" +
                                 now.getTime() + "-" + currentFileNum + 
                                 getFileNameExt(fileName).toLowerCase();
                         
            var firstUploadServerFileName = document.getElementById("firstUploadFileName");
            if (firstUploadServerFileName.value.length == 0) {
                firstUploadServerFileName.value = serverFileName;
            }

            var uploadUrl = getContextRoot() + "/upload/singleBinary/blog/" + serverFileName; 

            xhr = new XMLHttpRequest();  

            xhr.onreadystatechange = handleUploadState;
            xhr.upload.addEventListener("progress", updateProgress, false);
            xhr.upload.addEventListener("load", uploadComplete, false);

            xhr.open("POST", uploadUrl, true);  

	        if (!browserMSIE) {
                xhr.overrideMimeType('text/plain; charset=x-user-defined-binary');  
	        }
         
            if (firefoxDragDrop) {
                try {
                    xhr.sendAsBinary(file.getAsBinary());    
                } catch (ex) {
                    // Chrome has no file.getAsBinary() function
                    xhr.send(file);
                }
            } else {
                xhr.send(file);
            }    
	    }
	);
}

function handleUploadState() {
    if (xhr.readyState == 4) {
        var statusWin = document.getElementById("uploadStatus");
        statusWin.style.visibility = 'hidden';

        if (xhr.status == 200) {
			  
            totalLoaded += sizeOfCurrentFile;
			  
            // start uploading the next file
            var file = selectedForUpload.shift();
            if (file) {
		        currentFileNum++;
                var currentFileNumCont = document.getElementById("currentFileNum");
                currentFileNumCont.innerHTML = currentFileNum;  
				  
                new singleFileBinaryUpload(file)
            } else {
                if (firefoxDragDrop || uploadStartedByButton) {
                    document.getElementById("blogForm").submit();
                } else {
                    document.getElementById('lastUploadedFile').innerHTML = lastUploadedFile;
                    document.getElementById('lastUploaded').style.visibility = 'visible';
                    document.getElementById('lastUploaded').style.display = 'block';
                    document.getElementById('doneButton').style.visibility = 'visible';
                }
            }
        } else {
            alert(resourceBundle["upload.error"] + " " + lastUploadedFile);
            var file = selectedForUpload.shift();
            if (file) {
		        currentFileNum++;
                var currentFileNumCont = document.getElementById("currentFileNum");
                currentFileNumCont.innerHTML = currentFileNum;  
                new singleFileBinaryUpload(file)
			}
        }
    }
}

function updateProgress(e) {
    if (e.lengthComputable) {  
        var percent = Math.round((e.loaded * 100) / e.total);  
                
        document.getElementById("statusText").innerHTML = formatDecimalNumber(e.loaded) + " " + resourceBundle["label.of"] + " " + formatDecimalNumber(e.total) + " bytes (" + percent + "%)";

        document.getElementById("done").width = 3 * percent;

        document.getElementById("todo").width = 300 - (3 * percent);
			  
        percent = Math.round(((totalLoaded + e.loaded) * 100) / totalSizeSum);

        document.getElementById("totalStatusText").innerHTML = formatDecimalNumber(totalLoaded + e.loaded) + " " + resourceBundle["label.of"] + " " + formatDecimalNumber(totalSizeSum) + " bytes (" + percent + "%)";

        document.getElementById("totalDone").width = 3 * percent;

        document.getElementById("totalTodo").width = 300 - (3 * percent);
    }  
}
      
function uploadComplete(e) {
    document.getElementById("statusText").innerHTML = "100 %";

    document.getElementById("done").width = 300;

    document.getElementById("todo").width = 0;
}
      
function returnToList() {
    if (confirm(resourceBundle["blog.confirmCancel"])) {

        var returnURL = getContextRoot() + "/servlet?command=blog&cmd=list";
    
        var posInPage = document.getElementById("posInPage");
    
        if (posInPage && (posInPage.value.length > 0)) {
            returnURL = returnURL + "&random=" + ((new Date()).getTime()) + "#entry-" + posInPage.value;
        }
    
        window.location.href = returnURL;
    }
}

function editBlogEntry(fileName, posInPage) {
    window.location.href = getContextRoot() + "/servlet?command=blog&cmd=editEntry&fileName=" + encodeURIComponent(fileName) + "&posInPage=" + posInPage;
}

function jsComments(path) {
    var commentWin = window.open(getContextRoot() + "/servlet?command=listComments&actPath=" + encodeURIComponent(path),"commentWin","status=no,toolbar=no,location=no,menu=no,scrollbars=yes,width=550,height=400,resizable=yes,left=80,top=100,screenX=80,screenY=100");
    commentWin.focus();
}

function deleteBlogEntry(fileName) {
    if (!confirm(resourceBundle["blog.confirmDelete"])) {
        return;
    }

    showHourGlass();
    
    var url = getContextRoot() + "/servlet?command=blog&cmd=deleteEntry&fileName=" + encodeURIComponent(fileName);
    
    xmlRequest(url, function(req) {
        if (req.readyState == 4) {
            if (req.status == 200) {
			    var responseXml = req.responseXML;
    
                var success = null;
                var successItem = responseXml.getElementsByTagName("success")[0];
                if (successItem) {
                    success = successItem.firstChild.nodeValue;
                }         
    
                hideHourGlass();    
    
                if (success == "deleted") {
                    window.location.href = getContextRoot() + "/servlet?command=blog";
                } else {
                    alert(resourceBundle["blog.deleteError"]);
                }
            } else {
                alert(resourceBundle["alert.communicationFailure"]);
                hideHourGlass();    
            }
        }
    });
}

function moveBlogEntryUp(fileName, posInPage) {
    moveBlogEntry(fileName, "up", posInPage);
}

function moveBlogEntryDown(fileName, posInPage) {
    moveBlogEntry(fileName, "down", posInPage);
}

function moveBlogEntry(fileName, direction, posInPage) {

    showHourGlass();
    
    var url = getContextRoot() + "/servlet?command=blog&cmd=moveEntry&fileName=" + encodeURIComponent(fileName) + "&direction=" + direction;
    
    xmlRequest(url, function(req) {
        if (req.readyState == 4) {
            if (req.status == 200) {
			    var responseXml = req.responseXML;
    
                var success = null;
                var successItem = responseXml.getElementsByTagName("success")[0];
                if (successItem) {
                    success = successItem.firstChild.nodeValue;
                }         
    
                hideHourGlass();    
    
                if (success == "true") {
                    if ((direction == "up") && (posInPage > 1)) {
                        posInPage--;
                    } else if (direction == "down") {
                        posInPage++;
                    }
    
                    window.location.href = getContextRoot() + "/servlet?command=blog&random=" + (new Date().getTime()) + "#entry-" + posInPage;
                } else {
                    alert(resourceBundle["blog.moveError"]);
                }
            } else {
                alert(resourceBundle["alert.communicationFailure"]);
                hideHourGlass();    
            }
        }
    });
}

function changeBlogEntryPosition(fileName, posInPage) {
	
    showHourGlass();

    var changePosCont = document.createElement("div"); 
    changePosCont.id = "changePosCont";
    changePosCont.setAttribute("class", "changePosCont");
    changePosCont.setAttribute("posInPage", posInPage);
    
    document.body.appendChild(changePosCont);

    var xmlUrl = getContextRoot() + "/servlet?command=blog&cmd=altPositions&fileName=" + encodeURIComponent(fileName);
        
    var xslUrl = getContextRoot() + "/xsl/blog/altPositions.xsl";    
        
    htmlFragmentByXslt(xmlUrl, xslUrl, changePosCont, function() {
        setBundleResources();
        centerBox(changePosCont);
        changePosCont.style.visibility = "visible";
        hideHourGlass();
    });
}

function hidePositionSelection() {
	var changePosCont = document.getElementById("changePosCont");
	if (changePosCont) {
		changePosCont.parentNode.removeChild(changePosCont);
	}
}

function selectTargetPosition(targetPos) {
    showHourGlass();
	
	if (!targetPos) {
		var targetPosSelect = document.getElementById("targetPos");
		targetPos = targetPosSelect[targetPosSelect.selectedIndex].value;
	}
	
	document.getElementById("newPos").value = targetPos;
	
	var formData = getFormData(document.getElementById("targetPosForm"));
	
	xmlRequestPost(getContextRoot() + "/servlet", formData, handleMovedToPos);	
}

function handleMovedToPos(req) {
    if (req.readyState == 4) {
        if (req.status == 200) {
            var resultElem = req.responseXML.getElementsByTagName("result")[0];            
            var success = resultElem.getElementsByTagName("success")[0].firstChild.nodeValue;

            if (success == 'true') {
                window.location.href = getContextRoot() + "/servlet?command=blog";
            }
            hideHourGlass();    
        } else {
            alert(resourceBundle["alert.communicationFailure"]);
            hideHourGlass();    
        }
    }
}

function loadGoogleMapsAPIScriptCode(googleMapsAPIKey) {
    var script = document.createElement("script");
    script.type = "text/javascript";

    if (window.location.href.indexOf("https") == 0) {
        script.src = "https://maps.google.com/maps/api/js?callback=handleGoogleMapsApiReady&key=" + googleMapsAPIKey;
    } else {
        script.src = "http://maps.google.com/maps/api/js?callback=handleGoogleMapsApiReady&key=" + googleMapsAPIKey;
    }        
    document.body.appendChild(script);
}
  
function handleGoogleMapsApiReady() {
    // console.log("Google Maps API loaded");
}
  
var posMarker;
  
function selectLocation() {
    var markerPos = posMarker.getPosition();
  
    document.blogForm.latitude.value = markerPos.lat(); 
    document.blogForm.longitude.value = markerPos.lng();
       
    hideMap();
}
  
function showMap(selectLocation) {
    document.getElementById("mapFrame").style.display = 'block';

    var latitude = document.blogForm.latitude.value;

    var coordinatesNotYetSelected = false;

    if (latitude == '') {
        coordinatesNotYetSelected = true;
            
        if (selectLocation) {
            latitude = '51.1';
        } else {
            alert(resourceBundle["alert.missingLatitude"]);
            return;
        }
    }
  
    var longitude = document.blogForm.longitude.value;

    if (longitude == '') {
        coordinatesNotYetSelected = true;

        if (selectLocation) {
            longitude = '13.76';
        } else {
            alert(resourceBundle["alert.missingLongitude"]);
            return;
        }
    }

    var zoomFactor = parseInt(document.blogForm.zoomFactor[document.blogForm.zoomFactor.selectedIndex].value);
      
    var infoText;

    if (selectLocation) {
        infoText = resourceBundle["label.hintGoogleMapSelect"];
    } else {
        infoText = document.blogForm.infoText.value;
    }        
      
    var mapCenter = new google.maps.LatLng(latitude, longitude);
    
    var myOptions = {
        zoom: zoomFactor,
        center: mapCenter,
        mapTypeId: google.maps.MapTypeId.HYBRID
    }
      
    var map = new google.maps.Map(document.getElementById("map"), myOptions);      
          
    var markerPos = new google.maps.LatLng(latitude, longitude);

    posMarker = new google.maps.Marker({
        position: markerPos
    });

    posMarker.setMap(map);
        
    if ((selectLocation && coordinatesNotYetSelected) ||
        (!selectLocation && (infoText != ''))) {
        var infowindow = new google.maps.InfoWindow({
            content: '<div style="width:160px;height:40px;overflow-x:auto;overflow-y:auto">' + infoText + '</div>'
        });

        infowindow.open(map, posMarker);
    }    
        
    google.maps.event.addListener(map, 'click', function(event) {
        var clickedPos = event.latLng;
        posMarker.setPosition(clickedPos);
        // map.setCenter(clickedPos);
    });        

    centerBox(document.getElementById("mapFrame"));

    document.getElementById("mapFrame").style.visibility = 'visible';
    
    requestFullScreen(document.getElementById("map"));
    
    var mapCont = document.getElementById("map");
    
   	var mapButtonCont = document.createElement("form");
   	mapButtonCont.setAttribute("class", "mapButtonCont");
   	mapCont.appendChild(mapButtonCont);

   	var closeButton = document.createElement("input");
   	closeButton.setAttribute("type", "button");
   	closeButton.setAttribute("value", resourceBundle["button.closeMap"]);
   	closeButton.setAttribute("onclick", "hideMap()");
   	closeButton.setAttribute("class", "mapButton");
   	mapButtonCont.appendChild(closeButton);
   	
    if (selectLocation) {
       	var selectButton = document.createElement("input");
       	selectButton.setAttribute("type", "button");
       	selectButton.setAttribute("value", resourceBundle["button.save"]);
       	selectButton.setAttribute("onclick", "selectLocation()");
       	selectButton.setAttribute("class", "mapButton");
       	mapButtonCont.appendChild(selectButton);
    }
}  

function hideMap() {
	
    cancelFullScreen(document.getElementById("map"));
	
    document.getElementById("mapFrame").style.visibility = 'hidden';
    document.getElementById("mapFrame").style.display = 'none';
}

function toggleGeoData(checkbox) {

    var geoDataCont = document.getElementById("blogGeoTagCont");

    if (checkbox.checked) {
        geoDataCont.style.display = "block";
    } else {
        geoDataCont.style.display = "none";
    }
}

function publishBlog() {
    var publishCont = document.getElementById("publishCont");
        
    if (!publishCont) {
        alert('publishCont is not defined');
        return;
    }
        
    var contextRoot = getContextRoot();    
        
    var xmlUrl = contextRoot + "/servlet?command=blog&cmd=publishForm";
        
    var xslUrl = contextRoot + "/xsl/blog/publishBlog.xsl";    
        
    htmlFragmentByXslt(xmlUrl, xslUrl, publishCont, function() {
        setBundleResources();
        centerBox(publishCont);
        publishCont.style.visibility = "visible";
    });
}

function hidePublishForm() {
    var publishCont = document.getElementById("publishCont");
        
    if (!publishCont) {
        alert('publishCont is not defined');
        return;
    }

    publishCont.style.visibility = "hidden";
}

function validatePublishFormAndSubmit() {
    var daysPerPage = parseInt(document.getElementById("visitorDaysPerPage").value);

    if (isNaN(daysPerPage) || (daysPerPage < 1) || (daysPerPage > 32)) {
        alert(resourceBundle["blog.invalidDaysPerPageValue"]);
        document.getElementById("daysPerPage").focus();
        return;
    }
    
    var expirationDays = parseInt(document.getElementById("expirationDays").value);

    if (isNaN(expirationDays) || (expirationDays < 1) || (expirationDays > 10000)) {
        alert(resourceBundle["blog.invalidExpirationDays"]);
        document.getElementById("expirationDays").focus();
        return;
    }
    
    if (document.getElementById("language").value.length == 0) {
        alert(resourceBundle["error.missingLanguage"]);
        document.getElementById("language").focus();
        return;
    }
    
	var formData = getFormData(document.getElementById("publishForm"));
	
	xmlRequestPost(getContextRoot() + "/servlet", formData, showPublishResult);	
}

function showPublishResult(req) {
    if (req.readyState == 4) {
        if (req.status == 200) {
            var resultElem = req.responseXML.getElementsByTagName("result")[0];            
            var success = resultElem.getElementsByTagName("success")[0].firstChild.nodeValue;

            if (success == 'true') {
                publicUrl = resultElem.getElementsByTagName("publicUrl")[0].firstChild.nodeValue;            

                document.getElementById("publishTable").innerHTML = "";

                var tableRow = document.createElement("tr");
                document.getElementById("publishTable").appendChild(tableRow);

                var tableCell = document.createElement("td");
                tableCell.setAttribute("class", "formParm1");
                tableCell.innerHTML = resourceBundle["blog.publicLink"] + ":";
                tableRow.appendChild(tableCell);

                tableRow = document.createElement("tr");
                document.getElementById("publishTable").appendChild(tableRow);

                tableCell = document.createElement("td");
                tableCell.setAttribute("class", "formParm2");
                tableRow.appendChild(tableCell);
                
                var urlInput = document.createElement("textarea");
                urlInput.id = "publicUrl";
                urlInput.setAttribute("class", "publicLinkCopyField");
                urlInput.setAttribute("readonly", "readonly");
                urlInput.value = publicUrl;
                tableCell.appendChild(urlInput);
                
                urlInput.focus();
                urlInput.select();

                tableRow = document.createElement("tr");
                document.getElementById("publishTable").appendChild(tableRow);

                tableCell = document.createElement("td");
                tableCell.style.paddingTop = "20px";
                tableRow.appendChild(tableCell);

                var closeButton = document.createElement("input");
                closeButton.setAttribute("type", "button");
                closeButton.setAttribute("value", resourceBundle["button.closewin"]);
                closeButton.setAttribute("onclick", "hidePublishForm()");
                tableCell.appendChild(closeButton);
                
                var copyButton = document.createElement("input");
                copyButton.setAttribute("type", "button");
                copyButton.setAttribute("value", resourceBundle["button.copyToClip"]);
                copyButton.setAttribute("onclick", "copyPublicUrlToClip()");
                copyButton.setAttribute("style", "float:right");
                tableCell.appendChild(copyButton);
                
                document.getElementById("publishBlogButton").style.display = "none";                    
                document.getElementById("unpublishButton").style.display = "inline";
                document.getElementById("publicURLButton").style.display = "inline";

                centerBox(document.getElementById("publishCont"));
            }
        }
    }
}

function queryPublicLink(userIsVisitor) {
    var url = getContextRoot() + "/servlet?command=blog&cmd=getPublicURL";
    
    if (userIsVisitor) {
        xmlRequest(url, handleVisitorPublicLinkResult);
    } else {
        xmlRequest(url, handleQueryPublicLinkResult);
    }
}

function handleVisitorPublicLinkResult(req) {
    if (req.readyState == 4) {
        if (req.status == 200) {
            var resultElem = req.responseXML.getElementsByTagName("result")[0];            
            var success = resultElem.getElementsByTagName("success")[0].firstChild.nodeValue;
            if (success == 'true') {
                publicUrl = resultElem.getElementsByTagName("publicUrl")[0].firstChild.nodeValue;   
                jQuery(".icon-blog-share").css("display", "inline");
            }
        } else {
        	if (console.log) {
        		console.log("failed to query public link");
        	}
        }
    }
}

function handleQueryPublicLinkResult(req) {
    if (req.readyState == 4) {
        if (req.status == 200) {
            var resultElem = req.responseXML.getElementsByTagName("result")[0];            
            var success = resultElem.getElementsByTagName("success")[0].firstChild.nodeValue;

            if (success == 'true') {
                publicUrl = resultElem.getElementsByTagName("publicUrl")[0].firstChild.nodeValue;   
                document.getElementById("publicURLButton").style.display = "inline";
                document.getElementById("unpublishButton").style.display = "inline";
                jQuery(".icon-blog-share").css("display", "inline");
            } else {
                if (document.getElementById("publishBlogButton")) {
                    document.getElementById("publishBlogButton").style.display = "inline";
                }
            }
        } else {
        	if (console.log) {
        		console.log("failed to query public link");
        	}
        }
    }
}

function showPublicURL() {

    var publishCont = document.getElementById("publishCont");
        
    if (!publishCont) {
        alert('publishCont is not defined');
        return;
    }
    
    publishCont.innerHTML = "";
    
    var publishHead = document.createElement("div");
    publishHead.setAttribute("class", "promptHead");    
    publishHead.innerHTML = resourceBundle["blog.publishTitle"];
    publishCont.appendChild(publishHead);
    
    var publishTable = document.createElement("table");
    publishTable.id = "publishTable";
    publishTable.setAttribute("class", "blogPublishForm");    
    publishCont.appendChild(publishTable);
    
    var tableRow = document.createElement("tr");
    document.getElementById("publishTable").appendChild(tableRow);

    var tableCell = document.createElement("td");
    tableCell.setAttribute("class", "formParm1");
    tableCell.setAttribute("colspan", "2");
    tableCell.innerHTML = resourceBundle["blog.publicLink"] + ":";
    tableRow.appendChild(tableCell);

    tableRow = document.createElement("tr");
    document.getElementById("publishTable").appendChild(tableRow);

    tableCell = document.createElement("td");
    tableCell.setAttribute("class", "formParm2");
    tableCell.setAttribute("colspan", "2");
    tableRow.appendChild(tableCell);
    
    var urlInput = document.createElement("textarea");
    urlInput.id = "publicUrl";
    urlInput.setAttribute("class", "publicLinkCopyField");
    urlInput.setAttribute("readonly", "readonly");
    urlInput.value = publicUrl;
    tableCell.appendChild(urlInput);
                
    tableRow = document.createElement("tr");
    document.getElementById("publishTable").appendChild(tableRow);

    tableCell = document.createElement("td");
    tableCell.style.paddingTop = "20px";
    tableCell.style.paddingLeft = "8px";
    tableRow.appendChild(tableCell);

    var closeButton = document.createElement("input");
    closeButton.setAttribute("type", "button");
    closeButton.setAttribute("value", resourceBundle["button.closewin"]);
    closeButton.setAttribute("onclick", "hidePublishForm()");
    closeButton.style.marginBottom = "10px";
    tableCell.appendChild(closeButton);

    tableCell = document.createElement("td");
    tableCell.style.paddingTop = "20px";
    tableCell.style.textAlign = "right";
    tableRow.appendChild(tableCell);
    
    var copyButton = document.createElement("input");
    copyButton.setAttribute("type", "button");
    copyButton.setAttribute("value", resourceBundle["button.copyToClip"]);
    copyButton.setAttribute("onclick", "copyPublicUrlToClip()");
    copyButton.style.marginBottom = "10px";
    tableCell.appendChild(copyButton);
    
    centerBox(publishCont);
    
    publishCont.style.visibility = "visible";

    // urlInput.focus();
    urlInput.select();
}

function copyPublicUrlToClip() {
    document.getElementById("publicUrl").select();
    document.execCommand("Copy");
    hidePublishForm();
}

function copyPicUrlToClip() {
    document.getElementById("sharedPicUrl").select();
    document.execCommand("Copy");
    hidePublishForm();
}

function unpublish() {
    if (!confirm(resourceBundle["blog.confirmUnpublish"])) {
        return;
    }
    
    var url = getContextRoot() + "/servlet?command=blog&cmd=unpublish";

    xmlRequest(url, handleUnpublishResult);
}

function handleUnpublishResult(req) {
    if (req.readyState == 4) {
        if (req.status == 200) {
            var resultElem = req.responseXML.getElementsByTagName("result")[0];            
            var success = resultElem.getElementsByTagName("success")[0].firstChild.nodeValue;

            if (success == 'true') {
                document.getElementById("unpublishButton").style.display = "none";
                document.getElementById("publicURLButton").style.display = "none";
                document.getElementById("publishBlogButton").style.display = "inline";
                publicUrl = null;
                
                window.location.href = getContextRoot() + "/servlet?command=blog&cmd=list";
            } else {
                alert("failed to unpublish blog");
            }
        }
    }
}

function createDiv(parentNode, id, text, cssClass) {
    const div = document.createElement("div");
    if (id) {
    	div.id = id;
    }
    if (cssClass) {
        div.setAttribute("class", cssClass);
    }
    if (text) {
    	const label = document.createElement("label");
    	div.appendChild(label);
        label.innerHTML = text;
    }
    parentNode.appendChild(div);
    return div;
}

function shareBlogEntry(entryFileName) {

    let publishCont = document.getElementById("publishCont");
        
    if (!publishCont) {
        console.error("publishCont is not defined");
        return;
    }
    
	let entryDate = new Date(entryFileName.substring(0, 10));
	
	let beforeDay = new Date(entryDate.getTime() + (24 * 60 * 60 * 1000));
	
	let beforeDayStr = beforeDay.toISOString().substring(0,10);  
    
    let shareUrl = publicUrl + "?beforeDay=" + beforeDayStr + "&positionToFile=" + entryFileName; 
    
    publishCont.innerHTML = "";
    
    let publishHead = document.createElement("div");
    publishHead.setAttribute("class", "promptHead");    
    publishHead.innerHTML = resourceBundle["blog.shareTitle"];
    publishCont.appendChild(publishHead);
    
    const shareTextDiv = createDiv(publishCont, null, resourceBundle["blog.shareURL"] + ":", null);
    
    const mailtoUrl = "mailto:nobody@nowhere.com?subject=" + resourceBundle["blog.shareEmailSubject"] + "&body=" + encodeURIComponent(shareUrl);

    let emailLink = document.createElement("a");
    emailLink.setAttribute("href", mailtoUrl);
    emailLink.setAttribute("class", "icon-font icon-email emailLink");
    emailLink.setAttribute("title", resourceBundle["blog.shareByEmail"]);
    shareTextDiv.appendChild(emailLink);
    
    const shareUrlDiv = createDiv(publishCont, null, null, null);

    const urlInput = document.createElement("textarea");
    urlInput.id = "publicUrl";
    urlInput.setAttribute("class", "publicLinkCopyField");
    urlInput.setAttribute("readonly", "readonly");
    urlInput.value = shareUrl;
    shareUrlDiv.appendChild(urlInput);
    
    const copyButtonDiv = createDiv(publishCont, null, null, null);

    let copyButton = document.createElement("input");
    copyButton.setAttribute("type", "button");
    copyButton.setAttribute("value", resourceBundle["button.copyToClip"]);
    copyButton.setAttribute("onclick", "copyPublicUrlToClip()");
    copyButton.style.float = "right";
    copyButtonDiv.appendChild(copyButton);

    const sharePicButtonDiv = createDiv(publishCont, null, null, null);
    sharePicButtonDiv.style.clear = "both";
    
    let sharePicButton = document.createElement("input");
    sharePicButton.setAttribute("type", "button");
    sharePicButton.setAttribute("value", resourceBundle["blog.shareSinglePic"]);
    sharePicButton.setAttribute("onclick", "shareSinglePic('" + entryFileName + "')");
    sharePicButtonDiv.appendChild(sharePicButton);

    const sharePicCont = createDiv(publishCont, "sharePicCont", null, null);
    sharePicCont.style.display = "none";
    
    const sharePicTextDiv = createDiv(sharePicCont, null, resourceBundle["blog.sharedPicURL"] + ":", null);

    const picEmailLink = document.createElement("a");
    picEmailLink.id = "picEmailLink";
    picEmailLink.setAttribute("href", "javascript:void(0)");
    picEmailLink.setAttribute("class", "icon-font icon-email emailLink");
    picEmailLink.setAttribute("title", resourceBundle["blog.shareByEmail"]);
    sharePicTextDiv.appendChild(picEmailLink);
    
    const sharePicUrlDiv = createDiv(sharePicCont, null, null, null);

    const sharedPicUrl = document.createElement("textarea");
    sharedPicUrl.id = "sharedPicUrl";
    sharedPicUrl.setAttribute("class", "publicLinkCopyField");
    sharedPicUrl.setAttribute("readonly", "readonly");
    sharePicUrlDiv.appendChild(sharedPicUrl);
    
    const copyPicButtonDiv = createDiv(sharePicCont, null, null, null);

    const copyPicButton = document.createElement("input");
    copyPicButton.setAttribute("type", "button");
    copyPicButton.setAttribute("value", resourceBundle["blog.copyPublicPicLink"]);
    copyPicButton.setAttribute("onclick", "copyPicUrlToClip()");
    copyPicButton.style.float = "right";
    copyPicButtonDiv.appendChild(copyPicButton);
    
    const closeButtonDiv = createDiv(publishCont, null, null, null);
    closeButtonDiv.style.clear = "both";
    
    let closeButton = document.createElement("input");
    closeButton.setAttribute("type", "button");
    closeButton.setAttribute("value", resourceBundle["button.cancel"]);
    closeButton.setAttribute("onclick", "hidePublishForm()");
    closeButtonDiv.appendChild(closeButton);
    
    centerBox(publishCont);
    
    publishCont.style.visibility = "visible";

    urlInput.select();
}

function shareSinglePic(fileName) {  
    
    showHourGlass();
    
    var url = getContextRoot() + "/servlet?command=blog&cmd=shareSinglePic&fileName=" + encodeURIComponent(fileName);
    
    xmlRequest(url, function(req) {
        if (req.readyState == 4) {
            if (req.status == 200) {
			    var responseXml = req.responseXML;
    
                var success = null;
                var successItem = responseXml.getElementsByTagName("success")[0];
                if (successItem) {
                    success = successItem.firstChild.nodeValue;
                }         
    
                if ((success != null) && (success == "true")) {
                    var publicUrlItem = responseXml.getElementsByTagName("publicUrl")[0];
                    if (publicUrlItem) {
                    	const publicUrl = publicUrlItem.firstChild.nodeValue;
                        document.getElementById("sharedPicUrl").value = publicUrl;
                        document.getElementById("sharePicCont").style.display = "block";
                        
                        const mailtoUrl = "mailto:nobody@nowhere.com?subject=" + resourceBundle["blog.shareEmailPicSubject"] + "&body=" + encodeURIComponent(publicUrl);
                        document.getElementById("picEmailLink").setAttribute("href", mailtoUrl);
                    }         
                } else {
                    alert("failed to publish picture");
                }

                hideHourGlass();    
            } else {
                alert(resourceBundle["alert.communicationFailure"]);
                hideHourGlass();    
            }
        }
    });
}

function blogComments(fileName, posInPage) {

    var commentCont = document.getElementById("commentCont");
    
    var xmlUrl = getContextRoot() + '/servlet?command=blog&cmd=listComments&fileName=' + encodeURIComponent(fileName);
    
    var xslUrl = getContextRoot() + '/xsl/blog/comments.xsl';

    htmlFragmentByXslt(xmlUrl, xslUrl, commentCont, function() {
        document.getElementById("posInPage").value = posInPage;
    
        setBundleResources();
    
        centerBox(commentCont);

        commentCont.style.visibility = "visible";
    });
}

function closeBlogComments() {
    var posInPage = document.getElementById("posInPage").value;
    var commentNewLabel = document.getElementById("newComment-" + posInPage);
    if (commentNewLabel) {
        commentNewLabel.style.display = 'none';
        queryUnseenComments();        
    }

    var commentCont = document.getElementById("commentCont");
    commentCont.style.visibility = "hidden";
}

function submitComment() {
    if (document.getElementById("newComment").value.length == 0) {
        customAlert(resourceBundle["blog.newCommentEmpty"]);
        return;
    }
    
    var emailInput = document.getElementById("notifyOnAnswerEmail")
    if (emailInput) {
        if (!emailInput.validity.valid) {
        	customAlert(resourceBundle["alert.emailsyntax"]);
        	return;
        }
    }
    
    var commentCont = document.getElementById("commentCont");
    commentCont.style.visibility = "hidden";

    xmlRequestPost(getContextRoot() + "/servlet", getFormData(document.getElementById("blogCommentForm")), showPostCommentResult);
}

function showPostCommentResult(req) {
    if (req.readyState == 4) {
        if (req.status == 200) {
            var resultElem = req.responseXML.getElementsByTagName("result")[0];            
            var success = resultElem.getElementsByTagName("success")[0].firstChild.nodeValue;

            if (success == 'true') {
                var newCommentCount = resultElem.getElementsByTagName("newCommentCount")[0].firstChild.nodeValue;
                var posInPage = document.getElementById("posInPage").value;
                document.getElementById("comment-" + posInPage).innerHTML = newCommentCount;
                
                var commentNewLabel = document.getElementById("newComment-" + posInPage);
                if (commentNewLabel) {
                    commentNewLabel.style.display = 'none';
                    queryUnseenComments();
                }

                var commentCont = document.getElementById("commentCont");
                commentCont.style.visibility = "hidden";
                
                toast(resourceBundle["blog.commentAdded"], 2000);
            } else {
                alert("failed to create comment");
            }
        }
    }
}

function limitCommentText() { 
    var newComment = document.getElementById("newComment");
 
    if (newComment.value.length > 2048) {  
        newComment.value = newComment.value.substring(0, 2048);
    }
}
  
function confirmDelComments(fileName) {  
    if (!confirm(resourceBundle["confirm.delcomments"])) { 
        return;
    }
    
    showHourGlass();
    
    var url = getContextRoot() + "/servlet?command=blog&cmd=delComments&fileName=" + encodeURIComponent(fileName);
    
    xmlRequest(url, function(req) {
        if (req.readyState == 4) {
            if (req.status == 200) {
			    var responseXml = req.responseXML;
    
                var success = null;
                var successItem = responseXml.getElementsByTagName("success")[0];
                if (successItem) {
                    success = successItem.firstChild.nodeValue;
                }         
    
                if ((success != null) && (success == "true")) {
                    var posInPage = document.getElementById("posInPage").value;
                    document.getElementById("comment-" + posInPage).innerHTML = "0";
                
                    var commentNewLabel = document.getElementById("newComment-" + posInPage);
                    if (commentNewLabel) {
                        commentNewLabel.style.display = 'none';
                    }
    
                    var commentCont = document.getElementById("commentCont");
                    commentCont.style.visibility = "hidden";
                } else {
                    alert("failed to delete comments");
                }

                hideHourGlass();    
            } else {
                alert(resourceBundle["alert.communicationFailure"]);
                hideHourGlass();    
            }
        }
    });
}

function dayTitle(day) {

    var dayTitleCont = document.getElementById("blogDayTitleCont");
    var xmlUrl = getContextRoot() + '/servlet?command=blog&cmd=dayTitle&day=' + day;
    var xslUrl = getContextRoot() + '/xsl/blog/dayTitle.xsl';

    htmlFragmentByXslt(xmlUrl, xslUrl, dayTitleCont, function() {
        setBundleResources();
        centerBox(dayTitleCont);
        dayTitleCont.style.visibility = "visible";
        document.getElementById("dayTitleText").focus();
    });
}

function submitDayTitle() {
	const dayTitleCont = document.getElementById("blogDayTitleCont");
	dayTitleCont.style.visibility = "hidden";

    xmlRequestPost(getContextRoot() + "/servlet", getFormData(document.getElementById("dayTitleForm")), showPostDayTitleResult);
}

function showPostDayTitleResult(req) {
    if (req.readyState == 4) {
        if (req.status == 200) {
            const resultElem = req.responseXML.getElementsByTagName("result")[0];
            const success = resultElem.getElementsByTagName("success")[0].firstChild.nodeValue;
            if (success == 'true') {
                const day = resultElem.getElementsByTagName("day")[0].firstChild.nodeValue;
                const titleTextElem = resultElem.getElementsByTagName("titleText")[0].firstChild;
                let titleText = "";
                if (titleTextElem) {
                	titleText = titleTextElem.nodeValue;
                }
                if (day) {
                	const dayTitle = document.getElementById("dayTitle-" + day);
                	if (dayTitle) {
              		    dayTitle.innerHTML = titleText;
              		    if (titleText.length > 0) {
                  		    dayTitle.style.display = "block";
              		    } else {
                  		    dayTitle.style.display = "none";
              		    }
                	}
                }
            }
            const dayTitleCont = document.getElementById("blogDayTitleCont");
            dayTitleCont.style.visibility = "hidden";
        } else {
            customAlert("failed to save title text");
        }
    }
}

function closeDayTitle() {
	const dayTitleCont = document.getElementById("blogDayTitleCont");
	if (dayTitleCont) {
		dayTitleCont.style.visibility = "hidden";
	}
}

function limitDayTitleText() { 
    var dayTitleText = document.getElementById("dayTitleText");
 
    if (dayTitleText.value.length > 46) {  
    	dayTitleText.value = dayTitleText.value.substring(0, 46);
    }
}

function showSubscribers() {
    var subscribeCont = document.getElementById("subscribeCont");

    var xmlUrl = getContextRoot() + "/servlet?command=blog&cmd=listSubscribers";
        
    var xslUrl = getContextRoot() + "/xsl/blog/subscriberList.xsl";    
        
    htmlFragmentByXslt(xmlUrl, xslUrl, subscribeCont, function() {
        setBundleResources();
        centerBox(subscribeCont);
        subscribeCont.style.visibility = "visible";
    });
}

function showSubscribeForm() {
    var subscribeCont = document.getElementById("subscribeCont");
    
    var subscriberEmail = document.getElementById("subscriberEmail");
    if (subscriberEmail) {
        subscriberEmail.value = "";
    }
    
    subscribeCont.style.visibility = "visible";
}

function hideSubscribeForm() {
    var subscribeCont = document.getElementById("subscribeCont");
    
    subscribeCont.style.visibility = "hidden";
}

function submitSubscription() {
    var subscriberEmail = document.getElementById("subscriberEmail");
    
    if (!validateEmail(subscriberEmail.value)) {
        alert(resourceBundle["blog.invalidSubscriberEmail"]);
        subscriberEmail.select();
        subscriberEmail.focus();
        return;
    }
    
    var formData = getFormData(document.getElementById("subscribeForm"));
	
	xmlRequestPost(getContextRoot() + "/servlet", formData, handleSubscribeResult)	    
}

function handleSubscribeResult(req) {
    if (req.readyState == 4) {
        if (req.status == 200) {
            var resultElem = req.responseXML.getElementsByTagName("result")[0];            
            var success = resultElem.getElementsByTagName("success")[0].firstChild.nodeValue;

            if (success == 'true') {
                toast(resourceBundle["blog.subscribeSuccess"], 3000);
            } else {
                alert(resourceBundle["blog.subscribeError"]);
            }
            hideSubscribeForm();
        }
    }
}

function subscribeKeyPress(e) {
    e = e || window.event;
    if (e.keyCode == 13) {
        submitSubscription();
        return false;
    }
    return true;
}

function showSearchForm() {
    var searchFormCont = document.getElementById("searchFormCont");
    
    searchFormCont.style.visibility = "visible";
    
    var searchArgInput = document.getElementById("searchArg");
    searchArgInput.focus();
    if (searchArgInput.value.length > 0) {
    	searchArgInput.select();
    }
}

function hideSearchForm() {
    var searchFormCont = document.getElementById("searchFormCont");
    searchFormCont.style.visibility = "hidden";
}

function submitSearch() {
    var searchArg = document.getElementById("searchArg");
    
    if (searchArg.value.length < 2) {
        alert(resourceBundle["blog.searchArgMinLength"]);
        document.getElementById("searchArg").focus();
        return;
    }
    
    var formData = getFormData(document.getElementById("searchForm"));
	
	xmlRequestPost(getContextRoot() + "/servlet", formData, handleSearchResult)	    
}

function handleSearchResult(req) {
    if (req.readyState == 4) {
        if (req.status == 200) {

        	var resultElem = req.responseXML.getElementsByTagName("result")[0];            
            // var success = resultElem.getElementsByTagName("success")[0].firstChild.nodeValue;
            hideSearchForm();
        	
           	var searchResultCont = document.createElement("div");
           	searchResultCont.id = "searchResultCont";
           	searchResultCont.setAttribute("class", "searchResultCont");
           	document.documentElement.appendChild(searchResultCont);
           	
           	var searchArgLabel= document.createElement("label");
           	searchArgLabel.id = "searchArgLabel";
           	searchArgLabel.setAttribute("class", "searchResultSearchArg");
           	searchResultCont.appendChild(searchArgLabel); 

           	var searchArg = resultElem.getElementsByTagName("searchArg")[0].firstChild.nodeValue;
           	
           	var searchArgText = resourceBundle["blog.searchResultArg"] + ": " + searchArg;
           	searchArgLabel.innerHTML = searchArgText;

           	/*
           	var brElem= document.createElement("br");
           	searchResultCont.appendChild(brElem); 
           	*/
           	
           	var hitCountLabel= document.createElement("label");
           	hitCountLabel.id = "hitCountLabel";
           	hitCountLabel.setAttribute("class", "searchResultHitCount");
           	searchResultCont.appendChild(hitCountLabel); 
           	
           	var hitCountText = resourceBundle["blog.searchHitCount"] + ": " + resultElem.getElementsByTagName("hitCount")[0].firstChild.nodeValue;
           	hitCountLabel.innerHTML = hitCountText;
           	
           	var searchResultScrollPane= document.createElement("div");
           	searchResultScrollPane.id = "searchResultScrollPane";
           	searchResultScrollPane.setAttribute("class", "searchResultScrollPane");
           	searchResultCont.appendChild(searchResultScrollPane);
           	
           	var buttonCont= document.createElement("div");
           	buttonCont.setAttribute("class", "searchResultButtonCont");
           	searchResultCont.appendChild(buttonCont); 
           	
        	var searchAgainButton = document.createElement("input");
        	searchAgainButton.setAttribute("type", "button");
        	searchAgainButton.onclick = searchAgain;
        	searchAgainButton.value = resourceBundle["blog.searchAgain"];
        	buttonCont.appendChild(searchAgainButton);

        	var closeButton = document.createElement("input");
        	closeButton.setAttribute("type", "button");
        	closeButton.onclick = closeSearchResults;
        	closeButton.setAttribute("class", "searchResultCloseButton");
        	closeButton.value = resourceBundle["button.closewin"];
        	buttonCont.appendChild(closeButton);

           	var blogDayList = document.createElement("ul");
           	blogDayList.id = "blogDayList";
           	blogDayList.setAttribute("class", "searchHitDayList");
           	searchResultScrollPane.appendChild(blogDayList);
        	
        	var searchResults = resultElem.getElementsByTagName("searchResults")[0];

        	var daysWithSearchHits = getChildElementsByTagName(searchResults, "blogDay");
        	
        	for (var k = 0; k < daysWithSearchHits.length; k++) {
        		var blogDayListEntry = document.createElement("li");
        		blogDayListEntry.setAttribute("class", "searchHit");
        		blogDayList.appendChild(blogDayListEntry);

        		var blogLinkDate = getChildValueByTagName(daysWithSearchHits[k], "linkDate");
        		var blogDisplayDate = getChildValueByTagName(daysWithSearchHits[k], "displayDate");
 
        		var dateElem = document.createElement("span");
               	dateElem.setAttribute("class", "searchHitDate");
               	dateElem.innerHTML = blogDisplayDate;
               	blogDayListEntry.appendChild(dateElem);
        		
               	var searchHitsList = document.createElement("ul");
               	// searchHitsList.id = "searchHitsList";
               	searchHitsList.setAttribute("class", "searchHitsList");
               	blogDayListEntry.appendChild(searchHitsList);

            	var searchHits = getChildElementsByTagName(daysWithSearchHits[k], "searchHit");
            	
            	for (var i = 0; i < searchHits.length; i++) {
            			
               	    var beforeContext;
                   	var afterContext;
                   	
                   	var searchHitListEntry = document.createElement("li");
                   	searchHitListEntry.setAttribute("class", "searchHit");
                   	searchHitsList.appendChild(searchHitListEntry);

                   	var fileName = getChildValueByTagName(searchHits[i], "fileName");

                   	var isComment = getChildValueByTagName(searchHits[i], "isComment");
                   	
                   	var searchLinkElem = document.createElement("a");
                   	searchLinkElem.setAttribute("fileName", fileName);
                   	if (isComment) {
                       	searchLinkElem.setAttribute("class", "searchHitLink searchHitComment");
                   	} else {
                       	searchLinkElem.setAttribute("class", "searchHitLink");
                   	}
                   	searchLinkElem.onmouseover = function () {
                   		var timeoutFunctionCall = "previewSearchResult('" + this.getAttribute("fileName") + "')";
                   		searchPreviewTimeout = setTimeout(timeoutFunctionCall, 500);
                   	};
                   	searchLinkElem.setAttribute("onmouseout", "cancelSearchPreview()");
                   	
                   	searchLinkElem.setAttribute("href", getContextRoot() + "/servlet?command=blog&beforeDay=" + blogLinkDate + "&positionToFile=" + fileName);
                   	searchHitListEntry.appendChild(searchLinkElem);
                   	
                   	var beforeContext = getChildValueByTagName(searchHits[i], "beforeContext");
                   	if (beforeContext) {
                       	var beforeContextElem = document.createElement("span");
                       	beforeContextElem.setAttribute("class", "searchHitContext");
                       	beforeContextElem.innerHTML = beforeContext;
                       	searchLinkElem.appendChild(beforeContextElem);
                   	}

                   	var matchingText = getChildValueByTagName(searchHits[i], "matchingText");
                   	var searchArgElem = document.createElement("span");
                   	searchArgElem.setAttribute("class", "searchHit");
                   	searchArgElem.innerHTML = matchingText;
                   	searchLinkElem.appendChild(searchArgElem);
                   	    
                   	var afterContext = getChildValueByTagName(searchHits[i], "afterContext");
                   	if (afterContext) {
                       	var afterContextElem = document.createElement("span");
                       	afterContextElem.setAttribute("class", "searchHitContext");
                       	afterContextElem.innerHTML = afterContext;
                       	searchLinkElem.appendChild(afterContextElem);
                   	}
            	}
        	}
        	
           	searchResultCont.style.visibility = "visible"
        	
        } else {
        	alert("search error");
            hideSearchForm();
        }
    }
}

function closeSearchResults() {
	var searchResultCont = document.getElementById("searchResultCont");
	document.documentElement.removeChild(searchResultCont);
}

function searchAgain() {
	closeSearchResults();
	showSearchForm();
}

function searchKeyPress(e) {
    e = e || window.event;
    if (e.keyCode == 13) {
        submitSearch();
        return false;
    }
    return true;
}

function previewSearchResult(fileName) {
	searchPreviewTimeout = null;

   	var searchPreviewCont = document.createElement("div");
   	searchPreviewCont.id = "searchPreviewCont";
   	searchPreviewCont.setAttribute("class", "searchPreviewCont");
   	document.documentElement.appendChild(searchPreviewCont);
	
   	var previewPic = document.createElement("img");
   	previewPic.src = getContextRoot() + "/servlet?command=getFile&fileName=" + encodeURIComponent(fileName) + "&cached=true&thumb=true";
   	searchPreviewCont.appendChild(previewPic);

   	var blogEntryText = document.createElement("p");
   	blogEntryText.id = "searchPreviewText";
   	searchPreviewCont.appendChild(blogEntryText);
   	
   	document.documentElement.appendChild(searchPreviewCont);
   	
   	var searchResultCont = document.getElementById("searchResultCont");
   	if (searchResultCont) {
   		var resultContXpos = searchResultCont.offsetLeft;
   		var previewContWidth = searchPreviewCont.offsetWidth;
   		var previewXpos = resultContXpos - previewContWidth - 4;
   		if (previewXpos < 1) {
   			previewXpos = 1;
   		}
   		searchPreviewCont.style.left = previewXpos + "px";
   	}
   	
    searchPreviewActive = true;

    var ajaxUrl = getContextRoot() + "/servlet?command=getFileDesc&fileName=" + encodeURIComponent(fileName);
    
	xmlRequest(ajaxUrl, function(req) {
        if (req.readyState == 4) {
        	if (searchPreviewActive) {
                if (req.status == 200) {
                    var fileDescription = req.responseXML.getElementsByTagName("result")[0].firstChild.nodeValue;        
                    if (fileDescription && (fileDescription.length > 0)) {
                        var searchPreviewText = document.getElementById("searchPreviewText");
                        if (searchPreviewText) {
                        	searchPreviewText.innerText = shortText(fileDescription, 170);
                        }
                    }
                } else {
                    alert(resourceBundle["alert.communicationFailure"]);
                }
        	}
        }
	});
}

function cancelSearchPreview() {
	if (searchPreviewTimeout) {
		clearTimeout(searchPreviewTimeout);
		return;
	}
	
	if (!searchPreviewActive) {
		return;
	}
	
	document.documentElement.removeChild(document.getElementById("searchPreviewCont"));

	searchPreviewActive = false;
}

function showSettings() {

    var settingsCont = document.getElementById("settingsCont");
    
    var xmlUrl = getContextRoot() + "/servlet?command=blog&cmd=showSettings";
    
    var xslUrl = getContextRoot() + "/xsl/blog/settings.xsl";

    htmlFragmentByXslt(xmlUrl, xslUrl, settingsCont, function() {
        setBundleResources();
        centerBox(settingsCont);
        settingsCont.style.visibility = "visible";
    });
}

function hideSettings() {
    var settingsCont = document.getElementById("settingsCont");

    settingsCont.style.visibility = "hidden";
}

function validateSettingsForm() {
    
  	var daysPerPage = document.getElementById("daysPerPage").value;

    var pageSize = parseInt(daysPerPage);

    if ((daysPerPage == "") || isNaN(pageSize) || (pageSize < 1) || (pageSize > 64)) {
        alert(resourceBundle["blog.invalidDaysPerPage"]);
    	return;
    }
    
    var newPassword = document.getElementById("newPassword").value;
    
    if ((newPassword.length > 0) && (newPassword.length < 5)) {
        alert(resourceBundle["error.passwordlength"]);
        return;
    }

    var newPasswdConfirm = document.getElementById("newPasswdConfirm").value;
    
    if (newPassword != newPasswdConfirm) {
        alert(resourceBundle["error.pwmissmatch"]);
        return;
    }

    showHourGlass();

    xmlRequestPost(getContextRoot() + "/servlet", getFormData(document.getElementById("blogSettingsForm")), showSaveSettingsResult);
}

function showSaveSettingsResult(req) {
    if (req.readyState == 4) {
        if (req.status == 200) {
            var resultElem = req.responseXML.getElementsByTagName("result")[0];            
            var success = resultElem.getElementsByTagName("success")[0].firstChild.nodeValue;

            if (success != 'true') {
                alert("failed to save settings");
            }

            var pageSizeChanged = resultElem.getElementsByTagName("pageSizeChanged")[0].firstChild.nodeValue;
            var blogTitleChanged = resultElem.getElementsByTagName("blogTitleChanged")[0].firstChild.nodeValue;
            var stagingChanged = resultElem.getElementsByTagName("stagingChanged")[0].firstChild.nodeValue;
            var skinChanged = resultElem.getElementsByTagName("skinChanged")[0].firstChild.nodeValue;
            var languageChanged = resultElem.getElementsByTagName("languageChanged")[0].firstChild.nodeValue;
            var sortOrderChanged = resultElem.getElementsByTagName("sortOrderChanged")[0].firstChild.nodeValue;

            var settingsCont = document.getElementById("settingsCont");
            settingsCont.style.visibility = "hidden";
            
            hideHourGlass();

            if ((pageSizeChanged && (pageSizeChanged == "true")) || 
                (blogTitleChanged && (blogTitleChanged == "true")) ||
                (stagingChanged && (stagingChanged == "true")) ||
                (languageChanged && (languageChanged == "true")) ||
                (sortOrderChanged && (sortOrderChanged == "true")) ||
                (skinChanged && (skinChanged == "true"))) {
                window.location.href = getContextRoot() + "/servlet?command=blog";
            }
        }
        hideHourGlass();
    }
}
   
function selectDate() {
	daysWithEntries = new Array();	
	
    var url = getContextRoot() + "/servlet?command=blog&cmd=datesWithEntries";
    
    xmlRequest(url, function(req) {
        if (req.readyState == 4) {
            if (req.status == 200) {
                var responseXml = req.responseXML;
                var resultItem = responseXml.getElementsByTagName("datesWithEntries")[0];

                var listLength = resultItem.childNodes.length;
                
                for (var i = 0; i < listLength; i++) {
                	var childNode = resultItem.childNodes[i];
                	if ((childNode.nodeType == 1) && (childNode.tagName == "date")) {
                		daysWithEntries.push(childNode.firstChild.nodeValue);
                	}
                }
                
            	var dateFormat;
            	if (window.navigator.language.indexOf("en") == 0) {
            		dateFormat = "MM/dd/yyyy";
            	} else {
            		dateFormat = "dd.MM.yyyy";
            	}
            	
                cal1x.select(document.getElementById("blogDate"), "anchorDate", dateFormat);
                centerBox(document.getElementById("calDiv"));
            }
        }
    });          
}

function setSelectedDate(y, m, d) { 
    document.getElementById("dateDay").value = LZ(d);        
    document.getElementById("dateMonth").value = LZ(m);        
    document.getElementById("dateYear").value = y;        
            
    var selectedDate = new Date();
    selectedDate.setYear(y);
    selectedDate.setMonth(m - 1);
    selectedDate.setDate(d);
        
    var now = new Date();
           
    if (selectedDate.getTime() - (24 * 60 * 60 * 1000) > now.getTime()) {
        alert(resourceBundle["blog.dateInFuture"]);
    }
        
    var options = {year: 'numeric', month: '2-digit', day: '2-digit' };
       
    var language = (navigator.language || navigator.browserLanguage).split('-')[0];
       
    document.getElementById("blogDate").value = selectedDate.toLocaleDateString(language, options);
}

function rotateBlogPic(imgName, direction) {

    showHourGlass();

    var xmlUrl = getContextRoot() + "/servlet?command=blog&cmd=rotate&imgName=" + imgName + "&direction=" + direction;

	xmlRequest(xmlUrl, function(req) {
        if (req.readyState == 4) {
            if (req.status == 200) {
                var successItem = req.responseXML.getElementsByTagName("success")[0];            
                var success = successItem.firstChild.nodeValue;
             
                if (success == 'true') {
                    window.location.href = getContextRoot() + "/servlet?command=blog";
                } else {
                    alert(resourceBundle["blog.rotateError"]);
                }
                hideHourGlass();
            } else {
                hideHourGlass();
                alert(resourceBundle["blog.rotateError"]);
            }
        }
    });   
}

function like(imgName, posInPage) {
    if (!confirm(resourceBundle["blog.confirmLike"])) {
        return;
    }
     
    showHourGlass();

    var xmlUrl = getContextRoot() + "/servlet?command=blog&cmd=like&imgName=" + imgName;

	xmlRequest(xmlUrl, function(req) {
        if (req.readyState == 4) {
            hideHourGlass();
            if (req.status == 200) {
                var resultElem = req.responseXML.getElementsByTagName("result")[0];            
                var success = resultElem.getElementsByTagName("success")[0].firstChild.nodeValue;

                if (success == 'true') {
                    var newVoteCount = resultElem.getElementsByTagName("newVoteCount")[0].firstChild.nodeValue;
                    document.getElementById("voteCount-" + posInPage).innerHTML = newVoteCount;

                    document.getElementById("likeLink-" + posInPage).onclick = function() {javascript:void(0)};
                    document.getElementById("likeLink-" + posInPage).title = "";
                
                    toast(resourceBundle["blog.likeAdded"], 2000);
                } else {
                    alert("failed to like blog post");
                }
            }
        }
    });
}

function setTitlePic(imgName) {
    if (!confirm(resourceBundle["blog.confirmSetTitlePic"])) {
        return;
    }
     
    showHourGlass();

    var xmlUrl = getContextRoot() + "/servlet?command=blog&cmd=setTitlePic&imgName=" + imgName;

	xmlRequest(xmlUrl, function(req) {
        if (req.readyState == 4) {
            if (req.status == 200) {
                var successItem = req.responseXML.getElementsByTagName("success")[0];            
                var success = successItem.firstChild.nodeValue;
             
                if (success == 'true') {
                    window.location.href = getContextRoot() + "/servlet?command=blog";
                } else {
                    alert(resourceBundle["blog.setTitlePicError"]);
                }
                hideHourGlass();
            } else {
                hideHourGlass();
                alert(resourceBundle["blog.setTitlePicError"]);
            }
        }
    });   
}

function unsetTitlePic() {
    if (!confirm(resourceBundle["blog.confirmUnsetTitlePic"])) {
        return;
    }
    showHourGlass();

    var xmlUrl = getContextRoot() + "/servlet?command=blog&cmd=unsetTitlePic";

	xmlRequest(xmlUrl, function(req) {
        if (req.readyState == 4) {
            if (req.status == 200) {
                var successItem = req.responseXML.getElementsByTagName("success")[0];            
                var success = successItem.firstChild.nodeValue;
             
                if (success == 'true') {
                    window.location.href = getContextRoot() + "/servlet?command=blog";
                } else {
                    alert(resourceBundle["blog.setTitlePicError"]);
                }
                hideHourGlass();
            } else {
                hideHourGlass();
                alert(resourceBundle["blog.setTitlePicError"]);
            }
        }
    });   
}

function firefoxJumpToIdWorkaround() {
   // Firefox bug 645075
   if (browserFirefox) {
       if (location.href.indexOf('#') > -1) {
           if (location.href.length > location.href.indexOf('#') + 1) {
               var currentEntryId = location.href.substring(location.href.indexOf('#') + 1);
               var currentEntry = document.getElementById(currentEntryId);
               if (currentEntry) {
                   currentEntry.scrollIntoView();
               }
           }
       }        
   }
}
 
function switchEmojiSelection(textareaId) {
    var visible = (document.getElementById("emojiSelCont").style.display == "block");

    if (visible) {
        document.getElementById("emojiSelCont").style.display = "none";
    } else {
        var xmlUrl = getContextRoot() + "/servlet?command=blog&cmd=emojiList&textareaId=" + textareaId;
    
        var xslUrl = getContextRoot() + "/xsl/blog/emojiList.xsl";

        htmlFragmentByXslt(xmlUrl, xslUrl, document.getElementById("emojiSelCont"), function() {
            setBundleResources();
            document.getElementById("emojiSelCont").style.display = "block";
        });
    }
}

function insertEmoji(textAreaId, emojiName) {
    var textAreaField = document.getElementById(textAreaId);
    if (textAreaField) {
        var emojiPlaceHolder = " {" + emojiName + "} ";
        insertAtCursor(textAreaField, emojiPlaceHolder);
        textAreaField.focus();
    }
}

function showInsertLinkPrompt(textAreaId) {
	var urlInputBox = document.getElementById("urlInputCont");
	urlInputBox.style.display = "block";
	centerBox(urlInputBox);
	document.getElementById("urlLabel").focus();
}

function hideInsertLinkPrompt() {
	var urlInputBox = document.getElementById("urlInputCont");
	urlInputBox.style.display = "none";
	document.getElementById("urlLabel").value = "";
	document.getElementById("urlHref").value = "";
}

function insertLink(textAreaId) {
	var urlLabel = trim(document.getElementById("urlLabel").value);
	var urlHref = trim(document.getElementById("urlHref").value);
	
	if ((urlLabel.length == 0) || (urlHref.length == 0)) {
		alert(resourceBundle["blog.emptyLinkData"]);
		return;
	}
	
	if (urlHref.indexOf("://") < 0) {
	    urlHref = "http://" + urlHref;
	}
	
    var textAreaField = document.getElementById(textAreaId);
	var linkPlaceHolder = " [\"" + urlLabel + "\",\"" + urlHref + "\"] ";
    insertAtCursor(textAreaField, linkPlaceHolder);
    hideInsertLinkPrompt();
    
    textAreaField.focus();
}

function queryGeoData() {
	setTimeout(function() {
	        var url = getContextRoot() + "/servlet?command=ajaxRPC&method=checkForGeoData";
	    
	        xmlRequest(url, function(req) {
	            if (req.readyState == 4) {
	                if (req.status == 200) {
	                    var responseXml = req.responseXML;
	                    var resultItem = responseXml.getElementsByTagName("result")[0];
	                    var result = resultItem.firstChild.nodeValue;  
	                
	                    if (result && (result == "true")) {
	                        document.getElementById("mapAllLink").style.display = "inline";
	                    } 
	                }
	                
	                // cascading ajax calls for performance reasons
	                queryGPXTracks();
	            }
	        });          
		
	    }, 500);
}

function queryGPXTracks() {
    var url = getContextRoot() + "/servlet?command=ajaxRPC&method=checkForGPXTracks";
	    
	xmlRequest(url, function(req) {
	    if (req.readyState == 4) {
	        if (req.status == 200) {
	            var responseXml = req.responseXML;
	            var resultItem = responseXml.getElementsByTagName("result")[0];
	            var result = resultItem.firstChild.nodeValue;  
	                
	            if (result && (result == "true")) {
	                document.getElementById("gpxAllTracksLink").style.display = "inline";
	            } 
	        }
	        
            // cascading ajax calls for performance reasons
            if (document.getElementById("unseenCommentLink")) {
                queryUnseenComments();
            }
        }
    });          
}

function queryUnseenComments() {
    var url = getContextRoot() + "/servlet?command=ajaxRPC&method=checkForUnseenComments";
	    
	xmlRequest(url, function(req) {
	    if (req.readyState == 4) {
	        if (req.status == 200) {
	            var responseXml = req.responseXML;
	            var resultItem = responseXml.getElementsByTagName("result")[0];
	            var result = resultItem.firstChild.nodeValue;  
	                
               	var unseenCommentLink = document.getElementById("unseenCommentLink");
               	var unseenCommentCount = document.getElementById("unseenCommentCount");

               	if (result && (result != "0")) {
	               	unseenCommentLink.setAttribute("onclick", "showUnseenComment()");
	               	unseenCommentLink.style.display = "inline";
	                    	
	               	unseenCommentCount.innerText = result;
	               	unseenCommentCount.setAttribute("onclick", "showUnseenComment()");
	               	unseenCommentCount.style.display = "inline";
	            } else {
	               	unseenCommentLink.style.display = "none";
	               	unseenCommentCount.style.display = "none";
	            }
	        } else {
	            alert(resourceBundle["alert.communicationFailure"]);
	        }
	    }
	});          
}

function showUnseenComment() {
    var url = getContextRoot() + "/servlet?command=ajaxRPC&method=getFirstUnseenComment";
    
    xmlRequest(url, function(req) {
        if (req.readyState == 4) {
            if (req.status == 200) {
                var responseXml = req.responseXML;
                var resultItem = responseXml.getElementsByTagName("fileName")[0];
                var fileName = resultItem.firstChild.nodeValue;  

                var resultItem = responseXml.getElementsByTagName("linkDate")[0];
                var linkDate = resultItem.firstChild.nodeValue;  
                
            	var targetUrl = getContextRoot() + "/servlet?command=blog&beforeDay=" + linkDate + "&positionToFile=" + fileName;
            	
            	window.location.href = targetUrl;
            } else {
                alert(resourceBundle["alert.communicationFailure"]);
            }
        }
    });          
}

function googleMapAll() {
	var mapWinWidth =  screen.availWidth - 20;
	var mapWinHeight = screen.availHeight - 80;
	
    var mapWin = window.open(getContextRoot() + '/servlet?command=googleMapMulti', 'mapWin', 'status=no,toolbar=no,location=no,menu=no,width=' + mapWinWidth + ',height=' + mapWinHeight + ',resizable=yes,left=2,top=2,screenX=2,screenY=2');

    if (!mapWin) {
        alert(resourceBundle["alert.enablePopups"]);
    } else {
        mapWin.focus();
    }
}

function attachScrollHandler() {
    if (lowBandwidthMode) {
        return;
    }

    window.onscroll = function() {
		var scrollPosDiff = window.pageYOffset - lastScrollPos;

		if ((scrollPosDiff > 20) || (scrollPosDiff < (-20))) {
			lastScrollPos = window.pageYOffset;
			checkThumbnailsToReplace(true);
		}
	};
	
	// replace initially visible thumbnails
	setTimeout(checkThumbnailsToReplace, 500);
}

function checkThumbnailsToReplace(breakOnFirstReplaced) {

	var replacedImages = new Array();
	
	for (var i = 0; i < thumbnails.length; i++) {
        var pic = document.getElementById(thumbnails[i]);
        if (pic) {
        	if (isScrolledIntoView(pic)) {
        		var originalImgSrc = pic.getAttribute("origImgPath");
        		if (originalImgSrc) {
                    replaceTumbnail(pic, originalImgSrc);

                    pic.removeAttribute("origImgPath");
                    replacedImages.push(i);
            		if (breakOnFirstReplaced) {
            			break;
            		}
        		}
        	}
        }
	}
	
	for (var i = replacedImages.length - 1; i >= 0; i--) {
        thumbnails.splice(replacedImages[i], 1);
	}
	
	replacedImages = null;
}

function replaceTumbnail(pic, originalImgSrc) {
	
	var prefetchImg = new Image();    
    
	prefetchImg.onload = function() {
		pic.src = originalImgSrc;
		prefetchImg = null;
	};
	
	prefetchImg.src = originalImgSrc;
}

function replaceEditThumbnail() {
    var pic = document.getElementById("blogPic");
    if (pic) {
		var originalImgSrc = pic.getAttribute("origImgPath");
		if (originalImgSrc) {
            replaceTumbnail(pic, originalImgSrc);
            pic.removeAttribute("origImgPath");
		}
    }
}

function isScrolledIntoView(el) {
    var elemTop = el.getBoundingClientRect().top;
    var elemBottom = el.getBoundingClientRect().bottom;

    var isVisible = (elemTop >= 0) && (elemBottom <= window.innerHeight);
    return isVisible;
}

function publishNewEntries() {
    if (!confirm(resourceBundle["blog.confirmPublishNewEntries"])) {
        return;
    }
    
    window.location.href = getContextRoot() + "/servlet?command=blog&cmd=publishNewEntries";
}

function publishDay(dayToPublish) {
    if (!confirm(resourceBundle["blog.confirmPublishDay"])) {
        return;
    }
    
    window.location.href = getContextRoot() + "/servlet?command=blog&cmd=publishDay&day=" + dayToPublish;
}

function switchLowBandwidthMode() {
    showHourGlass();

    var xmlUrl = getContextRoot() + "/servlet?command=blog&cmd=switchLowBandwidthMode";

	xmlRequest(xmlUrl, function(req) {
        if (req.readyState == 4) {
            hideHourGlass();
            if (req.status == 200) {
                var resultElem = req.responseXML.getElementsByTagName("result")[0];            

                var newMode = resultElem.getElementsByTagName("newBandwidthMode")[0].firstChild.nodeValue;

            	var switchBandwidthLink = document.getElementById("switchBandwidthLink");
                if (newMode == "low") {
                	/*
                	switchBandwidthLink.setAttribute("class", "icon-font icon-signal blogMenu");
                	switchBandwidthLink.setAttribute("title", resourceBundle["blog.highBandwith"]);
                    */
                    window.location.href = getContextRoot() + "/servlet?command=blog";
                } else {
                	switchBandwidthLink.setAttribute("class", "icon-font icon-wifi blogMenu");
                	switchBandwidthLink.setAttribute("title", resourceBundle["blog.lowBandwith"]);
                	lowBandwidthMode = false;
                	attachScrollHandler();
                }
            } else {
                alert(resourceBundle["alert.communicationFailure"]);
                hideHourGlass();    
            }
        }
    });
}

function detachFile(imgName, posInPage) {
    if (!confirm(resourceBundle["blog.confirmDetach"])) {
        return;
    }
     
    showHourGlass();

    var xmlUrl = getContextRoot() + "/servlet?command=blog&cmd=detach&imgName=" + imgName;

	xmlRequest(xmlUrl, function(req) {
        if (req.readyState == 4) {
            hideHourGlass();
            if (req.status == 200) {
                var resultElem = req.responseXML.getElementsByTagName("result")[0];            
                var success = resultElem.getElementsByTagName("success")[0].firstChild.nodeValue;

                if (success == 'true') {
                	var attachmentLink = document.getElementById("attachment-" + posInPage);
                	attachmentLink.setAttribute("class", "icon-font icon-attachment icon-blog-attachment");
                	attachmentLink.removeAttribute("onclick");
                	attachmentLink.onclick = function() {attachFile(imgName, posInPage)};
                	attachmentLink.title = resourceBundle["blog.attach"];
                	
                	var geoTrackLink = document.getElementById("geoTrackLink-" + posInPage);
                	if (geoTrackLink) {
                		geoTrackLink.parentNode.removeChild(geoTrackLink);
                	} else {
                		attachmentIcon = document.getElementById("viewAttachmentIcon-" + posInPage);
                		if (attachmentIcon) {
                			attachmentIcon.parentNode.removeChild(attachmentIcon);
                		}
                	}
                } else {
                	// TODO: resourceBundle
                    alert("failed to detach file");
                }
            } else {
                alert(resourceBundle["alert.communicationFailure"]);
                hideHourGlass();    
            }
        }
    });
}

function attachFile(fileName, posInPage) {
    // window.location.href = getContextRoot() + "/servlet?command=blog&cmd=attachment&fileName=" + encodeURIComponent(fileName) + "&posInPage=" + posInPage;
    
    var uploadCont = document.createElement("div"); 
    uploadCont.id = "uploadCont";
    uploadCont.setAttribute("class", "uploadCont");
    uploadCont.setAttribute("posInPage", posInPage);
    
    document.body.appendChild(uploadCont);

    var uploadContHead = document.createElement("div");
    uploadContHead.setAttribute("class", "uploadContHead");
    
    uploadCont.appendChild(uploadContHead);
    
    var headlineText = document.createElement("span");
    headlineText.innerHTML = resourceBundle["upload.attachment.headline"];
    
    uploadContHead.appendChild(headlineText);

    var fileNameLabelCont = document.createElement("div");
    fileNameLabelCont.setAttribute("class", "uploadLabel");
    uploadCont.appendChild(fileNameLabelCont);

    var fileNameLabelText = document.createElement("span");
    fileNameLabelText.innerHTML = resourceBundle["upload.attachment.label"] + ":";
    fileNameLabelCont.appendChild(fileNameLabelText);
    
    var uploadForm = document.createElement("form");
    uploadForm.id = "uploadForm";
    uploadForm.setAttribute("class", "uploadAttachmentForm");
    uploadForm.setAttribute("enctype", "multipart/form-data");
    uploadForm.setAttribute("method", "post");
    uploadForm.setAttribute("action", getContextRoot() + "/servlet?command=blog&cmd=uploadAttachment"); 
    uploadForm.setAttribute("accept-charset", "utf-8");
    uploadCont.appendChild(uploadForm);    
    
    var fileInput = document.createElement("input");
    fileInput.setAttribute("type", "file");
    fileInput.setAttribute("name", "uploadFile");
    fileInput.onchange = function() {
    	uploadAttachment(this.files[0], fileName);
    };
    uploadForm.appendChild(fileInput);

    var buttonCont = document.createElement("div");
    buttonCont.setAttribute("class", "buttonCont");
    uploadCont.appendChild(buttonCont);
    
    var closeButton = document.createElement("input");
    closeButton.setAttribute("type", "button");
    closeButton.setAttribute("value", resourceBundle["button.closewin"]);
    closeButton.onclick = function() {
    	uploadCont.parentNode.removeChild(uploadCont);
    };
    buttonCont.appendChild(closeButton);
    
    centerBox(uploadCont);
}

function uploadAttachment(file, blogFileName) {
    
    var fileName;
    var fileSize;
    if (browserSafari) {
        fileName = file.fileName;
        fileSize = file.fileSize;
    } else {
        fileName = file.name
        fileSize = file.size;
    }
     
	var uploadCont = document.getElementById("uploadCont");
    uploadCont.style.visibility = "hidden";
	
	if (fileSize > ATTACHMENT_MAX_SIZE) {
    	alert(resourceBundle["upload.attachment.sizeLimitExceeded"] + " " + ATTACHMENT_MAX_SIZE + " bytes.");
    	uploadCont.parentNode.removeChild(uploadCont);
    	return;
    }
    
    sizeOfCurrentFile = fileSize;
	  
    lastUploadedFile = fileName;
      
    document.getElementById("currentFile").innerHTML = shortText(fileName, 50);
          
    document.getElementById("statusText").innerHTML = "0 " + resourceBundle["label.of"] + " " + formatDecimalNumber(fileSize) + " bytes ( 0%)";

    var statusWin = document.getElementById("uploadStatus");
    centerBox(statusWin);
    statusWin.style.visibility = 'visible';

    var now = new Date();

    var serverFileName = "attach-" + now.getTime() + getFileNameExt(fileName).toLowerCase();
                         
    var uploadUrl = getContextRoot() + "/upload/attachment/" + serverFileName + "/" + blogFileName; 

    xhr = new XMLHttpRequest();  

    xhr.onreadystatechange = handleAttachmentUploadState;
    xhr.upload.addEventListener("progress", updateAttachmentUploadProgress, false);
    xhr.upload.addEventListener("load", uploadComplete, false);

    xhr.open("POST", uploadUrl, true);  

    if (!browserMSIE) {
        xhr.overrideMimeType('text/plain; charset=x-user-defined-binary');  
    }
         
    if (firefoxDragDrop) {
        try {
            xhr.sendAsBinary(file.getAsBinary());    
        } catch (ex) {
            // Chrome has no file.getAsBinary() function
            xhr.send(file);
        }
    } else {
        xhr.send(file);
    }    
}

function handleAttachmentUploadState() {
    if (xhr.readyState == 4) {
        document.getElementById("uploadStatus").style.visibility = 'hidden';
        
        if (xhr.status == 200) {
        	var posInPage = uploadCont.getAttribute("posInPage");
            var returnURL = getContextRoot() + "/servlet?command=blog&cmd=list&random=" + ((new Date()).getTime()) + "#entry-" + posInPage;
        	window.location.href = returnURL;
        } else {
            alert(resourceBundle["upload.error"] + " " + lastUploadedFile);
        }
    }
}

function updateAttachmentUploadProgress(e) {
    if (e.lengthComputable) {  
        var percent = Math.round((e.loaded * 100) / e.total);  
        document.getElementById("statusText").innerHTML = formatDecimalNumber(e.loaded) + " " + resourceBundle["label.of"] + " " + formatDecimalNumber(e.total) + " bytes (" + percent + "%)";
        document.getElementById("done").width = 3 * percent;
        document.getElementById("todo").width = 300 - (3 * percent);
    }  
}

function viewAttachment(blogFileName, attachmentName) {
    var attachmentWin = window.open(getContextRoot() + "/servlet?command=getAttachment&attachmentName=" + encodeURIComponent(attachmentName), "attachmentWin");
    attachmentWin.focus();
}

function viewGeoTrack(blogFileName, attachmentName) {
    var geoTrackWin = window.open(getContextRoot() + "/servlet?command=viewGPX&attachmentName=" + encodeURIComponent(attachmentName), "geoTrackWin");
    geoTrackWin.focus();
}

function statistics() {
    showHourGlass();

    var statisticCont = document.createElement("div"); 
    statisticCont.id = "statisticCont";
    statisticCont.setAttribute("class", "statisticCont");
    
    document.body.appendChild(statisticCont);

    var xmlUrl = getContextRoot() + "/servlet?command=blog&cmd=statistics";
        
    var xslUrl = getContextRoot() + "/xsl/blog/statistics.xsl";    
        
    htmlFragmentByXslt(xmlUrl, xslUrl, statisticCont, function() {
        setBundleResources();
        centerBox(statisticCont);
        statisticCont.style.visibility = "visible";
        hideHourGlass();
    });
}

function hideStatistics() {
    var statisticCont = document.getElementById("statisticCont");
    if (statisticCont) {
        statisticCont.parentNode.removeChild(statisticCont);
    }
}

function gotoNextDay(clickTarget) {
	let currentBlogDateCont = clickTarget.parentNode;
	
	let nextSibling = currentBlogDateCont.nextSibling;
	while (nextSibling) {
		if (nextSibling.nodeType == 1) {
			let cssClass = nextSibling.getAttribute("class");
			if (cssClass && cssClass.includes("blogDate")) {
	            nextSibling.scrollIntoView();
				return;
			}
		}
		nextSibling = nextSibling.nextSibling;
	}
}

function gotoPrevDay(clickTarget) {
	let currentBlogDateCont = clickTarget.parentNode;
	
	let prevSibling = currentBlogDateCont.previousSibling;
	while (prevSibling) {
		if (prevSibling.nodeType == 1) {
			let cssClass = prevSibling.getAttribute("class");
			if (cssClass && cssClass.includes("blogDate")) {
				prevSibling.scrollIntoView();
				return;
			}
		}
		prevSibling = prevSibling.previousSibling;
	}
}

function showAllGPXTracks() {
    var geoTrackWin = window.open(getContextRoot() + "/servlet?command=multiGPXTrack", "geoTrackWin");
    geoTrackWin.focus();
}

function allDayOverview() {
	window.location.href = getContextRoot() + "/servlet?command=blog&cmd=overview";
}
