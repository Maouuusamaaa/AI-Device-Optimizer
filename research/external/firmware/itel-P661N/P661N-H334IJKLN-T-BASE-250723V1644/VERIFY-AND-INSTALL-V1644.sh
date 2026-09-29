#!/system/bin/sh
set -eu

APK="/sdcard/Download/SmartPanel-V1644.apk"
EXPECTED_SHA256="34965f3e4d5154cd5b1dcfdcc5a9b92b1343ac501de8acbe33655243d3cf47dd"
EXPECTED_CERT_SHA256="7e09506b9037d7267574c2bd4cc7102722e4306d682e6f1634483b8311d0c2bb"
EXPECTED_CERT_SHA1="be8cb9f95bcb5bfb04045034e5182634a2fca1fa"

echo "== 1. Baseline package =="
dumpsys package com.transsion.smartpanel | grep -E 'versionCode=|versionName=|codePath=|pkgFlags=' || true

echo "== 2. APK file =="
test -f "$APK"
sha256sum "$APK"
ACTUAL_SHA256="$(sha256sum "$APK" | awk '{print $1}')"
test "$ACTUAL_SHA256" = "$EXPECTED_SHA256"

echo "== 3. Signing certificate =="
if command -v apksigner >/dev/null 2>&1; then
  CERTS="$(apksigner verify --print-certs "$APK")"
else
  echo "apksigner not found in Termux PATH"
  exit 2
fi
printf '%s\n' "$CERTS"
printf '%s\n' "$CERTS" | grep -qi "$EXPECTED_CERT_SHA256"
printf '%s\n' "$CERTS" | grep -qi "$EXPECTED_CERT_SHA1"

echo "== 4. Package metadata from APK =="
if command -v aapt >/dev/null 2>&1; then
  aapt dump badging "$APK" | grep -E "package:|sdkVersion:|targetSdkVersion:" | head -n 3
else
  echo "aapt not found; skipping local manifest display"
fi

echo "== 5. Ready =="
echo "All pre-install checks passed."
echo "Run this script again with the following single command only after you have reviewed the output:"
echo "  rish -c 'pm install -r /sdcard/Download/SmartPanel-V1644.apk'"
