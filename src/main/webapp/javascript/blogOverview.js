function isPartiallyScrolledIntoView(el) {
    var elemTop = el.getBoundingClientRect().top;
    var elemBottom = el.getBoundingClientRect().bottom;

    var isVisible = (elemBottom >= 0) && (elemTop <= window.innerHeight);
    return isVisible;
}


function attachOverviewScrollHandler() {
    window.onscroll = function() {
		var scrollPosDiff = window.pageYOffset - lastScrollPos;
		if ((scrollPosDiff > 20) || (scrollPosDiff < (-20))) {
			lastScrollPos = window.pageYOffset;
			checkThumbnailsToLoad();
		}
	};
	
	// load initially visible thumbnails
	setTimeout(checkThumbnailsToLoad, 500);
}

function checkThumbnailsToLoad() {

	for (let i = 0; i < thumbnails.length; i++) {
        const pic = document.getElementById(thumbnails[i]);
        if (pic) {
    		const loaded = pic.getAttribute("loaded");
            if (!loaded) {
        	    if (isPartiallyScrolledIntoView(pic)) {
            		var imgPath = pic.getAttribute("imgPath");
            		if (imgPath) {
                        loadTumbnail(pic, imgPath);
                        pic.setAttribute("loaded", "true");
            		}
        	    }
            } else {
        	    if (!isPartiallyScrolledIntoView(pic)) {
                    const imgWidth = pic.width;
                    const imgHeight = pic.height;
                    pic.setAttribute("src", contextRoot + "/images/space.gif");
                    pic.style.width = imgWidth + "px";
                    pic.style.height = imgHeight + "px";
                    pic.removeAttribute("loaded");
        	    }
            }
        }
	}
}

function loadTumbnail(pic, imgSrc) {
	
	var prefetchImg = new Image();    
    
	prefetchImg.onload = function() {
		pic.src = imgSrc;
        pic.style.width = "auto";
        pic.style.height = "auto";
		prefetchImg = null;
	};
	
	prefetchImg.src = imgSrc;
}
