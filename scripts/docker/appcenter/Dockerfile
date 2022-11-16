FROM ecosiadev/chromium:vanilla-85.0.4183.127-synced

#Replace build deps file
COPY --chown=circleci:circleci ./install-build-deps.sh ./build/install-build-deps.sh

#COPY Bundle tool
COPY --chown=circleci:circleci ./bundletool-all-1.1.0.jar /home/circleci/

#install build deps
RUN cd /home/circleci/project && sudo build/install-build-deps-android.sh

#Prepare arm64 release folder
RUN cd /home/circleci/project && gn gen out/release_arm64 --args='target_cpu="arm64" target_os="android" android_channel="stable" is_debug=false is_official_build=true proprietary_codecs=true symbol_level=1 ffmpeg_branding="Chrome" disable_android_lint=true'

#Run build for arm64 monochrome
RUN cd /home/circleci/project && autoninja -C out/release_arm64 monochrome_public_bundle
