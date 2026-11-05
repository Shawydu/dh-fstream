# fstream-tv

Android TV app (Compose for TV + WebView embed player) for Chromecast with Google TV.

## Prerequisites

JDK 17, Android SDK (platform 35, build-tools, platform-tools). In every shell:

```sh
export JAVA_HOME=/opt/homebrew/opt/openjdk@17/libexec/openjdk.jdk/Contents/Home
export PATH=$JAVA_HOME/bin:$PATH
export ANDROID_HOME=$HOME/Library/Android/sdk
```

`local.properties` must contain `sdk.dir=<ANDROID_HOME>` (git-ignored).

## Build

```sh
./gradlew assembleDebug                 # app/build/outputs/apk/debug/app-debug.apk
./gradlew testDebugUnitTest assembleDebugAndroidTest
```

## Chromecast with Google TV setup

1. Settings > System > About: tap "Android TV OS build" 7 times to enable developer options.
2. Settings > System > Developer options: enable USB debugging and network (wireless) debugging.
3. Find the TV IP: Settings > Network & Internet > (your network).
4. Connect: `adb connect <tv-ip>:5555` and accept the prompt on the TV.

## Install and run

```sh
./gradlew installDebug
# or
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Launch from the home screen Apps row, or:

```sh
adb shell am start -n dev.fstream.tv/.MainActivity
```

## Logs

```sh
adb logcat --pid=$(adb shell pidof dev.fstream.tv)
```

## Uninstall

```sh
adb uninstall dev.fstream.tv
```
