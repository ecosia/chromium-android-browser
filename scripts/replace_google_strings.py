#!/usr/bin/env python3
import os
import copy
from lxml import etree as ET

STRINGS_DIR = os.path.join('chrome', 'browser', 'ui', 'android', 'strings')
STRINGS_PATH = os.path.join(STRINGS_DIR, 'android_chrome_strings.grd')
TRANSLATIONS_DIR = os.path.join(STRINGS_DIR, 'translations')

COMPONENTS_DIR = os.path.join('components')
COMPONENTS_CHROMIUM_STRINGS_PATH = os.path.join(COMPONENTS_DIR, 'components_chromium_strings.grd')
COMPONENTS_GOOGLE_CHROME_STRINGS_PATH = os.path.join(COMPONENTS_DIR, 'components_google_chrome_strings.grd')
COMPONENTS_STRINGS_PATH = os.path.join(COMPONENTS_DIR, 'components_strings.grd')
COMPONENTS_NEW_OR_SAD_TAB_TRINGS_PATH = os.path.join(COMPONENTS_DIR, 'new_or_sad_tab_strings.grdp')
COMPONENTS_ERROR_PAGE_STRINGS_PATH = os.path.join(COMPONENTS_DIR, 'error_page_strings.grdp')
COMPONENTS_PAGE_INFO_STRINGS_PATH = os.path.join(COMPONENTS_DIR, 'page_info_strings.grdp')
COMPONENTS_TRANSLATIONS_DIR = os.path.join(COMPONENTS_DIR, 'strings')

CONSTANTS_DIR = os.path.join('chrome', 'android', 'java', 'res_chromium_base', 'values')
CONSTANTS_PATH = os.path.join(CONSTANTS_DIR, 'channel_constants.xml')

FEATURES_DIR = os.path.join('chrome','android','features','tab_ui','java','strings')
FEATURES_PATH = os.path.join(FEATURES_DIR,'android_chrome_tab_ui_strings.grd')
FEATURES_TRANSLATIONS_DIR = os.path.join(FEATURES_DIR, 'translations')

CHROME_APP_DIR = os.path.join('chrome', 'app')
CHROME_APP_GOOGLE_CHROME_STRINGS_PATH = os.path.join(CHROME_APP_DIR, 'google_chrome_strings.grd')
CHROME_APP_CHROMIUM_STRINGS_PATH = os.path.join(CHROME_APP_DIR, 'chromium_strings.grd')
CHROME_APP_TRANSLATIONS_DIR = os.path.join(CHROME_APP_DIR, 'resources')

# The order is important because that's the order used for replacing the strings
GOOGLE_STRINGS = ['Google Chrome and Chrome OS', 'Google Chrome', 'Google LLC', 'Chrome', 'Chromium', 'Google']
ECOSIA_STRING = 'Ecosia'

def _valid_directory(arg, parser):
    if os.path.isdir(arg):
        return arg

    parser.error("Directory does not exist: {}".format(arg))


def replace(android_source):
    # Constants
    ecosify_constants(os.path.join(android_source, CONSTANTS_PATH))

    # Chrome strings
    ecosify_dir(TRANSLATIONS_DIR, ecosify_strings(os.path.join(android_source, STRINGS_PATH)), 'android_chrome_strings')

    # Components
    ecosify_dir(COMPONENTS_TRANSLATIONS_DIR, ecosify_strings(os.path.join(android_source, COMPONENTS_CHROMIUM_STRINGS_PATH)), 'components_chromium_strings')
    ecosify_dir(COMPONENTS_TRANSLATIONS_DIR, ecosify_strings(os.path.join(android_source, COMPONENTS_GOOGLE_CHROME_STRINGS_PATH)), 'components_google_chrome_strings')
    ecosify_dir(COMPONENTS_TRANSLATIONS_DIR, ecosify_strings(os.path.join(android_source, COMPONENTS_NEW_OR_SAD_TAB_TRINGS_PATH)), 'components_strings')
    ecosify_dir(COMPONENTS_TRANSLATIONS_DIR, ecosify_strings(os.path.join(android_source, COMPONENTS_STRINGS_PATH)), 'components_strings')
    ecosify_dir(COMPONENTS_TRANSLATIONS_DIR, ecosify_strings(os.path.join(android_source, COMPONENTS_ERROR_PAGE_STRINGS_PATH)), 'components_strings')
    ecosify_dir(COMPONENTS_TRANSLATIONS_DIR, ecosify_strings(os.path.join(android_source, COMPONENTS_PAGE_INFO_STRINGS_PATH)), 'components_strings')

    # Features
    ecosify_dir(FEATURES_TRANSLATIONS_DIR, ecosify_strings(os.path.join(android_source, FEATURES_PATH)), 'android_chrome_tab_ui_strings')

    # Chrome app
    ecosify_dir(CHROME_APP_TRANSLATIONS_DIR, ecosify_strings(os.path.join(android_source, CHROME_APP_GOOGLE_CHROME_STRINGS_PATH)), 'google_chrome_strings')
    ecosify_dir(CHROME_APP_TRANSLATIONS_DIR, ecosify_strings(os.path.join(android_source, CHROME_APP_CHROMIUM_STRINGS_PATH)), 'chromium_strings')
    


