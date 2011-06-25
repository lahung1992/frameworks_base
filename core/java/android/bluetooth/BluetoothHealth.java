/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.bluetooth;

import android.annotation.SdkConstant;
import android.content.Context;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Public API for Bluetooth Health Profile.
 *
 * <p>BluetoothHealth is a proxy object for controlling the Bluetooth
 * Service via IPC.
 *
 * <p> Use {@link BluetoothAdapter#getProfileProxy} to get
 * the BluetoothHealth proxy object. Use
 * {@link BluetoothAdapter#closeProfileProxy} to close the service connection.
 * @hide
 */
public final class BluetoothHealth implements BluetoothProfile {
    private static final String TAG = "BluetoothHealth";
    private static final boolean DBG = false;

    /**
     * Health Profile Source Role - the health device.
     */
    public static final int SOURCE_ROLE = 1 << 0;

    /**
     * Health Profile Sink Role the device talking to the health device.
     */
    public static final int SINK_ROLE = 1 << 1;

    /**
     * Health Profile - Channel Type used - Reliable
     */
    public static final int CHANNEL_TYPE_RELIABLE = 10;

    /**
     * Health Profile - Channel Type used - Streaming
     */
    public static final int CHANNEL_TYPE_STREAMING = 11;

    /**
     * @hide
     */
    public static final int CHANNEL_TYPE_ANY = 12;

    private final ArrayList<BluetoothHealthAppConfiguration> mAppConfigs =
        new ArrayList<BluetoothHealthAppConfiguration>();

    /**
     * Register an application configuration that acts as a Health SINK.
     * This is the configuration that will be used to communicate with health devices
     * which will act as the {@link #SOURCE_ROLE}. This is an asynchronous call and so
     * the callback is used to notify success or failure if the function returns true.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @param name The friendly name associated with the application or configuration.
     * @param dataType The dataType of the Source role of Health Profile to which
     *                   the sink wants to connect to.
     * @param callback A callback to indicate success or failure of the registration and
     *               all operations done on this application configuration.
     * @return If true, callback will be called.
     */
    public boolean registerSinkAppConfiguration(String name, int dataType,
            IBluetoothHealthCallback callback) {
        if (!isEnabled() || name == null) return false;

        if (DBG) log("registerSinkApplication(" + name + ":" + dataType + ")");
        return registerAppConfiguration(name, dataType, SINK_ROLE,
                CHANNEL_TYPE_ANY, callback);
    }

    /**
     * Register an application configuration that acts as a Health SINK or in a Health
     * SOURCE role.This is an asynchronous call and so
     * the callback is used to notify success or failure if the function returns true.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @param name The friendly name associated with the application or configuration.
     * @param dataType The dataType of the Source role of Health Profile.
     * @param channelType The channel type. Will be one of
     *                              {@link #CHANNEL_TYPE_RELIABLE}  or
     *                              {@link #CHANNEL_TYPE_STREAMING}
     * @param callback - A callback to indicate success or failure.
     * @return If true, callback will be called.
     * @hide
     */
    public boolean registerAppConfiguration(String name, int dataType, int role,
            int channelType, IBluetoothHealthCallback callback) {
        boolean result = false;
        if (!isEnabled() || !checkAppParam(name, role, channelType, callback)) return result;

        if (DBG) log("registerApplication(" + name + ":" + dataType + ")");
        BluetoothHealthAppConfiguration config =
                new BluetoothHealthAppConfiguration(name, dataType, role, channelType,
                callback);

        if (mService != null) {
            try {
                result = mService.registerAppConfiguration(config);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) Log.d(TAG, Log.getStackTraceString(new Throwable()));
        }

        if (result) mAppConfigs.add(config);
        return result;
    }

    /**
     * Unregister an application configuration that has been registered using
     * {@link #registerSinkAppConfiguration}
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @param config  The health app configuration
     * @return Success or failure.
     * @hide
     */
    public boolean unregisterAppConfiguration(BluetoothHealthAppConfiguration config) {
        boolean result = false;
        if (mService != null && isEnabled() && isValidAppConfig(config)) {
            try {
                result = mService.unregisterAppConfiguration(config);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) Log.d(TAG, Log.getStackTraceString(new Throwable()));
        }
        if (result) mAppConfigs.remove(config);
        return result;
    }

    /**
     * Connect to a health device which has the {@link #SOURCE_ROLE}.
     * This is an asynchrnous call. If this function returns true, the callback
     * associated with the application configuration will be called.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @param device The remote Bluetooth device.
     * @param config The application configuration which has been registed using
     *        {@link #registerSinkAppConfiguration(String, int, IBluetoothHealthCallback) }
     * @return If true, the callback associated with the application config will be called.
     */
    public boolean connectChannelToSource(BluetoothDevice device,
            BluetoothHealthAppConfiguration config) {
        if (mService != null && isEnabled() && isValidDevice(device) &&
                isValidAppConfig(config)) {
            try {
                return mService.connectChannelToSource(device, config);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) Log.d(TAG, Log.getStackTraceString(new Throwable()));
        }
        return false;
    }

    /**
     * Connect to a health device which has the {@link #SINK_ROLE}.
     * This is an asynchronous call. If this function returns true, the callback
     * associated with the application configuration will be called.
     *
     *<p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @param device The remote Bluetooth device.
     * @param config The application configuration which has been registed using
     *        {@link #registerSinkAppConfiguration(String, int, IBluetoothHealthCallback) }
     * @return If true, the callback associated with the application config will be called.
     * @hide
     */
    public boolean connectChannelToSink(BluetoothDevice device,
            BluetoothHealthAppConfiguration config, int channelType) {
        if (mService != null && isEnabled() && isValidDevice(device) &&
                isValidAppConfig(config)) {
            try {
                return mService.connectChannelToSink(device, config, channelType);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) Log.d(TAG, Log.getStackTraceString(new Throwable()));
        }
        return false;
    }

    /**
     * Disconnect a connected health channel.
     * This is an asynchronous call. If this function returns true, the callback
     * associated with the application configuration will be called.
     *
     *<p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @param device The remote Bluetooth device.
     * @param config The application configuration which has been registed using
     *        {@link #registerSinkAppConfiguration(String, int, IBluetoothHealthCallback) }
     * @param fd The file descriptor that was associated with the channel.
     * @return If true, the callback associated with the application config will be called.
     * @hide
     */
    public boolean disconnectChannel(BluetoothDevice device,
            BluetoothHealthAppConfiguration config, ParcelFileDescriptor fd) {
        if (mService != null && isEnabled() && isValidDevice(device) &&
                isValidAppConfig(config)) {
            try {
                return mService.disconnectChannel(device, config, fd);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) Log.d(TAG, Log.getStackTraceString(new Throwable()));
        }
        return false;
    }

    /**
     * Get the file descriptor of the main channel associated with the remote device
     * and application configuration.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * @param device The remote Bluetooth health device
     * @param config The application configuration
     * @return null on failure, ParcelFileDescriptor on success.
     */

    public ParcelFileDescriptor getMainChannelFd(BluetoothDevice device,
            BluetoothHealthAppConfiguration config) {
        if (mService != null && isEnabled() && isValidDevice(device) &&
                isValidAppConfig(config)) {
            try {
                return mService.getMainChannelFd(device, config);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) Log.d(TAG, Log.getStackTraceString(new Throwable()));
        }
        return null;
    }

    /**
     * Get the current connection state of the profile.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * This is not specific to any application configuration but represents the connection
     * state of the local Bluetooth adapter with the remote device. This can be used
     * by applications like status bar which would just like to know the state of the
     * local adapter.
     *
     * @param device Remote bluetooth device.
     * @return State of the profile connection. One of
     *               {@link #STATE_CONNECTED}, {@link #STATE_CONNECTING},
     *               {@link #STATE_DISCONNECTED}, {@link #STATE_DISCONNECTING}
     */
    public int getConnectionState(BluetoothDevice device) {
        if (mService != null && isEnabled() && isValidDevice(device)) {
            try {
                return mService.getHealthDeviceConnectionState(device);
            } catch (RemoteException e) {
                Log.e(TAG, e.toString());
            }
        } else {
            Log.w(TAG, "Proxy not attached to service");
            if (DBG) Log.d(TAG, Log.getStackTraceString(new Throwable()));
        }
        return STATE_DISCONNECTED;
    }

    /**
     * Get connected devices for this specific profile.
     *
     * <p> Return the set of devices which are in state {@link #STATE_CONNECTED}
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     *
     * This is not specific to any application configuration but represents the connection
     * state of the local Bluetooth adapter for this profile. This can be used
     * by applications like status bar which would just like to know the state of the
     * local adapter.
     * @return List of devices. The list will be empty on error.
     */
    public List<BluetoothDevice> getConnectedDevices() {
        if (mService != null && isEnabled()) {
            try {
                return mService.getConnectedHealthDevices();
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return new ArrayList<BluetoothDevice>();
            }
        }
        if (mService == null) Log.w(TAG, "Proxy not attached to service");
        return new ArrayList<BluetoothDevice>();
    }

    /**
     * Get a list of devices that match any of the given connection
     * states.
     *
     * <p> If none of the devices match any of the given states,
     * an empty list will be returned.
     *
     * <p>Requires {@link android.Manifest.permission#BLUETOOTH} permission.
     * This is not specific to any application configuration but represents the connection
     * state of the local Bluetooth adapter for this profile. This can be used
     * by applications like status bar which would just like to know the state of the
     * local adapter.
     *
     * @param states Array of states. States can be one of
     *              {@link #STATE_CONNECTED}, {@link #STATE_CONNECTING},
     *              {@link #STATE_DISCONNECTED}, {@link #STATE_DISCONNECTING},
     * @return List of devices. The list will be empty on error.
     */
    public List<BluetoothDevice> getDevicesMatchingConnectionStates(int[] states) {
        if (mService != null && isEnabled()) {
            try {
                return mService.getHealthDevicesMatchingConnectionStates(states);
            } catch (RemoteException e) {
                Log.e(TAG, "Stack:" + Log.getStackTraceString(new Throwable()));
                return new ArrayList<BluetoothDevice>();
            }
        }
        if (mService == null) Log.w(TAG, "Proxy not attached to service");
        return new ArrayList<BluetoothDevice>();
    }

     /** Health Channel Connection State - Disconnected */
    public static final int STATE_CHANNEL_DISCONNECTED  = 0;
    /** Health Channel Connection State - Connecting */
    public static final int STATE_CHANNEL_CONNECTING    = 1;
    /** Health Channel Connection State - Connected */
    public static final int STATE_CHANNEL_CONNECTED     = 2;
    /** Health Channel Connection State - Disconnecting */
    public static final int STATE_CHANNEL_DISCONNECTING = 3;

    /** Health App Configuration registration success */
    public static final int APPLICATION_REGISTRATION_SUCCESS = 0;
    /** Health App Configuration registration failure */
    public static final int APPLICATION_REGISTRATION_FAILURE = 1;
    /** Health App Configuration un-registration success */
    public static final int APPLICATION_UNREGISTRATION_SUCCESS = 2;
    /** Health App Configuration un-registration failure */
    public static final int APPLICATION_UNREGISTRATION_FAILURE = 3;

    private Context mContext;
    private ServiceListener mServiceListener;
    private IBluetooth mService;
    BluetoothAdapter mAdapter;

    /**
     * Create a BluetoothHealth proxy object.
     */
    /*package*/ BluetoothHealth(Context mContext, ServiceListener l) {
        IBinder b = ServiceManager.getService(BluetoothAdapter.BLUETOOTH_SERVICE);
        mServiceListener = l;
        mAdapter = BluetoothAdapter.getDefaultAdapter();
        if (b != null) {
            mService = IBluetooth.Stub.asInterface(b);
            if (mServiceListener != null) {
                mServiceListener.onServiceConnected(BluetoothProfile.HEALTH, this);
            }
        } else {
            Log.w(TAG, "Bluetooth Service not available!");

            // Instead of throwing an exception which prevents people from going
            // into Wireless settings in the emulator. Let it crash later when it is actually used.
            mService = null;
        }
    }

    private boolean isEnabled() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();

        if (adapter != null && adapter.getState() == BluetoothAdapter.STATE_ON) return true;
        log("Bluetooth is Not enabled");
        return false;
    }

    private boolean isValidDevice(BluetoothDevice device) {
        if (device == null) return false;

        if (BluetoothAdapter.checkBluetoothAddress(device.getAddress())) return true;
        return false;
    }

    private boolean isValidAppConfig(BluetoothHealthAppConfiguration config) {
        if (!mAppConfigs.isEmpty() && mAppConfigs.contains(config)) return true;
        log("Not a valid config: " + config);
        return false;
    }

    private boolean checkAppParam(String name, int role, int channelType,
            IBluetoothHealthCallback callback) {
        if (name == null || (role != SOURCE_ROLE && role != SINK_ROLE) ||
                (channelType != CHANNEL_TYPE_RELIABLE &&
                channelType != CHANNEL_TYPE_STREAMING &&
                channelType != CHANNEL_TYPE_ANY) || callback == null) {
            return false;
        }
        if (role == SOURCE_ROLE && channelType == CHANNEL_TYPE_ANY) return false;
        return true;
    }

    private static void log(String msg) {
        Log.d(TAG, msg);
    }
}