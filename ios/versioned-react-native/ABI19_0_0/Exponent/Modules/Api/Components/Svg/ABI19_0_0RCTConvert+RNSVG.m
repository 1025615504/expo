/**
 * Copyright (c) 2015-present, Horcrux.
 * All rights reserved.
 *
 * This source code is licensed under the MIT-style license found in the
 * LICENSE file in the root directory of this source tree.
 */

#import "ABI19_0_0RCTConvert+RNSVG.h"

#import "ABI19_0_0RNSVGPainterBrush.h"
#import "ABI19_0_0RNSVGSolidColorBrush.h"
#import <ReactABI19_0_0/ABI19_0_0RCTLog.h>
#import <ReactABI19_0_0/ABI19_0_0RCTFont.h>

@implementation ABI19_0_0RCTConvert (ABI19_0_0RNSVG)

ABI19_0_0RCT_ENUM_CONVERTER(ABI19_0_0RNSVGCGFCRule, (@{
                                     @"evenodd": @(kRNSVGCGFCRuleEvenodd),
                                     @"nonzero": @(kRNSVGCGFCRuleNonzero),
                                     }), kRNSVGCGFCRuleNonzero, intValue)

ABI19_0_0RCT_ENUM_CONVERTER(ABI19_0_0RNSVGVBMOS, (@{
                                  @"meet": @(kRNSVGVBMOSMeet),
                                  @"slice": @(kRNSVGVBMOSSlice),
                                  @"none": @(kRNSVGVBMOSNone)
                                  }), kRNSVGVBMOSMeet, intValue)

ABI19_0_0RCT_ENUM_CONVERTER(ABI19_0_0RNSVGTextAnchor, (@{
                                        @"auto": @(kRNSVGTextAnchorAuto),
                                        @"start": @(kRNSVGTextAnchorStart),
                                        @"middle": @(kRNSVGTextAnchorMiddle),
                                        @"end": @(kRNSVGTextAnchorEnd)
                                       }), kRNSVGTextAnchorAuto, intValue)

ABI19_0_0RCT_ENUM_CONVERTER(ABI19_0_0RNSVGUnits, (@{
                                     @"objectBoundingBox": @(kRNSVGUnitsObjectBoundingBox),
                                     @"userSpaceOnUse": @(kRNSVGUnitsUserSpaceOnUse),
                                     }), kRNSVGUnitsObjectBoundingBox, intValue)

+ (ABI19_0_0RNSVGCGFloatArray)ABI19_0_0RNSVGCGFloatArray:(id)json
{
    NSArray *arr = [self NSNumberArray:json];
    NSUInteger count = arr.count;

    ABI19_0_0RNSVGCGFloatArray array;
    array.count = count;
    array.array = nil;

    if (count) {
        // Ideally, these arrays should already use the same memory layout.
        // In that case we shouldn't need this new malloc.
        array.array = malloc(sizeof(CGFloat) * count);
        for (NSUInteger i = 0; i < count; i++) {
            array.array[i] = [arr[i] doubleValue];
        }
    }

    return array;
}

+ (ABI19_0_0RNSVGBrush *)ABI19_0_0RNSVGBrush:(id)json
{
    NSArray *arr = [self NSArray:json];
    NSUInteger type = [self NSUInteger:arr.firstObject];

    switch (type) {
        case 0: // solid color
            // These are probably expensive allocations since it's often the same value.
            // We should memoize colors but look ups may be just as expensive.
            return [[ABI19_0_0RNSVGSolidColorBrush alloc] initWithArray:arr];
        case 1: // brush
            return [[ABI19_0_0RNSVGPainterBrush alloc] initWithArray:arr];
        default:
            ABI19_0_0RCTLogError(@"Unknown brush type: %zd", type);
            return nil;
    }
}

+ (ABI19_0_0RNSVGPathParser *)ABI19_0_0RNSVGCGPath:(NSString *)d
{
    return [[ABI19_0_0RNSVGPathParser alloc] initWithPathString: d];
}

+ (CGRect)ABI19_0_0RNSVGCGRect:(id)json offset:(NSUInteger)offset
{
    NSArray *arr = [self NSArray:json];
    if (arr.count < offset + 4) {
        ABI19_0_0RCTLogError(@"Too few elements in array (expected at least %zd): %@", 4 + offset, arr);
        return CGRectZero;
    }
    return (CGRect){
        {[self CGFloat:arr[offset]], [self CGFloat:arr[offset + 1]]},
        {[self CGFloat:arr[offset + 2]], [self CGFloat:arr[offset + 3]]},
    };
}

+ (CGColorRef)ABI19_0_0RNSVGCGColor:(id)json offset:(NSUInteger)offset
{
    NSArray *arr = [self NSArray:json];
    if (arr.count < offset + 4) {
        ABI19_0_0RCTLogError(@"Too few elements in array (expected at least %zd): %@", 4 + offset, arr);
        return nil;
    }
    return [self CGColor:[arr subarrayWithRange:(NSRange){offset, 4}]];
}

+ (CGGradientRef)ABI19_0_0RNSVGCGGradient:(id)json offset:(NSUInteger)offset
{
    NSArray *arr = [self NSArray:json];
    if (arr.count < offset) {
        ABI19_0_0RCTLogError(@"Too few elements in array (expected at least %zd): %@", offset, arr);
        return nil;
    }
    arr = [arr subarrayWithRange:(NSRange){offset, arr.count - offset}];
    ABI19_0_0RNSVGCGFloatArray colorsAndOffsets = [self ABI19_0_0RNSVGCGFloatArray:arr];
    size_t stops = colorsAndOffsets.count / 5;
    CGColorSpaceRef rgb = CGColorSpaceCreateDeviceRGB();


    CGGradientRef gradient = CGGradientCreateWithColorComponents(
                                                                 rgb,
                                                                 colorsAndOffsets.array,
                                                                 colorsAndOffsets.array + stops * 4,
                                                                 stops
                                                                 );

    CGColorSpaceRelease(rgb);
    free(colorsAndOffsets.array);
    return (CGGradientRef)CFAutorelease(gradient);
}

@end
