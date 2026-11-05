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
2. Settings > System > Developer options: enable USB debugging and Wireless debugging.
3. Pair (Android 11+ TVs use a one-time pairing code): in Wireless debugging choose "Pair device with pairing code", then on the computer:
   ```sh
   adb pair <tv-ip>:<pairing-port>    # enter the 6-digit code shown on the TV
   ```
4. Connect: the connect port differs from the pairing port and changes whenever wireless debugging restarts. Find it with
   ```sh
   adb mdns services                  # look for the _adb-tls-connect._tcp entry, host:port
   adb connect <tv-ip>:<connect-port>
   adb devices                        # the TV should be listed as "device"
   ```
   The port is also shown on the Wireless debugging screen ("IP address & Port"). Older setups with classic network debugging can use `adb connect <tv-ip>:5555`.

## Install and run

```sh
./gradlew installDebug
# or
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Launch from the Google TV home screen: Apps tab > "Your apps" row > fstream. Or:

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
