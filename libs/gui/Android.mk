LOCAL_PATH:= $(call my-dir)
include $(CLEAR_VARS)

LOCAL_SRC_FILES:= \
	ISensorEventConnection.cpp \
	ISensorServer.cpp \
	ISurfaceTexture.cpp \
	Sensor.cpp \
	SensorChannel.cpp \
	SensorEventQueue.cpp \
	SensorManager.cpp \
	SurfaceTexture.cpp \
	SurfaceTextureClient.cpp \
	ISurfaceComposer.cpp \
	ISurface.cpp \
	ISurfaceComposerClient.cpp \
	IGraphicBufferAlloc.cpp \
	LayerState.cpp \
	Surface.cpp \
	SurfaceComposerClient.cpp \
	IMplSysConnection.cpp \
	IMplSysPedConnection.cpp \
	IMplConnection.cpp
	
LOCAL_SHARED_LIBRARIES := \
	libcutils \
	libutils \
	libbinder \
	libhardware \
	libhardware_legacy \
	libui \
	libEGL \
	libGLESv2 \
        libQcomUI

LOCAL_C_INCLUDES := hardware/qcom/display/libqcomui

ifeq ($(TARGET_QCOM_HDMI_OUT),true)
LOCAL_CFLAGS += -DQCOM_HDMI_OUT
endif

ifeq ($(TARGET_USES_TESTFRAMEWORK),true)
LOCAL_CFLAGS += -DGFX_TESTFRAMEWORK
LOCAL_SHARED_LIBRARIES += libtestframework
endif

LOCAL_MODULE:= libgui

ifeq ($(TARGET_BOARD_PLATFORM), tegra)
	LOCAL_CFLAGS += -DALLOW_DEQUEUE_CURRENT_BUFFER
endif

include $(BUILD_SHARED_LIBRARY)

ifeq (,$(ONE_SHOT_MAKEFILE))
include $(call first-makefiles-under,$(LOCAL_PATH))
endif
