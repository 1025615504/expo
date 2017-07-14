#import <ReactABI19_0_0/ABI19_0_0RCTViewManager.h>
#import <AVFoundation/AVFoundation.h>

@class ABI19_0_0EXBarCodeScanner;

typedef NS_ENUM(NSInteger, ABI19_0_0EXBarCodeScannerType) {
  ABI19_0_0EXBarCodeScannerTypeFront = AVCaptureDevicePositionFront,
  ABI19_0_0EXBarCodeScannerTypeBack = AVCaptureDevicePositionBack
};

typedef NS_ENUM(NSInteger, ABI19_0_0EXBarCodeScannerTorchMode) {
  ABI19_0_0EXBarCodeScannerTorchModeOff = AVCaptureTorchModeOff,
  ABI19_0_0EXBarCodeScannerTorchModeOn = AVCaptureTorchModeOn,
  ABI19_0_0EXBarCodeScannerTorchModeAuto = AVCaptureTorchModeAuto
};

@interface ABI19_0_0EXBarCodeScannerManager
    : ABI19_0_0RCTViewManager <AVCaptureMetadataOutputObjectsDelegate>

@property(nonatomic, strong) dispatch_queue_t sessionQueue;
@property(nonatomic, strong) AVCaptureSession *session;
@property(nonatomic, strong) AVCaptureDeviceInput *videoCaptureDeviceInput;
@property(nonatomic, strong) AVCaptureMetadataOutput *metadataOutput;
@property(nonatomic, strong) id runtimeErrorHandlingObserver;
@property(nonatomic, assign) NSInteger presetCamera;
@property(nonatomic, strong) AVCaptureVideoPreviewLayer *previewLayer;
@property(nonatomic, strong) NSArray *barCodeTypes;
@property(nonatomic, strong) ABI19_0_0EXBarCodeScanner *camera;

- (AVCaptureDevice *)deviceWithMediaType:(NSString *)mediaType
                      preferringPosition:(AVCaptureDevicePosition)position;
- (void)initializeCaptureSessionInput:(NSString *)type;
- (void)startSession;
- (void)stopSession;

@end
