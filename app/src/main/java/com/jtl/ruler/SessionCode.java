package com.jtl.ruler;

import java.util.NoSuchElementException;

/**
 * @author：TianLong
 * @date：2019/11/30 12:13
 */
public enum SessionCode {
    SessionOK(0, "OK"),
    ApkOrSdkTooOld(1, "ARCore或者Android版本太旧"),
    DeviceNotCompatible(2, "设备不兼容"),
    ArcoreNotInstalled(3, "ARCore尚未安装"),
    ArcoreRequestedInstalled(4, "请求安装ARCore"),
    UserDeclinedInstallation(5, "用户拒绝安装ARCore");
    private int mValue;
    private String mInfo;

    SessionCode(int value, String info) {
        this.mValue = value;
        this.mInfo = info;
    }

    public static SessionCode fromNative(int value) {
        SessionCode[] var1 = values();
        int var2 = var1.length;

        for (int var3 = 0; var3 < var2; ++var3) {
            SessionCode type = var1[var3];
            if (type.mValue == value) {
                return type;
            }
        }

        throw new NoSuchElementException();
    }

    public int toNative() {
        return this.mValue;
    }

    public String toInfo() {
        return this.mInfo;
    }
}
