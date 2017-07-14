/**
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

#import <UIKit/UIKit.h>

#import <ReactABI19_0_0/ABI19_0_0RCTViewControllerProtocol.h>

@class ABI19_0_0RCTNavItem;
@class ABI19_0_0RCTWrapperViewController;

@protocol ABI19_0_0RCTWrapperViewControllerNavigationListener <NSObject>

- (void)wrapperViewController:(ABI19_0_0RCTWrapperViewController *)wrapperViewController
didMoveToNavigationController:(UINavigationController *)navigationController;

@end

@interface ABI19_0_0RCTWrapperViewController : UIViewController <ABI19_0_0RCTViewControllerProtocol>

- (instancetype)initWithContentView:(UIView *)contentView NS_DESIGNATED_INITIALIZER;
- (instancetype)initWithNavItem:(ABI19_0_0RCTNavItem *)navItem;

@property (nonatomic, weak) id<ABI19_0_0RCTWrapperViewControllerNavigationListener> navigationListener;
@property (nonatomic, strong) ABI19_0_0RCTNavItem *navItem;

@end
