package fbl.lean.java.lock.util;

import sun.misc.Unsafe;

import java.lang.reflect.Field;

public final class UnsafeUtils {
    private static final Unsafe unsafe;

    static {
        Field f;
        try {
            f = Unsafe.class.getDeclaredField("theUnsafe");
        } catch (NoSuchFieldException var3) {
            throw new RuntimeException(var3);
        }

        f.setAccessible(true);

        try {
            unsafe = (Unsafe) f.get(null);
        } catch (IllegalAccessException var2) {
            throw new RuntimeException(var2);
        }
    }

    private UnsafeUtils() {
    }

    public static Unsafe getUnsafe() {
        return unsafe;
    }
}
