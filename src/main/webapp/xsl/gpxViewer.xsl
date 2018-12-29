<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">	
<xsl:output method="html" indent="yes" omit-xml-declaration="yes" encoding="UTF-8" 
    doctype-public="html" />

<xsl:strip-space elements="gpx" />

<xsl:template match="/">

<html>
  <head>

    <meta http-equiv="expires" content="0" />

    <title>Arcoiris geotrack viewer</title>

    <link rel="stylesheet" type="text/css">
      <xsl:attribute name="href"><xsl:value-of select="//contextRoot" />/styles/common.css</xsl:attribute>
    </link>

    <link rel="stylesheet" type="text/css">
      <xsl:attribute name="href"><xsl:value-of select="//contextRoot" />/styles/blog.css</xsl:attribute>
    </link>

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
      <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/servlet?command=getResourceBundle&amp;lang=<xsl:value-of select="/gpx/language" /></xsl:attribute>
    </script>

    <script type="text/javascript">
      var trackNumber = <xsl:value-of select="count(/gpx/track)" />;
      
      var currentTrack = 0;
      
      var filePath = '<xsl:value-of select="/gpx/filePath" />';
      
    </script>
    
  </head>

  <body>
    <xsl:if test="/gpx/track">
      <xsl:attribute name="onload">loadGoogleMapsAPICode('<xsl:value-of select="/gpx/googleMapsAPIKey" />')</xsl:attribute>
    </xsl:if>

    <xsl:if test="not(/gpx/track)">
      <script type="text/javascript">
          customAlert("GPX file does not contain any track data");
      </script>
    </xsl:if>

    <xsl:if test="/gpx/track">
      <div id="mapCont" class="gpsTrackMapCont"></div>

      <div id="gpsTrackMetaInfo"></div>
      
    </xsl:if>

  </body>
  
  <script type="text/javascript">
    setBundleResources();
  </script>

</html>

</xsl:template>

</xsl:stylesheet>
