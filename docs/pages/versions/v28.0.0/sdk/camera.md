---
title: Camera
---

A React component that renders a preview for the device's either front or back camera. Camera's parameters like zoom, auto focus, white balance and flash mode are adjustable. With use of `Camera` one can also take photos and record videos that are saved to the app's cache. Morever, the component is also capable of detecting faces and bar codes appearing on the preview.

> **Note**: Only one active Camera preview is supported currently. When using navigation, the best practice is to unmount previously rendered `Camera` component so next screens can use camera without issues.
<br/>

> **Note**: Android devices can use one of two available Camera apis underneath. This was previously chosen automatically, based on the device's Android system version and camera hardware capabilities. As we experienced some issues with Android's Camera2 API, we decided to choose the older API as a default. However, using the newer one is still possible through setting `useCamera2Api` prop to true. The change we made should be barely visible - the only thing that is not supported using the old Android's API is setting focus depth.
<br/>

> **Note**: The Camera API will not work on simulators or emulators.

Requires `Permissions.CAMERA`. Video recording requires `Permissions.AUDIO_RECORDING`.

### Basic Example

```javascript
import React from 'react';
import { Text, View, TouchableOpacity } from 'react-native';
import { Camera, Permissions } from 'expo';

export default class CameraExample extends React.Component {
  state = {
    hasCameraPermission: null,
    type: Camera.Constants.Type.back,
  };

  async componentWillMount() {
    const { status } = await Permissions.askAsync(Permissions.CAMERA);
    this.setState({ hasCameraPermission: status === 'granted' });
  }

  render() {
    const { hasCameraPermission } = this.state;
    if (hasCameraPermission === null) {
      return <View />;
    } else if (hasCameraPermission === false) {
      return <Text>No access to camera</Text>;
    } else {
      return (
        <View style={{ flex: 1 }}>
          <Camera style={{ flex: 1 }} type={this.state.type}>
            <View
              style={{
                flex: 1,
                backgroundColor: 'transparent',
                flexDirection: 'row',
              }}>
              <TouchableOpacity
                style={{
                  flex: 0.1,
                  alignSelf: 'flex-end',
                  alignItems: 'center',
                }}
                onPress={() => {
                  this.setState({
                    type: this.state.type === Camera.Constants.Type.back
                      ? Camera.Constants.Type.front
                      : Camera.Constants.Type.back,
                  });
                }}>
                <Text
                  style={{ fontSize: 18, marginBottom: 10, color: 'white' }}>
                  {' '}Flip{' '}
                </Text>
              </TouchableOpacity>
            </View>
          </Camera>
        </View>
      );
    }
  }
}
```

### Comprehensive Example

