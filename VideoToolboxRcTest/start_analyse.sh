#!/bin/bash

LOG_FILE=`ios-deploy --list --bundle_id com.github.piasy.VideoToolboxRcTest | grep "xlog"` && \
ios-deploy --download=${LOG_FILE} --bundle_id com.github.piasy.VideoToolboxRcTest --to . && \
ios-deploy --rm=${LOG_FILE} --bundle_id com.github.piasy.VideoToolboxRcTest && \
/usr/bin/python ../MediaCodecRcTest/decode_mars_nocrypt_log_file.py .${LOG_FILE} && \
rm .${LOG_FILE} && \
pythonw ../MediaCodecRcTest/analyse_log.py .${LOG_FILE}.log
