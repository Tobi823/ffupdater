#!/usr/bin/python3
import re
import pathlib

# add your files here
strings_files = [
    "ffupdater/src/main/res/values-pt-rBR/strings.xml",
    "ffupdater/src/main/res/values-pl/strings.xml",
    "ffupdater/src/main/res/values-ru/strings.xml",
    "ffupdater/src/main/res/values-fr/strings.xml",
    "ffupdater/src/main/res/values-de/strings.xml",
    "ffupdater/src/main/res/values-uk/strings.xml",
    "ffupdater/src/main/res/values/strings.xml",
    "ffupdater/src/main/res/values-bg/strings.xml",
    "ffupdater/src/main/res/values-ja/strings.xml",
    "ffupdater/src/main/res/values-cs/strings.xml",
    "ffupdater/src/main/res/values-it/strings.xml",
    "ffupdater/src/main/res/values-nb-rNO/strings.xml",
    "ffupdater/src/main/res/values-tr/strings.xml",
    "ffupdater/src/main/res/values-zh-rCN/strings.xml",
    "ffupdater/src/main/res/values-es/strings.xml",
    "ffupdater/src/main/res/values-hu/strings.xml",
]


# 1. transform the strings.xml file to a dict
# 2. sort the dict
# 3. transform the dict to a strings.xml file
def sort_strings_xml_file(path_to_file: str):
    entries = dict()
    current_entry_name = None

    # read entries from strings.xml file
    with open(path_to_file, "r") as file:
        for line in file.readlines():
            # a new entry was found in the strings.xml file
            if line.strip().startswith("<string ") or line.strip().startswith("<plurals "):
                current_entry_name = re.search(r'name="(.+)"', line).group(1)
                entries[current_entry_name] = ""

            # store content for the current entry
            if current_entry_name is not None:
                entries[current_entry_name] += line

            # stop recording for the current entry
            if line.strip().endswith("</string>") or line.strip().endswith("</plurals>"):
                current_entry_name = None

    entries = dict(sorted(entries.items()))

    # write results back to the strings.xml file
    with open(path_to_file, "w") as file:
        file.write(('<?xml version="1.0" encoding="utf-8"?>'
                    '<resources>'
                    f'{"".join(entries.values())}</resources>'))

    print(f"{path_to_file} was sorted")


for strings_file in strings_files:
    strings_file = f"{(pathlib.Path(__file__).parent.parent.absolute())}/{strings_file}"
    sort_strings_xml_file(strings_file)
