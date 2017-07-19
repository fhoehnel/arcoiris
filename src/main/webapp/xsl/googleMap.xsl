<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">	
<xsl:output method="html" indent="yes" omit-xml-declaration="yes" encoding="UTF-8" 
    doctype-public="html" />

<xsl:strip-space elements="geoTag" />

<!-- root node-->
<xsl:template match="/">

  <html style="height:100%">
    <head>

      <meta http-equiv="expires" content="0" />

      <link rel="stylesheet" type="text/css">
        <xsl:attribute name="href"><xsl:value-of select="//contextRoot" />/styles/common.css</xsl:attribute>
      </link>

      <link rel="stylesheet" type="text/css">
        <xsl:attribute name="href"><xsl:value-of select="//contextRoot" />/styles/blogskins/<xsl:value-of select="/blog/skin" />.css</xsl:attribute>
      </link>
  
      <title>arcoiris blog</title>

      <script type="text/javascript">
        <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/javascript/geoMap.js</xsl:attribute>
      </script>

      <script type="text/javascript">
  
        function handleGoogleMapsApiReady()
        {
            var latitude = '<xsl:value-of select="/geoTag/latitude" />';
            var longitude = '<xsl:value-of select="/geoTag/longitude" />';
            var zoomFactor = <xsl:value-of select="/geoTag/zoomFactor" />;

            var infoText = '<xsl:value-of select="/geoTag/infoText" />';
  
            var mapCenter = new google.maps.LatLng(latitude, longitude);
    
            var myOptions = {
                zoom: zoomFactor,
                center: mapCenter,
                mapTypeId: google.maps.MapTypeId.HYBRID
            }
      
            var map = new google.maps.Map(document.getElementById("map"), myOptions);      
    
            var marker = new google.maps.Marker({
                position: mapCenter
            });

            marker.setMap(map);    
    
            if (infoText != '') {
                var infowindow = new google.maps.InfoWindow({
                    content: '<div style="width:160px;height:30px;overflow-x:auto;overflow-y:auto">' + infoText + '</div>'
                });

                infowindow.open(map, marker);
            }    
        }  
    
        function loadGoogleMapsAPIScriptCode(googleMapsAPIKey) {
            var script = document.createElement("script");
            script.type = "text/javascript";

            if (window.location.href.indexOf("https") == 0) {
                script.src = "https://maps.google.com/maps/api/js?callback=handleGoogleMapsApiReady&amp;key=" + googleMapsAPIKey;
            } else {
                script.src = "http://maps.google.com/maps/api/js?callback=handleGoogleMapsApiReady&amp;key=" + googleMapsAPIKey;
            }        
            document.body.appendChild(script);
        }
    
      </script>

    </head>

    <body onload="loadGoogleMapsAPIScriptCode()" style="margin:0px;height:100%;">
      <xsl:attribute name="onload">loadGoogleMapsAPIScriptCode('<xsl:value-of select="/geoTag/googleMapsAPIKey" />')</xsl:attribute>

      <div id="map" style="width:100%;height:100%;"></div>

    </body>

  </html>

</xsl:template>

</xsl:stylesheet>
