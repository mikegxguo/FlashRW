LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_AAPT_FLAGS+= --auto-add-overlay \
                   --extra-packages mitacpavo
LOCAL_STATIC_JAVA_AAR_LIBRARIES:= mitacpavo

LOCAL_JAVA_LIBRARIES := bouncycastle framework#telephony-common
LOCAL_STATIC_JAVA_LIBRARIES := guava

LOCAL_MODULE_TAGS := optional

LOCAL_SRC_FILES := $(call all-java-files-under, src)

LOCAL_PACKAGE_NAME := FlashRW
LOCAL_CERTIFICATE := platform
LOCAL_DEX_PREOPT := false

#LOCAL_PROGUARD_FLAG_FILES := proguard.flags

include $(BUILD_PACKAGE)

# Use the folloing include to make our test apk.
include $(call all-makefiles-under,$(LOCAL_PATH))

# Introduce AAR library
include $(CLEAR_VARS)
LOCAL_PREBUILT_STATIC_JAVA_LIBRARIES := mitacpavo:libs/mitacpavolibs-1.05-release.aar
include $(BUILD_MULTI_PREBUILT)
