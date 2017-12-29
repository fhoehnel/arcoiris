<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">	
<xsl:output method="html" indent="yes" omit-xml-declaration="yes" encoding="UTF-8" />

<xsl:template match="/blog">

  <div class="promptHead" resource="blog.targetPosition"></div>
  
  <form id="targetPosForm">
  
    <input type="hidden" name="command" value="blog" />
    <input type="hidden" name="cmd" value="moveToPos" />
  
    <input type="hidden" name="fileName">
      <xsl:attribute name="value"><xsl:value-of select="fileName" /></xsl:attribute>
    </input>
    
    <input type="hidden" id="newPos" name="newPos" value="" />
  
    <div class="targetPosCont">
      <div>
        <input type="button" resource="blog.targetPosTop" onclick="selectTargetPosition('top')">
          <xsl:if test="isTop">
            <xsl:attribute name="disabled">disabled</xsl:attribute>
          </xsl:if>
        </input>
      </div>
      <div style="padding:20px 0">
        <span resource="blog.newPosInDay" />:
        <xsl:text> </xsl:text>
        <select name="targetPos" id="targetPos" onchange="selectTargetPosition()" style="width:60px">
          <xsl:for-each select="positions/pos">
            <option>
              <xsl:attribute name="value"><xsl:value-of select="." /></xsl:attribute>
              <xsl:if test="@disabled">
                <xsl:attribute name="disabled">disabled</xsl:attribute>
                <xsl:attribute name="selected">selected</xsl:attribute>
              </xsl:if>
              <xsl:value-of select="." />
            </option>
          </xsl:for-each>
        </select>       
      </div>
      <div>
        <input type="button" resource="blog.targetPosBottom" onclick="selectTargetPosition('bottom')">
          <xsl:if test="isBottom">
            <xsl:attribute name="disabled">disabled</xsl:attribute>
          </xsl:if>
        </input>
      </div>
    </div>  
  
    <div class="buttonCont">
      <input type="button" resource="button.close" onclick="hidePositionSelection()" />
    </div>
  
  </form>

</xsl:template>

</xsl:stylesheet>
