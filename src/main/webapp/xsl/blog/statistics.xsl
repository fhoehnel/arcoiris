<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">	
<xsl:output method="html" indent="yes" omit-xml-declaration="yes" encoding="UTF-8" />

<!-- root node-->
<xsl:template match="/">

  <div class="promptHead" resource="blog.headlineStatistics"></div>
  
  <div class="statisticList">
    <xsl:if test="//statisticEntry">
      <table>
        <xsl:for-each select="/statistics/period">

          <tr class="statisticDateRange">
            <td colspan="2">
              <xsl:value-of select="dateRange" />
            </td>
          </tr>

          <xsl:for-each select="statisticEntry">
            <tr>
              <td>
                <xsl:value-of select="category" />:
              </td>
              <td>
                <xsl:value-of select="count" />
              </td>
            </tr>
          </xsl:for-each>
        
        </xsl:for-each>
      </table>
    </xsl:if>
  </div>  
  
  <div style="text-align:center;margin:10px 0;">
    <input type="button" resource="button.closewin" onclick="hideStatistics()" />
  </div>

</xsl:template>

</xsl:stylesheet>
