function showLocationsOSM() {
    osmMap = new OpenLayers.Map("mapDiv");
    osmMap.addLayer(new OpenLayers.Layer.OSM());
    const pois = new OpenLayers.Layer.Text("My Points", {
        location: contextRoot + "/servlet?command=osmMultiPOIList",
        projection: osmMap.displayProjection
    });
    osmMap.addLayer(pois);

    // without the next 3 lines the map is not shown at all - will be overridden by the zoomToExtent in the loadend event handler
    const centerLonLat = new OpenLayers.LonLat(13, 51);
    const zoom = 9;
    osmMap.setCenter(centerLonLat, zoom);

    pois.events.register("loadend", pois, function() {
        const extent = pois.getDataExtent();
        if (extent) {
            osmMap.zoomToExtent(extent);
        }
    });
}