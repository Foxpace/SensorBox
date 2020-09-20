# SensorBox - record all your sensors to csv files

![Preview](AppImages/image1.png)

[![API](https://img.shields.io/badge/API-19%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=19)
[![license](https://img.shields.io/badge/license-Apache%202-blue)](https://www.apache.org/licenses/LICENSE-2.0) 

## Features:

Whole functionality is hidden under SensorServices library, where one foreground service operates with sensors, GPS and other APIs.

* stores sensor outputs into the csv: *timestamp, values, accuracy*
* values from the sensors are in raw format - **no resampling**
* pick native sensor speed limits
* compatible with GPS
* can use **Activity recognitionAPI from Android**
* write custom annotation during measurement
* all extra information are stored in JSON
* check sensor attributes and preview of the outputs
* measurement can be customized :
  *  write own key words to measurement 
  *  set up timed alarms
  *  set up countdown to start measurement
  *  stop on low battery measurement
  *  lock CPU, add app to whitelist
* compatible with **Wear Os** with similar features

## [Download it on Google Play here](https://play.google.com/store/apps/details?id=motionapps.sensorbox&hl=en_CA)