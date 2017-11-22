#import "ABI21_0_0EXBannerViewManager.h"
#import "ABI21_0_0EXBannerView.h"
#import "ABI21_0_0EXFacebook.h"

@implementation ABI21_0_0EXBannerViewManager

ABI21_0_0RCT_EXPORT_MODULE(CTKBannerViewManager)

@synthesize bridge = _bridge;

- (UIView *)view
{
  if (![ABI21_0_0EXFacebook facebookAppIdFromNSBundle]) {
    ABI21_0_0RCTLogWarn(@"No Facebook app id is specified. Facebook ads may have undefined behavior.");
  }
  return [ABI21_0_0EXBannerView new];
}

ABI21_0_0RCT_EXPORT_VIEW_PROPERTY(size, NSNumber)
ABI21_0_0RCT_EXPORT_VIEW_PROPERTY(placementId, NSString)
ABI21_0_0RCT_EXPORT_VIEW_PROPERTY(onAdPress, ABI21_0_0RCTBubblingEventBlock)
ABI21_0_0RCT_EXPORT_VIEW_PROPERTY(onAdError, ABI21_0_0RCTBubblingEventBlock)

@end
