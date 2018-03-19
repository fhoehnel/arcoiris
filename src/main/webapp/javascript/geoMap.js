    function showMapSelection(counter)
    {
        if (counter) 
        {
            document.getElementById("mapIcon-" + counter).style.display = "none";
            document.getElementById("geoLocSel-" + counter).style.display = "block";
        }
        else 
        {
            document.getElementById("mapIcon").style.display = "none";
            document.getElementById("geoLocSel").style.display = "block";
        }
    }
      
    function geoMapFolderSelected(folderPath) 
    {
        var mapSel = document.getElementById("geoLocSel");
    
        var idx = mapSel.selectedIndex;

        var mapType = mapSel.options[idx].value;

    	mapSel.selectedIndex = 0;
    	
        mapSel.style.display = "none";
        document.getElementById("mapIcon").style.display = "inline";

	    var mapWinWidth =  screen.availWidth - 20;
	    var mapWinHeight = screen.availHeight - 80;

        if (mapType == "1")
        {
            var mapWin = window.open(getContextRoot() + '/servlet?command=osMap&path=' + encodeURIComponent(folderPath),'mapWin','status=no,toolbar=no,location=no,menu=no,width=' + mapWinWidth + ',height=' + mapWinHeight + ',resizable=yes,left=1,top=1,screenX=1,screenY=1');
            if (!mapWin) 
            {
            	alert(resourceBundle["alert.enablePopups"]);
            }
            else
            {
                mapWin.focus();
            }
        } 
        else if (mapType == "2")
        {
            var mapWin = window.open(getContextRoot() + '/servlet?command=googleMap&path=' + encodeURIComponent(folderPath),'mapWin','status=no,toolbar=no,location=no,menu=no,width=' + mapWinWidth + ',height=' + mapWinHeight + ',resizable=yes,left=1,top=1,screenX=1,screenY=1');
            if (!mapWin) 
            {
            	alert(resourceBundle["alert.enablePopups"]);
            } 
            else
            {
                mapWin.focus();
            }
        } 
        else
        {
            window.location.href = getContextRoot() + "/servlet?command=googleEarthFolderPlacemark";
        }
    }  

    function geoMapFileSelected(fileName, counter) 
    {
        var mapSel;
        if (counter) 
        {
            mapSel = document.getElementById("geoLocSel-" + counter);
        }
        else 
        {
            mapSel = document.getElementById("geoLocSel");
        }
    
        var idx = mapSel.selectedIndex;

        var mapType = mapSel.options[idx].value;

        mapSel.selectedIndex = 0;
        
        var mapIcon;
        if (counter)
        {
            mapIcon = document.getElementById("mapIcon-" + counter)
        }
        else 
        {
            mapIcon = document.getElementById("mapIcon")
        }
        
        if (mapIcon) {
            mapSel.style.display = "none";
            mapIcon.style.display = "inline";
        }

	    var mapWinWidth = screen.availWidth - 20;
	    var mapWinHeight = screen.availHeight - 80;

        if (mapType == "1")
        {
            var mapWin = window.open(getContextRoot() + '/servlet?command=osMap&fileName=' + encodeURIComponent(fileName),'_blank','status=no,toolbar=no,location=no,menu=no,width=' + mapWinWidth + ',height=' + mapWinHeight + ',resizable=yes,left=1,top=1,screenX=1,screenY=1');
            if (!mapWin) 
            {
            	alert(resourceBundle["alert.enablePopups"]);
            }
            else
            {
                mapWin.focus();
            }
        } 
        else if (mapType == "2")
        {
            var mapWin = window.open(getContextRoot() + '/servlet?command=googleMap&fileName=' + encodeURIComponent(fileName),'_blank','status=no,toolbar=no,location=no,menu=no,width=' + mapWinWidth + ',height=' + mapWinHeight + ',resizable=yes,left=1,top=1,screenX=1,screenY=1');
            if (!mapWin) 
            {
            	alert(resourceBundle["alert.enablePopups"]);
            }
            else
            {
                mapWin.focus();
            }
        } 
        else
        {
            window.location.href = getContextRoot() + "/servlet?command=googleEarthPlacemark&fileName=" + fileName;
        }
    }  

    function showImageOnMap(picFileName) {
        removeImageFromMap();
    
    	var picContElem = document.createElement("div");
    	picContElem.id = "picOnMapCont";
    	picContElem.setAttribute("class", "picOnMap");
    	document.getElementById("map").firstChild.appendChild(picContElem);
	    
	    var closeIconElem = document.createElement("img");
	    closeIconElem.setAttribute("src", getContextRoot() + "/images/winClose.gif");
        closeIconElem.setAttribute("style", "float:right");
        picContElem.appendChild(closeIconElem);
	    
        var picElem = document.createElement("img");
        picElem.id = "picOnMap";
	    picElem.onload = resizeAndShowPic;
    	picElem.setAttribute("class", "picOnMap");
        picElem.setAttribute("src", getContextRoot() + "/servlet?command=getFile&fileName=" + encodeURIComponent(picFileName));
        picElem.setAttribute("picFileName", picFileName);
        picContElem.appendChild(picElem);
        
	    centerBox(picContElem);
	    
	    picContElem.onclick = removeImageFromMap;
    }
    
    function resizeAndShowPic() {
    	var picOnMapElem = document.getElementById("picOnMap");
    	
    	var thumbDimensions = calculateAspectRatioFit(picOnMapElem.width, picOnMapElem.height, 400, 400);
    	picOnMapElem.style.width = thumbDimensions.width + "px";
    	picOnMapElem.style.height = thumbDimensions.height + "px";
    	
    	if (thumbDimensions.height < 399) {
        	var picContElem = document.getElementById("picOnMapCont");
        	picContElem.style.height = (thumbDimensions.height + 30) + "px";
    	}
    	if (thumbDimensions.width < 399) {
        	var picContElem = document.getElementById("picOnMapCont");
        	picContElem.style.width = thumbDimensions.width + "px";
    	}
    	
    	picOnMapElem.style.display = 'inline';
    	
    	
    	var picFileName = picOnMapElem.getAttribute("picFileName");
    	
        var ajaxUrl = getContextRoot() + "/servlet?command=getFileDesc&fileName=" + encodeURIComponent(picFileName);
        
    	xmlRequest(ajaxUrl, function(req) {
            if (req.readyState == 4) {
                if (req.status == 200) {
                    var fileDescription = req.responseXML.getElementsByTagName("result")[0].firstChild.nodeValue;        
                    if (fileDescription && (fileDescription.length > 0)) {
                        picOnMapElem.setAttribute("title", fileDescription);
                    }
                } else {
                    alert(resourceBundle["alert.communicationFailure"]);
	            }
            }
    	});
    }
    
    function removeImageFromMap() {
    	var picContElem = document.getElementById("picOnMapCont");
    	if (picContElem) {
        	picContElem.parentNode.removeChild(picContElem);
    	}
    }