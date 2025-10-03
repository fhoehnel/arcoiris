var osmMap;

function fillSideCont() {
    console.log("winWidth: " + getWinWidth());
    if (getWinWidth() < 1530) {
        return;
    }
    createSideCalendar();
    createSideMap();
}

function createSideCalendar() {
    const sideCalCont = document.createElement("div");
    sideCalCont.setAttribute("id", "sideCalCont");
    sideCalCont.setAttribute("class", "calendarCont");
    document.getElementById("sideCont").appendChild(sideCalCont);

    const sideCal = new CalendarPopup("sideCalCont", false);
    sideCal.setReturnFunction("gotoSelectedDate");
    sideCal.showYearNavigation();
    if ("German" === language) {
        sideCal.setWeekStartDay(1);
    }

    selectDate(sideCal,"sideContDate", "anchorSideDate");
}

function createSideMap() {
    const sideMapCont = document.createElement("div");
    sideMapCont.setAttribute("id", "sideMapCont");
    sideMapCont.setAttribute("class", "sideMapCont");
    document.getElementById("sideCont").appendChild(sideMapCont);

    if (geoCoordinates.length > 0) {
        const sideMap = document.createElement("div");
        sideMap.setAttribute("id", "sideMap");
        sideMap.setAttribute("class", "sideMap");
        sideMapCont.appendChild(sideMap);

        if (sideContMapType === 2) {
            loadGoogleMapsAPIScriptCode(googleMapsAPIKey, "handleSideMapsApiReady");
        } else {
            createSideOsmMap();
        }
    }
}

function createSideOsmMap() {
    let centerLatitude = 51;
    let centerLongitude = 13;
    if (geoCoordinates.length > 0) {
        centerLatitude = geoCoordinates[geoCoordinates.length - 1][0];
        centerLongitude = geoCoordinates[geoCoordinates.length - 1][1];
    }

    osmMap = new OpenLayers.Map("sideMap");
    osmMap.addLayer(new OpenLayers.Layer.OSM());
    // does not work :-(
    // map.addControl(new OpenLayers.Control.FullScreen());

    const centerLonLat =
        new OpenLayers.LonLat(centerLongitude, centerLatitude)
            .transform(
                new OpenLayers.Projection("EPSG:4326"), // transform from WGS 1984
                osmMap.getProjectionObject() // to Spherical Mercator Projection
            );

    var markers = new OpenLayers.Layer.Markers( "Markers" );
    osmMap.addLayer(markers);

    for (let i = 0; i < geoCoordinates.length; i++) {
        let lonLat = new OpenLayers.LonLat(geoCoordinates[i][1], geoCoordinates[i][0])
            .transform(
                new OpenLayers.Projection("EPSG:4326"),
                osmMap.getProjectionObject()
            );

        markers.addMarker(new OpenLayers.Marker(lonLat));
    }

    const zoom = 9;
    osmMap.setCenter(centerLonLat, zoom);

    createOsmFullScreenOnToggle();
}

function createOsmFullScreenOnToggle() {
    const fullScreenToggle = document.createElement("div");
    fullScreenToggle.setAttribute("id", "fullScreenToggle");
    fullScreenToggle.setAttribute("class", "fullScreenToggle");
    document.getElementById("sideMapCont").appendChild(fullScreenToggle);

    const fullScreenIcon = document.createElement("i");
    fullScreenIcon.setAttribute("id", "fullScreenIcon");
    fullScreenIcon.setAttribute("class", "icon-font icon-link fullScreenIcon");
    fullScreenIcon.setAttribute("title", "fullscreen map");
    fullScreenIcon.setAttribute("onclick", "toggleFullscreenOn()")
    fullScreenToggle.appendChild(fullScreenIcon);
}

function toggleFullscreenOn() {
    const sideMap = document.getElementById("sideMap");
    sideMap.parentNode.removeChild(sideMap);
    let cssClasses = sideMap.getAttribute("class");
    cssClasses += " sideMapFullScreen";
    sideMap.setAttribute("class", cssClasses);
    sideMap.style.width = document.body.clientWidth;
    sideMap.style.height = window.innerHeight;
    document.body.appendChild(sideMap);
    osmMap.updateSize();

    createOsmFullScreenOffToggle();
}

function createOsmFullScreenOffToggle() {
    const fullScreenOffToggle = document.createElement("div");
    fullScreenOffToggle.setAttribute("id", "fullScreenOffToggle");
    fullScreenOffToggle.setAttribute("class", "fullScreenOffToggle");
    document.getElementById("sideMap").appendChild(fullScreenOffToggle);

    const fullScreenIcon = document.createElement("i");
    fullScreenIcon.setAttribute("id", "fullScreenOffIcon");
    fullScreenIcon.setAttribute("class", "icon-font icon-close fullScreenIcon");
    fullScreenIcon.setAttribute("title", "small map");
    fullScreenIcon.setAttribute("onclick", "toggleFullscreenOff()")
    fullScreenOffToggle.appendChild(fullScreenIcon);
}

function toggleFullscreenOff() {
    const sideMap = document.getElementById("sideMap");
    sideMap.parentNode.removeChild(sideMap);
    let cssClasses = sideMap.getAttribute("class");
    cssClasses = cssClasses.substring(0, cssClasses.indexOf(" sideMapFullScreen"));
    sideMap.setAttribute("class", cssClasses);
    sideMap.style.width = 260;
    sideMap.style.height = 280;
    document.getElementById("sideMapCont").appendChild(sideMap);
    osmMap.updateSize();

    const fullScreenOffToggle = document.getElementById("fullScreenOffToggle");
    fullScreenOffToggle.parentNode.removeChild(fullScreenOffToggle);
    createOsmFullScreenOnToggle();
}

function handleSideMapsApiReady() {
    const zoomFactor = 8;

    let centerLatitude = 51;
    let centerLongitude = 13;
    if (geoCoordinates.length > 0) {
        centerLatitude = geoCoordinates[geoCoordinates.length - 1][0];
        centerLongitude = geoCoordinates[geoCoordinates.length - 1][1];
    }

    const myOptions = {
        zoom: zoomFactor,
        center: new google.maps.LatLng(centerLatitude, centerLongitude),
        mapTypeId: google.maps.MapTypeId.HYBRID
    }

    const map = new google.maps.Map(document.getElementById("sideMap"), myOptions);

    for (let i = 0; i < geoCoordinates.length; i++) {
        const markerPos = new google.maps.LatLng(geoCoordinates[i][0], geoCoordinates[i][1]);

        marker = new google.maps.Marker({
            position: markerPos,
            title: geoCoordinates[i][2]
        });
        marker.setMap(map);
    }
}
