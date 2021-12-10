package com.github.unidbg.ios.objc;

import com.github.unidbg.debugger.ida.Utils;
import com.github.unidbg.ios.MachOModule;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class Objc2Method {

    private static class Method {
        private final int name;
        private final int types;
        private final long imp;
        private Method(int name, int types, long imp) {
            this.name = name;
            this.types = types;
            this.imp = imp;
        }
        private Objc2Method createMethod(ByteBuffer buffer) {
            buffer.position(this.name);
            String methodName = Utils.readCString(buffer);
            buffer.position(this.types);
            String typesName = Utils.readCString(buffer);
            return new Objc2Method(methodName, typesName, imp);
        }
    }

    static List<Objc2Method> loadMethods(ByteBuffer buffer, long baseMethods, MachOModule mm) {
        if (baseMethods == 0) {
            return Collections.emptyList();
        }
        int pos = mm.virtualMemoryAddressToFileOffset(baseMethods);
        buffer.position(pos);
        int entsize = buffer.getInt() & ~3;
        boolean flag = (entsize & 0x80000000) != 0;
        entsize &= ~0x80000000;
        int count = buffer.getInt();
        if (entsize != 24 && entsize != 12) {
            throw new IllegalStateException("Invalid entsize: " + entsize + ", baseMethods=0x" + Long.toHexString(baseMethods) + ", flag=" + flag);
        }
        List<Method> methods = new ArrayList<>(count);
        if (entsize == 24) {
            for (int i = 0; i < count; i++) {
                long name = buffer.getLong();
                long types = buffer.getLong();
                long imp = buffer.getLong();
                Method method = new Method(mm.virtualMemoryAddressToFileOffset(name), mm.virtualMemoryAddressToFileOffset(types), imp);
                methods.add(method);
            }
        } else {
            for (int i = 0; i < count; i++) {
                long name = buffer.getInt();
                long types = buffer.getInt();
                long imp = buffer.getInt();
                Method method = new Method(mm.virtualMemoryAddressToFileOffset(name), mm.virtualMemoryAddressToFileOffset(types), imp);
                methods.add(method);
            }
        }
        List<Objc2Method> list = new ArrayList<>(count);
        for (Method method : methods) {
            list.add(method.createMethod(buffer));
        }
        return list;
    }

    final String name;
    private final String types;
    final long imp;

    private Objc2Method(String name, String types, long imp) {
        this.name = name;
        this.types = types;
        this.imp = imp;
    }

    @Override
    public String toString() {
        return "Objc2Method{" +
                "name=" + name +
                ", types=" + types +
                ", imp=0x" + Long.toHexString(imp) +
                '}';
    }
}
