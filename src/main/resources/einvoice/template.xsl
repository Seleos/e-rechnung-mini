<?xml version="1.0" encoding="UTF-8"?>
<!--
  Invoice layout: turns the structured invoice (CII, EN 16931) into an XSL-FO page layout.
  Everything printed on the invoice is read from the XML. Only details the XML does not carry
  (logo, register details, website ...) are passed in as parameters.
  A copy of this file in a company folder (template.xsl) replaces the default layout for that company.
-->
<xsl:stylesheet version="3.0"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                xmlns:rsm="urn:un:unece:uncefact:data:standard:CrossIndustryInvoice:100"
                xmlns:ram="urn:un:unece:uncefact:data:standard:ReusableAggregateBusinessInformationEntity:100"
                xmlns:udt="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:100"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-result-prefixes="rsm ram udt xs">

    <xsl:output method="xml" encoding="UTF-8"/>

    <xsl:param name="logo" select="''"/>
    <xsl:param name="managingDirector" select="''"/>
    <xsl:param name="registerCourt" select="''"/>
    <xsl:param name="registerNumber" select="''"/>
    <xsl:param name="bankName" select="''"/>
    <xsl:param name="phone" select="''"/>
    <xsl:param name="email" select="''"/>
    <xsl:param name="web" select="''"/>
    <xsl:param name="contact" select="''"/>
    <xsl:param name="period" select="''"/>
    <xsl:param name="customerCountry" select="''"/>
    <xsl:param name="footerExtra" select="''"/>

    <!--
      Language of the invoice ("de" or "en"). The labels below are filled from invoice_<language>.properties;
      the German defaults make the stylesheet usable on its own.
    -->
    <xsl:param name="language" select="'de'"/>
    <xsl:param name="label.invoiceTitle" select="'Rechnung Nr.'"/>
    <xsl:param name="label.number" select="'Rechnungsnummer'"/>
    <xsl:param name="label.date" select="'Rechnungsdatum'"/>
    <xsl:param name="label.period" select="'Leistungszeitraum'"/>
    <xsl:param name="label.customerVatId" select="'Ihre USt-IdNr.'"/>
    <xsl:param name="label.contact" select="'Ansprechpartner'"/>
    <xsl:param name="label.phone" select="'Telefon'"/>
    <xsl:param name="label.page" select="'Seite'"/>
    <xsl:param name="label.position" select="'Pos'"/>
    <xsl:param name="label.quantity" select="'Menge'"/>
    <xsl:param name="label.text" select="'Text'"/>
    <xsl:param name="label.unitPrice" select="'Einzelpreis'"/>
    <xsl:param name="label.lineTotal" select="'Gesamtpreis'"/>
    <xsl:param name="label.net" select="'Gesamt netto'"/>
    <xsl:param name="label.vatPrefix" select="'zzgl.'"/>
    <xsl:param name="label.vatSuffix" select="'% MwSt.'"/>
    <xsl:param name="label.gross" select="'Gesamt brutto'"/>
    <xsl:param name="label.total" select="'Gesamtbetrag'"/>
    <xsl:param name="label.managingDirector" select="'Geschäftsführung'"/>
    <xsl:param name="label.vatId" select="'USt-IdNr.'"/>
    <xsl:param name="label.taxNumber" select="'St.-Nr.'"/>
    <xsl:param name="unit.HUR" select="'h'"/>
    <xsl:param name="unit.H87" select="'Stk'"/>
    <xsl:param name="unit.DAY" select="'Tage'"/>
    <xsl:param name="unit.MON" select="'Monate'"/>
    <xsl:param name="unit.LS" select="'pauschal'"/>
    <xsl:param name="unit.KMT" select="'km'"/>
    <xsl:param name="unit.MIN" select="'min'"/>

    <xsl:decimal-format name="de" decimal-separator="," grouping-separator="."/>

    <xsl:variable name="font" select="'Liberation Sans'"/>
    <xsl:variable name="grey" select="'#555555'"/>
    <xsl:variable name="doc" select="/rsm:CrossIndustryInvoice/rsm:ExchangedDocument"/>
    <xsl:variable name="tx" select="/rsm:CrossIndustryInvoice/rsm:SupplyChainTradeTransaction"/>
    <xsl:variable name="seller" select="$tx/ram:ApplicableHeaderTradeAgreement/ram:SellerTradeParty"/>
    <xsl:variable name="buyer" select="$tx/ram:ApplicableHeaderTradeAgreement/ram:BuyerTradeParty"/>
    <xsl:variable name="settlement" select="$tx/ram:ApplicableHeaderTradeSettlement"/>
    <xsl:variable name="totals" select="$settlement/ram:SpecifiedTradeSettlementHeaderMonetarySummation"/>
    <xsl:variable name="number" select="string($doc/ram:ID)"/>
    <xsl:variable name="currency" select="string($settlement/ram:InvoiceCurrencyCode)"/>
    <!-- Euro amounts carry the sign, all other currencies their ISO code -->
    <xsl:variable name="currencySymbol" select="if ($currency = 'EUR' or $currency = '') then '€' else $currency"/>
    <xsl:variable name="vatId" select="string($seller/ram:SpecifiedTaxRegistration/ram:ID[@schemeID='VA'])"/>
    <xsl:variable name="taxNumber" select="string($seller/ram:SpecifiedTaxRegistration/ram:ID[@schemeID='FC'])"/>

    <!-- "17.11.2026" in German, "17 November 2026" in English -->
    <xsl:template name="date">
        <xsl:param name="d"/>
        <xsl:value-of select="if ($language = 'de')
                then concat(substring($d,7,2), '.', substring($d,5,2), '.', substring($d,1,4))
                else format-date(xs:date(concat(substring($d,1,4), '-', substring($d,5,2), '-', substring($d,7,2))),
                                 '[D] [MNn] [Y]', 'en', (), ())"/>
    </xsl:template>

    <!-- Amount with currency. Unit prices may carry up to four decimals, all other amounts have two. -->
    <xsl:template name="money">
        <xsl:param name="value"/>
        <xsl:param name="maxDecimals" select="2"/>
        <xsl:variable name="optional" select="string-join(for $i in 3 to $maxDecimals return '#', '')"/>
        <xsl:value-of select="concat(if ($language = 'de')
                then format-number(number($value), concat('#.##0,00', $optional), 'de')
                else format-number(number($value), concat('#,##0.00', $optional)), '&#160;', $currencySymbol)"/>
    </xsl:template>

    <xsl:template name="unit">
        <xsl:param name="code"/>
        <xsl:choose>
            <xsl:when test="$code='HUR'"><xsl:value-of select="$unit.HUR"/></xsl:when>
            <xsl:when test="$code='H87' or $code='C62'"><xsl:value-of select="$unit.H87"/></xsl:when>
            <xsl:when test="$code='DAY'"><xsl:value-of select="$unit.DAY"/></xsl:when>
            <xsl:when test="$code='MON'"><xsl:value-of select="$unit.MON"/></xsl:when>
            <xsl:when test="$code='LS'"><xsl:value-of select="$unit.LS"/></xsl:when>
            <xsl:when test="$code='KMT'"><xsl:value-of select="$unit.KMT"/></xsl:when>
            <xsl:when test="$code='MIN'"><xsl:value-of select="$unit.MIN"/></xsl:when>
            <xsl:otherwise><xsl:value-of select="$code"/></xsl:otherwise>
        </xsl:choose>
    </xsl:template>

    <!-- One footer line, omitted when empty -->
    <xsl:template name="line">
        <xsl:param name="text"/>
        <xsl:if test="normalize-space($text) != ''">
            <fo:block><xsl:value-of select="$text"/></fo:block>
        </xsl:if>
    </xsl:template>

    <xsl:template match="/">
        <fo:root font-family="{$font}" font-size="10pt" xml:lang="{$language}">
            <fo:layout-master-set>
                <fo:simple-page-master master-name="first" page-height="29.7cm" page-width="21cm">
                    <!-- Positions follow the German business letter standard DIN 5008, form B -->
                    <fo:region-body margin-top="9.85cm" margin-right="2cm" margin-bottom="3.2cm" margin-left="2.5cm"/>
                    <fo:region-before region-name="header-first" extent="9.85cm" precedence="true"/>
                    <fo:region-after region-name="footer" extent="2.6cm" precedence="true"/>
                    <fo:region-start region-name="marks" extent="1cm"/>
                </fo:simple-page-master>
                <fo:simple-page-master master-name="rest" page-height="29.7cm" page-width="21cm">
                    <fo:region-body margin-top="2.8cm" margin-right="2cm" margin-bottom="3.2cm" margin-left="2.5cm"/>
                    <fo:region-before region-name="header-rest" extent="2.8cm"/>
                    <fo:region-after region-name="footer" extent="2.6cm"/>
                </fo:simple-page-master>
                <fo:page-sequence-master master-name="A4">
                    <fo:repeatable-page-master-alternatives>
                        <fo:conditional-page-master-reference page-position="first" master-reference="first"/>
                        <fo:conditional-page-master-reference page-position="rest" master-reference="rest"/>
                    </fo:repeatable-page-master-alternatives>
                </fo:page-sequence-master>
            </fo:layout-master-set>

            <fo:page-sequence master-reference="A4">

                <!-- First page header: logo, address window, reference block -->
                <fo:static-content flow-name="header-first">
                    <fo:block-container absolute-position="absolute" top="1.3cm" left="10cm" width="9cm" height="2.8cm">
                        <fo:block text-align="right">
                            <xsl:choose>
                                <xsl:when test="$logo != ''">
                                    <fo:external-graphic src="{$logo}" content-height="2.2cm" content-width="scale-to-fit" scaling="uniform"/>
                                </xsl:when>
                                <xsl:otherwise>
                                    <fo:inline font-size="18pt" font-weight="bold"><xsl:value-of select="$seller/ram:Name"/></fo:inline>
                                </xsl:otherwise>
                            </xsl:choose>
                        </fo:block>
                    </fo:block-container>

                    <!-- Return address: last line of the 17.7 mm zone above the address (45 to 62.7 mm from the top) -->
                    <fo:block-container absolute-position="absolute" top="4.5cm" left="2.5cm" width="8cm" height="1.77cm"
                                        display-align="after">
                        <fo:block font-size="7pt" color="{$grey}" text-decoration="underline">
                            <xsl:value-of select="$seller/ram:Name"/>
                            <xsl:text>, </xsl:text>
                            <xsl:value-of select="$seller/ram:PostalTradeAddress/ram:LineOne"/>
                            <xsl:text>, </xsl:text>
                            <xsl:value-of select="concat($seller/ram:PostalTradeAddress/ram:PostcodeCode,' ',$seller/ram:PostalTradeAddress/ram:CityName)"/>
                        </fo:block>
                    </fo:block-container>

                    <!-- Address zone: 62.7 to 90 mm from the top, 25 mm from the left -->
                    <fo:block-container absolute-position="absolute" top="6.27cm" left="2.5cm" width="8cm" height="2.73cm">
                        <fo:block font-size="11pt" line-height="1.35">
                            <fo:block><xsl:value-of select="$buyer/ram:Name"/></fo:block>
                            <xsl:if test="normalize-space($buyer/ram:PostalTradeAddress/ram:LineTwo) != ''">
                                <fo:block><xsl:value-of select="$buyer/ram:PostalTradeAddress/ram:LineTwo"/></fo:block>
                            </xsl:if>
                            <fo:block><xsl:value-of select="$buyer/ram:PostalTradeAddress/ram:LineOne"/></fo:block>
                            <fo:block><xsl:value-of select="concat($buyer/ram:PostalTradeAddress/ram:PostcodeCode,' ',$buyer/ram:PostalTradeAddress/ram:CityName)"/></fo:block>
                            <xsl:if test="$customerCountry != ''">
                                <fo:block><xsl:value-of select="$customerCountry"/></fo:block>
                            </xsl:if>
                        </fo:block>
                    </fo:block-container>

                    <!-- Information block: 125 mm from the left, 50 mm from the top -->
                    <fo:block-container absolute-position="absolute" top="5cm" left="12.5cm" width="6.5cm" height="4.5cm">
                        <fo:table table-layout="fixed" width="100%" font-size="9pt" line-height="1.4">
                            <fo:table-column column-width="2.9cm"/>
                            <fo:table-column column-width="3.6cm"/>
                            <fo:table-body>
                                <xsl:call-template name="info">
                                    <xsl:with-param name="label" select="$label.number"/>
                                    <xsl:with-param name="value" select="$number"/>
                                </xsl:call-template>
                                <xsl:call-template name="info">
                                    <xsl:with-param name="label" select="$label.date"/>
                                    <xsl:with-param name="value">
                                        <xsl:call-template name="date">
                                            <xsl:with-param name="d" select="$doc/ram:IssueDateTime/udt:DateTimeString"/>
                                        </xsl:call-template>
                                    </xsl:with-param>
                                </xsl:call-template>
                                <xsl:call-template name="info">
                                    <xsl:with-param name="label" select="$label.period"/>
                                    <xsl:with-param name="value" select="$period"/>
                                </xsl:call-template>
                                <xsl:call-template name="info">
                                    <xsl:with-param name="label" select="$label.customerVatId"/>
                                    <xsl:with-param name="value" select="string($buyer/ram:SpecifiedTaxRegistration/ram:ID[@schemeID='VA'])"/>
                                </xsl:call-template>
                                <xsl:call-template name="info">
                                    <xsl:with-param name="label" select="$label.contact"/>
                                    <xsl:with-param name="value" select="$contact"/>
                                </xsl:call-template>
                                <xsl:call-template name="info">
                                    <xsl:with-param name="label" select="$label.phone"/>
                                    <xsl:with-param name="value" select="$phone"/>
                                </xsl:call-template>
                            </fo:table-body>
                        </fo:table>
                    </fo:block-container>
                </fo:static-content>

                <!--
                  Fold marks at 105 and 210 mm and the punch mark at 148.5 mm from the top.
                  The region starts below the header, so the positions are given relative to 98.5 mm.
                -->
                <fo:static-content flow-name="marks">
                    <xsl:for-each select="('0.65cm', '5cm', '11.15cm')">
                        <fo:block-container absolute-position="absolute" top="{.}" left="0.5cm" width="0.35cm" height="0.1cm">
                            <fo:block font-size="1pt" line-height="1pt">
                                <fo:leader leader-pattern="rule" leader-length="0.35cm" rule-thickness="0.3pt" color="#888888"/>
                            </fo:block>
                        </fo:block-container>
                    </xsl:for-each>
                </fo:static-content>

                <!-- Header of following pages -->
                <fo:static-content flow-name="header-rest">
                    <fo:block margin-left="2.5cm" margin-right="2cm" padding-top="1.4cm" font-size="9pt" color="{$grey}" text-align-last="justify">
                        <xsl:value-of select="concat($label.invoiceTitle, ' ', $number)"/>
                        <fo:leader leader-pattern="space"/>
                        <xsl:value-of select="concat($label.page, ' ')"/><fo:page-number/>
                    </fo:block>
                </fo:static-content>

                <!-- Footer: built from company master data, empty lines are omitted -->
                <fo:static-content flow-name="footer">
                    <fo:block margin-left="2.5cm" margin-right="2cm" border-top="0.5pt solid #999999" padding-top="5pt">
                        <fo:table table-layout="fixed" width="100%" font-size="7pt" line-height="1.4" color="{$grey}">
                            <fo:table-column column-width="proportional-column-width(1)"/>
                            <fo:table-column column-width="proportional-column-width(1)"/>
                            <fo:table-column column-width="proportional-column-width(1.15)"/>
                            <fo:table-column column-width="proportional-column-width(1.15)"/>
                            <!-- Reset the indent inherited from the surrounding block, otherwise FOP shifts the cells -->
                            <fo:table-body start-indent="0pt" end-indent="0pt">
                                <fo:table-row>
                                    <fo:table-cell>
                                        <fo:block><xsl:value-of select="$seller/ram:Name"/></fo:block>
                                        <fo:block><xsl:value-of select="$seller/ram:PostalTradeAddress/ram:LineOne"/></fo:block>
                                        <fo:block><xsl:value-of select="concat($seller/ram:PostalTradeAddress/ram:PostcodeCode,' ',$seller/ram:PostalTradeAddress/ram:CityName)"/></fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell>
                                        <fo:block/>
                                        <xsl:if test="$managingDirector != ''">
                                            <fo:block><xsl:value-of select="$label.managingDirector"/></fo:block>
                                        </xsl:if>
                                        <xsl:call-template name="line"><xsl:with-param name="text" select="$managingDirector"/></xsl:call-template>
                                        <xsl:call-template name="line"><xsl:with-param name="text" select="$registerCourt"/></xsl:call-template>
                                        <xsl:call-template name="line"><xsl:with-param name="text" select="$registerNumber"/></xsl:call-template>
                                    </fo:table-cell>
                                    <fo:table-cell>
                                        <fo:block/>
                                        <xsl:call-template name="line"><xsl:with-param name="text" select="$bankName"/></xsl:call-template>
                                        <xsl:for-each select="$settlement/ram:SpecifiedTradeSettlementPaymentMeans[1]">
                                            <xsl:call-template name="line"><xsl:with-param name="text" select="ram:PayeePartyCreditorFinancialAccount/ram:IBANID"/></xsl:call-template>
                                            <xsl:call-template name="line"><xsl:with-param name="text" select="ram:PayeeSpecifiedCreditorFinancialInstitution/ram:BICID"/></xsl:call-template>
                                        </xsl:for-each>
                                    </fo:table-cell>
                                    <fo:table-cell>
                                        <fo:block/>
                                        <xsl:call-template name="line"><xsl:with-param name="text" select="$email"/></xsl:call-template>
                                        <xsl:call-template name="line"><xsl:with-param name="text" select="$web"/></xsl:call-template>
                                        <xsl:if test="$vatId != ''">
                                            <fo:block><xsl:value-of select="concat($label.vatId, ' ', $vatId)"/></fo:block>
                                        </xsl:if>
                                        <xsl:if test="$vatId = '' and $taxNumber != ''">
                                            <fo:block><xsl:value-of select="concat($label.taxNumber, ' ', $taxNumber)"/></fo:block>
                                        </xsl:if>
                                    </fo:table-cell>
                                </fo:table-row>
                            </fo:table-body>
                        </fo:table>
                        <xsl:if test="$footerExtra != ''">
                            <fo:block font-size="7pt" color="{$grey}" space-before="3pt"><xsl:value-of select="$footerExtra"/></fo:block>
                        </xsl:if>
                    </fo:block>
                </fo:static-content>

                <!-- Invoice body -->
                <fo:flow flow-name="xsl-region-body">
                    <fo:block font-size="13pt" font-weight="bold" space-after="14pt">
                        <xsl:value-of select="concat($label.invoiceTitle, ' ', $number)"/>
                    </fo:block>

                    <fo:table table-layout="fixed" width="100%">
                        <fo:table-column column-width="1cm"/>
                        <fo:table-column column-width="2.3cm"/>
                        <fo:table-column column-width="proportional-column-width(1)"/>
                        <fo:table-column column-width="2.6cm"/>
                        <fo:table-column column-width="2.9cm"/>
                        <fo:table-header>
                            <fo:table-row font-weight="bold" border-bottom="0.8pt solid #000000">
                                <fo:table-cell padding="4pt 0"><fo:block><xsl:value-of select="$label.position"/></fo:block></fo:table-cell>
                                <fo:table-cell padding="4pt 0"><fo:block><xsl:value-of select="$label.quantity"/></fo:block></fo:table-cell>
                                <fo:table-cell padding="4pt 0"><fo:block><xsl:value-of select="$label.text"/></fo:block></fo:table-cell>
                                <fo:table-cell padding="4pt 0"><fo:block text-align="right"><xsl:value-of select="$label.unitPrice"/></fo:block></fo:table-cell>
                                <fo:table-cell padding="4pt 0"><fo:block text-align="right"><xsl:value-of select="$label.lineTotal"/></fo:block></fo:table-cell>
                            </fo:table-row>
                        </fo:table-header>
                        <fo:table-body>
                            <xsl:for-each select="$tx/ram:IncludedSupplyChainTradeLineItem">
                                <fo:table-row border-bottom="0.4pt solid #BBBBBB" keep-together.within-page="always">
                                    <fo:table-cell padding="5pt 0">
                                        <fo:block><xsl:value-of select="ram:AssociatedDocumentLineDocument/ram:LineID"/></fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="5pt 0">
                                        <fo:block>
                                            <xsl:value-of select="if ($language = 'de')
                                                    then format-number(number(ram:SpecifiedLineTradeDelivery/ram:BilledQuantity), '#.##0,####', 'de')
                                                    else format-number(number(ram:SpecifiedLineTradeDelivery/ram:BilledQuantity), '#,##0.####')"/>
                                            <xsl:text>&#160;</xsl:text>
                                            <xsl:call-template name="unit">
                                                <xsl:with-param name="code" select="string(ram:SpecifiedLineTradeDelivery/ram:BilledQuantity/@unitCode)"/>
                                            </xsl:call-template>
                                        </fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="5pt 6pt 5pt 0">
                                        <fo:block><xsl:value-of select="ram:SpecifiedTradeProduct/ram:Name"/></fo:block>
                                        <xsl:if test="normalize-space(ram:SpecifiedTradeProduct/ram:Description) != ''">
                                            <fo:block linefeed-treatment="preserve" color="{$grey}" font-size="9pt">
                                                <xsl:value-of select="ram:SpecifiedTradeProduct/ram:Description"/>
                                            </fo:block>
                                        </xsl:if>
                                    </fo:table-cell>
                                    <fo:table-cell padding="5pt 0">
                                        <fo:block text-align="right">
                                            <xsl:call-template name="money">
                                                <xsl:with-param name="value" select="ram:SpecifiedLineTradeAgreement/ram:NetPriceProductTradePrice/ram:ChargeAmount"/>
                                                <xsl:with-param name="maxDecimals" select="4"/>
                                            </xsl:call-template>
                                        </fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="5pt 0">
                                        <fo:block text-align="right">
                                            <xsl:call-template name="money">
                                                <xsl:with-param name="value" select="ram:SpecifiedLineTradeSettlement/ram:SpecifiedTradeSettlementLineMonetarySummation/ram:LineTotalAmount"/>
                                            </xsl:call-template>
                                        </fo:block>
                                    </fo:table-cell>
                                </fo:table-row>
                            </xsl:for-each>
                        </fo:table-body>
                    </fo:table>

                    <!-- Totals -->
                    <fo:table table-layout="fixed" width="100%" space-before="8pt" keep-together.within-page="always">
                        <fo:table-column column-width="proportional-column-width(1)"/>
                        <fo:table-column column-width="4.6cm"/>
                        <fo:table-column column-width="2.9cm"/>
                        <fo:table-body>
                            <xsl:choose>
                                <xsl:when test="$settlement/ram:ApplicableTradeTax[ram:CategoryCode='S']">
                                    <xsl:call-template name="totalRow">
                                        <xsl:with-param name="label" select="$label.net"/>
                                        <xsl:with-param name="value" select="$totals/ram:TaxBasisTotalAmount"/>
                                    </xsl:call-template>
                                    <xsl:for-each select="$settlement/ram:ApplicableTradeTax[ram:CategoryCode='S']">
                                        <xsl:call-template name="totalRow">
                                            <xsl:with-param name="label" select="concat($label.vatPrefix, ' ',
                                                    if ($language = 'de')
                                                    then format-number(number(ram:RateApplicablePercent), '0,##', 'de')
                                                    else format-number(number(ram:RateApplicablePercent), '0.##'),
                                                    ' ', $label.vatSuffix)"/>
                                            <xsl:with-param name="value" select="ram:CalculatedAmount"/>
                                        </xsl:call-template>
                                    </xsl:for-each>
                                    <xsl:call-template name="totalRow">
                                        <xsl:with-param name="label" select="$label.gross"/>
                                        <xsl:with-param name="value" select="$totals/ram:GrandTotalAmount"/>
                                        <xsl:with-param name="bold" select="true()"/>
                                    </xsl:call-template>
                                </xsl:when>
                                <xsl:otherwise>
                                    <xsl:call-template name="totalRow">
                                        <xsl:with-param name="label" select="$label.total"/>
                                        <xsl:with-param name="value" select="$totals/ram:GrandTotalAmount"/>
                                        <xsl:with-param name="bold" select="true()"/>
                                    </xsl:call-template>
                                </xsl:otherwise>
                            </xsl:choose>
                        </fo:table-body>
                    </fo:table>

                    <!-- Mandatory notes for 0 % cases (small business, reverse charge, third country) -->
                    <xsl:for-each select="$settlement/ram:ApplicableTradeTax[normalize-space(ram:ExemptionReason) != '']">
                        <fo:block space-before="14pt"><xsl:value-of select="ram:ExemptionReason"/></fo:block>
                    </xsl:for-each>

                    <xsl:if test="normalize-space($settlement/ram:SpecifiedTradePaymentTerms[1]/ram:Description) != ''">
                        <fo:block space-before="14pt" line-height="1.4">
                            <xsl:value-of select="normalize-space($settlement/ram:SpecifiedTradePaymentTerms[1]/ram:Description)"/>
                        </fo:block>
                    </xsl:if>
                </fo:flow>
            </fo:page-sequence>
        </fo:root>
    </xsl:template>

    <xsl:template name="info">
        <xsl:param name="label"/>
        <xsl:param name="value"/>
        <xsl:if test="normalize-space($value) != ''">
            <fo:table-row>
                <fo:table-cell><fo:block color="{$grey}"><xsl:value-of select="$label"/></fo:block></fo:table-cell>
                <fo:table-cell><fo:block text-align="right"><xsl:value-of select="$value"/></fo:block></fo:table-cell>
            </fo:table-row>
        </xsl:if>
    </xsl:template>

    <xsl:template name="totalRow">
        <xsl:param name="label"/>
        <xsl:param name="value"/>
        <xsl:param name="bold" select="false()"/>
        <fo:table-row>
            <xsl:if test="$bold">
                <xsl:attribute name="font-weight">bold</xsl:attribute>
            </xsl:if>
            <fo:table-cell><fo:block/></fo:table-cell>
            <fo:table-cell padding="2pt 0"><fo:block><xsl:value-of select="$label"/></fo:block></fo:table-cell>
            <fo:table-cell padding="2pt 0">
                <fo:block text-align="right">
                    <xsl:call-template name="money"><xsl:with-param name="value" select="$value"/></xsl:call-template>
                </fo:block>
            </fo:table-cell>
        </fo:table-row>
    </xsl:template>
</xsl:stylesheet>
