# nativeBridge 通过反射注入 JS 桥接对象，保留公开方法名。
-keepclassmembers class com.tideengine.mobile.bridge.GameWebViewBridge {
    public *;
}
