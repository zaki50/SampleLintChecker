package org.zakky.lint;

import static com.android.SdkConstants.ATTR_NAME;
import static com.android.SdkConstants.ANDROID_URI;
import static com.android.SdkConstants.PREFIX_ANDROID;
import static com.android.SdkConstants.TAG_APPLICATION;
import com.google.common.collect.Lists;
import org.objectweb.asm.tree.MethodInsnNode;
import static org.zakky.lint.DetectorUtil.findMethodCall;
import static org.zakky.lint.DetectorUtil.findTargetMethod;

import static org.zakky.lint.DetectorUtil.Predicate;

import com.android.annotations.NonNull;
import com.android.annotations.Nullable;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.ClassContext;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.ResourceXmlDetector;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.Speed;
import com.android.tools.lint.detector.api.XmlContext;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.w3c.dom.Element;

import java.util.Collection;
import java.util.EnumSet;
import java.util.List;

public class PrngFixDetector extends ResourceXmlDetector implements
        Detector.ClassScanner, Detector.XmlScanner {

    public static final Issue ISSUE = Issue.create("PrngFix", //$NON-NLS-1$
            "擬似乱数生成器の初期化バグへの対処を行っているかをチェックします",
            "擬似乱数生成器の初期化バグへの対処を行っているかをチェックします",
            "擬似乱数生成器の初期化バグへの対処を行っているかをチェックします",
            Category.USABILITY, 4, Severity.WARNING,
            new Implementation(PrngFixDetector.class,
                    EnumSet.<Scope> of(Scope.MANIFEST, Scope.CLASS_FILE)));

    private static final String ON_CREATE_METHOD_NAME = "onCreate";
    private static final String ON_CREATE_METHOD_DESC = "()V";

    private static final String INITIALIZE_METHOD_CLASS = "org/zakky/prngfix/PRNGFixes";
    private static final String INITIALIZE_METHOD_NAME = "apply";
    private static final String INITIALIZE_METHOD_DESC = "()V";


    private String mApplicationClassName;
    private String mApplicationCanonicalClassName;

    @Override
    public Speed getSpeed() {
        return Speed.FAST;
    }

    @Override
    public void beforeCheckProject(Context context) {
        super.beforeCheckProject(context);
    }

    @Override
    public void afterCheckProject(@NonNull Context context) {
        super.afterCheckProject(context);
    }

    @Override
    @Nullable
    public Collection<String> getApplicableElements() {
        return Lists.newArrayList(TAG_APPLICATION);
    }

    @Override
    @Nullable
    public Collection<String> getApplicableAttributes() {
        return Lists.newArrayList(ATTR_NAME);
    }

    // methods for XmlScanner

    @Override
    public void visitElement(XmlContext context, Element element) {
        if (context.getDriver().getPhase() != 1) {
            return;
        }

        if (context.getProject().isLibrary()) {
            // ライブラリプロジェクトはチェック不要
            return;
        }

        if (element.getNamespaceURI() != null // application is not android NS
                || !element.getTagName().equals(TAG_APPLICATION)) {
            return;
        }

        final String appClassName = element.getAttributeNS(ANDROID_URI, ATTR_NAME);
        if (appClassName == null || appClassName.length() == 0) {
            // android:name 未指定は警告
            context.report(
                    ISSUE,
                    element,
                    context.getLocation(element),
                    String.format("%1$s%2$s 属性に Application クラスを指定してください",
                            PREFIX_ANDROID, ATTR_NAME),
                    null);
            return;
        }

        mApplicationClassName = appClassName;
        mApplicationCanonicalClassName = appClassName.replace('.', '/');
    }

    // methods for ClassScanner

    @Override
    public void checkClass(ClassContext context, ClassNode node) {
        if (context.getDriver().getPhase() != 1) {
            return;
        }
        if (mApplicationCanonicalClassName == null || mApplicationCanonicalClassName.isEmpty()) {
            // 警告済みなのでチェック不要
            return;
        }
        if (!mApplicationCanonicalClassName.equals(node.name)) {
            // チェック対象クラスではない
            return;
        }

        @SuppressWarnings("unchecked")
        final List<MethodNode> methods = node.methods;
        final MethodNode method = findTargetMethod(methods, IS_ON_CREATE_METHOD);
        if (method == null) {
            context.report(
                    ISSUE,
                    context.getLocation(node),
                    String.format("%1$s クラスで onCreate() をオーバーライドして "
                            + "PRNGFixes#apply() を呼び出してください。",
                            mApplicationClassName),
                    null);
            return;
        }

        System.out.println(method.name + method.desc);

        final MethodInsnNode methodCall = findMethodCall(method.instructions,
                IS_INITIALIZE_METHOD_CALL);
        if (methodCall == null) {
            context.report(
                    ISSUE,
                    context.getLocation(method, node),
                    String.format("%1$s#onCreate() から "
                            + "PRNGFixes#apply() の呼び出しがありません。",
                            mApplicationClassName),
                    null);
            return;
        }
    }

    private static Predicate<MethodNode> IS_ON_CREATE_METHOD = new Predicate<MethodNode>() {
        public boolean apply(MethodNode method) {
            return method.name.equals(ON_CREATE_METHOD_NAME)
                    && method.desc.equals(ON_CREATE_METHOD_DESC);
        }
    };

    private static Predicate<MethodInsnNode> IS_INITIALIZE_METHOD_CALL = new Predicate<MethodInsnNode>() {
        public boolean apply(MethodInsnNode call) {
            return call.owner.equals(INITIALIZE_METHOD_CLASS)
                    && call.name.equals(INITIALIZE_METHOD_NAME)
                    && call.desc.equals(INITIALIZE_METHOD_DESC);
        }
    };
}
