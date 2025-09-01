#!/bin/bash
set -e

echo "🔍 config.json okunuyor..."

APP_NAME=$(jq -r .app_name config.json)
PACKAGE_NAME=$(jq -r .package_name config.json)
VERSION_CODE=$(jq -r .version_code config.json)
VERSION_NAME=$(jq -r .version config.json)
ICON_BASE64=$(jq -r .icon_base64 config.json)

echo "✅ Uygulama adı: $APP_NAME"
echo "✅ Paket adı: $PACKAGE_NAME"
echo "✅ Versiyon: $VERSION_NAME ($VERSION_CODE)"

echo "🔍 colors.json okunuyor..."

STATUS_BAR_COLOR=$(jq -r .status_bar_color colors.json)
NAV_BAR_COLOR=$(jq -r .navigation_bar_color colors.json)
MAIN_BG_COLOR=$(jq -r .main_background_color colors.json)

echo "🎨 Status Bar Rengi: $STATUS_BAR_COLOR"
echo "🎨 Navigation Bar Rengi: $NAV_BAR_COLOR"
echo "🎨 Main Background Rengi: $MAIN_BG_COLOR"

echo "🖼️  icon.png dosyası oluşturuluyor (base64 decode)..."
echo "$ICON_BASE64" | base64 -d > icon.png

if [ ! -s icon.png ]; then
  echo "❌ icon.png oluşturulamadı veya boş, çıkılıyor."
  exit 1
fi

echo "🛠️  build.gradle.kts güncelleniyor..."
sed -i "s|applicationId = \".*\"|applicationId = \"$PACKAGE_NAME\"|" app/build.gradle.kts
sed -i "s|versionCode = .*|versionCode = $VERSION_CODE|" app/build.gradle.kts
sed -i "s|versionName = \".*\"|versionName = \"$VERSION_NAME\"|" app/build.gradle.kts

echo "🧹 mipmap klasörleri temizleniyor..."
rm -rf app/src/main/res/mipmap-*

echo "🖼️  İkonlar üretiliyor..."
for size in 48 72 96 144 192; do
  case $size in
    48) folder="mipmap-mdpi" ;;
    72) folder="mipmap-hdpi" ;;
    96) folder="mipmap-xhdpi" ;;
    144) folder="mipmap-xxhdpi" ;;
    192) folder="mipmap-xxxhdpi" ;;
  esac

  mkdir -p app/src/main/res/$folder

  convert icon.png -resize ${size}x${size} app/src/main/res/$folder/ic_launcher.png

  convert icon.png -resize ${size}x${size} -alpha set -background none -fill none \
    -draw "circle $((size/2)),$((size/2)) $((size/2)),$((size-1))" \
    app/src/main/res/$folder/ic_launcher_round.png || \
    cp app/src/main/res/$folder/ic_launcher.png app/src/main/res/$folder/ic_launcher_round.png
done

echo "🔄 AndroidManifest.xml güncelleniyor..."
MANIFEST_FILE="app/src/main/AndroidManifest.xml"
sed -i "s|package=\".*\"|package=\"$PACKAGE_NAME\"|" "$MANIFEST_FILE"
sed -i "s|android:label=\".*\"|android:label=\"$APP_NAME\"|" "$MANIFEST_FILE"

echo "🔄 MainActivity.kt package güncelleniyor ve dosya konumu değişiyor..."
OLD_PACKAGE_PATH="app/src/main/kotlin/com/example/myapp"
NEW_PACKAGE_PATH="app/src/main/kotlin/$(echo $PACKAGE_NAME | tr '.' '/')"

mkdir -p "$NEW_PACKAGE_PATH"

# Package satırını güncelle ve yeni klasöre taşı
sed "s|^package .*|package $PACKAGE_NAME|" "$OLD_PACKAGE_PATH/MainActivity.kt" > "$NEW_PACKAGE_PATH/MainActivity.kt"

# Eski klasörü kaldır
rm -rf "$OLD_PACKAGE_PATH"

echo "🖍️ colors.xml güncelleniyor..."
COLORS_XML="app/src/main/res/values/colors.xml"
cat > "$COLORS_XML" <<EOF
<resources>
    <color name="status_bar_color">$STATUS_BAR_COLOR</color>
    <color name="navigation_bar_color">$NAV_BAR_COLOR</color>
    <color name="main_background_color">$MAIN_BG_COLOR</color>
    <color name="app_background">$MAIN_BG_COLOR</color>
</resources>
EOF

echo "📦 Debug APK derleniyor..."
./gradlew assembleDebug

echo "📦 Release APK derleniyor..."
./gradlew assembleRelease

echo "📦 AAB (Android App Bundle) derleniyor..."
./gradlew bundleRelease

echo "✅ Tüm derlemeler tamamlandı."

# Derlenen dosyaların yollarını değişkene al
DEBUG_APK=$(find app/build/outputs/apk/debug -name '*.apk' | head -n 1)
RELEASE_APK=$(find app/build/outputs/apk/release -name '*.apk' | head -n 1)
RELEASE_AAB=$(find app/build/outputs/bundle/release -name '*.aab' | head -n 1)

# Konsola yazdır
echo "Debug APK: $DEBUG_APK"
echo "Release APK: $RELEASE_APK"
echo "Release AAB: $RELEASE_AAB"

# Dosya yollarını txt dosyalarına yaz (GitHub Actions için)
echo "$DEBUG_APK" > release-debug.txt
echo "$RELEASE_APK" > release-release.txt
echo "$RELEASE_AAB" > release-aab.txt
