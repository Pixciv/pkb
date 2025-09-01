[app]

# (str) Title of your application
title = MyKivyApp

# (str) Package name
package.name = mykivyapp

# (str) Package domain (reverse domain style)
package.domain = org.example

# (str) Application version
version = 1.0.0

# (str) Source code where the main.py lives
source.dir = .

# (str) Main entry point of the app
source.main = main.py

# (list) Application requirements
# örnek: kivy, requests
requirements = python3,kivy==2.2.1,requests

# (str) Icon of the app
icon.filename = %(source.dir)s/icon.png

# (str) Presplash / splash image
presplash.filename = %(source.dir)s/presplash.png

# (str) Supported orientation (landscape, portrait)
orientation = portrait

# (list) Permissions
android.permissions = INTERNET, WRITE_EXTERNAL_STORAGE

# (int) Minimum API your app supports
android.minapi = 21

# (int) Target API
android.target = 33

# (bool) Copy assets directory
copy_assets = 1

# (list) Exclude some files
exclude_patterns = *.pyc, *.pyo, *.swp, .git, .github

# (bool) Use android private storage
android.private_storage = True

# (str) Python version to use
# Buildozer default 3.10, Github Actions ile uyumlu
python.version = 3.10

# (str) Android NDK path (isteğe bağlı, CI/CD için gerekli)
android.ndk_path = /usr/local/lib/android/sdk/ndk/27.3.13750724

# (str) Android SDK path (isteğe bağlı, CI/CD için gerekli)
android.sdk_path = /usr/local/lib/android/sdk
