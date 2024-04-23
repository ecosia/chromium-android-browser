package org.ecosia.utils;


import org.chromium.base.version_info.VersionInfo;

public class BuildInfoProviderImplementation implements BuildInfoProvider {

    @Override
    public boolean isDebugBuild() {
        return !VersionInfo.isOfficialBuild();
    }

}
