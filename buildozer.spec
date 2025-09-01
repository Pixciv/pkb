[app]

# (str) Title of your application
title = MyKivyApp

# (str) Package name
package.name = mykivyapp

# (str) Package domain (reverse domain style)
package.domain = org.example

# (str) Source code where the main.py live
source.dir = .

# (str) Main entry point of the app
source.main = main.py

# (list) Application requirements
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
python.version = 3.10

# (str) Version of your app
version = 1.0.0

# (str) Optional version regex if otomatik version almak istiyorsan
# version.regex = ...

# (str) Gradle dependencies (örnek)
# android.gradle_dependencies = 'com.android.support:appcompat-v7:28.0.0'
