package tech.harmonysoft.oss.traute.javac.common;

import com.sun.source.tree.*;
import com.sun.source.util.TreeScanner;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;

public class PackageInfoManager {

    private final ConcurrentMap<String/* package name */, Set<String> /* annotations */> packageAnnotations
            = new ConcurrentHashMap<>();

    @NotNull
    public Set<String> getPackageAnnotations(@NotNull String packageName) {
        return packageAnnotations.computeIfAbsent(packageName, p -> emptySet());
    }

    public void onCompilationUnit(@NotNull CompilationUnitTree compilationUnit) {
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
