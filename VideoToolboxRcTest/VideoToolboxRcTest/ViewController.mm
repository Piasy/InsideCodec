//
/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Piasy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
//


#import "ViewController.h"

#import "RTCFileVideoCapturer.h"
#import "ARDFileCaptureController.h"
#import "RTCMTLVideoView.h"
#import "RTCVideoCodecH264.h"
#include <mars/xlog/appender.h>

@interface ViewController ()<RTCVideoCapturerDelegate>

@end

@implementation ViewController {
    ARDFileCaptureController* _fileCaptureController NS_AVAILABLE_IOS(10);
    RTCMTLVideoView* _videoView;
    RTCVideoEncoderH264* _encoder;
    int64_t _startTs;
}

- (void)viewDidLoad {
    [super viewDidLoad];
    // Do any additional setup after loading the view, typically from a nib.

    _startTs = -1;

    RTCFileVideoCapturer* fileCapturer =
        [[RTCFileVideoCapturer alloc] initWithDelegate:self];

    _fileCaptureController =
        [[ARDFileCaptureController alloc] initWithCapturer:fileCapturer];
    [_fileCaptureController startCapture];

    _videoView = [[RTCMTLVideoView alloc] initWithFrame:CGRectZero];
    _videoView.frame = self.view.bounds;

    [self.view addSubview:_videoView];

    RCTestConfig* config = [[RCTestConfig alloc] init];
    config.updateBr = true;
    config.initBr = 500;
    config.brStep = 100;
    config.outputWidth = 448;
    config.outputHeight = 800;
    config.outputFps = 30;
    config.outputKeyFrameInterval = 2;
    
    _encoder = [[RTCVideoEncoderH264 alloc] initTestConfig:config];
    
    RTCVideoEncoderSettings* setting = [[RTCVideoEncoderSettings alloc] init];
    setting.name = @"H264";
    setting.width = 640;
    setting.height = 480;
    setting.startBitrate = 300;
    setting.maxBitrate = 800000;
    setting.minBitrate = 30;
    setting.maxFramerate = 60;
    setting.qpMax = 56;
    setting.mode = RTCVideoCodecModeRealtimeVideo;

    [_encoder startEncodeWithSettings:setting numberOfCores:2];
}

- (void)didReceiveMemoryWarning {
    [super didReceiveMemoryWarning];
    // Dispose of any resources that can be recreated.
}

- (void)capturer:(nonnull RTCVideoCapturer*)capturer
    didCaptureVideoFrame:(nonnull RTCVideoFrame*)frame {
    if (_startTs == -1) {
        _startTs = frame.timeStampNs;
    } else if (_startTs == frame.timeStampNs) {
        [_fileCaptureController stopCapture];
        appender_close();
        return;
    }
    
    [_videoView renderFrame:frame];
    [_encoder encode:frame
        codecSpecificInfo:nil
               frameTypes:@[ @(RTCFrameTypeVideoFrameDelta) ]];
}

@end
