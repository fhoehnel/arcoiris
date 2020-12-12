<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">	
<xsl:output method="html" indent="yes" omit-xml-declaration="yes" encoding="UTF-8" 
    doctype-public="html" />

<xsl:strip-space elements="gpx" />

<!-- root node-->
<xsl:template match="/">

<html>
  <head>

    <meta http-equiv="expires" content="0" />

    <link rel="stylesheet" type="text/css" href="/webfilesys/styles/common.css" />

    <title>WebFileSys GPX track viewer</title>

    <script type="text/javascript">
      <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/javascript/browserCheck.js</xsl:attribute>
    </script>
    <script type="text/javascript">
      <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/javascript/util.js</xsl:attribute>
    </script>
    <script type="text/javascript">
      <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/javascript/ajaxCommon.js</xsl:attribute>
    </script>
    <script type="text/javascript">
      <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/javascript/gpxTrack.js</xsl:attribute>
    </script>

    <script type="text/javascript">
      <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/javascript/resourceBundle.js</xsl:attribute>
    </script>
    <script type="text/javascript">
      <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/servlet?command=getResourceBundle&amp;lang=<xsl:value-of select="/gpxTracks/language" /></xsl:attribute>
    </script>

    <script type="text/javascript">
      var trackNumber = <xsl:value-of select="count(/gpxTracks/gpxFiles/gpxFile)" />;
      
      var currentTrack = 0;
      
      var gpxFiles = new Array();
      
      <xsl:for-each select="/gpxTracks/gpxFiles/gpxFile">
          gpxFiles.push('<xsl:value-of select="." />');
      </xsl:for-each>
      
    </script>
    
  </head>

  <body>
    <xsl:attribute name="onload">loadGoogleMapsAPICode('<xsl:value-of select="/gpxTracks/googleMapsAPIKey" />')</xsl:attribute>

    <div id="mapCont" class="gpsTrackMapCont"></div>

    <div id="gpsTrackMetaInfo"></div>

  </body>
  
  <script type="text/javascript">
    setBundleResources();
  </script>

</html>

</xsl:template>

</xsl:stylesheet>
