<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">	
<xsl:output method="html" indent="yes" omit-xml-declaration="yes" encoding="UTF-8" 
    doctype-public="html" />

<xsl:strip-space elements="settings" />

<!-- root node-->
<xsl:template match="/">

<script language="javascript">
</script>

<div class="blogSettings">

  <div class="blogSettingsHead">
    <span resource="blog.settingsHeadline"></span>
  </div>

  <form accept-charset="utf-8" id="blogSettingsForm" name="blogSettingsForm" method="post" class="blogSettingsForm">
    <xsl:attribute name="action"><xsl:value-of select="//contextRoot" />/servlet</xsl:attribute>
  
    <input type="hidden" name="command" value="blog" />
    <input type="hidden" name="cmd" value="saveSettings" />

    <table width="100%">
      <tr>
        <td class="formParm1">
          <label for="blogTitle" resource="blog.titleText" />:
        </td>
        <td class="formParm2">
          <input type="text" id="blogTitle" name="blogTitle" class="settings">
            <xsl:if test="/settings/blogTitleText">
              <xsl:attribute name="value"><xsl:value-of select="/settings/blogTitleText" /></xsl:attribute>
            </xsl:if>
           </input>
        </td>
      </tr>

      <tr>
        <td class="formParm1">
          <label for="daysPerPage" resource="blog.daysPerPage" />:
        </td>
        <td class="formParm2">
          <input type="text" id="daysPerPage" name="daysPerPage" class="settings">
            <xsl:if test="/settings/daysPerPage">
              <xsl:attribute name="value"><xsl:value-of select="/settings/daysPerPage" /></xsl:attribute>
            </xsl:if>
           </input>
        </td>
      </tr>

      <tr>
        <td class="formParm1">
          <label for="skin" resource="blog.skinList" />:
        </td>
        <td class="formParm2">
          <select id="skin" name="skin">
            <xsl:for-each select="/settings/skins/skin">
              <option>
                <xsl:attribute name="value"><xsl:value-of select="." /></xsl:attribute>
                <xsl:if test="/settings/activeSkin = .">
                  <xsl:attribute name="selected">selected</xsl:attribute>
                </xsl:if>
                <xsl:value-of select="." />
              </option>
            </xsl:for-each>
          </select>
        </td>
      </tr>

      <tr>
        <td class="formParm1">
          <label for="newLanguage" resource="label.language" />:
        </td>
        <td class="formParm2">
          <select id="newLanguage" name="newLanguage">
            <xsl:for-each select="/settings/languages/language">
              <option>
                <xsl:attribute name="value"><xsl:value-of select="." /></xsl:attribute>
                <xsl:if test="@selected">
                  <xsl:attribute name="selected">true</xsl:attribute>
                </xsl:if>
                <xsl:value-of select="." />
              </option>
            </xsl:for-each>
          </select>
        </td>
      </tr>

      <tr>
        <td class="formParm1" colspan="2" nowrap="nowrap">
	      <input type="checkbox" id="stagedPublication" name="stagedPublication">
	        <xsl:if test="/settings/stagedPublication">
	          <xsl:attribute name="checked">checked</xsl:attribute>
	        </xsl:if>
	      </input>
	      <label for="stagedPublication" resource="blog.stagedPublication" />
        </td>
      </tr>

      <tr>
        <td class="formParm1" colspan="2" nowrap="nowrap">
	      <input type="checkbox" id="notifyOnNewComment" name="notifyOnNewComment">
	        <xsl:if test="/settings/notifyOnNewComment">
	          <xsl:attribute name="checked">checked</xsl:attribute>
	        </xsl:if>
	      </input>
	      <label for="notifyOnNewComment" resource="blog.notifyOnNewComment" />
        </td>
      </tr>

      <tr>
        <td class="formParm1">
          <label for="password" resource="blog.newPassword" />:
        </td>
        <td class="formParm2">
          <input type="password" id="newPassword" name="newPassword" class="settings">
           </input>
        </td>
      </tr>

      <tr>
        <td class="formParm1">
          <label for="password" resource="blog.newPasswdConfirm" />:
        </td>
        <td class="formParm2">
          <input type="password" id="newPasswdConfirm" name="newPasswdConfirm" class="settings">
           </input>
        </td>
      </tr>

      <tr>
        <td colspan="2">
          <div class="buttonCont">        
            <input type="button" resource="button.save" onclick="validateSettingsForm()" />
            <input type="button" resource="button.cancel" class="rightAlignedButton" onclick="hideSettings()" />
          </div>
        </td>
      </tr>

    </table>
    
  </form>

</div>

</xsl:template>

</xsl:stylesheet>
