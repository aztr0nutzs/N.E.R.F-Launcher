Minimal icon pack asset root: assets/icon_packs/minimal/

Contract:
- One file per app package name.
- Format: <package_name>.png (png/webp/jpg/jpeg supported by runtime).
- Runtime resolution path: icon_packs/<pack_name>/<package_name>.<ext>

Shipped package coverage:
- com.nerf.launcher.png
- com.android.calculator2.png
- com.android.calendar.png
- com.android.settings.png
- com.android.camera2.png
- com.android.chrome.png
- com.android.contacts.png
- com.android.deskclock.png
- com.android.dialer.png
- com.android.documentsui.png
- com.android.messaging.png
- com.android.vending.png
- com.google.android.apps.maps.png
- com.google.android.apps.messaging.png
- com.google.android.apps.nbu.files.png
- com.google.android.apps.photos.png
- com.google.android.calculator.png
- com.google.android.calendar.png
- com.google.android.contacts.png
- com.google.android.deskclock.png
- com.google.android.dialer.png
- com.google.android.gm.png
- com.google.android.googlequicksearchbox.png
- com.google.android.youtube.png

Runtime behavior:
- These entries are real renderable bitmap assets.
- Only package names with a matching image file are overridden by this pack.
- All other apps fall back to the system icon chain.
