<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">	
<xsl:output method="html" indent="yes" omit-xml-declaration="yes" encoding="UTF-8" 
    doctype-public="html" />

<xsl:strip-space elements="blog" />

<!-- root node-->
<xsl:template match="/">

  <html class="blog">
    <head>

      <meta http-equiv="expires" content="0" />

      <title>arcoiris blog</title>
      
      <link rel="stylesheet" type="text/css">
        <xsl:attribute name="href"><xsl:value-of select="//contextRoot" />/styles/common.css</xsl:attribute>
      </link>

      <link rel="stylesheet" type="text/css">
        <xsl:attribute name="href"><xsl:value-of select="//contextRoot" />/styles/blog.css</xsl:attribute>
      </link>

      <link rel="stylesheet" type="text/css">
        <xsl:attribute name="href"><xsl:value-of select="//contextRoot" />/styles/blogskins/<xsl:value-of select="/blog/skin" />.css</xsl:attribute>
      </link>
      
      <script type="text/javascript">
        <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/javascript/browserCheck.js</xsl:attribute>
      </script>

      <script type="text/javascript">
        <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/javascript/resourceBundle.js</xsl:attribute>
      </script>
      <script type="text/javascript">
        <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/servlet?command=getResourceBundle&amp;lang=<xsl:value-of select="/blog/language" /></xsl:attribute>
      </script>

    </head>

    <body class="blog">
      
      <div class="blogCont">
      
        <div class="blogHeadline">
          <xsl:value-of select="/blog/blogTitle" />
        </div> 

        <xsl:if test="/blog/success">
          <div class="blogEmpty">
            <span resource="blog.unsubscribeSuccess"></span>
          </div>
        </xsl:if>
      
        <xsl:if test="not(/blog/success)">
          <span resource="blog.unsubscribeFailure" class="blogErrorMsg"></span>
        </xsl:if>

      </div>
      
    </body>
    
    <script type="text/javascript">
      setBundleResources();
    </script>
    
  </html>

</xsl:template>

</xsl:stylesheet>
