/**
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
#pragma once

#include <ABI32_0_0fabric/ABI32_0_0components/view/ViewEventEmitter.h>

namespace facebook {
namespace ReactABI32_0_0 {

class ImageEventEmitter:
  public ViewEventEmitter {

public:
  using ViewEventEmitter::ViewEventEmitter;

  void onLoadStart() const;
  void onLoad() const;
  void onLoadEnd() const;
  void onProgress() const;
  void onError() const;
  void onPartialLoad() const;
};

} // namespace ReactABI32_0_0
} // namespace facebook
