<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">	
<xsl:output method="html" indent="yes" omit-xml-declaration="yes" encoding="UTF-8" 
    doctype-public="html" />

<xsl:strip-space elements="userDiskQuota" />

  <xsl:template match="/">

    <div>

      <div class="diskQuotaHead">
        <span resource="label.accountSize"></span>
      </div>

      <table>
        <tr>
          <td class="formParm1">
            <label resource="label.userid"></label>:
          </td>
          <td class="formParm2">
            <span><xsl:value-of select="/userDiskQuota/userid" /></span>
          </td>
        </tr>
        
        <tr>
          <td class="formParm1">
            <label resource="label.diskQuota"></label>:
          </td>
          <td class="formParm2">
            <span><xsl:value-of select="/userDiskQuota/diskQuotaFormatted" /></span> KByte
          </td>
        </tr>

        <tr>
          <td class="formParm1">
            <label resource="label.spaceUsed"></label>:
          </td>
          <td class="formParm2">
            <span><xsl:value-of select="/userDiskQuota/usedSpaceFormatted" /></span> KByte
          </td>
        </tr>

        <tr>
          <td class="formParm1">
            <label resource="label.usagePercent"></label>:
          </td>
          <td class="formParm2">
            <span>
              <xsl:if test="/userDiskQuota/overLimit">
                <xsl:attribute name="class">usageOverLimit</xsl:attribute>
              </xsl:if>
              <xsl:value-of select="/userDiskQuota/usagePercent" />
              %
            </span> 
          </td>
        </tr>

        <tr>
          <td colspan="2">
            <div class="driveUsageBar">
              <xsl:if test="/userDiskQuota/overLimit">
                <xsl:attribute name="style">background-color:red</xsl:attribute>
              </xsl:if>
            
              <xsl:if test="not(/userDiskQuota/overLimit)">
                <img height="20">
                  <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/images/bluedot.gif</xsl:attribute>
                  <xsl:attribute name="width"><xsl:value-of select="/userDiskQuota/progressBarWidth" /></xsl:attribute>
                </img> 
              </xsl:if>

              <xsl:if test="/userDiskQuota/overLimit">
                <img height="20">
                  <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/images/lightBlueDot.gif</xsl:attribute>
                  <xsl:attribute name="width"><xsl:value-of select="/userDiskQuota/progressBarWidth" /></xsl:attribute>
                </img> 
              </xsl:if>
            </div>          
          </td>
        </tr>
      </table>

      <div class="buttonCont" style="text-align:center">        
        <input type="button" resource="button.ok" class="rightAlignedButton" onclick="hideDiskQuota()" />
      </div>

    </div>

  </xsl:template>

</xsl:stylesheet>
