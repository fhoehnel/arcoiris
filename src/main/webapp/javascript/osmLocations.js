function showLocationsOSM() {
    osmMap = new OpenLayers.Map("mapDiv");
    osmMap.addLayer(new OpenLayers.Layer.OSM());

    const markers = new OpenLayers.Layer.Markers( "Markers" );
    osmMap.addLayer(markers);

    for (let i = 0; i < geoCoordinates.length; i++) {
        let lonLat = new OpenLayers.LonLat(geoCoordinates[i].lon, geoCoordinates[i].lat)
            .transform(
                new OpenLayers.Projection("EPSG:4326"),
                osmMap.getProjectionObject()
            );
        markers.addMarker(new OpenLayers.Marker(lonLat));
    }

    const centerLonLat = new OpenLayers.LonLat(geoCoordinates[0].lon, geoCoordinates[0].lat)
    const zoom = 9;
    osmMap.setCenter(centerLonLat, zoom);

    osmMap.zoomToExtent(markers.getDataExtent());
}