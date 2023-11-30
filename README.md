# TennisTune

Welcome to the GitHub repository for TennisTune! This application is designed for tennis players and enthusiasts who want to optimize their playing experience by tracking the string tension of their racquets.

## Features

- **Tension measurement:** Measure the string tension of your tennis racquet.
- **Racquet management:** Have multiple racquets? Create and manage racquets to track them all.
- **Racquet customization:** You can customize the head size & string mass density for tension calculation.
- **Display units:** Switch between lb and kg.
- **Dark theme:** The app supports both light and dark themes depending on the phone's setting.

## Screenshots

<p float="left">
<img src="/screenshots/ui_light_home_tension_display.png" alt="Home page of the app with 
measured tension displayed" title="Home Page" width="20%">
<img src="/screenshots/ui_light_settings.png" alt="The user can switch between lb and kg units 
in settings page" title="Settings" width="20%">
<img src="/screenshots/ui_light_racquet_list.png" alt="The racquet list with racquet names 
displayed. The user can select, add, edit or delete racquets." title="Racquet List" width="20%">
<img src="/screenshots/ui_light_racquet_edit.png" alt="The user can edit racquet specs of an 
existing racquet, such as racquet name, head size and string mass density. Adding a racquet 
follows a similar user interface." title="Racquet" width="20%">
</p>

## How the tension is measured

The tension is calculated based on the audio frequency by tapping on the string bed. We invite 
you to visit the project's [wiki pages](https://github.com/low-earth-orbit/tennis-tune/wiki) for an overview of algorithms.

## How to use the app

The app should ideally be used in a quiet environment.

1. Remove the dampener from the string bed, if you use one.
2. Hold the tennis racquet at the throat. Do not touch the string bed.
3. Use a firm object (such as a spoon or a screwdriver) to repeatedly tap on the string bed.
4. The app will listen to the sound and display the tension.
5. Click the reset button to restart.
6. Customize your racquet specs (head size and string mass density) to get a more accurate result.

## Supported devices

- Android 7 (SDK/API level 24) and up
- Tested on phones
- Not optimized for Wear OS/tablet/desktop
- Not optimized for landscape mode
