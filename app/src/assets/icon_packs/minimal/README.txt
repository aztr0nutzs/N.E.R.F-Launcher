Minimal icon pack asset root: assets/icon_packs/minimal/

Contract:
- One file per app package name.
- Format: <package_name>.png (png/webp/jpg/jpeg supported by runtime).
- Runtime resolution path: icon_packs/<pack_name>/<package_name>.<ext>

Included sample mappings:
- com.nerf.launcher.png
- com.android.settings.png
- com.android.camera2.png
- com.android.chrome.png
- com.google.android.googlequicksearchbox.png

Repository note:
- Binary icon files are intentionally replaced with `*.placeholder.txt` files for PR portability.
- Replace each placeholder with a real image file named exactly as listed above before shipping.