Check out a full example at [expo/camerja](https://github.com/expo/camerja). You can try it with Expo at [@community/camerja](https://expo.io/@community/camerja).

### props

- **type**

Camera facing. Use one of `Camera.Constants.Type`. When `Type.front`, use the front-facing camera. When `Type.back`, use the back-facing camera. Default: `Type.back`.

- **flashMode**

Camera flash mode. Use one of `Camera.Constants.FlashMode`. When `on`, the flash on your device will turn on when taking a picture, when `off`, it won't. Setting to `auto` will fire flash if required, `torch` turns on flash during the preview. Default: `off`.

- **autoFocus**

State of camera auto focus. Use one of `Camera.Constants.AutoFocus`. When `on`, auto focus will be enabled, when `off`, it wont't and focus will lock as it was in the moment of change but it can be adjusted on some devices via `focusDepth` prop.

- **zoom** (_float_)

A value between 0 and 1 being a percentage of device's max zoom. 0 - not zoomed, 1 - maximum zoom. Default: 0.

- **whiteBalance**

Camera white balance. Use one of `Camera.Constants.WhiteBalance`: `auto`, `sunny`, `cloudy`, `shadow`, `fluorescent`, `incandescent`. If a device does not support any of these values previous one is used.

- **focusDepth** (_float_)

Distance to plane of sharpest focus. A value between 0 and 1: 0 - infinity focus, 1 - focus as close as possible. Default: 0. For Android this is available only for some devices and when `useCamera2Api` is set to true.

- **ratio** (_string_)

Android only. A string representing aspect ratio of the preview, eg. `4:3`, `16:9`, `1:1`. To check if a ratio is supported by the device use `getSupportedRatiosAsync`. Default: `4:3`.

- **pictureSize** (_string_)

A string representing the size of pictures `takePictureAsync` will take. Available sizes can be fetched with `getAvailablePictureSizesAsync`.

- **onCameraReady** (_function_)

Callback invoked when camera preview has been set.

- **onFacesDetected** (_function_)

Callback invoked with results of face detection on the preview. It will receive an object containing:

- **faces** (_array_) - array of faces objects:
  - **faceID (_number_)** -- a face identifier (used for tracking, if the same face appears on consecutive frames it will have the same `faceID`).
  - **bounds (_object_)** -- an object containing:
    - **origin (`{ x: number, y: number }`)** -- position of the top left corner of a square containing the face in view coordinates,
    - **size (`{ width: number, height: number }`)** -- size of the square containing the face in view coordinates,
  - **rollAngle (_number_)** -- roll angle of the face (bank),
  - **yawAngle (_number_)** -- yaw angle of the face (heading, turning head left or right),
  - **smilingProbability (_number_)** -- probability that the face is smiling,
  - **leftEarPosition (`{ x: number, y: number}`)** -- position of the left ear in view coordinates,
  - **rightEarPosition (`{ x: number, y: number}`)** -- position of the right ear in view coordinates,
  - **leftEyePosition (`{ x: number, y: number}`)** -- position of the left eye in view coordinates,
  - **leftEyeOpenProbability (_number_)** -- probability that the left eye is open,
  - **rightEyePosition (`{ x: number, y: number}`)** -- position of the right eye in view coordinates,
  - **rightEyeOpenProbability (_number_)** -- probability that the right eye is open,
  - **leftCheekPosition (`{ x: number, y: number}`)** -- position of the left cheek in view coordinates,
  - **rightCheekPosition (`{ x: number, y: number}`)** -- position of the right cheek in view coordinates,
  - **mouthPosition (`{ x: number, y: number}`)** -- position of the center of the mouth in view coordinates,
  - **leftMouthPosition (`{ x: number, y: number}`)** -- position of the left edge of the mouth in view coordinates,
  - **rightMouthPosition (`{ x: number, y: number}`)** -- position of the right edge of the mouth in view coordinates,
  - **noseBasePosition (`{ x: number, y: number}`)** -- position of the nose base in view coordinates.

`smilingProbability`, `leftEyeOpenProbability` and `rightEyeOpenProbability` are returned only if `faceDetectionClassifications` property is set to `.all`.

Positions of face landmarks are returned only if `faceDetectionLandmarks` property is set to `.all`.

See also `FaceDetector` component.

- **faceDetectionMode** (_Camera.Constants.FaceDetection.Mode_)

Mode of the face detection. Use one of `Camera.Constants.FaceDetection.Mode.{fast, accurate}`.

- **faceDetectionLandmarks** (_Camera.Constants.FaceDetection.Landmarks_)

Whether to detect landmarks on the faces. Use one of `Camera.Constants.FaceDetection.Landmarks.{all, none}`. See FaceDetector documentation for details.

- **faceDetectionClassifications** (_Camera.Constants.FaceDetection.Classifications_)

Whether to run additional classifications on the faces. Use one of `Camera.Constants.FaceDetection.Classifications.{all, none}`. See FaceDetector documentation for details.

- **onMountError** (_function_)

Callback invoked when camera preview could not been started. It is provided with an error object that contains a `message`.

- **onBarCodeRead (_function_)**

Callback that is invoked when a bar code has been successfully read. The callback is provided with an Object of the shape `{ type: string, data: string }`, where the type refers to the bar code type that was scanned and the data is the information encoded in the bar code (in this case of QR codes, this is often a URL)

- **barCodeTypes (_Array\<string\>_)**

An array of bar code types. Usage: `Camera.Constants.BarCodeType.<codeType>` where `codeType` is one of the listed above. Default: all supported bar code types. For example: `barCodeTypes={[Camera.Constants.BarCodeType.qr]}`

- **useCamera2Api** (_boolean_)

Android only. Whether to use Android's Camera2 API. See `Note` at the top of this page.

## Methods

To use methods that Camera exposes one has to create a components `ref` and invoke them using it.

```javascript
// ...
<Camera ref={ref => { this.camera = ref; }} />
// ...
snap = async () => {
  if (this.camera) {
    let photo = await this.camera.takePictureAsync();
  }
};
```

### `takePictureAsync`

Takes a picture and saves it to app's cache directory. Photos are rotated to match device's orientation and scaled to match the preview. Anyway on Android it is essential to set `ratio` prop to get a picture with correct dimensions.

#### Arguments

-   **options (_object_)** --

      A map of options:

    -   **quality (_number_)** -- Specify the quality of compression, from 0 to 1. 0 means compress for small size, 1 means compress for maximum quality.
    -   **base64 (_boolean_)** -- Whether to also include the image data in Base64 format.
    -   **exif (_boolean_)** -- Whether to also include the EXIF data for the image.
    -   **onPictureSaved (_function_)** -- A callback invoked when picture is saved. If set, the promise of this method will resolve immediately with no data after picture is captured. The data that it should contain will be passed to this callback. If displaying or processing a captured photo right after taking it is not your case, this callback lets you skip waiting for it to be saved.

#### Returns

Returns a Promise that resolves to an object: `{ uri, width, height, exif, base64 }` where `uri` is a URI to the local image file (useable as the source for an `Image` element) and `width, height` specify the dimensions of the image. `base64` is included if the `base64` option was truthy, and is a string containing the JPEG data of the image in Base64--prepend that with `'data:image/jpg;base64,'` to get a data URI, which you can use as the source for an `Image` element for example. `exif` is included if the `exif` option was truthy, and is an object containing EXIF data for the image--the names of its properties are EXIF tags and their values are the values for those tags.

The local image URI is temporary. Use [`Expo.FileSystem.copyAsync`](../filesystem/#expofilesystemcopyasyncoptions) to make a permanent copy of the image.

### `recordAsync`

Starts recording a video that will be saved to cache directory. Videos are rotated to match device's orientation. Flipping camera during a recording results in stopping it.

#### Arguments

-   **options (_object_)** --

      A map of options:

    -   **quality (_VideoQuality_)** -- Specify the quality of recorded video. Usage: `Camera.Constants.VideoQuality['<value>']`, possible values: for 16:9 resolution `2160p`, `1080p`, `720p`, `480p` : `Android only` and for 4:3 `4:3` (the size is 640x480). If the chosen quality is not available for a device, the highest available is chosen.
    -   **maxDuration (_number_)** -- Maximum video duration in seconds.
    -   **maxFileSize (_number_)** -- Maximum video file size in bytes.
    -   **mute (_boolean_)** -- If present, video will be recorded with no sound.

#### Returns

Returns a Promise that resolves to an object containing video file `uri` property. The Promise is returned if `stopRecording` was invoked, one of `maxDuration` and `maxFileSize` is reached or camera preview is stopped.

### `stopRecording`

Stops recording if any is in progress.

### `getSupportedRatiosAsync`

Android only. Get aspect ratios that are supported by the device and can be passed via `ratio` prop.

#### Returns

Returns a Promise that resolves to an array of strings representing ratios, eg. `['4:3', '1:1']`.

### `getAvailablePictureSizesAsync`

Get picture sizes that are supported by the device for given `ratio`.

#### Arguments

-   **ratio (_string_)** -- A string representing aspect ratio of sizes to be returned.

#### Returns

Returns a Promise that resolves to an array of strings representing picture sizes that can be passed to `pictureSize` prop. The list varies across Android devices but is the same for every iOS.

### `pausePreview`

Pauses the camera preview. It is not recommended to use `takePictureAsync` when preview is paused.

### `resumePreview`

Resumes the camera preview.

# Supported bar code formats

| Bar code format | iOS  | Android |
| --------------- | ---- | ------- |
| aztec           | Yes  | Yes     |
| codabar         | No   | Yes     |
| code39          | Yes  | Yes     |
| code93          | Yes  | Yes     |
| code128         | Yes  | Yes     |
| code138         | Yes  | No      |
| code39mod43     | Yes  | No      |
| datamatrix      | Yes  | Yes     |
| ean13           | Yes  | Yes     |
| ean8            | Yes  | Yes     |
| interleaved2of5 | Yes  | No      |
| itf14           | Yes* | Yes     |
| maxicode        | No   | Yes     |
| pdf417          | Yes  | Yes     |
| rss14           | No   | Yes     |
| rssexpanded     | No   | Yes     |
| upc_a           | No   | Yes     |
| upc_e           | Yes  | Yes     |
| upc_ean         | No   | Yes     |
| qr              | Yes  | Yes     |

* sometimes when an ITF-14 barcode is recognized it's type is set to `interleaved2of5`.

#