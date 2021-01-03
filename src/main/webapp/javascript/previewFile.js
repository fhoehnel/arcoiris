const PREVIEW_PIC_SIZE = 400;

let filePreviewTimeout = null;

let filePreviewActive = false;

function addPreviewHandler() {
	
	$(".blogOverviewPic > a > img").each(function() {
  	    $(this).mouseover(function() {
	   		var timeoutFunctionCall = "previewSearchResult('" + $(this).attr("imgPath") + "')";
	   		filePreviewTimeout = setTimeout(timeoutFunctionCall, 500);
		});			

		$(this).mouseout(function() {
			cancelSearchPreview();
		});
	});
}

function previewSearchResult(imgPath) {
	filePreviewTimeout = null;

   	const filePreviewCont = document.createElement("div");
   	filePreviewCont.id = "filePreviewCont";
   	filePreviewCont.setAttribute("class", "filePreviewCont");
   	document.documentElement.appendChild(filePreviewCont);
   	
   	const previewPic = document.createElement("img");
   	
   	previewPic.onload = function() {
   		
		const picOrigWidth = getNaturalWidth(previewPic);
		const picOrigHeight = getNaturalHeight(previewPic);
		
		if ((picOrigWidth > PREVIEW_PIC_SIZE) || (picOrigHeight > PREVIEW_PIC_SIZE)) {
			if (picOrigWidth > picOrigHeight) {
				previewPic.width = PREVIEW_PIC_SIZE;
				let scaledHeight = picOrigHeight * PREVIEW_PIC_SIZE / picOrigWidth;
				previewPic.height = scaledHeight;
				filePreviewCont.style.height = scaledHeight + "px";
			} else {
				previewPic.height = PREVIEW_PIC_SIZE;
				let scaledWidth =  picOrigWidth * PREVIEW_PIC_SIZE / picOrigHeight;
				previewPic.width = scaledWidth;
				filePreviewCont.style.width = scaledWidth + "px";
			}
		} else {
			filePreviewCont.style.height = picOrigHeight + "px";
			filePreviewCont.style.width = picOrigWidth + "px";
		}

		previewPic.style.display = "inline";
	};
   	
   	previewPic.src = imgPath;
   	filePreviewCont.appendChild(previewPic);
   	
    filePreviewActive = true;
}

function cancelSearchPreview() {
	if (filePreviewTimeout) {
		clearTimeout(filePreviewTimeout);
		return;
	}
	
	if (!filePreviewActive) {
		return;
	}
	
	document.documentElement.removeChild(document.getElementById("filePreviewCont"));

	filePreviewActive = false;
}

function getNaturalWidth(pic) {
    if (pic.naturalWidth) {
        return pic.naturalWidth;
    }

    // workaround for MSIE
    const origWidthAttrib = pic.getAttribute("origWidth");
            
    if (origWidthAttrib) {
	    return parseInt(origWidthAttrib);
    }
    
    return 0;
}

function getNaturalHeight(pic) {
    if (pic.naturalHeight) {
        return pic.naturalHeight;
    }

    // workaround for MSIE
    const origHeightAttrib = pic.getAttribute("origHeight");
            
    if (origHeightAttrib) {
	    return parseInt(origHeightAttrib);
    }
    
    return 0;
}
