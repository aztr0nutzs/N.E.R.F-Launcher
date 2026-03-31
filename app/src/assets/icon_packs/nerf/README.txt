Nerf icon pack asset root: assets/icon_packs/nerf/

Contract:
- One file per app package name.
- Format: <package_name>.png (png/webp/jpg/jpeg supported by runtime).
- Runtime resolution path: icon_packs/<pack_name>/<package_name>.<ext>

Shipped package coverage:
- com.nerf.launcher.png
- com.android.settings.png
- com.android.camera2.png
- com.android.chrome.png
- com.google.android.googlequicksearchbox.png

Runtime behavior:
- These entries are real renderable bitmap assets.
- Only package names with a matching image file are overridden by this pack.
- All other apps fall back to the system icon chain.
