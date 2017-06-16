<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">	
  <xsl:output method="html" indent="yes" omit-xml-declaration="yes" encoding="UTF-8"/>

  <xsl:template name="paging">
  
          <div class="blogPagingCont">
            <xsl:if test="/blog/paging/prevPageBefore">
              <xsl:if test="/blog/sortOrder = '1'">
                <a class="icon-font icon-paging icon-page-first" titleResource="blog.pagingNewest">
                  <xsl:attribute name="href"><xsl:value-of select="//contextRoot" />/servlet?command=blog&amp;beforeDay=2030-01-01</xsl:attribute>
                </a>
                <a class="icon-font icon-paging icon-page-prev" titleResource="blog.pagingNewer">
                  <xsl:attribute name="href"><xsl:value-of select="//contextRoot" />/servlet?command=blog&amp;afterDay=<xsl:value-of select="/blog/paging/prevPageBefore" /></xsl:attribute>
                </a>
              </xsl:if>
              <xsl:if test="/blog/sortOrder = '2'">
                <a class="icon-font icon-paging icon-page-first" titleResource="blog.pagingOldest">
                  <xsl:attribute name="href"><xsl:value-of select="//contextRoot" />/servlet?command=blog&amp;beforeDay=1990-01-01</xsl:attribute>
                </a>
                <a class="icon-font icon-paging icon-page-prev" titleResource="blog.pagingOlder">
                  <xsl:attribute name="href"><xsl:value-of select="//contextRoot" />/servlet?command=blog&amp;beforeDay=<xsl:value-of select="/blog/paging/prevPageBefore" /></xsl:attribute>
                </a>
              </xsl:if>
            </xsl:if>
            <xsl:if test="/blog/paging/nextPageAfter">
              <xsl:if test="/blog/sortOrder = '1'">
                <a class="icon-font icon-paging icon-page-last" titleResource="blog.pagingOldest">
                  <xsl:attribute name="href"><xsl:value-of select="//contextRoot" />/servlet?command=blog&amp;beforeDay=1990-01-01</xsl:attribute>
                </a>
                <a class="icon-font icon-paging icon-page-next" titleResource="blog.pagingOlder">
                  <xsl:attribute name="href"><xsl:value-of select="//contextRoot" />/servlet?command=blog&amp;beforeDay=<xsl:value-of select="/blog/paging/nextPageAfter" /></xsl:attribute>
                </a>
              </xsl:if>
              <xsl:if test="/blog/sortOrder = '2'">
                <a class="icon-font icon-paging icon-page-last" titleResource="blog.pagingNewest">
                  <xsl:attribute name="href"><xsl:value-of select="//contextRoot" />/servlet?command=blog&amp;beforeDay=2030-01-01</xsl:attribute>
                </a>
                <a class="icon-font icon-paging icon-page-next" titleResource="blog.pagingNewer">
                  <xsl:attribute name="href"><xsl:value-of select="//contextRoot" />/servlet?command=blog&amp;afterDay=<xsl:value-of select="/blog/paging/nextPageAfter" /></xsl:attribute>
                </a>
              </xsl:if>
            </xsl:if>
          </div>
  
  </xsl:template>

</xsl:stylesheet>
