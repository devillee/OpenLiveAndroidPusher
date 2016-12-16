LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := librtmp
LOCAL_SRC_FILES := \
	src/amf.c \
	src/hashswf.c \
	src/log.c \
	src/parseurl.c \
	src/rtmp.c \
	xiecc_rtmp.c \
LOCAL_SRC_FILES += org_openlive_android_rtmp_RtmpClient.c
LOCAL_C_INCLUDES := $(LOCAL_PATH)/include
LOCAL_CFLAGS := -Wall -O2 -DSYS=posix -DNO_CRYPTO
LOCAL_LDLIBS := -llog -ljnigraphics -lz -landroid -lm -pthread -Wunused-value

include $(BUILD_SHARED_LIBRARY)
