<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">	
<xsl:output method="html" indent="yes" omit-xml-declaration="yes" encoding="UTF-8" 
    doctype-public="html" />

<xsl:strip-space elements="fileComments" />

<!-- root node-->
<xsl:template match="/">

<div class="blogComments">

  <div class="commentContHead">
    <span resource="label.commentList"></span>
  </div>

  <form accept-charset="utf-8" id="blogCommentForm" name="blogCommentForm" method="post">
    <xsl:attribute name="action"><xsl:value-of select="//contextRoot" />/servlet</xsl:attribute>
  
    <input type="hidden" name="command" value="blog" />
    <input type="hidden" name="cmd" value="addComment" />

    <input type="hidden" name="fileName">
      <xsl:attribute name="value"><xsl:value-of select="/fileComments/fileName" /></xsl:attribute>
    </input>

    <input id="posInPage" type="hidden" name="posInPage" value="" />

    <div class="blogCommentList">

      <table class="blogCommentForm" width="100%">
   
        <xsl:if test="not(/fileComments/comments) or not(/fileComments/comments/comment)">
          <tr>
            <td class="formParm1" resource="label.nocomments"></td>
          </tr>
        </xsl:if>

        <xsl:if test="/fileComments/comments and /fileComments/comments/comment">
            
          <xsl:for-each select="/fileComments/comments/comment">
            <tr>
              <td class="formParm1">
                <xsl:value-of select="user" />,&#160;
                <xsl:value-of select="date" />
              </td>
            </tr>
              
            <tr>
              <td class="formParm2" style="padding-left:20px;">
              
                <xsl:for-each select="msg/*">
                  <xsl:if test="local-name(.) = 'emoji'">
                    <xsl:text> </xsl:text>
                    <img class="blogEmoticon">
                      <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/emoticons/<xsl:value-of select="." />.png</xsl:attribute>
                    </img>
                    <xsl:text> </xsl:text>
                  </xsl:if>
                  <xsl:if test="local-name(.) = 'fragment'">
                    <xsl:value-of select="." />
                  </xsl:if>
                </xsl:for-each>
                
              </td>
            </tr>
   
            <tr><td class="formParm1">&#160;</td></tr>
    
          </xsl:for-each>

        </xsl:if>
    
        <xsl:if test="/fileComments/modifyPermission">

          <tr>
            <td>
              <table style="width:100%">
                <tr>
                  <td>
                    <table style="width:100%">
                      <tr>
                        <td class="formParm1">
                          <span resource="label.addcomment"></span>:
                        </td>
                        <td style="text-align:right">
                          <a href="javascript:switchEmojiSelection('newComment')" class="icon-font icon-smiley blogEmojiSel" titleResource="blog.showEmojis"></a>
                        </td>
                      </tr>
                      <tr>
                        <td class="formParm2" colspan="2">
                          <textarea id="newComment" name="newComment" cols="100" rows="4" wrap="virtual" class="commentText" 
                              onKeyup="limitCommentText()" onChange="limitCommentText()"></textarea>
                        </td>
                      </tr>
                    </table>
                  </td>

                  <td class="emojiSelCont">
                    <div id="emojiSelCont" class="emojiSelCont emojSelComment"></div>
                  </td>
                </tr>
              </table>
            </td>
          </tr>

		  <xsl:if test="/fileComments/virtualUser">
            <tr>
              <td class="formParm1">
                <span resource="label.commentAuthor"></span>:
                &#160;
                <input type="text" name="author" style="width:150px" />
              </td>
            </tr>
            <tr>
              <td class="formParm1">
                <label for="notifyOnAnswerEmail" resource="label.commentNotifyEmail"></label>:
                &#160;
                <input type="email" id="notifyOnAnswerEmail" name="notifyOnAnswerEmail" style="width:220px" />
              </td>
            </tr>
          </xsl:if>
          
        </xsl:if>
	  
        <tr>
          <td>
            <div class="buttonCont">
        
              <xsl:if test="/fileComments/modifyPermission">
			
                <input type="button" resource="button.addComment">
                  <xsl:attribute name="onclick">javascript:submitComment()</xsl:attribute>
                </input>              

                <xsl:if test="not(/fileComments/readonly)">
                  <xsl:if test="/fileComments/comments and /fileComments/comments/comment"> 
                    <input type="button" resource="button.delComments">
                      <xsl:attribute name="onclick">javascript:confirmDelComments('<xsl:value-of select="/fileComments/fileName" />')</xsl:attribute>
                    </input>              
                  </xsl:if>  
                </xsl:if>  
              
              </xsl:if>              

              <input type="button" resource="button.closewin">
                <xsl:attribute name="onclick">closeBlogComments()</xsl:attribute>
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
