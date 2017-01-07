<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">	
<xsl:output method="html" indent="yes" omit-xml-declaration="yes" encoding="UTF-8" 
    doctype-public="html" />

<xsl:strip-space elements="login" />

<!-- root node-->
<xsl:template match="/">

<html>
<head>

<meta http-equiv="X-UA-Compatible" content="IE=Edge" />

<meta http-equiv="expires" content="0" />

<meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=yes" />

<link rel="stylesheet" type="text/css">
  <xsl:attribute name="href"><xsl:value-of select="//contextRoot" />/styles/common.css</xsl:attribute>
</link>

<link rel="stylesheet" type="text/css">
  <xsl:attribute name="href"><xsl:value-of select="//contextRoot" />/styles/blogskins/<xsl:value-of select="/login/css" />.css</xsl:attribute>
</link>

<link rel="shortcut icon">
  <xsl:attribute name="href"><xsl:value-of select="//contextRoot" />/images/arcoiris-icon.png</xsl:attribute>
</link>

<title>
  arcoiris blog: 
  <xsl:value-of select="/login/localHost"/>
  -
  <xsl:value-of select="/login/version"/>
</title>

<script type="text/javascript">
  <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/javascript/browserCheck.js</xsl:attribute>
</script>

<script type="text/javascript">
  <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/javascript/resourceBundle.js</xsl:attribute>
</script>
<script type="text/javascript">
  <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/servlet?command=getResourceBundle&amp;lang=<xsl:value-of select="/login/language" /></xsl:attribute>
</script>

<script language="javascript">

  function about() {
      infowindow = window.open('<xsl:value-of select="//contextRoot" />/servlet?command=versionInfo','infowindow','status=no,toolbar=no,location=no,menu=no,width=300,height=230,resizable=no,left=250,top=150,screenX=250,screenY=150');
      infowindow.focus();
  }

  function setFocus() {
      document.passwordform.userid.focus();
  }

  <xsl:if test="/login/authFailed">
    alert(resourceBundle["alert.invalidlogin"]);
  </xsl:if>

  <xsl:if test="/login/activationSuccess">
    alert(resourceBundle["activationSuccessful"]);
  </xsl:if>

  document.cookie = 'CookieTest=1;';
  var idx = document.cookie.indexOf("CookieTest=") + 11;
  var cookieValue = '';
  if (idx >= 0) {
      cookieValue = document.cookie.substring(idx, idx + 1);
  }
    
  if (cookieValue != '1') {
      alert('Cookies must be enabled to login to this web site!');
  }

</script>

</head>

<body onload="setFocus()">

<div class="centerBox">
  <div class="loginBox">
    <table border="0" cellpadding="5" cellspacing="0" width="100%">
      <tr>
        <td class="loginTitle" style="padding-left:10px">

          <div class="logoImage">
            <img>
              <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/images/arcoiris-book.png</xsl:attribute>
            </img>
	      </div>
          
          <div class="loginTitle">
            <span resource="label.login.title"></span>
          </div>
        </td>
        
        <td>
          <form accept-charset="utf-8" name="passwordform" method="post">
            <xsl:attribute name="action"><xsl:value-of select="//contextRoot" />/servlet</xsl:attribute>
            
            <input type="hidden" name="command" value="login" />
          
            <table border="0" cellpadding="5" cellspacing="0" width="100%">
              <tr>
                <td colspan="2">
                  &#160;
                </td>
              </tr>
              <tr>
                <td class="loginFormLabel">
                  <label for="userid" resource="label.userid" />:
                </td>
                <td class="value">
                  <input type="text" id="userid" name="userid" maxlength="64" style="width:100px;" required="required"/>
                </td>
              </tr>
              <tr>
                <td class="loginFormLabel">
                  <label for="password" resource="label.password" />:
                </td>
                <td class="value">
                  <input type="password" id="password" name="password" maxlength="64" style="width:100px;" required="required"/>
                </td>
              </tr>
              <tr>
                <td>
                  &#160;
                </td>
                <td>
                  <input type="submit" name="logonbutton" resource="label.logon" class="loginButton" />
                </td>
              </tr>
              <tr>
                <td colspan="2" style="text-align:right;padding-right:10px;">
                  <xsl:if test="/login/openRegistration">
                    <a class="registrationLink" resource="label.registerself">
                      <xsl:attribute name="href"><xsl:value-of select="contextRoot" />servlet?command=registerSelf</xsl:attribute>
                    </a>
                  </xsl:if>
                </td>
              </tr>
              <tr>
                <td colspan="2" style="text-align:right;padding-right:10px;">
                  <a class="aboutLink" href="javascript:about()" resource="label.about"></a>
                </td>
              </tr>
            </table>
          </form>
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
