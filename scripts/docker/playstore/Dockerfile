#Building on top of monochrome to save compile time
FROM ecosiadev/chromium:vanilla-85.0.4183.127-arm64-monochrome

#Prepare arm release folder
RUN cd /home/circleci/project && gn gen out/release_arm --args='target_cpu="arm" target_os="android" android_channel="stable" is_debug=false is_official_build=true proprietary_codecs=true symbol_level=1 ffmpeg_branding="Chrome" disable_android_lint=true'

#Prepare arm64 release folder
#RUN cd /home/circleci/project && gn gen out/release_arm64 --args='target_cpu="arm64" target_os="android" android_channel="stable" is_debug=false is_official_build=true proprietary_codecs=true symbol_level=1 ffmpeg_branding="Chrome" disable_android_lint=true'

#Prepare x86 release folder
RUN cd /home/circleci/project && gn gen out/release_x86 --args='target_cpu="x86" target_os="android" android_channel="stable" is_debug=false is_official_build=true proprietary_codecs=true symbol_level=1 ffmpeg_branding="Chrome" disable_android_lint=true'

#Prepare x64 release folder
RUN cd /home/circleci/project && gn gen out/release_x64 --args='target_cpu="x64" target_os="android" android_channel="stable" is_debug=false is_official_build=true proprietary_codecs=true symbol_level=1 ffmpeg_branding="Chrome" disable_android_lint=true'

### Run 5 builds

#arm 5.0+ (Lollipop)
RUN cd /home/circleci/project && autoninja -C out/release_arm chrome_modern_public_bundle

#x86 5.0+ (Lollipop)
RUN cd /home/circleci/project && autoninja -C out/release_x86 chrome_modern_public_bundle

#arm64 7.0+ (Nougat) and 10+ (Q)
#already built inside monochrome image
#RUN cd /home/circleci/project && autoninja -C out/release_arm64 monochrome_public_bundle
RUN cd /home/circleci/project && autoninja -C out/release_arm64 trichrome_chrome_bundle

# x64 5.0 + (Lollipop)
RUN cd /home/circleci/project && autoninja -C out/release_x64 chrome_modern_public_bundle
