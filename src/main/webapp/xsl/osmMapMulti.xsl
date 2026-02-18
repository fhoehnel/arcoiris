<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">	
<xsl:output method="html" indent="yes" omit-xml-declaration="yes" encoding="UTF-8" 
    doctype-public="html" />

<xsl:strip-space elements="geoData" />

<xsl:template match="/">

<html style="height:100%">
<head>

  <meta http-equiv="expires" content="0" />

  <link rel="stylesheet" type="text/css">
    <xsl:attribute name="href"><xsl:value-of select="//contextRoot" />/styles/common.css</xsl:attribute>
  </link>

  <link rel="stylesheet" type="text/css">
    <xsl:attribute name="href"><xsl:value-of select="//contextRoot" />/styles/icons.css</xsl:attribute>
  </link>

  <link rel="stylesheet" type="text/css">
    <xsl:attribute name="href"><xsl:value-of select="//contextRoot" />/styles/blogskins/<xsl:value-of select="/geoData/skin" />.css</xsl:attribute>
  </link>
  
  <title>
    arcoiris blog OSM map
  </title>

  <script type="text/javascript">
    <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/javascript/browserCheck.js</xsl:attribute>
  </script>
  <script type="text/javascript">
    <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/javascript/util.js</xsl:attribute>
  </script>
  <script type="text/javascript">
    <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/javascript/osmLocations.js</xsl:attribute>
  </script>
  <script type="text/javascript">
    <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/javascript/openStreetMaps/OpenLayers-2.13.1.js</xsl:attribute>
  </script>

  <script type="text/javascript">
    <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/javascript/resourceBundle.js</xsl:attribute>
  </script>
  <script type="text/javascript">
    <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/servlet?command=getResourceBundle&amp;lang=<xsl:value-of select="/geoData/language" /></xsl:attribute>
  </script>

  <script type="text/javascript">
    var geoCoordinates = [];

    <xsl:for-each select="/geoData/markers/marker">
        geoCoordinates.push({
          lon: <xsl:value-of select="longitude" />,
          lat: <xsl:value-of select="latitude" />,
          infoText: '<xsl:value-of select="infoText" />'
        });
    </xsl:for-each>
    
  </script>
</head>

<body style="margin:0px;height:100%;">
  <xsl:attribute name="onload">showLocationsOSM()</xsl:attribute>

  <div id="mapDiv" class="gpsTrackMapCont"></div>

  <script type="text/javascript">
    setBundleResources();
  </script>

</body>

</html>

</xsl:template>

</xsl:stylesheet>
