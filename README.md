# countdowntext
todo
public class BluetoothDeviceManager {
    private final static String TAG = BluetoothDeviceManager.class.getSimpleName();
    private final static UUID MY_UUID = UUID.randomUUID();
    private final static String NAME = "BluetoothDevice";
    private final Context context;
    private final BluetoothAdapter bluetoothAdapter;
    private final BroadcastReceiver broadcastReceiver;
    private BluetoothA2dp bluetoothA2dp;
    private BluetoothProfile bluetoothInput;
    private int isSystemApp;

    private HashMap<String, BondedDevice> myDevicesMap; //蓝牙设备的hardware address作为键
    private HashMap<String, UnboundDevice> otherDevicesMap;//蓝牙设备的hardware address作为键


    private BluetoothStateChangedListener listener;

    public BluetoothDeviceManager(Context context, BluetoothStateChangedListener listener) {
        isSystemApp = PackageInfoUtil.isSystemApp(context.getApplicationContext());
        this.listener = listener;
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothAdapter.getProfileProxy(context, mProfileServiceListener, BluetoothProfile.A2DP);
        bluetoothAdapter.getProfileProxy(context, mProfileServiceListener, 4);
        if (bluetoothAdapter == null) {
            throw new RuntimeException("Bluetooth is not supported!! Please call BluetoothDeviceManager.isSupportBluetooth() first before create object of BluetoothDeviceManager.");
        }
        broadcastReceiver = new BluetoothBroadcast();
        myDevicesMap = new HashMap<>(8);
        otherDevicesMap = new HashMap<>(16);
    }

    public boolean openBluetooth() {
        Log.e(TAG, "openBluetooth: " + bluetoothAdapter.isEnabled());
        if (!bluetoothAdapter.isEnabled()) { // 蓝牙未开启，则开启蓝牙
            if (!bluetoothAdapter.enable()) {
                Log.e(TAG, "openBluetooth: some problem prevent this action of turning on bluetooth");
                return false;
            }
        }
        return true;
    }

    public boolean closeBluetooth() {
        Log.e(TAG, "closeBluetooth: " + bluetoothAdapter.isEnabled());
        if (bluetoothAdapter.isEnabled()) {
            if (!bluetoothAdapter.disable()) {
                Log.e(TAG, "closeBluetooth: some problem prevent this action of turning off bluetooth");
                return false;
            }
        }
        return true;
    }

    public void startScan() {
        otherDevicesMap.clear();
//        devicesSet.clear();
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
        boolean b = bluetoothAdapter.startDiscovery();
    }

    public void stopScan() {
        //otherDevicesMap.clear();
//        devicesSet.clear();
        if (bluetoothAdapter.isDiscovering()) {
            bluetoothAdapter.cancelDiscovery();
        }
    }

