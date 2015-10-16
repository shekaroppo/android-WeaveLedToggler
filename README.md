# Weave LED Toggler Quickstart
Demonstrates basic usage of the Weave API to toggle LEDs on the brillo developer board

## Build
This sample uses the Gradle build system.  To build this project, use the `gradlew build` command
or import the project into Android Studio.

## Run
To run this sample you will need at least one Android device and one Brillo device.  The brillo device must have the [https://developers.google.com/brillo/eap/guides/develop/weave-integration](ledflasher demo) installed.  You also need to go through the  "Bootstrapping and registration" flow to associate the Brillo device with your account.

Turn on the Brillo device, and open this application on the Android device.  As soon as the application detects the Brillo device, LED toggle switches will appear in the app.  You can use them to flip the onboard LEDs on and off!

## Next Steps
Visit [https://developers.google.com/weave/]
(https://developers.google.com/weave/) for more information on the Weave API.
