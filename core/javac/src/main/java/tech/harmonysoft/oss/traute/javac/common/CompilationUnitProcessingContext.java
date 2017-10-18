package tech.harmonysoft.oss.traute.javac.common;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Names;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Holds data necessary for processing a {@link CompilationUnitTree} given by {@code javac}
 */
public class CompilationUnitProcessingContext {

    private final Set<String> notNullAnnotations = new HashSet<>();
    private final Set<String> imports            = new HashSet<>();

    @NotNull private final TreeMaker          astFactory;
    @NotNull private final Names              symbolsTable;
    @NotNull private final TrautePluginLogger logger;
    @NotNull private final StatsCollector     statsCollector;

    private final boolean verbose;

    public CompilationUnitProcessingContext(@NotNull Collection<String> notNullAnnotations,
                                            @NotNull TreeMaker astFactory,
                                            @NotNull Names symbolsTable,
                                            @NotNull TrautePluginLogger logger,
                                            @NotNull StatsCollector statsCollector,
                                            boolean verbose)
    {
        this.statsCollector = statsCollector;
        this.verbose = verbose;
        this.notNullAnnotations.addAll(notNullAnnotations);
        this.astFactory = astFactory;
        this.symbolsTable = symbolsTable;
        this.logger = logger;
    }

    public void addImport(@NotNull String importText) {
        imports.add(importText);
    }

    @NotNull
    public Set<String> getNotNullAnnotations() {
        return notNullAnnotations;
    }

    @NotNull
    public Set<String> getImports() {
        return imports;
    }

    @NotNull
    public TreeMaker getAstFactory() {
        return astFactory;
    }

    @NotNull
    public Names getSymbolsTable() {
        return symbolsTable;
    }

    @NotNull
    public TrautePluginLogger getLogger() {
        return logger;
    }

    @NotNull
    public StatsCollector getStatsCollector() {
        return statsCollector;
    }

    public boolean isVerbose() {
        return verbose;
    }
}
