#!/bin/bash
# Fast local build script for VinPlayM3U
# Usage: ./build-local.sh [assembleDebug|assembleRelease|compileDebugKotlin]

set -e

TASK="${1:-assembleDebug}"

echo "🔧 Building VinPlayM3U - Task: $TASK"
echo "=================================================="

# Use Gradle with optimized settings for local development
./gradlew "$TASK" \
  --no-daemon \
  -Dorg.gradle.jvmargs="-Xmx2g -XX:+UseParallelGC" \
  -Dorg.gradle.workers.max=2 \
  -Dkotlin.daemon.jvm.options="-Xmx512m" \
  --configure-on-demand \
  --parallel

echo "=================================================="
echo "✅ Build completed successfully!"
echo "📦 APK location: app/build/outputs/apk/debug/app-debug.apk"
