# 🌳 Ecosia for Android (Chromium)

## Getting involved

The Ecosia team is small and currently has limited capacity to involve the open-source community. We won't be reviewing issues or pull requests for now but are working towards changing this in the future.

### Thank you note

Ecosia for Android is based on a fork of the code of "Chromium for Android". We want to express our gratitude to all the original contributors and Google for releasing your code to the world.

## :floppy_disk: Getting started

### Recommended Build System

* 64-bit machine
* 4GB or higher RAM
* 8GB or higher swap space
* 100GB or higher free hard-disk space
* 64-bit Ubuntu 18.04/ 20.04

## Build instructions

Instructions provided in this page are similar to [Chromium's build instructions](https://www.chromium.org/developers/how-tos/android-build-instructions).

See more references and links [here](#books-chromium-references)

### Setup your system

#### Update Linux packages

```sh
sudo apt-get update
```

#### GIT

git version is 2.18.0+ is required, instructions [here](https://git-scm.com/download/linux)

(This is to avoid a bug while tracking png files)

#### Python

Python 2.7 is required for building Chromium

```sh
sudo apt-get install python -f
```

Python 3 is used for specific tools, like the strings replacement script

```sh
sudo apt-get update
sudo apt-get install python3.6
```

We use *lxml* as xml parser for the strings replacement script

```sh
sudo apt-get install python3-lxml
```

#### depot_tools

1. Get [depot_tools](https://www.chromium.org/developers/how-tos/depottools)

```sh
git clone https://chromium.googlesource.com/chromium/tools/depot_tools.git
```

2. Add depot_tools to ~/.bashrc

```sh
export PATH=$PATH:$HOME/depot_tools
```

#### JDK

Ensure that JDK is set to JDK 8

```sh
sudo update-alternatives --config java
```

It is enough to check if 'java-8-openjdk-amd64' is listed for now since the build process sets this automatically.

If 'java-8-openjdk-amd64' is not listed then install it:

```sh
sudo apt-get -y install openjdk-8-jre openjdk-8-jdk
```

After this, set up the following to OpenJDK 8:

```sh
sudo update-alternatives --config javac
sudo update-alternatives --config java
sudo update-alternatives --config javaws
sudo update-alternatives --config javap
sudo update-alternatives --config jar
sudo update-alternatives --config jarsigner
```

### Getting the source code

The following instructions will lead to a clean checkout and compile. This will take your whole day. There is a shortcut described in a later section called 'Getting a cached built'.

#### Clone the repo

1. Create a chromium parent directory and cd there

  ```sh
  mkdir chromium && cd chromium
  ```

2. Clone the repository to the *src* directory. Insert the *shallow-since* when you want to save disk space and we don't necessarily need the history before August 2020.

  ```sh
  git clone git@github.com:ecosia/chromium-android.git --shallow-since=2020-08-01 src
  ```

Make sure you checkout the right branch before you continue as the default might not be your desired one.

Your target branch might not be listed (for example if your target is a newer version but we didn't make that our default branch yet). If so, you can do the following:

  ```sh
  git fetch origin <target-branch>:<target-branch> --depth 100
  git checkout <target-branch>
  ```

#### Pull current versions of key build files

There are some scripts used during the build which may not execute anymore due to external changes. You may very well consider updating them at this point (you can verify this by looking for your OS name within the `install-build-deps.sh`, if it's missing, you'll need a newer version). Those are the files in question:

```
https://github.com/chromium/chromium/blob/master/build/install-build-deps.sh
https://github.com/chromium/chromium/blob/master/third_party/android_deps/fetch_all.py
```

**PLEASE NOTE:** The `install-build-deps.sh` file from above might still not execute correctly, therefore please check that it contains your ubuntu version code name. Look for the following snippet:

```
supported_codenames="(codename_a|codename_b|...)
```

If missing, just add your version code to it. You can find out the latter by entering the following command:

```sh
lsb_release -c
```

#### Prepare build environment

Set environment variables needed for builds, by adding the following in your bash profile. Find the values in our secrets management system.

```sh
export ANDROID_KEYSTORE_PASSWORD=[]
export ANDROID_APPCENTER_ID=[]
export CLOUDFLARE_CLIENT_ID=[]
export CLOUDFLARE_CLIENT_SECRET=[]
```

*Don't forget to re-source or restart the terminal after this step!*

Get third parties and run hooks

```sh
src/scripts/getThirdParties.sh
```

**Note:** On your first build you likely will need to run the "getThirdParties" script with **sudo** permissions.

*(Optional) to log performance output to autoninja builds, use the environment variable `NINJA_SUMMARIZE_BUILD=1`, either set or export it in your bash profile.*

#### First build

The first build will take quite long depending on the power of the system you are using (minimum 6 hours). The ninja build system is optimised for incremental builds though, so the dev, build, test cycle will be okay, maybe something more like 2 minutes for changes only to some java files.

You must consider your first build as part of the setup because it takes so long. See the [build](#build) section to do your first build.

### Getting a cached build

We provide zip-files which contain a shallow checkout of our codebase including all dependencies and a cached build for arm debug.

A cached build can be triggered by creating a tag containing the word `cached`. The files will show up in the CircleCI artifacts tab. Those artifacts only last **30 days**.

After having downloaded the cached build, some steps need to be taken to convert it to a working environment.

1. Download the zip files into a folder
2. Concatenate the parts
  
  ```bash
  cat Chromium.zip* > ~/Chromium.zip
  ```

3. Unzip
  
  ```bash
  unzip Chromium.zip -d /target-direcoty
  ```

### Getting a vanilla build

We provide a mechanism to create vanilla Chromium builds of the same major.

A vanilla build can be triggered by creating a tag prefixing with `vanilla_` followed by the desired Chromium version-tag. For example `vanilla_111.0.5563.116`.

This will create a monochrome3264 build and upload it to appcenter.

#### Convert cached folder to a working directory

The folder contains a shallow .git with a single tag of depth 1. To use it, the usual git commands needs adaptation. Some examples:

Fetching a new branch

```bash
git fetch --update-shallow origin ecosia-111.0.5563.58:ecosia-111.0.5563.58
```

Pushing to remote

```bash
git push origin --set-upstream ecosia-111.0.5563.58
```

## 🛠️ Contributing code

There are some useful tools for managing Ecosia flavoured chromium in our [scripts](scripts) folder.

### Build

If you will be testing your changes on a device then you will want to build an arm configuration, if on the other hand you will be using the emulator, then you will want to build x86 or x64.

From the `src` directory, use our [build script](scripts#buildsh).

e.g. for debug arm build:

```sh
autoninja -C out/debug_arm chrome_public_apk
```

The resultant apk is in *src/out/debug_arm/apks*

### Deploy

Use the chromium tools for easy debug deploys (this will install, run the app, and launch into logcat)

```sh
out/debug_arm/bin/chrome_public_apk run
```

### Install

To install the apk file on the device

```sh
adb install out/debug_arm/apks/chrome_public_apk
```

### Adding new Android code

All newly added java files have to be added to [ecosia_android_sources.gni](chrome/android/ecosia/ecosia_android_sources.gni) so that they are packaged into the apk.

Android related additions (such as a new activity) will need to be also referenced in the [AndroidManifest.xml](chrome/android/java/AndroidManifest.xml) (as usual).

### Adding Android dependency via gradle

Add your dependency to `/src/third_party/android_deps/build.gradle`

For example:

```gradle
compile "androidx.lifecycle:lifecycle-process:${androidXArchComponentsVersion}"
```

Run fetch_all.py in the same folder.

```sh
./fetch_all.py
```

This will alter `third_party/android_deps/BUILD.gn` and download all the needed files.

Now this library can be referenced from third party libraries e.g. from `/third_party/snowplow/BUILD.gn`

```gn
import("//build/config/android/rules.gni")

android_aar_prebuilt("snowplow_java") {
  deps = [
    "//third_party/okhttp:okhttp_java",
    "//third_party/android_deps:androidx_lifecycle_lifecycle_process_java",
  ]
  aar_path = "snowplow-android-tracker-1.4.0.aar"
}
```

Add reference to the folder in `/chrome/android/BUILD.gn`

```gn
android_library("chrome_java") {
  deps = [
    ...,
    "//third_party/snowplow:snowplow_java",
    ...
  ]
```

### Add third party library (aar)

Get the code (for example from [maven](https://mvnrepository.com/)) and place into a seprate folder in `/third_party` e.g. `/third_party/snowplow`

Create a `BUILD.gn` file describing the build rules. See above how to.

Reference the new folder in `/chrome/android/BUILD.gn`

```sh
  "//third_party/snowplow:snowplow_java",
```

Run `gn args /out/your_directory` to have all the gn-files being regenerated.

Make sure to have `update_android_aar_prebuilts = true` in your list of arguments for this run.

### Unit testing

There is a suite of unit tests that test only our implemented logic, they live in `chrome/android/junit/src/org/ecosia/`.

Build the test suite

```sh
ninja -C out/Default ecosia_junit_tests
```

Then run the tests

```sh
out/Default/bin/run_ecosia_junit_tests
```

One can also run a subset of tests

```sh
out/Default/bin/run_ecosia_junit_tests -f "org.ecosia.firstrun.*"
```

Note that this hasn't been extensively used as yet, and there could be some issues yet.

### Integration testing

This is not yet configured. But the files will live in `chrome/android/javatests/src/org/ecosia/` and the config and commands will be similar to the unit testing section above.

### Run chromium tests

More information about chromium tests in [Android Test Instructions](https://chromium.googlesource.com/chromium/src.git/+/master/docs/testing/android_test_instructions.md)

### Replacing Google strings

In order to replace the Google default strings into Ecosia, you can use the following python script from `src`:

```sh
python3 scripts/replace_google_strings.py .
```

When a string in a `grd` file changes, the IDs in the corresponding `xtb` file should change too according to the logic described in [GenerateMessageId](tools/grit/grit/extern/tclib.py#L34). The `replace_google_strings` script will take care of the translations IDs.
More information about Chromium strings can be found here:

* https://www.programmersought.com/article/5309879288/
* https://www.chromium.org/developers/design-documents/ui-localization
* https://www.chromium.org/developers/tools-we-use-in-chromium/grit/grit-users-guide

### Translations :interrobang:

There are two types of translations to consider. First we are modifying existing chromium strings and this is not using the default android translations flow. The second type is that we DO use default android translations inside our own views (onboarding and new tab page for example).

#### Automated workflow

We make use of the [Transifex-Github-Integration](https://help.transifex.com/en/articles/6265125-github-installation-and-configuration) to provide localised string resources.

1. Change the translation source file [chrome/android/java/res_ecosia/values/ecosia_strings.xml](chrome/android/java/res_ecosia/values/ecosia_strings.xml) on main
    1. This can either be done as part of your PR (takes effect only after merge)
    2. Or you can edit/add it directly in the Transifex web interface (will result in faster translations)
2. After merge, the integration will read the source file and push it to [transifex](https://app.transifex.com/ecosia/ecosia-android-browser/1f086fe48c90085af55ec5c2abc9662c/)
3. A pull request will be created to main as soon as new translations are ready.
4. Before crafting a RC, make sure that all translations are merged.

#### Manual Setup (Legacy)

:warning: This not needed anymore and just being documented in case that the automated process (see section above) does not work.

Create a transifex credentials file in your home directory

```sh
echo "[https://www.transifex.com]
api_hostname  = https://api.transifex.com
hostname      = https://www.transifex.com
username      = ***
password      = ***
rest_hostname = https://rest.api.transifex.com
token         = ***
" > ~/.transifexrc
```

##### Install transifex cli

```sh
sudo apt-get install python3-pip
sudo pip install transifex-client
```

##### Pull strings

```sh
tx pull -f
```

##### Add Android Strings

Pull first! Then add string to `chrome/android/java/res_ecosia/values/ecosia_strings.xml`.

```sh
tx push -s
```

#### Chromium strings

##### Adding strings

To add new strings, either add them to transifex (by downloading the json resource, adding the string and re-uploading the file) or use the IDs as already specified in transifex. Then run the translation script:

```sh
docker-compose up translations
```

Alternatively if docker is not configured, use just the python script from `src`

```sh
python3 scripts/translations/translate.py .
```

### Adding resources

You can find our Ecosia resources in `chrome/android/java/res_ecosia`. Here you can add layouts, drawables, values etc. There are a few things to be aware of:

1. If you change any files in there (add, rename, remove etc.) then you need to run `gn args` before building.
2. These resources are aggregated with existing chromium resources, therefore the file names (and probably ids, names etc.) need to be different to avoid conflict. The practice is usually to prepend `ecosia_` for example.
3. After adding new resources, they should be included in [ecosia_java_resources.gni](chrome/android/ecosia/ecosia_java_resources.gni). In this same file, at the top, there is a useful script that helps creating the list.

#### Add ecosia resources to another chromium package

Chromium Code is strictly modularized. To be able to access our strings and resources in a package which is not the default app, add the ecosia resources as `deps` in the corresponding BUILD.gn.

```gn
android_resources("java_resources") {
  sources = [
    ...
  ]

  deps = [
    ...,
    "//ui/android:ecosia_ui_java_resources",
  ]
}
```

#### Import assets from Figma

When exporting assets from Figma, please ensure to add all the density suffixes: `_mdpi`, `_hdpi`, `_xhdpi`, `_xxhdpi` and `_xxxhdpi`.
After exporting the images, they will be downloaded as a ZIP file. This file should be extracted, and the contents should be copied to the corresponding `res` folder and all the files should be renamed.
For easing this process, you can use the [Figma export tool](scripts/figma_export_tool.py). It requires 3 arguments: the path to the ZIP file that was exported from Figma, the resources folder where the images should be placed and an optional argument `--night` that tells if the images should be placed in the `night` mode folders.
Here's an example of use, from the `scripts` folder:

```gn
python3 figma_export_tool.py ~/Downloads/AndroidMaster.zip ~/chromium_111/src/chrome/android/java/res_ecosia onboarding_image --night
```

### C++ logging

Here's an example of how to add logging for c++ code:

```gn
#include <android/log.h>

__android_log_print(ANDROID_LOG_ERROR, "TRACKERS", "%s", Str);
```

More information can be found in the [official developer ndk documents](https://developer.android.com/ndk/reference/group/logging).

### Disable warnings

It is not recommended to disable the warnings, but in case it is required, here's an example of how to do that for a specific block of code:

```gn
//#pragma GCC diagnostic push
//#pragma GCC diagnostic ignored "-Wunreachable-code"
//...here goes the code that is causing the warning...
//#pragma GCC diagnostic pop
```

## :rocket: Automated Release

To trigger a fully automated creation of release AABs just create a tag on the desired commit of the following format:
**v_[version_name]_[version_code]**

for example:

```sh
git tag v_4.0.1_210 && git push origin v_4.0.1_210
```

This will then create arm, arm64, x86 and x64 release apks and upload them to [AppCenter](https://appcenter.ms/orgs/Ecosia-GmbH/apps/Ecosia)

### :cn: Huawei App Gallery release

To trigger a fully automated creation of the arm64 APK just create a tag on the desired commit of the following format:
**huawei_[version_name]_[version_code]**

for example:

```sh
git tag huawei_8.0.2_600 && git push origin huawei_8.0.2_600
```

## Architectural Decision records

Our most important tech related architectural decions can be found [here](scripts/adr/)

## Adblockplus

See [this doc](scripts/AdblockplusIntegration.md)

## Migrating to a new version of chromium

See [this doc](scripts/ChromiumMigrationGuide.md)

## :books: Chromium references

Chromium is an open-source browser project that aims to build a safer, faster,
and more stable way for all users to experience the web.

The project's web site is [https://www.chromium.org](chromium.org).

Documentation in the source is rooted in [docs/README.md](docs/README.md).

Learn how to [Get Around the Chromium Source Code Directory Structure
](https://www.chromium.org/developers/how-tos/getting-around-the-chrome-source-code).

* [Source Repository](https://chromium.googlesource.com/chromium/src/)
* [Checking out and building Chromium for Android](https://chromium.googlesource.com/chromium/src/+/master/docs/android_build_instructions.md)
* [Eclipse Configuration for Android](https://chromium.googlesource.com/chromium/src.git/+/master/docs/eclipse.md)
* [Android Studio Configuration](https://chromium.googlesource.com/chromium/src/+/master/docs/android_studio.md)
  * If you followed the steps above, you can create the gradle project by running from `src`:

```sh
 build/android/gradle/generate_gradle.py --output-directory out/Default --target '//chrome/android:chrome_public_apk' --target '//chrome/android:chrome_junit_tests'
 ```

* The mentioned `ChromiumStyle.xml` does not exist, instead you can use `src/tools/android/checkstyle/chromium-style-5.0.xml` with the `CheckStyle-IDEA` plugin
