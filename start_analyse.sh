#!/usr/bin/env bash

LOG_FILE=`adb shell ls /sdcard/rc_test_log/`
adb pull /sdcard/rc_test_log/${LOG_FILE}
adb shell rm /sdcard/rc_test_log/${LOG_FILE}

/usr/bin/python decode_mars_nocrypt_log_file.py ${LOG_FILE}
python analyse_log.py ${LOG_FILE}.log