def ecosify_constants(file_path):
    print("Replacing consants in {}".format(file_path))

    tree = ET.parse(file_path)
    for message in tree.xpath('//string'):
        raw_message = ET.tostring(message).decode('UTF-8')
        if any(string in raw_message for string in GOOGLE_STRINGS):
            message.text = replace_google_strings(message.text)
    tree.write(file_path, encoding='utf-8', doctype='<?xml version="1.0" encoding="UTF-8"?>',pretty_print=True, with_comments=True)


def ecosify_strings(file_path):
    print("Replacing strings in {}".format(file_path))

    ecosified_string_ids = []
    old_messages = []
    new_messages = []
    old_message_ids = []
    new_message_ids = []

    tree = ET.parse(file_path)
    for message in tree.xpath('//message[not(@translateable="false")]'): # avoid not translateable strings
        raw_message = ET.tostring(message).decode('UTF-8')
        if any(string in raw_message for string in GOOGLE_STRINGS):
            old_messages.append(copy.copy(message))

            # This won't replace the tail if it exists (if there's a tag splitting the text)
            message.text = replace_google_strings(message.text)

            # Check the existing placeholders
            placeholders = message.findall('ph')
            for placeholder in placeholders:
                examples = placeholder.findall('ex')
                for example in examples:
                    example.text = replace_google_strings(example.text)

                # The tail is replaced after every placeholder element
                placeholder.tail = replace_google_strings(placeholder.tail)

            new_messages.append(copy.copy(message))
    tree.write(file_path, encoding='utf-8', doctype='<?xml version="1.0" encoding="UTF-8"?>',pretty_print=True, with_comments=True)

    for message in old_messages:
        old_message_ids.append(message_id(message))

    for message in new_messages:
        new_message_ids.append(message_id(message))

    if len(old_message_ids) == len(new_message_ids):
        for i, message in enumerate(old_messages):
            ecosified_string_ids.append([old_message_ids[i], new_message_ids[i]])
    else:
        print('Error: ids list mismatch')

    return ecosified_string_ids


def ecosify_dir(dir_path, ecosified_ids, translations_name):
    for filename in os.listdir(dir_path):
        file_path = os.path.join(dir_path, filename)
        if filename.endswith(".xtb"):
            if filename.startswith(translations_name):
                ecosify_translations(file_path, ecosified_ids)
            continue
        else:
            print("File extension not valid {}".format(file_path))
            continue


def ecosify_translations(file_path, ecosified_ids):
    print("Replacing strings in tx file {}".format(file_path))

    tree = ET.parse(file_path)
    for id_pair in ecosified_ids:
        for translation in tree.xpath('//translation[@id={}]'.format(id_pair[0])):
            # Replace the ids
            translation.attrib['id'] = id_pair[1]

            # Check that the text is a string and replace
            if isinstance(translation.text, str):
                translation.text = replace_google_strings(translation.text)

            # Replace each existing tail if there are placeholders
            placeholders = translation.findall('ph')
            for placeholder in placeholders:
                if placeholder.tail is not None:
                    placeholder.tail = replace_google_strings(placeholder.tail)

    tree.write(file_path, encoding='utf-8', xml_declaration=True, pretty_print=True, with_comments=True, doctype='<!DOCTYPE translationbundle>')


def message_id(message, encoding = "UTF-8"):
    # Generate the template grd file
    temp_strings_file = 'temp_string_src.grd'
    os.system('python3 tools/grit/grit.py newgrd ' + temp_strings_file)

    # Parse strings file
    strings_tree = ET.parse(temp_strings_file)
    grit = strings_tree.getroot()
    release = grit.find('release')
    messages = release.find('messages')

    # Add message
    messages.append(message)
    strings_tree.write(temp_strings_file)

    # Generate translation file
    temp_translations_file = 'temp_string_tx.xmb'
    os.system('python3 tools/grit/grit.py -i temp_string_src.grd xmb ' + temp_translations_file)

    # Parse translations file
    translations_tree = ET.parse(temp_translations_file)
    messagebundle = translations_tree.getroot()
    msg =  messagebundle.find('msg')
    msg_id = msg.get('id')

    # Clean up temp files
    os.system('rm -rf ' + temp_strings_file)
    os.system('rm -rf ' + temp_translations_file)

    return msg_id


def replace_google_strings(text):
    for google_string in GOOGLE_STRINGS:
        text = text.replace(google_string, ECOSIA_STRING)
    return text


if __name__ == "__main__":
    import argparse

    parser = argparse.ArgumentParser(
        description="Replace Google and Chromium strings with Ecosia",
        formatter_class=argparse.ArgumentDefaultsHelpFormatter
    )
    parser.add_argument('android_source', type=lambda arg: _valid_directory(arg, parser),
                        help="The android project's src folder")
    args = parser.parse_args()

    replace(args.android_source)
