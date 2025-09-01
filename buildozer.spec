[app]

# App basic info
title = MyKivyApp
package.name = mykivyapp
package.domain = org.example

# Source
source.dir = .
source.main = main.py

# Requirements
requirements = python3,kivy==2.2.1,requests

# Icon and presplash
icon.filename = %(source.dir)s/icon.png
presplash.filename = %(source.dir)s/presplash.png

# Orientation
orientation = portrait

# Permissions
android.permissions = INTERNET, WRITE_EXTERNAL_STORAGE

# API levels
android.minapi = 21
android.target = 33

# Storage and assets
copy_assets = 1
android.private_storage = True

# Exclude patterns
exclude_patterns = *.pyc, *.pyo, *.swp, .git, .github

# Python version
python.version = 3.10

# NDK/SDK paths (GitHub Actions & local)
android.ndk_path = /usr/local/lib/android/sdk/ndk/27.3.13750724
android.sdk_path = /usr/local/lib/android/sdk
