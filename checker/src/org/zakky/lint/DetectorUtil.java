/*
 * Copyright(C) 2013 TOYOTA InfoTechnology Center Co.,LTD. All Rights Reserved.
 */

package org.zakky.lint;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.Charset;
import java.util.List;
import java.util.ListIterator;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Project;

class DetectorUtil {
    private DetectorUtil() {
    }

    public interface Predicate<T> {
        public boolean apply(T in);
    }

    public static MethodNode findTargetMethod(List<MethodNode> methods,
            Predicate<? super MethodNode> predicate) {
        for (MethodNode method : methods) {
            if (predicate.apply(method)) {
                return method;
            }
        }
        return null;
    }

    public static MethodInsnNode findMethodCall(InsnList instructions,
            Predicate<MethodInsnNode> predicate) {
        @SuppressWarnings("unchecked")
        final ListIterator<AbstractInsnNode> it = instructions.iterator();
        while (it.hasNext()) {
            final AbstractInsnNode insn = it.next();
            if (insn.getType() != AbstractInsnNode.METHOD_INSN) {
                continue;
            }
            final MethodInsnNode call = (MethodInsnNode) insn;
            if (predicate.apply(call)) {
                return call;
            }
        }
        return null;
    }

    /*
     * プライベートメソッド群
     */
    private static final Charset MANIFEST_XML_ENCODING = Charset.forName("UTF-8");

    private static void closeQuietly(InputStream is) {
        if (is == null) {
            return;
        }
        try {
            is.close();
        } catch (IOException e) {
            // ignore
        }
    }

    private static void closeQuietly(Reader reader) {
        if (reader == null) {
            return;
        }
        try {
            reader.close();
        } catch (IOException e) {
            // ignore
        }
    }

    private static File getMainManifestFile(Project project) {
        try {
            try {
                final Method methodFor22_3 = project.getClass().getMethod("getManifestFiles");
                @SuppressWarnings("unchecked")
                final List<File> manifestFiles = (List<File>) methodFor22_3.invoke(project);
                if (manifestFiles.isEmpty()) {
                    return null;
                }
                return manifestFiles.get(0);
            } catch (NoSuchMethodException e) {
                try {
                    final Method oldMethod = project.getClass().getMethod("getManifestFile");
                    final File manifestFile = (File) oldMethod.invoke(project);
                    return manifestFile; // may be null
                } catch (NoSuchMethodException e1) {
                    throw new RuntimeException("unsupported Lint API version.");
                }
            }
        } catch (IllegalAccessException e) {
            return null;
        } catch (InvocationTargetException e) {
            final Throwable targetException = e.getTargetException();
            if (targetException instanceof RuntimeException) {
                throw (RuntimeException) targetException;
            }
            if (targetException instanceof Error) {
                throw (Error) targetException;
            }
            throw new RuntimeException(targetException);
        }
    }
}
