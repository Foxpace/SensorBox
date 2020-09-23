# SensorBox - record all your sensors to csv files

<p align="center">
<img src="https://github.com/Creative-Motion-Apps/SensorBox/blob/master/AppImages/icon.png" width="250">
</p>

<p align="center">
<img src="https://github.com/Creative-Motion-Apps/SensorBox/blob/master/AppImages/sensorbox_preview.png" width="250">
</p>

The SensorBox provides easy way to access sensors in Android phone and Wear Os. You can customize measurements in many ways, which is suitable for development of other apps. The outputs of the app are raw outputs of the system.

## [Download it on Google Play here](https://play.google.com/store/apps/details?id=motionapps.sensorbox&hl=en_CA)
[![API](https://img.shields.io/badge/API-19%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=19)
[![license](https://img.shields.io/badge/license-Apache%202-blue)](https://www.apache.org/licenses/LICENSE-2.0) 

## Features:

Whole functionality is hidden under SensorServices library, where one foreground service operates with sensors, GPS and other APIs.

* stores **sensor outputs** into the csv: *timestamp, values, accuracy*
* values from the sensors are in raw format - **no resampling**
* pick native sensor speed limits
* compatible with **GPS**
* can use **Activity recognition API from Android** and **Significant motion sensor**
* write custom annotation during measurement
* all extra information are stored in **JSON**
* check sensor attributes and preview of the outputs
* measurement can be customized :
  *  write own key words to measurement 
  *  set up timed alarms
  *  set up countdown to start measurement
  *  stop on low battery measurement
  *  lock CPU, add app to whitelist
* compatible with **Wear Os** with similar features

## Organization of code and the libraries:

### Code
* whole code is in **Kotlin**
* the phone app follows **MVI architecture** - activity/fragment -> ViewModel -> repository
  * There are 2 activities created with this architecture :
    * **MainActivity** - created with other fragments like HomeFragment, AdvancedFragment, SettingsFragment, ...  - these **fragments share one ViewModel** defined by MainActivity. Meanwhile for the navigation is used androidx fragment navigation library.
    * **MeasurementActivity** - alone activity to create annotations / stop the measurement if it is proceeding - **has its own ViewModel**
* In the phone application is used **Hilt - dependency injection library** 
* **The phone app and the Wear Os app use the same SensorService Library** which covers all the requirements from the apps. The library provides intents builders for both of them. 
* **WearOsLib** provides easy and comprehensive code of how to find other device and send messages, send file between them. 

### Libraries:

* **app / wear** - implementation for the phone / wearable respectively 
* **CountDownDialog** - library for creation of the countdowns, with interaface to interact and custom Dialog
* **Flipper** - [Storage access framework](https://github.com/baldapps/Flipper) created by [baldapps](https://github.com/baldapps)
* **Sensorservices** - main background service, which registers all the sensors and other providers of the data
* **WearOsLib** - general library for communication of the phone and wearable and vice versa

## Third parties:

Thanks goes to:

* [GraphView](https://github.com/jjoe64/GraphView) - chart library 
[![license](https://img.shields.io/badge/license-Apache%202-blue)](https://www.apache.org/licenses/LICENSE-2.0) 
* [Flipper](https://github.com/baldapps/Flipper) - Storage access framework
[![license](https://img.shields.io/github/license/mashape/apistatus.svg?style=flat-square)](https://raw.githubusercontent.com/nok/sklearn-porter/master/license.txt) 
* [AppInto](https://github.com/AppIntro/AppIntro) - introduction to the app for the first launch
[![license](https://img.shields.io/badge/license-Apache%202-blue)](https://www.apache.org/licenses/LICENSE-2.0) 
* [Material dialogs](https://github.com/afollestad/material-dialogs) - dialogs with material design style
[![license](https://img.shields.io/badge/license-Apache%202-blue)](https://www.apache.org/licenses/LICENSE-2.0) 
* [Number picker](https://github.com/ShawnLin013/NumberPicker) - create custom number pickers
[![license](https://img.shields.io/github/license/mashape/apistatus.svg?style=flat-square)](https://raw.githubusercontent.com/nok/sklearn-porter/master/license.txt) 
* [Android about page](https://github.com/medyo/android-about-page) - easy way to create about page
[![license](https://img.shields.io/github/license/mashape/apistatus.svg?style=flat-square)](https://raw.githubusercontent.com/nok/sklearn-porter/master/license.txt) 
* [Licenses dialog](https://github.com/PSDev/LicensesDialog) - dialog to aggreate all licences - check out for the full licenses
[![license](https://img.shields.io/badge/license-Apache%202-blue)](https://www.apache.org/licenses/LICENSE-2.0) 