<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">	
<xsl:output method="html" indent="yes" omit-xml-declaration="yes" encoding="UTF-8" 
    doctype-public="html" />

<xsl:strip-space elements="registration" />

<!-- root node-->
<xsl:template match="/registration">

<html>
<head>

  <meta http-equiv="expires" content="0" />

  <link rel="stylesheet" type="text/css">
    <xsl:attribute name="href"><xsl:value-of select="//contextRoot" />/styles/common.css</xsl:attribute>
  </link>

  <script type="text/javascript">
    <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/javascript/browserCheck.js</xsl:attribute>
  </script>

  <script type="text/javascript">
    <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/javascript/resourceBundle.js</xsl:attribute>
  </script>
  <script type="text/javascript">
    <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/servlet?command=getResourceBundle&amp;lang=<xsl:value-of select="/registration/language" /></xsl:attribute>
  </script>

  <title resource="label.regtitle"></title>

</head>

<body>

<div class="centerBox">
  <div class="registrationBox">

    <table border="0" cellpadding="5" cellspacing="0" width="100%">
      <tr>
        <td class="loginTitle" style="padding-left:10px;vertical-align:top;">
          <img border="0">
            <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/images/logo.gif</xsl:attribute>
          </img>
          <div style="width:100%;padding-top:10px;padding-left:0px;">
            arcoiris blog
            <br/><br/>
            <span class="selfRegistrationSubTitle" resource="label.regtitle"></span>
          </div>
        </td>
        
        <td width="65%" class="registrationConfirmation">
          <span resource="registrationConfirmation"></span>
          
          <br/><br/>

          <input type="button" resource="button.registrationToLogin">
            <xsl:attribute name="onclick">window.location.href='<xsl:value-of select="//contextRoot" />/servlet'</xsl:attribute>
          </input>
          
        </td>
      </tr>
    </table>

  </div>
</div>

</body>

<script type="text/javascript">
  setBundleResources();
</script>

</html>

</xsl:template>

</xsl:stylesheet>
