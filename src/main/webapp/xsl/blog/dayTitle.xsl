<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">	
<xsl:output method="html" indent="yes" omit-xml-declaration="yes" encoding="UTF-8" 
    doctype-public="html" />

<xsl:strip-space elements="dayTitle" />

<!-- root node-->
<xsl:template match="/">

<div class="dayTitleFormPopup">

  <div class="dayTitleContHead">
    <span resource="label.dayTitle"></span>
  </div>

  <form accept-charset="utf-8" id="dayTitleForm" name="dayTitleForm" method="post">
    <xsl:attribute name="action"><xsl:value-of select="//contextRoot" />/servlet</xsl:attribute>
  
    <input type="hidden" name="command" value="blog" />
    <input type="hidden" name="cmd" value="changeDayTitle" />

    <input type="hidden" name="day">
      <xsl:attribute name="value"><xsl:value-of select="/dayTitle/day" /></xsl:attribute>
    </input>

    <div class="dayTitleFormCont">

      <table class="dayTitleForm" width="100%">
   
        <tr>
          <td class="formParm1">
            <span resource="label.dayTitleDate"></span>:
          </td>
          <td class="formParm2">
            <xsl:value-of select="/dayTitle/displayDate" />
          </td>
        </tr>
        <tr>
          <td class="formParm1" colspan="2">
            <span resource="label.dayTitle"></span>:
          </td>
        </tr>
        <tr>
          <td class="formParm2" colspan="2">
            <textarea id="dayTitleText" name="titleText" cols="100" rows="3" wrap="virtual" class="commentText" 
                      onKeyup="limitDayTitleText()" onChange="limitDayTitleText()"><xsl:value-of select="/dayTitle/titleText"/></textarea>
          </td>
        </tr>
	  
        <tr>
          <td colspan="2">
            <div class="buttonCont">
        
              <input type="button" resource="button.addComment">
                <xsl:attribute name="onclick">submitDayTitle()</xsl:attribute>
              </input>              

              <input type="button" resource="button.closewin">
                <xsl:attribute name="onclick">closeDayTitle()</xsl:attribute>
              </input>
		    </div>
          </td>
        </tr>
      </table>
    
    </div>
    
  </form>

</div>

</xsl:template>

</xsl:stylesheet>
