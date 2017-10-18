package tech.harmonysoft.oss.traute.javac.common;

import com.sun.source.tree.CompilationUnitTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Names;
import org.jetbrains.annotations.NotNull;
import tech.harmonysoft.oss.traute.javac.settings.TrautePluginSettings;

import java.util.HashSet;
import java.util.Set;

/**
 * Holds data necessary for processing a {@link CompilationUnitTree} given by {@code javac}
 */
public class CompilationUnitProcessingContext {

    private final Set<String> imports = new HashSet<>();

    @NotNull private final TrautePluginSettings pluginSettings;
    @NotNull private final TreeMaker            astFactory;
    @NotNull private final Names                symbolsTable;
    @NotNull private final TrautePluginLogger   logger;
    @NotNull private final StatsCollector       statsCollector;

    public CompilationUnitProcessingContext(@NotNull TrautePluginSettings pluginSettings,
                                            @NotNull TreeMaker astFactory,
                                            @NotNull Names symbolsTable,
                                            @NotNull TrautePluginLogger logger,
                                            @NotNull StatsCollector statsCollector)
    {
        this.pluginSettings = pluginSettings;
        this.statsCollector = statsCollector;
        this.astFactory = astFactory;
        this.symbolsTable = symbolsTable;
        this.logger = logger;
    }

    public void addImport(@NotNull String importText) {
        imports.add(importText);
    }

    @NotNull
    public TrautePluginSettings getPluginSettings() {
        return pluginSettings;
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
}
