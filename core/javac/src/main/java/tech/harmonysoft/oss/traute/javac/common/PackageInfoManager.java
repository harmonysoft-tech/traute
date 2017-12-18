package tech.harmonysoft.oss.traute.javac.common;

import com.sun.source.tree.*;
import com.sun.source.util.TreeScanner;
import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.common.util.TrauteConstants;

import javax.tools.JavaFileObject;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class PackageInfoManager {

    private static final String SUFFIX = TrauteConstants.PACKAGE_INFO + JavaFileObject.Kind.SOURCE.extension;

    private final ConcurrentMap<String/* package name */, Set<String> /* annotations */> packageAnnotations
            = new ConcurrentHashMap<>();

    @NotNull
    public Set<String> getPackageAnnotations(@NotNull String packageName) {
        return packageAnnotations.computeIfAbsent(packageName, p -> {
            try {
                Class<?> clazz = Class.forName(packageName + "." + TrauteConstants.PACKAGE_INFO);
                Annotation[] annotations = clazz.getAnnotations();
                if (annotations != null) {
                    return Arrays.stream(annotations).map(a -> a.annotationType().getName()).collect(toSet());
                }
            } catch (ClassNotFoundException ignore) {
            }
            return emptySet();
        });
    }

    public void onCompilationUnit(@NotNull CompilationUnitTree compilationUnit) {
        JavaFileObject sourceFile = compilationUnit.getSourceFile();
        if (sourceFile == null) {
            return;
        }
        String name = sourceFile.getName();
        if (name == null) {
            return;
        }
        if (!name.endsWith(SUFFIX)) {
            return;
        }
        ExpressionTree packageNameExpression = compilationUnit.getPackageName();
        String packageName = packageNameExpression == null ? "" : packageNameExpression.toString();
        Collection<String> packageAnnotationsInSource = compilationUnit.getPackageAnnotations()
                                                                       .stream()
                                                                       .map(AnnotationTree::getAnnotationType)
                                                                       .filter(Objects::nonNull)
                                                                       .map(Object::toString)
                                                                       .collect(toList());
        Set<String> imports = new HashSet<>();
        compilationUnit.accept(new TreeScanner<Void, Void>() {
            @Override
            public Void visitImport(ImportTree node, Void aVoid) {
                if (!node.isStatic()) {
                    Tree identifier = node.getQualifiedIdentifier();
                    if (identifier != null) {
                        imports.add(identifier.toString());
                    }
                }
                return null;
            }
        }, null);
        Set<String> resultingPackageAnnotations = new HashSet<>();
        // Filter out all exact imports like 'import java.util.List'
        for (String anImport : imports) {
            if (anImport.endsWith(".*")) {
                continue;
            }
            for (String packageAnnotation : packageAnnotationsInSource) {
                if (anImport.endsWith(packageAnnotation)) {
                    resultingPackageAnnotations.add(anImport);
                    packageAnnotationsInSource.remove(packageAnnotation);
                    break;
                }
            }
        }

        // Process wildcard imports
        for (String anImport : imports) {
            if (anImport.endsWith(".*")) {
                String prefix = anImport.substring(0, anImport.length() - 1);
                for (String packageAnnotation : packageAnnotationsInSource) {
                    resultingPackageAnnotations.add(prefix + packageAnnotation);
                }
            }
        }
        packageAnnotations.computeIfAbsent(packageName, p -> new HashSet<>()).addAll(resultingPackageAnnotations);
    }
}
