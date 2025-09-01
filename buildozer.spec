[app]
# (str) Title of your application
title = MyKivyApp

# (str) Package name
package.name = mykivyapp

# (str) Package domain (reverse domain)
package.domain = org.example

# (str) Source code where main.py is located
source.include_exts = py,png,jpg,kv,atlas

# (str) Application version
version = 0.1

# (str) Kivy entry point
# main.py içinde build() fonksiyonu olan dosya
source.main = main.py

# (str) Supported orientation
orientation = portrait

# (list) Permissions
android.permissions = INTERNET,WRITE_EXTERNAL_STORAGE,READ_EXTERNAL_STORAGE

# (str) Target SDK
android.sdk = 30

# (str) Build tools version
android.build_tools = 33.0.2

# (str) NDK version
android.ndk = 25b

# (str) Target API
android.api = 30

# (bool) Use --private storage
android.private_storage = True

# (bool) Presplash
android.presplash = False

# (bool) Debug mode
log_level = 2

# (str) Supported architectures
android.arch = armeabi-v7a, arm64-v8a

# (list) Requirements (from requirements.txt)
requirements = python3,kivy,requests
