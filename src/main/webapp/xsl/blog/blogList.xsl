<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform">	
<xsl:output method="html" indent="yes" omit-xml-declaration="yes" encoding="UTF-8" />

<xsl:strip-space elements="blog" />

<!-- root node-->
<xsl:template match="/">

  <html class="blog">
    <head>
      <meta http-equiv="X-UA-Compatible" content="IE=Edge" />

      <meta http-equiv="expires" content="0" />

      <!--        
      <meta name="viewport" content="width=800, initial-scale=1.0, user-scalable=yes" />
      -->
      <meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=yes" />

      <title>arcoiris blog</title>

      <link rel="shortcut icon">
        <xsl:attribute name="href"><xsl:value-of select="//contextRoot" />/images/arcoiris-icon.png</xsl:attribute>
      </link>

      <link rel="stylesheet" type="text/css">
        <xsl:attribute name="href"><xsl:value-of select="//contextRoot" />/styles/common.css</xsl:attribute>
      </link>

      <link rel="stylesheet" type="text/css">
        <xsl:attribute name="href"><xsl:value-of select="//contextRoot" />/styles/blog.css</xsl:attribute>
      </link>

      <link rel="stylesheet" type="text/css">
        <xsl:attribute name="href"><xsl:value-of select="//contextRoot" />/styles/icons.css</xsl:attribute>
      </link>

      <link rel="stylesheet" type="text/css">
        <xsl:attribute name="href"><xsl:value-of select="//contextRoot" />/styles/blogskins/<xsl:value-of select="/blog/skin" />.css</xsl:attribute>
      </link>
      
      <style id="calendarStyle"></style>
      
      <script type="text/javascript">
        <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/javascript/browserCheck.js</xsl:attribute>
      </script>
      <script type="text/javascript">
        <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/javascript/util.js</xsl:attribute>
      </script>
      <script type="text/javascript">
        <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/javascript/xmlUtil.js</xsl:attribute>
      </script>
      <script type="text/javascript">
        <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/javascript/ajaxCommon.js</xsl:attribute>
      </script>
      <script type="text/javascript">
        <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/javascript/ajaxUpload.js</xsl:attribute>
      </script>
      <script type="text/javascript">
        <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/javascript/popupPicture.js</xsl:attribute>
      </script>
      <script type="text/javascript">
        <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/javascript/calendar/CalendarPopup.js</xsl:attribute>
      </script>
      <script type="text/javascript">
        <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/javascript/calendar/AnchorPosition.js</xsl:attribute>
      </script>
      <script type="text/javascript">
        <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/javascript/calendar/date.js</xsl:attribute>
      </script>
      <script type="text/javascript">
        <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/javascript/calendar/PopupWindow.js</xsl:attribute>
      </script>
      <script type="text/javascript">
        <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/javascript/geoMap.js</xsl:attribute>
      </script>
      <script type="text/javascript">
        <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/javascript/blog.js</xsl:attribute>
      </script>

      <script type="text/javascript">
        <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/javascript/resourceBundle.js</xsl:attribute>
      </script>
      <script type="text/javascript">
        <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/servlet?command=getResourceBundle&amp;lang=<xsl:value-of select="/blog/language" /></xsl:attribute>
      </script>
      
      <script type="text/javascript">
        var sortOrder = <xsl:value-of select="/blog/sortOrder" />;
      
        function setCalendarStyles() 
        {
            if (browserFirefox) 
            {
                var calendarCssElem = document.getElementById("calendarStyle");
                calendarCssElem.innerHTML = getCalStyles();
            }
        }

        if (!browserFirefox) 
        {
            document.write(getCalendarStyles());
        }
  
        var cal1x = new CalendarPopup("calDiv");
   
        function selectDate()
        {
            cal1x.setReturnFunction("setSelectedDate");
            cal1x.select(document.getElementById("blogDate"), "anchorDate", "MM/dd/yyyy");
            centerBox(document.getElementById("calDiv"));
        }

        function setSelectedDate(y, m, d) 
        { 
            var selectedDate = new Date();
            selectedDate.setYear(y);
            selectedDate.setMonth(m - 1);
            selectedDate.setDate(d);
            
            var dayParamName;
            if (sortOrder == 1) {
                selectedDate.setMilliseconds(selectedDate.getMilliseconds() + (24 * 60 * 60 * 1000));
                dayParamName = "beforeDay";
            } else {
                selectedDate.setMilliseconds(selectedDate.getMilliseconds() - (24 * 60 * 60 * 1000));
                dayParamName = "afterDay";
            }

            var dayParamVal = selectedDate.getFullYear() + "-" + LZ(selectedDate.getMonth() + 1) + "-" + LZ(selectedDate.getDate());

            window.location.href = "<xsl:value-of select="//contextRoot" />/servlet?command=blog&amp;" + dayParamName + "=" + dayParamVal;
        }

        function scrollToCurrentEntry() {
            <xsl:if test="/blog/posInPage">
              document.getElementById('entry-<xsl:value-of select="/blog/posInPage" />').scrollIntoView();
            </xsl:if>
        }
     
      </script>
      
    </head>

    <body class="blog">
      <xsl:if test="not(/blog/readonly)">
        <xsl:attribute name="onload">setCalendarStyles();queryPublicLink();firefoxJumpToIdWorkaround();scrollToCurrentEntry();queryGeoData();attachScrollHandler();</xsl:attribute>
      </xsl:if>
      <xsl:if test="/blog/readonly">
        <xsl:attribute name="onload">setCalendarStyles();firefoxJumpToIdWorkaround();scrollToCurrentEntry();queryGeoData();attachScrollHandler();</xsl:attribute>
      </xsl:if>
      
      <div class="blogCont">
      
        <xsl:if test="/blog/blogTitlePic">
          <div>
            <xsl:attribute name="style">background-image:url('<xsl:value-of select="/blog/blogTitlePic" />');background-size:100%;</xsl:attribute>
            <xsl:if test="/blog/readonly">
              <xsl:attribute name="class">blogTitlePic</xsl:attribute>
            </xsl:if>
            <xsl:if test="not(/blog/readonly)">
              <xsl:attribute name="class">blogTitlePic blogTitlePointer</xsl:attribute>
              <xsl:attribute name="onclick">unsetTitlePic()</xsl:attribute>
              <xsl:attribute name="titleResource">blog.unsetTitlePic</xsl:attribute>
            </xsl:if>
          </div>
        </xsl:if>
      
        <div class="blogHeadline">
          <!-- 
          <span resource="blog.listHeadline"></span>: 
          -->
          <xsl:value-of select="/blog/blogTitle" />
        </div> 
      
        <xsl:if test="not(/blog/readonly)">
          <a href="#" class="icon-font icon-menu blogMenu" titleResource="blog.settingsHeadline">
            <xsl:attribute name="onClick">showSettings()</xsl:attribute>
          </a>
          <a href="javascript:showSearchForm()" class="icon-font icon-search blogMenu" titleResource="blog.search" />
        </xsl:if>

        <div class="blogCalenderCont">
          <a href="#" name="anchorDate" id="anchorDate" class="icon-font icon-calender blogCalender" titleResource="blog.calendarTitle">
            <xsl:attribute name="onClick">selectDate()</xsl:attribute>
          </a>
          <input type="text" id="blogDate" style="display:none" />
        </div>
        
        <div class="blogDateRange">
          <xsl:if test="/blog/dateRangeFrom">
            <span><xsl:value-of select="/blog/dateRangeFrom" /></span>
          </xsl:if>
          <xsl:if test="/blog/dateRangeFrom or /blog/dateRangeUntil">
            ...
          </xsl:if>
          <xsl:if test="/blog/dateRangeUntil">
            <span><xsl:value-of select="/blog/dateRangeUntil" /></span>
          </xsl:if>
        </div>
      
        <div class="rightAlignedButton blogButtonCont">
          <xsl:if test="not(/blog/readonly)">
          
            <xsl:if test="/blog/blogEntries/blogDate">
              <input id="unpublishButton" type="button" resource="blog.buttonUnpublish" onclick="javascript:unpublish()" style="display:none" />

              <input id="publicURLButton" type="button" resource="blog.buttonPublicLink" onclick="showPublicURL()" style="display:none" />
              <input id="publishBlogButton" type="button" resource="blog.buttonPublish" onclick="publishBlog()" style="display:none"/>
            </xsl:if>

            <input type="button" resource="blog.buttonCreate">
              <xsl:attribute name="onclick">window.location.href='<xsl:value-of select="//contextRoot" />/servlet?command=blog&amp;cmd=post'</xsl:attribute>
            </input> 

            <input type="button" resource="blog.showSubscribers" onclick="showSubscribers()" />

            <input type="button" id="mapAllLink" resource="blog.buttonMapAll" onclick="googleMapAll()" style="display:none" />

            <xsl:if test="/blog/blogEntries/blogDate/dayEntries/file/staged">
              <input type="button" resource="blog.publishNewEntries" onclick="publishNewEntries()" />
            </xsl:if>

            <input type="button" resource="blog.buttonlogout">
              <xsl:attribute name="onclick">window.location.href='<xsl:value-of select="//contextRoot" />/servlet?command=logout'</xsl:attribute>
            </input>
          </xsl:if>
          <xsl:if test="/blog/readonly">
            <a href="javascript:googleMapAll()" id="mapAllLink" class="icon-font icon-globe blogMapAll" style="display:none;" titleResource="blog.mapAll" />
            <a href="javascript:showSubscribeForm()" class="icon-font icon-watch blogSubscribe" titleResource="blog.subscribe" />
            <a href="javascript:showSearchForm()" class="icon-font icon-search blogSearch" titleResource="blog.search" />
          </xsl:if>
        </div>   
    
        <xsl:if test="/blog/blogEntries/blogDate">
    
        <xsl:if test="/blog/paging/prevPageBefore or /blog/paging/nextPageAfter">
          <xsl:call-template name="paging" />
        </xsl:if>
        
        <xsl:if test="not(/blog/paging/prevPageBefore) and not(/blog/paging/nextPageAfter)">
          <div style="height:12px;clear:both;"></div>
        </xsl:if>
    
        <xsl:for-each select="/blog/blogEntries/blogDate">
        
          <xsl:variable name="level1Position" select="position()"/>
        
          <div class="blogDate">
            <xsl:value-of select="formattedDate" />
          </div>
        
          <xsl:for-each select="dayEntries/file">
          
            <div>
              <xsl:attribute name="id">entry-<xsl:value-of select="pagePicCounter" /></xsl:attribute>
              <xsl:if test="align='left'">
                <xsl:attribute name="class">blogEntry storyPictureLeft</xsl:attribute>
              </xsl:if>
              <xsl:if test="align='right'">
                <xsl:attribute name="class">blogEntry storyPictureRight</xsl:attribute>
              </xsl:if>

              <a>
                <xsl:attribute name="href">javascript:showPicturePopup('<xsl:value-of select="imgPathForScript" />', <xsl:value-of select="xpix" />, <xsl:value-of select="ypix" />)</xsl:attribute>

                <img border="0" titleResource="blog.showFullSize">
                  <xsl:attribute name="id">pic-<xsl:value-of select="pagePicCounter" /></xsl:attribute>
                  <xsl:attribute name="src"><xsl:value-of select="imgPath" /></xsl:attribute>
                  <xsl:attribute name="width"><xsl:value-of select="thumbnailWidth" /></xsl:attribute>
                  <xsl:attribute name="height"><xsl:value-of select="thumbnailHeight" /></xsl:attribute>
                  <xsl:if test="origImgPath">
                    <xsl:attribute name="origImgPath"><xsl:value-of select="origImgPath" /></xsl:attribute>
                  </xsl:if>
                  <xsl:if test="align='right'">
                    <xsl:attribute name="class">storyPicture alignRight</xsl:attribute>  
                  </xsl:if>
                  <xsl:if test="align='left'">
                    <xsl:attribute name="class">storyPicture alignLeft</xsl:attribute>  
                  </xsl:if>
                </img>
              </a>
            
              <span class="descrText">
                <xsl:if test="description">
                  <xsl:for-each select="description/*">
                    <xsl:if test="local-name(.) = 'emoji'">
                      <xsl:text> </xsl:text>
                      <img class="blogEmoticon">
                        <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/emoticons/<xsl:value-of select="." />.png</xsl:attribute>
                      </img>
                      <xsl:text> </xsl:text>
                    </xsl:if>
                    <xsl:if test="local-name(.) = 'link'">
                      <xsl:text> </xsl:text>
                      <a target="_blank">
                        <xsl:attribute name="href"><xsl:value-of select="./url" /></xsl:attribute>
                        <xsl:attribute name="title"><xsl:value-of select="./url" /></xsl:attribute>
                        <xsl:value-of select="./label" />
                      </a>
                      <xsl:text> </xsl:text>
                    </xsl:if>
                    <xsl:if test="local-name(.) = 'fragment'">
                      <xsl:value-of select="." />
                    </xsl:if>
                  </xsl:for-each>
                </xsl:if>
              </span>

              <xsl:if test="description">
                <br/>
              </xsl:if>

              <xsl:if test="staged">
                <span class="unpublished" resource="blog.entryUnpublished"></span>
              </xsl:if>
              <xsl:if test="not(staged)">
                <a class="comments" titleResource="label.comments">
                  <xsl:attribute name="href">javascript:blogComments('<xsl:value-of select="@name" />', '<xsl:value-of select="pagePicCounter" />')</xsl:attribute>
                  <xsl:text>(</xsl:text>
                  <span>
                    <xsl:attribute name="id">comment-<xsl:value-of select="pagePicCounter" /></xsl:attribute>
                    <xsl:value-of select="comments" />
                  </span>
                  <xsl:text> </xsl:text><span resource="label.comments"></span>
                  <xsl:if test="newComments">
                    <span>
                      <xsl:attribute name="id">newComment-<xsl:value-of select="pagePicCounter" /></xsl:attribute>
                      <xsl:text>, </xsl:text>
                      <span class="newComment" resource="comments.unread"></span>
                    </span>
                  </xsl:if>
                  <xsl:text>)</xsl:text>
                </a>
              </xsl:if>
          
              <br/>

              <xsl:if test="not(/blog/readonly)">
                <a class="icon-font icon-edit icon-blog-edit" titleResource="label.edit">
                  <xsl:attribute name="href">javascript:editBlogEntry('<xsl:value-of select="@name" />', '<xsl:value-of select="pagePicCounter" />')</xsl:attribute>
                </a>
          
                &#160;
          
                <a class="icon-font icon-delete icon-blog-delete" titleResource="label.delete">
                  <xsl:attribute name="href">javascript:deleteBlogEntry('<xsl:value-of select="@name" />')</xsl:attribute>
                </a>

                &#160;
             
                <a href="javascript:void(0)" id="rotateLeftIcon" class="icon-font icon-rotate-left icon-blog-rotate" titleResource="blog.rotateLeft">
                  <xsl:attribute name="onClick">rotateBlogPic('<xsl:value-of select="@name" />', 'left')</xsl:attribute>
                </a>
                &#160;
                <a href="javascript:void(0)" id="rotateRightIcon" class="icon-font icon-rotate-right icon-blog-rotate" titleResource="blog.rotateRight">
                  <xsl:attribute name="onClick">rotateBlogPic('<xsl:value-of select="@name" />', 'right')</xsl:attribute>
                </a>

                <xsl:if test="position() != 1">
                  &#160;
                  <a class="icon-font icon-arrow-up icon-blog-move" titleResource="blog.moveUp">
                    <xsl:attribute name="href">javascript:moveBlogEntryUp('<xsl:value-of select="@name" />', '<xsl:value-of select="pagePicCounter" />')</xsl:attribute>
                  </a>
                </xsl:if>

                <xsl:if test="position() != last()">
                  &#160;
                  <a class="icon-font icon-arrow-down icon-blog-move" titleResource="blog.moveDown">
                    <xsl:attribute name="href">javascript:moveBlogEntryDown('<xsl:value-of select="@name" />', '<xsl:value-of select="pagePicCounter" />')</xsl:attribute>
                  </a>
                </xsl:if>

                <xsl:if test="not(staged)">
                  &#160;
                  <a href="#" id="titlePicIcon" class="icon-font icon-heart icon-blog-titlePic" titleResource="blog.makeTitlePic">
                    <xsl:attribute name="onClick">setTitlePic('<xsl:value-of select="@name" />')</xsl:attribute>
                  </a>
                </xsl:if>
                
                &#160;
                <a href="javascript:void(0)">
                  <xsl:attribute name="id">attachment-<xsl:value-of select="pagePicCounter" /></xsl:attribute>
                  <xsl:if test="attachment or geoTrack">
                    <xsl:attribute name="class">icon-font icon-attachment icon-blog-attachment icon-remove</xsl:attribute>
                    <xsl:attribute name="titleResource">blog.detach</xsl:attribute>
                    <xsl:attribute name="onClick">detachFile('<xsl:value-of select="@name" />', '<xsl:value-of select="pagePicCounter" />')</xsl:attribute>
                  </xsl:if>
                  <xsl:if test="not(attachment or geoTrack)">
                    <xsl:attribute name="class">icon-font icon-attachment icon-blog-attachment</xsl:attribute>
                    <xsl:attribute name="titleResource">blog.attach</xsl:attribute>
                    <xsl:attribute name="onClick">attachFile('<xsl:value-of select="@name" />', '<xsl:value-of select="pagePicCounter" />')</xsl:attribute>
                  </xsl:if>
                </a>
                
              </xsl:if>
              
              <xsl:if test="(not(/blog/readonly) and not(staged)) or (/blog/readonly and not(ratingAllowed))">

                <xsl:if test="voteCount != 0">
                  &#160;

                  <a id="likeIcon" class="icon-font icon-like icon-blog-like" titleResource="blog.likeTitle"></a>
              
                  <xsl:text> </xsl:text>
              
                  <span titleResource="blog.likeTitle" class="likeCount">
                    <xsl:attribute name="id">voteCount-<xsl:value-of select="pagePicCounter" /></xsl:attribute>
                    <xsl:value-of select="voteCount" />
                  </span>
                </xsl:if>

              </xsl:if>
              
              <xsl:if test="/blog/readonly and ratingAllowed">
                &#160;

                <a href="javascript:void(0)" class="icon-font icon-like icon-blog-like" titleResource="blog.like">
                  <xsl:attribute name="id">likeLink-<xsl:value-of select="pagePicCounter" /></xsl:attribute>
                  <xsl:attribute name="onClick">like('<xsl:value-of select="@name" />', '<xsl:value-of select="pagePicCounter" />')</xsl:attribute>
                </a>
              
                <span titleResource="blog.likeTitle" class="likeCount">
                  <xsl:attribute name="id">voteCount-<xsl:value-of select="pagePicCounter" /></xsl:attribute>
                  (<xsl:value-of select="voteCount" />)
                </span>
              </xsl:if>
              
              
              <xsl:if test="geoTag or geoTrack or attachment">
 
                <div>
                  <xsl:if test="geoTag">
                    <div>
                      <xsl:attribute name="id">mapIcon-<xsl:value-of select="$level1Position" />-<xsl:value-of select="position()" /></xsl:attribute>
                      <a class="blogGeoTagLink">
                        <xsl:attribute name="href">javascript:showMapSelection('<xsl:value-of select="$level1Position" />-<xsl:value-of select="position()" />')</xsl:attribute>
                        <span resource="geoMapLinkShort"></span>
                      </a>
                    </div>
                  
                    <select class="pictureAlbum">
                      <xsl:attribute name="id">geoLocSel-<xsl:value-of select="$level1Position" />-<xsl:value-of select="position()" /></xsl:attribute>
                      <xsl:attribute name="onchange">geoMapFileSelected('<xsl:value-of select="@name" />', '<xsl:value-of select="$level1Position" />-<xsl:value-of select="position()" />')</xsl:attribute>
                      <option value="0" resource="selectMapType" />
                      <option value="1" resource="mapTypeOSM" />
                      <option value="2" resource="mapTypeGoogleMap" />
                      <option value="3" resource="mapTypeGoogleEarth" />
                    </select>
                  </xsl:if>

                  <xsl:if test="geoTrack">
                    <a class="blogGeoTagLink">
                      <xsl:attribute name="id">geoTrackLink-<xsl:value-of select="pagePicCounter" /></xsl:attribute>
                      <xsl:attribute name="href">javascript:void(0)</xsl:attribute>
                      <xsl:attribute name="onclick">viewGeoTrack('<xsl:value-of select="@name" />', '<xsl:value-of select="geoTrack" />')</xsl:attribute>
                      <span resource="geoTrackLink"></span>
                    </a>
                  </xsl:if>

                  <xsl:if test="attachment">
                    <a href="javascript:void(0)" class="icon-font icon-file icon-blog-file" titleResource="blog.viewAttach">
                      <xsl:attribute name="id">viewAttachmentIcon-<xsl:value-of select="pagePicCounter" /></xsl:attribute>
                      <xsl:attribute name="onClick">viewAttachment('<xsl:value-of select="@name" />', '<xsl:value-of select="attachment" />')</xsl:attribute>
                    </a>
                  </xsl:if>

                </div>  
                  
              </xsl:if>
              
            </div>      
        
          </xsl:for-each>
        
        </xsl:for-each>

        <xsl:if test="/blog/paging/prevPageBefore or /blog/paging/nextPageAfter">
          <xsl:call-template name="paging" />
        </xsl:if>
        
        </xsl:if>
        
        <xsl:if test="not(/blog/blogEntries/blogDate)">
          <xsl:if test="/blog/empty">
            <div class="blogEmpty" resource="blog.empty"></div>
          </xsl:if>
          <xsl:if test="not(/blog/empty)">
            <div class="blogEmpty" resource="blog.dateRangeEmpty"></div>
          </xsl:if>
        </xsl:if>
    
      </div>
      
      <div class="poweredBy">
        powered by arcoiris blog
        <a href="http://www.webfilesys.de/arcoiris" target="_blank"> (www.webfilesys.de/arcoiris)</a>
      </div>
    
      <script type="text/javascript">
        var thumbnails = new Array();
        
        <xsl:for-each select="/blog/blogEntries/blogDate">
          <xsl:for-each select="dayEntries/file">
            <xsl:if test="thumbnail">
              thumbnails.push("pic-<xsl:value-of select="pagePicCounter" />");
            </xsl:if>
          </xsl:for-each>
        </xsl:for-each>          
      </script>    
    
    </body>
    
    <div id="calDiv"></div>
    
    <div id="picturePopup" style="position:absolute;top:50px;left:150px;width:400px;height:400px;background-color:#c0c0c0;padding:0px;visibility:hidden;border-style:ridge;border-color:white;border-width:6px;z-index:2;">
      <img id="zoomPic" src="" border="0" style="width:100%;height:100%;" onclick="hidePopupPicture()"/>
      <div id="popupClose" style="position:absolute;top:5px;left:5px;width:16px;height:14px;padding:0px;visibility:hidden;border-style:none;z-index:3">
        <img border="0" width="16" height="14" onclick="hidePopupPicture()">
          <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/images/winClose.gif</xsl:attribute>
        </img>
      </div>
    </div>
    
    <div id="publishCont" class="blogPublishCont"></div>

    <div id="commentCont" class="blogCommentCont"></div>
    
    <xsl:if test="not(/blog/readonly)">
      <div id="settingsCont" class="blogSettingsCont"></div>
    </xsl:if>    

    <xsl:if test="not(/blog/readonly)">
      <div id="subscribeCont" class="blogSubscribeCont">
      </div>
    </xsl:if>

    <xsl:if test="/blog/readonly">
      <div id="subscribeCont" class="blogSubscribeCont">
        <form id="subscribeForm" method="post" class="blogSubscribeForm">
          <xsl:attribute name="action"><xsl:value-of select="//contextRoot" />/servlet</xsl:attribute>
          
          <input type="hidden" name="command" value="blog" />
          <input type="hidden" name="cmd" value="subscribe" />
          <ul class="subscribeForm">
            <li>
              <label resource="blog.subscribePrompt"></label>
            </li>
            <li>
              <input type="text" id="subscriberEmail" name="subscriberEmail" onkeypress="return subscribeKeyPress(event);" />
            </li>
            <li>
              <input type="button" resource="blog.subscribeButton" onclick="submitSubscription()" />
              <input type="button" resource="button.cancel" onclick="hideSubscribeForm() "/>
            </li>
            <li>
              <span class="blogSmall" resource="blog.unsubscribeHint"></span>
            </li>
          </ul>
        </form>
      </div>
    </xsl:if>    
    
    <div id="searchFormCont" class="blogSearchFormCont">
      <form id="searchForm" method="post" class="blogSubscribeForm">
          <xsl:attribute name="action"><xsl:value-of select="//contextRoot" />/servlet</xsl:attribute>

          <input type="hidden" name="command" value="blog" />
          <input type="hidden" name="cmd" value="search" />
          <ul class="subscribeForm">
            <li>
              <label resource="blog.labelSearchArg"></label>
            </li>
            <li>
              <input type="text" id="searchArg" name="searchArg" onkeypress="return searchKeyPress(event);" />
            </li>
            <li>
              <input type="checkbox" id="searchComments" name="searchComments" />
              <span resource="blog.searchIncludeComments"></span>
            </li>
            <li>
              <input type="button" resource="blog.searchButton" onclick="submitSearch()" />
              <input type="button" resource="button.cancel" onclick="hideSearchForm()" style="float: right" />
            </li>
          </ul>
        </form>
    </div>
    
    <xsl:if test="not(/blog/readonly)">
    
      <div id="uploadStatus" class="uploadStatus" style="visibility:hidden">
        <table border="0" width="100%" cellpadding="2" cellspacing="0">
          <tr>
            <th class="headline" style="border-width:0;border-bottom-width:1px;" resource="label.uploadStatus"></th>
          </tr>
        </table>
	
	    <div id="currentFile" class="uploadStatusCurrentFile"></div>
  
        <center>

          <div class="uploadStatusBar">
            <img id="done" width="1" height="20" border="0">
              <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/images/bluedot.gif</xsl:attribute>
            </img>
            <img id="todo" width="299" height="20" border="0">  
              <xsl:attribute name="src"><xsl:value-of select="//contextRoot" />/images/space.gif</xsl:attribute>
            </img>
          </div>

          <table border="0" cellspacing="0" cellpadding="0" style="width:300px">
            <tr>
              <td class="fileListData">
                <div id="statusText" class="uploadStatusText">
                  0 
                  <span resource="label.of"></span>
                  0 bytes (0 %)
                </div>
              </td>
            </tr>
          </table>
	  
        </center>
  
      </div>
    
    </xsl:if>
    
    <script type="text/javascript">
      setBundleResources();
    </script>
    
  </html>

</xsl:template>

<xsl:include href="paging.xsl" />

</xsl:stylesheet>
