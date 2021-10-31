#!/bin/sh

set -eu

type xsltproc > /dev/null
type xmllint > /dev/null

sort_xml_file() {
  xsltproc --output "$1" android_strings_format.xslt "$1"
  export XMLLINT_INDENT="    "
  xmllint --format --encode "utf-8" --output "$1" "$1"
}

sort_xml_file "../ffupdater/src/main/res/values/strings.xml"
sort_xml_file "../ffupdater/src/main/res/values-bg/strings.xml"
sort_xml_file "../ffupdater/src/main/res/values-de/strings.xml"
sort_xml_file "../ffupdater/src/main/res/values-fr/strings.xml"
sort_xml_file "../ffupdater/src/main/res/values-ja/strings.xml"
sort_xml_file "../ffupdater/src/main/res/values-pl/strings.xml"
sort_xml_file "../ffupdater/src/main/res/values-pt-rBR/strings.xml"
sort_xml_file "../ffupdater/src/main/res/values-ru/strings.xml"
sort_xml_file "../ffupdater/src/main/res/values-uk/strings.xml"