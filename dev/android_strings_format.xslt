<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output 
    method="xml" 
    version="1.0" 
    encoding="UTF-8" 
    indent="yes" 
    omit-xml-declaration="yes"/>

<xsl:template match="/resources">
<resources>

<xsl:for-each select="string">
<xsl:sort select="@name"/>
<string name="{@name}">
    <xsl:copy-of select="@* | node()"/>
</string>
</xsl:for-each>

</resources>
</xsl:template>

</xsl:stylesheet>
