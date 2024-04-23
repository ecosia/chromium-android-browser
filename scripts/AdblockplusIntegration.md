# Integration Adblockplus

## Adding ABP to a clean repository

1. Create a patch:

        git diff --binary --full-index "TAG".."abp/abp" > abp.patch

Where `TAG` is the tag that the current abp branch is based on (e.g. `65.0.3325.181`).

2. Apply patch:

        git apply --3way abp.patch

If you get "repository lacks the necessary blob to fall back on 3-way merge" do `git fetch --tags abp` and/or `git fetch --tags upstream`.

3. Fix merge conflicts (e.g. with `git mergetool`)

## ABP specific build args:

Add the following to `args.gn`: `v8_component_build = true` and `use_chromium_android_linker = false`