    /**
     * 创建BluetoothDeviceManager，开始其他操作之前调用
     */
    public void start() {
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        intentFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        intentFilter.addAction(BluetoothDevice.ACTION_FOUND);
        intentFilter.addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        intentFilter.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED);//设备建立连接
        intentFilter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED); //设备断开连接
        intentFilter.addAction(BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED); //BluetoothA2dp连接状态
        intentFilter.addAction(BluetoothHidHost.ACTION_CONNECTION_STATE_CHANGED);
        intentFilter.setPriority(IntentFilter.SYSTEM_HIGH_PRIORITY);
        context.registerReceiver(broadcastReceiver, intentFilter);
        if (bluetoothAdapter.isEnabled()) {
            listener.onBluetoothOpened();
            setBluetoothDiscoverable();

        }
    }

    public void connect(BluetoothDevice device) {
        if (bluetoothAdapter.isDiscovering()){
            bluetoothAdapter.cancelDiscovery();
        }
        BluetoothProfile bluetoothProfile = getBluetoothProfile(device);
        if (bluetoothProfile != null) {
            Log.i(TAG, "connect device:" + device.getName());
            try {
                //得到BluetoothInputDevice然后反射connect连接设备
                Method method = bluetoothProfile.getClass().getMethod("connect",
                        BluetoothDevice.class);
                method.invoke(bluetoothProfile, device);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    public void disconnect(BluetoothDevice device) {
        if (bluetoothAdapter.isDiscovering()){
            bluetoothAdapter.cancelDiscovery();
        }
        BluetoothProfile bluetoothProfile = getBluetoothProfile(device);
        if (bluetoothProfile != null) {
            Log.i(TAG, "connect device:" + device.getName());
            try {
                //得到BluetoothInputDevice然后反射connect连接设备
                Method method = bluetoothProfile.getClass().getMethod("disconnect",
                        BluetoothDevice.class);
                method.invoke(bluetoothProfile, device);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void pair(BluetoothDevice device) {
        if (bluetoothAdapter.isDiscovering()){
            bluetoothAdapter.cancelDiscovery();
        }
        device.createBond();
    }

    public void refreshBondedDevice() {
        if (bluetoothAdapter.isEnabled()) {
            listener.onBondedListRefresh(getBondDevices());
        }
    }

    public void ignore(BluetoothDevice bluetoothDevice) {
        //取消配对removeBond是一个异步操作，需要去监听配对状态的改变，来更新UI
        try {
            Method m = bluetoothDevice.getClass().getMethod("removeBond");
            m.setAccessible(true);
            m.invoke(bluetoothDevice);
        } catch (Exception e) {
            Log.e(TAG, e.toString());
        }
    }


    /**
     * UI销毁时调用
     */
    public void quit() {
        bluetoothAdapter.cancelDiscovery();
        closeProfile();
        context.unregisterReceiver(broadcastReceiver);
    }


    /**
     * 判断设备是否支持蓝牙
     *
     * @return true支持，false不支持
     */
    public static boolean isSupportBluetooth() {
        return BluetoothAdapter.getDefaultAdapter() != null;
    }


    /**
     * 获取蓝牙的打开装填
     *
     * @return true打开，false关闭
     */
    public boolean isBluetoothOpen() {
        return bluetoothAdapter.isEnabled();
    }

    public boolean confirmPairKey(BluetoothDevice bluetoothDevice) {
        if (isSystemApp != 1) {
            Log.e(TAG, "confirmPairKey cannot use, since this is not a system app ");
            return false;
        }
        if (bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
            bluetoothDevice.setPairingConfirmation(true);
            return true;
        }
        return false;
    }

    /**
     * 点击配对框的取消按钮后，取消配对
     * @param device 蓝牙设备
     * @return 是否成功
     */
    public boolean cancelPairing(BluetoothDevice device){
        boolean res;
        try {
            Method cancelPairingUserInput = device.getClass().getMethod("cancelPairingUserInput");
            res = (boolean) cancelPairingUserInput.invoke(device);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
            res = false;
        }
        return res;
    }

    public boolean inputKey(BluetoothDevice bluetoothDevice, String key) {
        if (isSystemApp != 1) {
            Log.e(TAG, "confirmPairKey cannot use, since this is not a system app ");
            return false;
        }
        if (bluetoothDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
            byte[] pinBytes = convertPinToBytes(key);
            if (pinBytes == null) {
                return false;
            }
            bluetoothDevice.setPin(pinBytes);
            return true;
        }
        return false;
    }


    public interface BluetoothStateChangedListener {
        void onBluetoothOpened();

        void onBluetoothClosed();

        void onDiscoveryStarted();

        void onDiscoveryFinished();

        void onDeviceFound(UnboundDevice device);

        void onDeviceBondStateChange(UnboundDevice device, int state, int preState);

        void onDeviceConnectStateChange(BondedDevice device, int state, int preState);

        void onPairingType(int type, BluetoothDevice device, String key);

        void onPairingCancel();

        void onBondedListRefresh(ArrayList<BondedDevice> deviceSet);

    }

    private byte[] convertPinToBytes(String pin) {
        if (pin == null) {
            return null;
        }
        byte[] pinBytes;
        try {
            pinBytes = pin.getBytes("UTF-8");
        } catch (UnsupportedEncodingException uee) {
            Log.e(TAG, "UTF-8 not supported?!?");  // this should not happen
            return null;
        }
        if (pinBytes.length <= 0 || pinBytes.length > 16) {
            return null;
        }
        return pinBytes;
    }

//    private ArrayList<BondedDevice> createBondedDevices(Set<BluetoothDevice> set) {
//        ArrayList<BondedDevice> bondedDevices = new ArrayList<>();
//        for (BluetoothDevice bluetoothDevice : set) {
//            BondedDevice bondedDevice = createBondedDevice(bluetoothDevice);
//            myDevicesMap.put(bluetoothDevice.getAddress(), bondedDevice);
//            bondedDevices.add(bondedDevice);
//        }
//        return bondedDevices;
//    }

    private UnboundDevice createUnboundDevice(BluetoothDevice device) {
        UnboundDevice unboundDevice;
        BluetoothUtil.BluetoothType bluetoothType = BluetoothUtil.getBluetoothType(device.getBluetoothClass().getMajorDeviceClass(),
                device.getBluetoothClass().getDeviceClass());
        String name = device.getName();
        switch (bluetoothType){
            case HEADSET:
                unboundDevice = new HeadphoneDeviceHasNotBonded(name, Device.DeviceState.UN_PAIR, device);
                break;
            case KEYBOARD:
                unboundDevice = new KeyboardDeviceHasNotBonded(name, Device.DeviceState.UN_PAIR, device);
                break;
            case MOUSE:
                unboundDevice = new MouseDeviceHasNotBonded(name, Device.DeviceState.UN_PAIR, device);
                break;
            case PHONE:
                unboundDevice = new PhoneDeviceHasNotBonded(name, Device.DeviceState.UN_PAIR, device);
                break;
            case OTHER:
            default:
                unboundDevice = new UnknownTypeDeviceHasNotBonded(name, Device.DeviceState.UN_PAIR, device);
                break;
        }

        return unboundDevice;
    }

    private ArrayList<BondedDevice> getBondDevices(){
        Set<BluetoothDevice> bondedDevices = bluetoothAdapter.getBondedDevices();
        myDevicesMap.clear();
        for (BluetoothDevice bondedDevice : bondedDevices) {
            myDevicesMap.put( bondedDevice.getAddress(), createBondedDevice(bondedDevice));
        }
        return new ArrayList<BondedDevice>(myDevicesMap.values());
    }

    private BondedDevice createBondedDevice(BluetoothDevice device) {
        BondedDevice bondedDevice;
        BluetoothUtil.BluetoothType bluetoothType = BluetoothUtil.getBluetoothType(device.getBluetoothClass().getMajorDeviceClass(),
                device.getBluetoothClass().getDeviceClass());
        Device.DeviceState connectState = getDeviceState(device);
        String name = device.getName();
        switch (bluetoothType) {
            case KEYBOARD:
                bondedDevice = new KeyboardDeviceHasBonded(name, connectState, device);
                break;
            case MOUSE:
                bondedDevice = new MouseDeviceHasBonded(name, connectState, device);
                break;
            case HEADSET:
                bondedDevice = new HeadphoneDeviceHasBonded(name, connectState, device);
                break;
            case PHONE:
                bondedDevice = new PhoneDeviceHasBonded(name, connectState, device);
                break;
            case OTHER:
            default:
                bondedDevice = new UnknownTypeDeviceHasBonded(name, connectState, device);
                break;

        }
        Log.e(TAG, "createBondedDevice: " + bondedDevice.getClass().getSimpleName());
        return bondedDevice;
    }

    //已绑定的蓝牙设备：已连接、连接中、断开连接中，断开连接
    private Device.DeviceState getDeviceState(BluetoothDevice device) {
        Device.DeviceState connectState = Device.DeviceState.NONE;
        BluetoothUtil.BluetoothType bluetoothType = BluetoothUtil.getBluetoothType(device.getBluetoothClass().getMajorDeviceClass(),
                device.getBluetoothClass().getDeviceClass());
        switch (bluetoothType) {
            case MOUSE:
            case KEYBOARD:
                if (bluetoothInput != null) {
                    int connectionState = bluetoothInput.getConnectionState(device);
                    Log.e(TAG, "getDeviceState: " + device.getName() + " " + connectionState);
                    connectState = getDeviceConnectState(connectionState);
                }
                break;
            case HEADSET:
                if (bluetoothA2dp != null) {
                    int connectionState = bluetoothA2dp.getConnectionState(device);
                    connectState = getDeviceConnectState(connectionState);
                }
                break;
            case PHONE:
            case OTHER:
            default:
                break;
        }

        return connectState;
    }

    private Device.DeviceState getDeviceConnectState(int state) {
        if (state == BluetoothProfile.STATE_CONNECTED) {
            return Device.DeviceState.CONNECTED;
        } else if (state == BluetoothProfile.STATE_CONNECTING) {
            return Device.DeviceState.CONNECTING;
        } else if (state == STATE_DISCONNECTED) {
            return Device.DeviceState.DISCONNECTED;
        } else {
            return Device.DeviceState.NONE;
        }
    }

    private void setBluetoothDiscoverable() {
        Log.e(TAG, "setBluetoothDiscoverable");
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        try {
            Method setDiscoverableTimeout = BluetoothAdapter.class.getMethod("setDiscoverableTimeout", int.class);
            setDiscoverableTimeout.setAccessible(true);
            Method setScanMode = BluetoothAdapter.class.getMethod("setScanMode", int.class, int.class);
            setScanMode.setAccessible(true);

            setDiscoverableTimeout.invoke(adapter, 0);
            setScanMode.invoke(adapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE, 0);
        } catch (Exception e) {
            e.printStackTrace();

        }
    }

    /**
     * 根据device类型获取对应的profile
     * @param device 设备
     * @return bluetoothProfile
     */
    private BluetoothProfile getBluetoothProfile(BluetoothDevice device){
        BluetoothUtil.BluetoothType bluetoothType = BluetoothUtil.getBluetoothType(device.getBluetoothClass().getMajorDeviceClass(),
                device.getBluetoothClass().getDeviceClass());
        BluetoothProfile bluetoothProfile = null;
        switch (bluetoothType) {
            case MOUSE:
            case KEYBOARD:
                if (bluetoothInput != null) {
                    bluetoothProfile = bluetoothInput;
                }
                break;
            case HEADSET:
                if (bluetoothA2dp != null) {
                    bluetoothProfile = bluetoothA2dp;
                }
                break;
            case PHONE:
            case OTHER:
            default:
                break;
        }
        return bluetoothProfile;
    }

    private void setBluetoothUndiscovered() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        try {
            Method setDiscoverableTimeout = BluetoothAdapter.class.getMethod("setDiscoverableTimeout", int.class);
            setDiscoverableTimeout.setAccessible(true);
            Method setScanMode = BluetoothAdapter.class.getMethod("setScanMode", int.class, int.class);
            setScanMode.setAccessible(true);
            setDiscoverableTimeout.invoke(adapter, 1);
            setScanMode.invoke(adapter, BluetoothAdapter.SCAN_MODE_CONNECTABLE, 1);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void closeProfile() {
        if (bluetoothInput != null){
            bluetoothAdapter.closeProfileProxy(4, bluetoothInput);
        }
        if (bluetoothA2dp != null){
            bluetoothAdapter.closeProfileProxy(BluetoothProfile.A2DP, bluetoothA2dp);
        }
    }

    private class BluetoothBroadcast extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                Log.i(TAG, "onReceive: ACTION_STATE_CHANGED");
                Object o;
                Bundle extras = intent.getExtras();
                if (extras == null || (o = extras.get(BluetoothAdapter.EXTRA_STATE)) == null) {
                    return;
                }
                int state = (int) o;
                Log.e(TAG, "onReceive: BluetoothAdapter - " + state);
                if (BluetoothAdapter.STATE_ON == state && listener != null) {
                    setBluetoothDiscoverable();
                    listener.onBluetoothOpened();
                } else if (BluetoothAdapter.STATE_OFF == state && listener != null) {
                    listener.onBluetoothClosed();
                }

            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(intent.getAction())) {
                Log.i(TAG, "onReceive: ACTION_DISCOVERY_STARTED");
                listener.onDiscoveryStarted();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(intent.getAction())) {
                Log.i(TAG, "onReceive: ACTION_DISCOVERY_FINISHED");
                listener.onDiscoveryFinished();
            } else if (BluetoothDevice.ACTION_FOUND.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.i(TAG, "onReceive: ACTION_FOUND" + device.getName());
                if (device.getName() == null || otherDevicesMap.containsKey(device.getAddress()) || myDevicesMap.containsKey(device.getAddress())) return;
//                devicesSet.add(device);
                UnboundDevice unboundDevice = createUnboundDevice(device);
                otherDevicesMap.put(device.getAddress(), unboundDevice);
                listener.onDeviceFound(unboundDevice);
            } else if (BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(intent.getAction())) {
                BluetoothDevice device = (BluetoothDevice) intent.getExtras().get(BluetoothDevice.EXTRA_DEVICE);
                int state = (int) intent.getExtras().get(BluetoothDevice.EXTRA_BOND_STATE);
                int preState = (int) intent.getExtras().get(BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE);
                Log.i(TAG, "onReceive: ACTION_BOND_STATE_CHANGED " + device.getName() + ": " + preState +"-->" + state);
                UnboundDevice unboundDevice = otherDevicesMap.get(device.getAddress());
                //device为null说明当前改变配对状态的device之前已经配过对
                if (unboundDevice == null){
                    listener.onBondedListRefresh(getBondDevices());
                }else {
                    if (state == BluetoothDevice.BOND_BONDED){
                        otherDevicesMap.remove(device.getAddress());
                    }
                    listener.onDeviceBondStateChange(unboundDevice, state, preState);
                }
//                }
            } else if (BluetoothDevice.ACTION_PAIRING_REQUEST.equals(intent.getAction())) {
                if (isSystemApp != 1) return; //如果不是系统app，没有系统权限，则不主动处理配对消息，由系统机制直接处理
                abortBroadcast();
                Log.i(TAG, "onReceive: ACTION_PAIRING_REQUEST");
                Bundle extras = intent.getExtras();
                if (extras == null) return;

                int type = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_VARIANT,
                        BluetoothDevice.ERROR);
                BluetoothDevice o = (BluetoothDevice) intent.getExtras().get(BluetoothDevice.EXTRA_DEVICE);
                Log.e(TAG, "onReceive: device " + o.getName());
                int pairingKey = intent.getIntExtra(BluetoothDevice.EXTRA_PAIRING_KEY,
                        BluetoothDevice.ERROR);

                Log.e(TAG, "onReceive: type " + type);
                switch (type) {
                    case BluetoothDevice.ERROR:
                        break;
                    //提示用户输入pin码或passkey   ------秘钥接入 （Passkey Entry）
                    case BluetoothDevice.PAIRING_VARIANT_PIN:
                        listener.onPairingType(0, o, null);
                        break;
                    //提示用户确定显示在屏幕上的passkey   ------使用简单 （Just Works)
                    case BluetoothDevice.PAIRING_VARIANT_PASSKEY_CONFIRMATION:
                        listener.onPairingType(2, o, String.valueOf(pairingKey));
                        break;
                    default:
                        Log.e(TAG, "Incorrect pairing type received, not showing any dialog");
                }

            } else if (BluetoothDevice.ACTION_ACL_CONNECTED.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.e(TAG, "onReceive: ACTION_ACL_CONNECTED " + device.getName());
                connect(device);
            } else if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                Log.e(TAG, "onReceive: ACTION_ACL_DISCONNECTED " + device.getName());
                //connect(device);
            } else if (BluetoothA2dp.ACTION_CONNECTION_STATE_CHANGED.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
//                intent.getExtras().get(EXTRA_STATE)
                int state = (int) intent.getExtras().get(BluetoothProfile.EXTRA_STATE);
                int pre_state = (int) intent.getExtras().get(BluetoothProfile.EXTRA_PREVIOUS_STATE);
                Log.e(TAG, "onReceive: A2DP ACTION_CONNECTION_STATE_CHANGED " + device.getName() + " " + state);
                BondedDevice bondedDevice = myDevicesMap.get(device.getAddress());
                listener.onDeviceConnectStateChange(bondedDevice, state, pre_state);
            } else if (BluetoothHidHost.ACTION_CONNECTION_STATE_CHANGED.equals(intent.getAction())) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                int state = (int) intent.getExtras().get(BluetoothProfile.EXTRA_STATE);
                int pre_state = (int) intent.getExtras().get(BluetoothProfile.EXTRA_PREVIOUS_STATE);
                Log.e(TAG, "onReceive: HID ACTION_CONNECTION_STATE_CHANGED " + device.getName() + " " + state);

                BondedDevice bondedDevice = myDevicesMap.get(device.getAddress());
                listener.onDeviceConnectStateChange(bondedDevice, state, pre_state);
            }

        }
    }


//    private class AcceptThread extends Thread {
//        private final BluetoothServerSocket mmServerSocket;
//
//        public AcceptThread() {
//            BluetoothServerSocket tmp = null;
//            try {
//                tmp = bluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME, MY_UUID);
//            } catch (IOException ignored) {
//            }
//            mmServerSocket = tmp;
//        }
//
//        public void run() {
//            BluetoothSocket socket = null;
//            // 在后台一直监听客户端的请求
//            while (true) {
//                try {
//                    socket = mmServerSocket.accept();
//                } catch (IOException e) {
//                    break;
//                }
//                if (socket != null) {
//                    try {
//                        mmServerSocket.close();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }
//                    break;
//                }
//            }
//        }
//
//        public void cancel() {
//            try {
//                mmServerSocket.close();
//            } catch (IOException ignored) {
//            }
//        }
//    }
//
//    private class ConnectThread extends Thread {
//        private final BluetoothSocket mmSocket;
//        private final BluetoothDevice mmDevice;
//
//        public ConnectThread(BluetoothDevice device) {
//            BluetoothSocket tmp = null;
//            mmDevice = device;
//            try {
//                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
//            } catch (IOException ignored) {
//            }
//            mmSocket = tmp;
//        }
//
//        public void run() {
//            bluetoothAdapter.cancelDiscovery();
//
//            try {
//                mmSocket.connect();
//            } catch (IOException connectException) {
//                try {
//                    mmSocket.close();
//                } catch (IOException ignored) {
//                }
//            }
//        }
//
//        public void cancel() {
//            try {
//                mmSocket.close();
//            } catch (IOException ignored) {
//            }
//        }
//    }

    private BluetoothProfile.ServiceListener mProfileServiceListener = new BluetoothProfile.ServiceListener() {
        @Override
        public void onServiceConnected(int profile, BluetoothProfile proxy) {
            Log.i(TAG, "onServiceConnected profile=" + profile);
            if (profile == BluetoothProfile.A2DP) {//播放音乐
                Log.e(TAG, "onServiceConnected: get A2dp profile");
                bluetoothA2dp = (BluetoothA2dp) proxy; //转换
            }
            if (profile == 4) {
                Log.e(TAG, "onServiceConnected: get Input profile");
                bluetoothInput = proxy;
            }
            listener.onBondedListRefresh(getBondDevices());
//            if (true) {
//                List<BluetoothDevice> devices = new ArrayList<>();
//                if (bluetoothA2dp != null) {
//                    List<BluetoothDevice> deviceList = bluetoothA2dp.getConnectedDevices();
//                    if (deviceList != null && deviceList.size() > 0) {
//                        devices.addAll(deviceList);
//                    }
//                }
//            }

        }

        @Override
        public void onServiceDisconnected(int profile) {
            Log.i(TAG, "onServiceDisconnected profile=" + profile);
            if (profile == BluetoothProfile.A2DP) {
                Log.e(TAG, "onServiceDisconnected: A2dp");
                bluetoothA2dp = null;
            }
            if (profile == 4) {
                Log.e(TAG, "onServiceDisconnected: Input");
                bluetoothInput = null;
            }
        }
    };
