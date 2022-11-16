import os
import subprocess

# TODO remove "disable_android_lint = true " from "release_args" after fixing lint warnings
def _args(build_type, cpu_type, version_name, version_code, android_keystore_password, appcenter_id, abp_telemetry_token, cloudflare_client_id, cloudflare_client_secret):
    common_args = ""\
        + "android_channel = \\\"stable\\\" " \
        + "target_os = \\\"android\\\" " \
        + f"android_override_version_name = \\\"{version_name}\\\"  " \
        + f"android_override_version_code = \\\"{version_code}\\\" " \
        + f"appcenter_secret = \\\"{appcenter_id}\\\" " \
        + f"cloudflare_client_id = \\\"{cloudflare_client_id}\\\" " \
        + f"cloudflare_client_secret = \\\"{cloudflare_client_secret}\\\" " \
        + "treat_warnings_as_errors = false " \
        + "ffmpeg_branding = \\\"Chrome\\\" "
    debug_args = "" \
        + "is_debug = true " \
        + "is_official_build = false " \
        + "icu_use_data_file = false " \
        + "is_component_build = true " \
        + "symbol_level = 1 " \
        + "rtc_build_examples = false " \
        + "disable_android_lint = true " \
        + "enable_nacl = false " \
        + "blink_symbol_level = 0 " \
        + "use_errorprone_java_compiler = false"    
    release_args = "" \
        + "is_debug = false " \
        + "is_official_build = true " \
        + "proprietary_codecs = true " \
        + "symbol_level = 1 " \
        + "android_keystore_name = \\\"ecosia\\\" " \
        + f"android_keystore_password = \\\"{android_keystore_password}\\\" " \
        + "disable_android_lint = true " \
        + "eyeo_telemetry_client_id = \\\"ecosia\\\" " \
        + f"eyeo_telemetry_activeping_auth_token = \\\"{abp_telemetry_token}\\\" " \
        + "android_keystore_path = \\\"//ecosia.keystore\\\""
    cpu_args = f" target_cpu = \\\"{cpu_type}\\\" "
    monochrome64_args = " min_monochrome_sdk_version = 31"

    result_args = common_args
    result_args += cpu_args
    result_args += release_args if build_type.startswith("release") else debug_args
    if build_type == "release_monochrome_64":
        result_args += monochrome64_args

    return result_args


def _out_path(build_type, cpu_type):
    # only take first bit for build type
    # both release and release_monochrome_64 share the same build folder
    # release_monochrome_64 -> release
    # release -> release
    return 'out/' + build_type.split('_')[0] + '_' + cpu_type


def _gen_cmd(out_path, args):
    return f"gn gen {out_path} --args=\" {gn_args}\" --color"


# example output
# gn gen out/debug_x86 --args="is_component_build = true is_debug = true"
if __name__ == '__main__':
    yellow_color = '\033[1;33m'
    no_color = '\033[0m'

    import argparse

    parser = argparse.ArgumentParser(description="Run builds")
    parser.add_argument('target', type=str, choices=["release", "debug", "release_monochrome_64"], help='Target to build: debug or release or release_monochrome_64')
    # The valid architectures are listed here: https://chromium.googlesource.com/chromium/src/+/master/docs/android_build_instructions.md#figuring-out-target_cpu 
    parser.add_argument('cpu', type=str, choices=["arm", "arm64", "x86", "x64"], help='Architecture to target: arm, arm64, x86, x64')
    parser.add_argument('--version-name', '--v', type=str, default=100, help='Version code to use in build args. Default 100')
    parser.add_argument('--version-code', '--n', type=int, default=100, help='Version name to use in build args. Default 100')
    args = parser.parse_args()

    android_keystore_password = os.environ.get("ANDROID_KEYSTORE_PASSWORD")
    if not android_keystore_password:
        print(f"{yellow_color}Warning:{no_color} Didn't find the ANDROID_KEYSTORE_PASSWORD var in your env.")
        android_keystore_password = "NOT_SET"

    abp_telemetry_token = os.environ.get("ABP_TELEMETRY_TOKEN")
    if not abp_telemetry_token:
        print(f"{yellow_color}Warning:{no_color} Didn't find the ABP_TELEMETRY_TOKEN var in your env.")
        abp_telemetry_token = "NOT_SET"
        
    appcenter_id = os.environ.get("ANDROID_APPCENTER_ID")
    if not appcenter_id:
        print(f"{yellow_color}Warning:{no_color} Didn't find the ANDROID_APPCENTER_ID var in your env.")
        appcenter_id = "NOT_SET"

    cloudflare_client_id = os.environ.get("CLOUDFLARE_CLIENT_ID")
    if not cloudflare_client_id:
        print(f"{yellow_color}Warning:{no_color} Didn't find the CLOUDFLARE_CLIENT_ID var in your env.")
        cloudflare_client_id = "NOT_SET"

    cloudflare_client_secret = os.environ.get("CLOUDFLARE_CLIENT_SECRET")
    if not cloudflare_client_secret:
        print(f"{yellow_color}Warning:{no_color} Didn't find the CLOUDFLARE_CLIENT_SECRET var in your env.")
        cloudflare_client_secret = "NOT_SET"

    v_name = args.version_name if args.version_name is not None else 100
    v_code = args.version_code if args.version_code is not None else 100

    gn_out_path = _out_path(args.target, args.cpu)
    gn_args = _args(args.target, args.cpu, args.version_name, args.version_code, android_keystore_password, appcenter_id, abp_telemetry_token, cloudflare_client_id, cloudflare_client_secret)
    gen_cmd = _gen_cmd(gn_out_path, gn_args)

    print(f"\nGenerating output folder {yellow_color}{gn_out_path}{no_color}")
    print(f"{gen_cmd}")

    # Beware that shell=True means to run as a raw command, i.e. potential for abuse, but should be fine as the input is sanitised
    subprocess.run(f"{gen_cmd}", shell=True, universal_newlines=True)
