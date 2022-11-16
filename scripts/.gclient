solutions = [
  {
    "url": "https://chromium.googlesource.com/chromium/src.git",
    "managed": False,
    "name": "src",
    "deps_file": ".DEPS.git",
    "custom_deps": {},
    "custom_vars": { 
      "checkout_configuration": "small",
      "checkout_nacl": False, 
    },
  },
]
target_os = ["android"]
