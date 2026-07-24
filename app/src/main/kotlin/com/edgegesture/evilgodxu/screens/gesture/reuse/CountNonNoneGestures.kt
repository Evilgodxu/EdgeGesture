package com.edgegesture.evilgodxu.screens.gesture.reuse

import com.edgegesture.evilgodxu.data.gesture.GestureAction
import com.edgegesture.evilgodxu.data.gesture.GestureSettingsState

// 统计所有非 NONE 的手势动作总数
fun countNonNoneGestures(settings: GestureSettingsState): Int {
    var count = 0
    with(settings.leftEdge) {
        if (swipeRight != GestureAction.NONE) count++
        if (swipeRightLong != GestureAction.NONE) count++
        if (swipeUp != GestureAction.NONE) count++
        if (swipeUpLong != GestureAction.NONE) count++
        if (swipeDown != GestureAction.NONE) count++
        if (swipeDownLong != GestureAction.NONE) count++
    }
    if (settings.leftSegmentCount >= 2) {
        with(settings.leftEdgeSegment2) {
            if (swipeRight != GestureAction.NONE) count++
            if (swipeRightLong != GestureAction.NONE) count++
            if (swipeUp != GestureAction.NONE) count++
            if (swipeUpLong != GestureAction.NONE) count++
            if (swipeDown != GestureAction.NONE) count++
            if (swipeDownLong != GestureAction.NONE) count++
        }
    }
    if (settings.leftSegmentCount >= 3) {
        with(settings.leftEdgeSegment3) {
            if (swipeRight != GestureAction.NONE) count++
            if (swipeRightLong != GestureAction.NONE) count++
            if (swipeUp != GestureAction.NONE) count++
            if (swipeUpLong != GestureAction.NONE) count++
            if (swipeDown != GestureAction.NONE) count++
            if (swipeDownLong != GestureAction.NONE) count++
        }
    }
    with(settings.rightEdge) {
        if (swipeLeft != GestureAction.NONE) count++
        if (swipeLeftLong != GestureAction.NONE) count++
        if (swipeUp != GestureAction.NONE) count++
        if (swipeUpLong != GestureAction.NONE) count++
        if (swipeDown != GestureAction.NONE) count++
        if (swipeDownLong != GestureAction.NONE) count++
    }
    if (settings.rightSegmentCount >= 2) {
        with(settings.rightEdgeSegment2) {
            if (swipeLeft != GestureAction.NONE) count++
            if (swipeLeftLong != GestureAction.NONE) count++
            if (swipeUp != GestureAction.NONE) count++
            if (swipeUpLong != GestureAction.NONE) count++
            if (swipeDown != GestureAction.NONE) count++
            if (swipeDownLong != GestureAction.NONE) count++
        }
    }
    if (settings.rightSegmentCount >= 3) {
        with(settings.rightEdgeSegment3) {
            if (swipeLeft != GestureAction.NONE) count++
            if (swipeLeftLong != GestureAction.NONE) count++
            if (swipeUp != GestureAction.NONE) count++
            if (swipeUpLong != GestureAction.NONE) count++
            if (swipeDown != GestureAction.NONE) count++
            if (swipeDownLong != GestureAction.NONE) count++
        }
    }
    with(settings.bottomEdge) {
        if (swipeUp != GestureAction.NONE) count++
        if (swipeUpLong != GestureAction.NONE) count++
        if (swipeLeft != GestureAction.NONE) count++
        if (swipeLeftLong != GestureAction.NONE) count++
        if (swipeRight != GestureAction.NONE) count++
        if (swipeRightLong != GestureAction.NONE) count++
    }
    if (settings.bottomSegmentCount >= 2) {
        with(settings.bottomEdgeSegment2) {
            if (swipeUp != GestureAction.NONE) count++
            if (swipeUpLong != GestureAction.NONE) count++
            if (swipeLeft != GestureAction.NONE) count++
            if (swipeLeftLong != GestureAction.NONE) count++
            if (swipeRight != GestureAction.NONE) count++
            if (swipeRightLong != GestureAction.NONE) count++
        }
    }
    if (settings.bottomSegmentCount >= 3) {
        with(settings.bottomEdgeSegment3) {
            if (swipeUp != GestureAction.NONE) count++
            if (swipeUpLong != GestureAction.NONE) count++
            if (swipeLeft != GestureAction.NONE) count++
            if (swipeLeftLong != GestureAction.NONE) count++
            if (swipeRight != GestureAction.NONE) count++
            if (swipeRightLong != GestureAction.NONE) count++
        }
    }
    return count
}
