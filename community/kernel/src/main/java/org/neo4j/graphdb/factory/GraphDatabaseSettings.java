/*
 * Copyright (c) 2002-2017 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.graphdb.factory;

import java.io.File;
import java.time.Duration;
import java.util.List;

import org.neo4j.configuration.Description;
import org.neo4j.configuration.Dynamic;
import org.neo4j.configuration.Internal;
import org.neo4j.configuration.LoadableConfig;
import org.neo4j.configuration.ReplacedBy;
import org.neo4j.csv.reader.Configuration;
import org.neo4j.graphdb.config.Setting;
import org.neo4j.helpers.AdvertisedSocketAddress;
import org.neo4j.helpers.ListenSocketAddress;
import org.neo4j.io.ByteUnit;
import org.neo4j.kernel.configuration.BoltConnectorValidator;
import org.neo4j.kernel.configuration.ConfigurationMigrator;
import org.neo4j.kernel.configuration.GraphDatabaseConfigurationMigrator;
import org.neo4j.kernel.configuration.Group;
import org.neo4j.kernel.configuration.GroupSettingSupport;
import org.neo4j.kernel.configuration.HttpConnectorValidator;
import org.neo4j.kernel.configuration.Migrator;
import org.neo4j.kernel.configuration.Settings;
import org.neo4j.kernel.configuration.Title;
import org.neo4j.kernel.configuration.ssl.SslPolicyConfigValidator;
import org.neo4j.kernel.impl.cache.MonitorGc;
import org.neo4j.logging.Level;

import static org.neo4j.kernel.configuration.Settings.ANY;
import static org.neo4j.kernel.configuration.Settings.BOOLEAN;
import static org.neo4j.kernel.configuration.Settings.BYTES;
import static org.neo4j.kernel.configuration.Settings.DEFAULT;
import static org.neo4j.kernel.configuration.Settings.DOUBLE;
import static org.neo4j.kernel.configuration.Settings.DURATION;
import static org.neo4j.kernel.configuration.Settings.FALSE;
import static org.neo4j.kernel.configuration.Settings.INTEGER;
import static org.neo4j.kernel.configuration.Settings.LONG;
import static org.neo4j.kernel.configuration.Settings.NO_DEFAULT;
import static org.neo4j.kernel.configuration.Settings.PATH;
import static org.neo4j.kernel.configuration.Settings.STRING;
import static org.neo4j.kernel.configuration.Settings.STRING_LIST;
import static org.neo4j.kernel.configuration.Settings.TRUE;
import static org.neo4j.kernel.configuration.Settings.advertisedAddress;
import static org.neo4j.kernel.configuration.Settings.buildSetting;
import static org.neo4j.kernel.configuration.Settings.derivedSetting;
import static org.neo4j.kernel.configuration.Settings.illegalValueMessage;
import static org.neo4j.kernel.configuration.Settings.legacyFallback;
import static org.neo4j.kernel.configuration.Settings.list;
import static org.neo4j.kernel.configuration.Settings.listenAddress;
import static org.neo4j.kernel.configuration.Settings.matches;
import static org.neo4j.kernel.configuration.Settings.min;
import static org.neo4j.kernel.configuration.Settings.options;
import static org.neo4j.kernel.configuration.Settings.optionsIgnoreCase;
import static org.neo4j.kernel.configuration.Settings.pathSetting;
import static org.neo4j.kernel.configuration.Settings.range;
import static org.neo4j.kernel.configuration.Settings.setting;

/**
 * Settings for Neo4j.
 */
public class GraphDatabaseSettings implements LoadableConfig
{
    /**
     * Data block sizes for dynamic array stores.
     */
    public static final int DEFAULT_BLOCK_SIZE = 128;
    public static final int DEFAULT_LABEL_BLOCK_SIZE = 64;
    public static final int MINIMAL_BLOCK_SIZE = 16;

    // default unspecified transaction timeout
    public static final long UNSPECIFIED_TIMEOUT = 0L;

    @SuppressWarnings( "unused" ) // accessed by reflection
    @Migrator
    private static final ConfigurationMigrator migrator = new GraphDatabaseConfigurationMigrator();

    @Internal
    @Description( "Root relative to which directory settings are resolved. This is set in code and should never be " +
            "configured explicitly." )
    public static final Setting<File> neo4j_home =
            setting( "unsupported.dbms.directories.neo4j_home", PATH, NO_DEFAULT );

    @Title( "Read only database" )
    @Description( "Only allow read operations from this Neo4j instance. " +
            "This mode still requires write access to the directory for lock purposes." )
    public static final Setting<Boolean> read_only = setting( "dbms.read_only", BOOLEAN, FALSE );

    @Title( "Disconnected" )
    @Internal
    @Description( "Disable all protocol connectors." )
    public static final Setting<Boolean> disconnected = setting( "unsupported.dbms.disconnected", BOOLEAN, FALSE );

    @Description( "Print out the effective Neo4j configuration after startup." )
    @Internal
    public static final Setting<Boolean> dump_configuration = setting( "unsupported.dbms.report_configuration",
            BOOLEAN, FALSE );

    @Description( "A strict configuration validation will prevent the database from starting up if unknown " +
            "configuration options are specified in the neo4j settings namespace (such as dbms., ha., cypher., etc). " +
            "This is currently false by default but will be true by default in 4.0." )
    public static final Setting<Boolean> strict_config_validation =
            setting( "dbms.config.strict_validation", BOOLEAN, FALSE );

    @Description( "Whether to allow a store upgrade in case the current version of the database starts against an " +
            "older store version. " +
            "Setting this to `true` does not guarantee successful upgrade, it just " +
            "allows an upgrade to be performed." )
    @Deprecated
    @ReplacedBy( "dbms.allow_upgrade" )
    public static final Setting<Boolean> allow_store_upgrade = setting( "dbms.allow_format_migration", BOOLEAN, FALSE );

    @Description( "Whether to allow an upgrade in case the current version of the database starts against an older version." )
    public static final Setting<Boolean> allow_upgrade = setting( "dbms.allow_upgrade", BOOLEAN, FALSE );

    @Description( "Database record format. Valid values: `standard`, `high_limit`. " +
            "The `high_limit` format is available for Enterprise Edition only. " +
            "It is required if you have a graph that is larger than 34 billion nodes, 34 billion relationships, or 68 billion properties. " +
            "A change of the record format is irreversible. " +
            "Certain operations may suffer from a performance penalty of up to 10%, which is why this format is not switched on by default." )
    public static final Setting<String> record_format = setting( "dbms.record_format", Settings.STRING, "" );

    // Cypher settings
    // TODO: These should live with cypher
    @Description( "Set this to specify the default parser (language version)." )
    public static final Setting<String> cypher_parser_version = setting(
            "cypher.default_language_version",
            options( "2.3", "3.1", "3.3","3.4", DEFAULT ), DEFAULT );

    @Description( "Set this to specify the default planner for the default language version." )
    public static final Setting<String> cypher_planner = setting(
            "cypher.planner",
            options( "COST", "RULE", DEFAULT ), DEFAULT );

    @Description( "Set this to specify the behavior when Cypher planner or runtime hints cannot be fulfilled. "
            + "If true, then non-conformance will result in an error, otherwise only a warning is generated." )
    public static final Setting<Boolean> cypher_hints_error = setting( "cypher.hints_error", BOOLEAN, FALSE );

    @Description( "This setting is associated with performance optimization. Set this to `true` in situations where " +
                  "it is preferable to have any queries using the 'shortestPath' function terminate as soon as " +
                  "possible with no answer, rather than potentially running for a long time attempting to find an " +
                  "answer (even if there is no path to be found). " +
                  "For most queries, the 'shortestPath' algorithm will return the correct answer very quickly. However " +
                  "there are some cases where it is possible that the fast bidirectional breadth-first search " +
                  "algorithm will find no results even if they exist. This can happen when the predicates in the " +
                  "`WHERE` clause applied to 'shortestPath' cannot be applied to each step of the traversal, and can " +
                  "only be applied to the entire path. When the query planner detects these special cases, it will " +
                  "plan to perform an exhaustive depth-first search if the fast algorithm finds no paths. However, " +
                  "the exhaustive search may be orders of magnitude slower than the fast algorithm. If it is critical " +
                  "that queries terminate as soon as possible, it is recommended that this option be set to `true`, " +
                  "which means that Neo4j will never consider using the exhaustive search for shortestPath queries. " +
                  "However, please note that if no paths are found, an error will be thrown at run time, which will " +
                  "need to be handled by the application." )
    public static final Setting<Boolean> forbid_exhaustive_shortestpath = setting(
            "cypher.forbid_exhaustive_shortestpath", BOOLEAN, FALSE );

    @Description( "This setting is associated with performance optimization. The shortest path algorithm does not " +
                  "work when the start and end nodes are the same. With this setting set to `false` no path will " +
                  "be returned when that happens. The default value of `true` will instead throw an exception. " +
                  "This can happen if you perform a shortestPath search after a cartesian product that might have " +
                  "the same start and end nodes for some of the rows passed to shortestPath. If it is preferable " +
                  "to not experience this exception, and acceptable for results to be missing for those rows, then " +
                  "set this to `false`. If you cannot accept missing results, and really want the shortestPath " +
                  "between two common nodes, then re-write the query using a standard Cypher variable length pattern " +
                  "expression followed by ordering by path length and limiting to one result." )
    public static final Setting<Boolean> forbid_shortestpath_common_nodes = setting(
            "cypher.forbid_shortestpath_common_nodes", BOOLEAN, TRUE );

    @Description( "Set this to specify the default runtime for the default language version." )
    @Internal
    public static final Setting<String> cypher_runtime = setting(
            "unsupported.cypher.runtime",
            options( "INTERPRETED", "COMPILED", "SLOTTED" ,  DEFAULT ), DEFAULT );

    @Description( "Enable tracing of compilation in cypher." )
    @Internal
    public static final Setting<Boolean> cypher_compiler_tracing = setting( "unsupported.cypher.compiler_tracing", BOOLEAN, FALSE );

    @Description( "The number of Cypher query execution plans that are cached." )
    public static Setting<Integer> query_cache_size =
            buildSetting( "dbms.query_cache_size", INTEGER, "1000" ).constraint( min( 0 ) ).build();

    @Description( "The threshold when a plan is considered stale. If any of the underlying" +
                  " statistics used to create the plan has changed more than this value, " +
                  "the plan is considered stale and will be replanned. " +
                  "A value of 0 means always replan, and 1 means never replan." )
    public static Setting<Double> query_statistics_divergence_threshold = buildSetting(
            "cypher.statistics_divergence_threshold", DOUBLE, "0.75" ).constraint( range( 0.0, 1.0 ) ).build();

    @Description( "The threshold when a warning is generated if a label scan is done after a load csv " +
                  "where the label has no index" )
    @Internal
    public static Setting<Long> query_non_indexed_label_warning_threshold = setting(
            "unsupported.cypher.non_indexed_label_warning_threshold", LONG, "10000" );

    @Description( "To improve IDP query planning time, we can restrict the internal planning table size, " +
                  "triggering compaction of candidate plans. The smaller the threshold the faster the planning, " +
                  "but the higher the risk of sub-optimal plans." )
    @Internal
    public static Setting<Integer> cypher_idp_solver_table_threshold = buildSetting(
            "unsupported.cypher.idp_solver_table_threshold", INTEGER, "128" ).constraint( min( 16 ) ).build();

    @Description( "To improve IDP query planning time, we can restrict the internal planning loop duration, " +
                  "triggering more frequent compaction of candidate plans. The smaller the threshold the " +
                  "faster the planning, but the higher the risk of sub-optimal plans." )
    @Internal
    public static Setting<Long> cypher_idp_solver_duration_threshold = buildSetting(
            "unsupported.cypher.idp_solver_duration_threshold", LONG, "1000" ).constraint( min( 10L ) ).build();

    @Description( "The minimum lifetime of a query plan before a query is considered for replanning" )
    public static Setting<Duration> cypher_min_replan_interval = setting( "cypher.min_replan_interval", DURATION, "10s" );

    @Description( "Determines if Cypher will allow using file URLs when loading data using `LOAD CSV`. Setting this "
                  + "value to `false` will cause Neo4j to fail `LOAD CSV` clauses that load data from the file system." )
    public static Setting<Boolean> allow_file_urls = setting( "dbms.security.allow_csv_import_from_file_urls", BOOLEAN, TRUE );

    @Description( "Sets the root directory for file URLs used with the Cypher `LOAD CSV` clause. This must be set to a single "
                  + "directory, restricting access to only those files within that directory and its subdirectories." )
    public static Setting<File> load_csv_file_url_root = pathSetting( "dbms.directories.import", NO_DEFAULT );

    @Description( "Selects whether to conform to the standard https://tools.ietf.org/html/rfc4180 for interpreting " +
                  "escaped quotation characters in CSV files loaded using `LOAD CSV`. Setting this to `false` will use" +
                  " the standard, interpreting repeated quotes '\"\"' as a single in-lined quote, while `true` will " +
                  "use the legacy convention originally supported in Neo4j 3.0 and 3.1, allowing a backslash to " +
                  "include quotes in-lined in fields." )
    public static Setting<Boolean> csv_legacy_quote_escaping =
            setting( "dbms.import.csv.legacy_quote_escaping", BOOLEAN,
                    Boolean.toString( Configuration.DEFAULT_LEGACY_STYLE_QUOTING ) );

    @Description( "Enables or disables tracking of how much time a query spends actively executing on the CPU." )
    public static Setting<Boolean> track_query_cpu_time = setting( "dbms.track_query_cpu_time", BOOLEAN, TRUE );
    @Description( "Enables or disables tracking of how many bytes are allocated by the execution of a query." )
    public static Setting<Boolean> track_query_allocation = setting( "dbms.track_query_allocation", BOOLEAN, TRUE );

    @Description( "The maximum amount of time to wait for the database to become available, when " +
                  "starting a new transaction." )
    @Internal
    public static final Setting<Duration> transaction_start_timeout =
            setting( "unsupported.dbms.transaction_start_timeout", DURATION, "1s" );

    @Internal
    @Description( "Please use dbms.transaction.timeout instead." )
    @Deprecated
    @ReplacedBy( "dbms.transaction.timeout" )
    public static final Setting<Boolean> execution_guard_enabled =
            setting( "unsupported.dbms.executiontime_limit.enabled", BOOLEAN, FALSE );

    @Description( "The maximum time interval of a transaction within which it should be completed." )
    @Dynamic
    public static final Setting<Duration> transaction_timeout = setting( "dbms.transaction.timeout", DURATION, String
            .valueOf( UNSPECIFIED_TIMEOUT ) );

    @Description( "The maximum time interval within which lock should be acquired." )
    public static final Setting<Duration> lock_acquisition_timeout = setting( "dbms.lock.acquisition.timeout", DURATION,
            String.valueOf( UNSPECIFIED_TIMEOUT ) );

    @Description( "Configures the time interval between transaction monitor checks. Determines how often " +
            "monitor thread will check transaction for timeout." )
    public static final Setting<Duration> transaction_monitor_check_interval =
            setting( "dbms.transaction.monitor.check.interval", DURATION, "2s" );

    @Description( "The maximum amount of time to wait for running transactions to complete before allowing "
                  + "initiated database shutdown to continue" )
    public static final Setting<Duration> shutdown_transaction_end_timeout =
            setting( "dbms.shutdown_transaction_end_timeout", DURATION, "10s" );

    @Description( "Location of the database plugin directory. Compiled Java JAR files that contain database " +
                 "procedures will be loaded if they are placed in this directory." )
    public static final Setting<File> plugin_dir = pathSetting( "dbms.directories.plugins", "plugins" );

    @Description( "Threshold for rotation of the debug log." )
    public static final Setting<Long> store_internal_log_rotation_threshold =
            buildSetting( "dbms.logs.debug.rotation.size", BYTES, "20m" ).constraint( range( 0L, Long.MAX_VALUE ) ).build();

    @Description( "Debug log contexts that should output debug level logging" )
    @Internal
    public static final Setting<List<String>> store_internal_debug_contexts = setting( "unsupported.dbms.logs.debug.debug_loggers",
            list( ",", STRING ), "org.neo4j.diagnostics,org.neo4j.cluster.protocol,org.neo4j.kernel.ha" );

    @Description( "Debug log level threshold." )
    public static final Setting<Level> store_internal_log_level = setting( "dbms.logs.debug.level",
            options( Level.class ), "INFO" );

    @Description( "Maximum time to wait for active transaction completion when rotating counts store" )
    @Internal
    public static final Setting<Duration> counts_store_rotation_timeout =
            setting( "unsupported.dbms.counts_store_rotation_timeout", DURATION, "10m" );

    @Description( "Minimum time interval after last rotation of the debug log before it may be rotated again." )
    public static final Setting<Duration> store_internal_log_rotation_delay =
            setting( "dbms.logs.debug.rotation.delay", DURATION, "300s" );

    @Description( "Maximum number of history files for the debug log." )
    public static final Setting<Integer> store_internal_log_max_archives =
            buildSetting( "dbms.logs.debug.rotation.keep_number", INTEGER, "7" ).constraint( min( 1 ) ).build();

    @Description( "Configures the transaction interval between check-points. The database will not check-point more " +
                  "often  than this (unless check pointing is triggered by a different event), but might check-point " +
                  "less often than this interval, if performing a check-point takes longer time than the configured " +
                  "interval. A check-point is a point in the transaction logs, from which recovery would start from. " +
                  "Longer check-point intervals typically means that recovery will take longer to complete in case " +
                  "of a crash. On the other hand, a longer check-point interval can also reduce the I/O load that " +
                  "the database places on the system, as each check-point implies a flushing and forcing of all the " +
                  "store files.  The default is '100000' for a check-point every 100000 transactions." )
    public static final Setting<Integer> check_point_interval_tx =
            buildSetting( "dbms.checkpoint.interval.tx", INTEGER, "100000" ).constraint( min( 1 ) ).build();

    @Description( "Configures the time interval between check-points. The database will not check-point more often " +
                  "than this (unless check pointing is triggered by a different event), but might check-point less " +
                  "often than this interval, if performing a check-point takes longer time than the configured " +
                  "interval. A check-point is a point in the transaction logs, from which recovery would start from. " +
                  "Longer check-point intervals typically means that recovery will take longer to complete in case " +
                  "of a crash. On the other hand, a longer check-point interval can also reduce the I/O load that " +
                  "the database places on the system, as each check-point implies a flushing and forcing of all the " +
                  "store files." )
    public static final Setting<Duration> check_point_interval_time =
            setting( "dbms.checkpoint.interval.time", DURATION, "15m" );

    @Description( "Limit the number of IOs the background checkpoint process will consume per second. " +
                  "This setting is advisory, is ignored in Neo4j Community Edition, and is followed to " +
                  "best effort in Enterprise Edition. " +
                  "An IO is in this case a 8 KiB (mostly sequential) write. Limiting the write IO in " +
                  "this way will leave more bandwidth in the IO subsystem to service random-read IOs, " +
                  "which is important for the response time of queries when the database cannot fit " +
                  "entirely in memory. The only drawback of this setting is that longer checkpoint times " +
                  "may lead to slightly longer recovery times in case of a database or system crash. " +
                  "A lower number means lower IO pressure, and consequently longer checkpoint times. " +
                  "The configuration can also be commented out to remove the limitation entirely, and " +
                  "let the checkpointer flush data as fast as the hardware will go. " +
                  "Set this to -1 to disable the IOPS limit." )
    public static final Setting<Integer> check_point_iops_limit = setting( "dbms.checkpoint.iops.limit", INTEGER, "300" );

    // Auto Indexing
    @Description( "Controls the auto indexing feature for nodes. Setting it to `false` shuts it down, " +
            "while `true` enables it by default for properties listed in the dbms.auto_index.nodes.keys setting." )
    @Internal
    @Deprecated
    public static final Setting<Boolean> node_auto_indexing = setting( "dbms.auto_index.nodes.enabled", BOOLEAN,
            FALSE );

    @Description( "A list of property names (comma separated) that will be indexed by default. This applies to " +
            "_nodes_ only." )
    @Internal
    @Deprecated
    public static final Setting<List<String>> node_keys_indexable = setting( "dbms.auto_index.nodes.keys",
            STRING_LIST, "" );

    @Description( "Controls the auto indexing feature for relationships. Setting it to `false` shuts it down, " +
            "while `true` enables it by default for properties listed in the dbms.auto_index.relationships.keys " +
            "setting." )
    @Internal
    @Deprecated
    public static final Setting<Boolean> relationship_auto_indexing =
            setting( "dbms.auto_index.relationships.enabled", BOOLEAN, FALSE );

    @Description( "A list of property names (comma separated) that will be indexed by default. This applies to " +
            "_relationships_ only." )
    @Internal
    @Deprecated
    public static final Setting<List<String>> relationship_keys_indexable =
            setting( "dbms.auto_index.relationships.keys", STRING_LIST, "" );

    // Index sampling
    @Description( "Enable or disable background index sampling" )
    public static final Setting<Boolean> index_background_sampling_enabled =
            setting( "dbms.index_sampling.background_enabled", BOOLEAN, TRUE );

    @Description( "Size of buffer used by index sampling. " +
                 "This configuration setting is no longer applicable as from Neo4j 3.0.3. " +
                 "Please use dbms.index_sampling.sample_size_limit instead." )
    @Deprecated
    @ReplacedBy( "dbms.index_sampling.sample_size_limit" )
    public static final Setting<Long> index_sampling_buffer_size = buildSetting( "dbms.index_sampling.buffer_size",
            BYTES, "64m" ).constraint( range( /* 1m */ 1048576L, (long) Integer.MAX_VALUE ) ).build();

    @Description( "Index sampling chunk size limit" )
    public static final Setting<Integer> index_sample_size_limit = buildSetting( "dbms.index_sampling.sample_size_limit",
            INTEGER, String.valueOf( ByteUnit.mebiBytes( 8 ) ) ).constraint( range( (int) ByteUnit.mebiBytes( 1 ),
            Integer.MAX_VALUE ) ).build();

    @Description( "Percentage of index updates of total index size required before sampling of a given index is " +
            "triggered" )
    public static final Setting<Integer> index_sampling_update_percentage =
            buildSetting( "dbms.index_sampling.update_percentage", INTEGER, "5" ).constraint( min( 0 ) ).build();

    // Lucene settings
    @Description( "The maximum number of open Lucene index searchers." )
    public static Setting<Integer> lucene_searcher_cache_size = buildSetting( "dbms.index_searcher_cache_size",INTEGER,
            Integer.toString( Integer.MAX_VALUE ) ).constraint( min( 1 ) ).build();

    // Lucene schema indexes
    @Internal
    public static final Setting<Boolean> multi_threaded_schema_index_population_enabled =
            setting( "unsupported.dbms.multi_threaded_schema_index_population_enabled", BOOLEAN, TRUE );

    @Internal
    public static final Setting<Boolean> enable_native_schema_index =
            setting( "unsupported.dbms.enable_native_schema_index", BOOLEAN, TRUE );

    // Store settings
    @Description( "Make Neo4j keep the logical transaction logs for being able to backup the database. " +
            "Can be used for specifying the threshold to prune logical logs after. For example \"10 days\" will " +
            "prune logical logs that only contains transactions older than 10 days from the current time, " +
            "or \"100k txs\" will keep the 100k latest transactions and prune any older transactions." )
    public static final Setting<String> keep_logical_logs = buildSetting( "dbms.tx_log.rotation.retention_policy",
            STRING, "7 days" ).constraint( illegalValueMessage( "must be `true`/`false` or " +
                    "of format '<number><optional unit> <type>' for example `100M size` for " +
                    "limiting logical log space on disk to 100Mb," +
                    " or `200k txs` for limiting the number of transactions to keep to 200 000", matches(ANY) ) ).build();

    @Description( "Specifies at which file size the logical log will auto-rotate. Minimum accepted value is 1M. " )
    public static final Setting<Long> logical_log_rotation_threshold =
            buildSetting( "dbms.tx_log.rotation.size", BYTES, "250M" ).constraint( min( ByteUnit.mebiBytes( 1 ) ) ).build();

    @Description( "Use a quick approach for rebuilding the ID generators. This give quicker recovery time, " +
            "but will limit the ability to reuse the space of deleted entities." )
    @Internal
    public static final Setting<Boolean> rebuild_idgenerators_fast =
            setting( "unsupported.dbms.id_generator_fast_rebuild_enabled", BOOLEAN, TRUE );

    // Store memory settings
    @Description( "Target size for pages of mapped memory. If set to 0, then a reasonable default is chosen, " +
                 "depending on the storage device used." )
    @Internal
    @Deprecated
    public static final Setting<Long> mapped_memory_page_size =
            setting( "unsupported.dbms.memory.pagecache.pagesize", BYTES, "0" );

    @SuppressWarnings( "unchecked" )
    @Description( "The amount of memory to use for mapping the store files, in bytes (or kilobytes with the 'k' " +
                  "suffix, megabytes with 'm' and gigabytes with 'g'). If Neo4j is running on a dedicated server, " +
                  "then it is generally recommended to leave about 2-4 gigabytes for the operating system, give the " +
                  "JVM enough heap to hold all your transaction state and query context, and then leave the rest for " +
                  "the page cache. If no page cache memory is configured, then a heuristic setting is computed based " +
                  "on available system resources." )
    public static final Setting<Long> pagecache_memory =
            buildSetting( "dbms.memory.pagecache.size", BYTES, null)
                    .constraint( min( 8192 * 30L ) ).build();

    @Description( "Specify which page swapper to use for doing paged IO. " +
                  "This is only used when integrating with proprietary storage technology." )
    public static final Setting<String> pagecache_swapper =
            setting( "dbms.memory.pagecache.swapper", STRING, null );

    /**
     * Block size properties values depends from selected record format.
     * We can't figured out record format until it will be selected by corresponding edition.
     * As soon as we will figure it out properties will be re-evaluated and overwritten, except cases of user
     * defined value.
     */
    @Description( "Specifies the block size for storing strings. This parameter is only honored when the store is " +
            "created, otherwise it is ignored. " +
            "Note that each character in a string occupies two bytes, meaning that e.g a block size of 120 will hold " +
            "a 60 character long string before overflowing into a second block. " +
            "Also note that each block carries a ~10B of overhead so record size on disk will be slightly larger " +
            "than the configured block size" )
    @Internal
    public static final Setting<Integer> string_block_size = buildSetting( "unsupported.dbms.block_size.strings", INTEGER,
            "0" ).constraint( min( 0 ) ).build();

    @Description( "Specifies the block size for storing arrays. This parameter is only honored when the store is " +
            "created, otherwise it is ignored. " +
            "Also note that each block carries a ~10B of overhead so record size on disk will be slightly larger " +
            "than the configured block size" )
    @Internal
    public static final Setting<Integer> array_block_size = buildSetting( "unsupported.dbms.block_size.array_properties",
            INTEGER, "0" ).constraint( min( 0 ) ).build();

    @Description( "Specifies the block size for storing labels exceeding in-lined space in node record. " +
            "This parameter is only honored when the store is created, otherwise it is ignored. " +
            "Also note that each block carries a ~10B of overhead so record size on disk will be slightly larger " +
            "than the configured block size" )
    @Internal
    public static final Setting<Integer> label_block_size = buildSetting( "unsupported.dbms.block_size.labels", INTEGER,
            "0" ).constraint( min( 0 ) ).build();

    @Description( "Specifies the size of id batches local to each transaction when committing. " +
            "Committing a transaction which contains changes most often results in new data records being created. " +
            "For each record a new id needs to be generated from an id generator. " +
            "It's more efficient to allocate a batch of ids from the contended id generator, which the transaction " +
            "holds and generates ids from while creating these new records. " +
            "This setting specifies how big those batches are. " +
            "Remaining ids are freed back to id generator on clean shutdown." )
    @Internal
    public static final Setting<Integer> record_id_batch_size = buildSetting( "unsupported.dbms.record_id_batch_size", INTEGER,
            "20" ).constraint( range( 1, 1_000 ) ).build();

    @Description( "An identifier that uniquely identifies this graph database instance within this JVM. " +
            "Defaults to an auto-generated number depending on how many instance are started in this JVM." )
    @Internal
    public static final Setting<String> forced_kernel_id = buildSetting( "unsupported.dbms.kernel_id", STRING, NO_DEFAULT ).constraint(
            illegalValueMessage( "has to be a valid kernel identifier", matches( "[a-zA-Z0-9]*" ) ) ).build();

    @SuppressWarnings( "unused" )
    @Description( "Amount of time in ms the GC monitor thread will wait before taking another measurement." )
    @Internal
    public static final Setting<Duration> gc_monitor_interval = MonitorGc.Configuration.gc_monitor_wait_time;

    @SuppressWarnings( "unused" )
    @Description( "The amount of time in ms the monitor thread has to be blocked before logging a message it was " +
            "blocked." )
    @Internal
    public static final Setting<Duration> gc_monitor_block_threshold = MonitorGc.Configuration.gc_monitor_threshold;

    @Description( "Relationship count threshold for considering a node to be dense" )
    public static final Setting<Integer> dense_node_threshold =
            buildSetting( "dbms.relationship_grouping_threshold", INTEGER, "50" ).constraint( min( 1 ) ).build();

    @Description( "Log executed queries that take longer than the configured threshold, dbms.logs.query.threshold. " +
            "Log entries are by default written to the file _query.log_ located in the Logs directory. " +
            "For location of the Logs directory, see <<file-locations>>. " +
            "This feature is available in the Neo4j Enterprise Edition." )
    @Dynamic
    public static final Setting<Boolean> log_queries =
            setting( "dbms.logs.query.enabled", BOOLEAN, FALSE );

    @Description( "Path of the logs directory." )
    public static final Setting<File> logs_directory = pathSetting( "dbms.directories.logs", "logs" );

    @Description( "Path to the query log file." )
    public static final Setting<File> log_queries_filename = derivedSetting( "dbms.logs.query.path",
            logs_directory,
            logs -> new File( logs, "query.log" ),
            PATH );

    @Description( "Path to the debug log file." )
    public static final Setting<File> store_internal_log_path = derivedSetting( "dbms.logs.debug.path",
            logs_directory,
            logs -> new File( logs, "debug.log" ),
            PATH );

    @Description( "Log parameters for the executed queries being logged." )
    public static final Setting<Boolean> log_queries_parameter_logging_enabled =
            setting( "dbms.logs.query.parameter_logging_enabled", BOOLEAN, TRUE );

    @Description( "Log detailed time information for the executed queries being logged." )
    public static final Setting<Boolean> log_queries_detailed_time_logging_enabled =
            setting( "dbms.logs.query.time_logging_enabled", BOOLEAN, FALSE );

    @Description( "Log allocated bytes for the executed queries being logged." )
    public static final Setting<Boolean> log_queries_allocation_logging_enabled =
            setting( "dbms.logs.query.allocation_logging_enabled", BOOLEAN, FALSE );

    @Description( "Log page hits and page faults for the executed queries being logged." )
    public static final Setting<Boolean> log_queries_page_detail_logging_enabled =
            setting( "dbms.logs.query.page_logging_enabled", BOOLEAN, FALSE );

    @Description( "If the execution of query takes more time than this threshold, the query is logged - " +
                 "provided query logging is enabled. Defaults to 0 seconds, that is all queries are logged." )
    @Dynamic
    public static final Setting<Duration> log_queries_threshold =
            setting( "dbms.logs.query.threshold", DURATION, "0s" );

    @Description( "The file size in bytes at which the query log will auto-rotate. If set to zero then no rotation " +
            "will occur. Accepts a binary suffix `k`, `m` or `g`." )
    public static final Setting<Long> log_queries_rotation_threshold = buildSetting( "dbms.logs.query.rotation.size",
            BYTES, "20m" ).constraint( range( 0L, Long.MAX_VALUE ) ).build();

    @Description( "Maximum number of history files for the query log." )
    public static final Setting<Integer> log_queries_max_archives = buildSetting( "dbms.logs.query.rotation.keep_number",
            INTEGER, "7" ).constraint( min( 1 ) ).build();

    @Description( "Specifies number of operations that batch inserter will try to group into one batch before " +
                  "flushing data into underlying storage." )
    @Internal
    public static final Setting<Integer> batch_inserter_batch_size = setting( "unsupported.tools.batch_inserter.batch_size", INTEGER,
            "10000" );

    /**
     * @deprecated - lucene label index has been removed.
     */
    @Deprecated
    public enum LabelIndex
    {
        /**
         * Native label index. Generally the best option.
         */
        NATIVE,

        /**
         * Label index backed by Lucene.
         */
        LUCENE,

        /**
         * Selects which ever label index is present in a store, or the default (NATIVE) if no label index present.
         */
        AUTO
    }

    /**
     * @deprecated - lucene label index has been removed, thus 'native' is only viable option and this setting is not needed.
     */
    @Deprecated
    @Description( "Backend to use for label --> nodes index" )
    @Internal
    public static final Setting<String> label_index =
            setting( "dbms.label_index", optionsIgnoreCase( LabelIndex.NATIVE.name(), LabelIndex.AUTO.name() ),
                    LabelIndex.NATIVE.name() );

    // Security settings

    @Description( "Enable auth requirement to access Neo4j." )
    public static final Setting<Boolean> auth_enabled = setting( "dbms.security.auth_enabled", BOOLEAN, FALSE );

    @Internal
    public static final Setting<File> auth_store =
            pathSetting( "unsupported.dbms.security.auth_store.location", NO_DEFAULT );

    @Internal
    public static final Setting<Integer> auth_max_failed_attempts =
            buildSetting( "unsupported.dbms.security.auth_max_failed_attempts", INTEGER, "3" ).constraint( min( 0 ) ).build();

    @Description( "A list of procedures and user defined functions (comma separated) that are allowed full access to " +
            "the database. The list may contain both fully-qualified procedure names, and partial names with the " +
            "wildcard '*'. Note that this enables these procedures to bypass security. Use with caution." )
    public static final Setting<String> procedure_unrestricted =
            setting( "dbms.security.procedures.unrestricted", Settings.STRING, "" );

    @Description( "Specifies whether or not dbms.killQueries produces a verbose output, with information about which queries were not found" )
    public static final Setting<Boolean> kill_query_verbose =
            setting( "dbms.procedures.kill_query_verbose", BOOLEAN, TRUE );

    @Description( "Whether or not to release the exclusive schema lock is while building uniqueness constraints index" )
    @Internal
    public static final Setting<Boolean> release_schema_lock_while_building_constraint = setting(
            "unsupported.dbms.schema.release_lock_while_building_constraint", BOOLEAN, FALSE );

    @Description( "A list of procedures (comma separated) that are to be loaded. " +
            "The list may contain both fully-qualified procedure names, and partial names with the wildcard '*'. " +
            "If this setting is left empty no procedures will be loaded." )
    public static final Setting<String> procedure_whitelist =
            setting( "dbms.security.procedures.whitelist", Settings.STRING, "*" );
    // Bolt Settings

    @Description( "Default network interface to listen for incoming connections. " +
            "To listen for connections on all interfaces, use \"0.0.0.0\". " +
            "To bind specific connectors to a specific network interfaces, " +
            "specify the +listen_address+ properties for the specific connector." )
    public static final Setting<String> default_listen_address =
            setting( "dbms.connectors.default_listen_address", STRING, "127.0.0.1" );

    @Description( "Default hostname or IP address the server uses to advertise itself to its connectors. " +
            "To advertise a specific hostname or IP address for a specific connector, " +
            "specify the +advertised_address+ property for the specific connector." )
    public static final Setting<String> default_advertised_address =
            setting( "dbms.connectors.default_advertised_address", STRING, "localhost" );

    @Internal
    public static final Setting<Boolean> bolt_logging_enabled = setting( "unsupported.dbms.logs.bolt.enabled",
            BOOLEAN, FALSE );

    @Internal
    public static final Setting<File> bolt_log_filename = derivedSetting( "unsupported.dbms.logs.bolt.path",
            GraphDatabaseSettings.logs_directory, logsDir -> new File( logsDir, "bolt.log" ), PATH );

    @Description( "Create an archive of an index before re-creating it if failing to load on startup." )
    @Internal
    public static final Setting<Boolean> archive_failed_index = setting(
            "unsupported.dbms.index.archive_failed", BOOLEAN, FALSE );

    // Needed to validate config, accessed via reflection
    @SuppressWarnings( "unused" )
    public static final BoltConnectorValidator boltValidator = new BoltConnectorValidator();

    // Needed to validate config, accessed via reflection
    @SuppressWarnings( "unused" )
    public static final SslPolicyConfigValidator sslPolicyConfigValidator = new SslPolicyConfigValidator();

    @Description( "The maximum amount of time to wait for the database state represented by the bookmark." )
    public static final Setting<Duration> bookmark_ready_timeout = buildSetting(
            "dbms.transaction.bookmark_ready_timeout", DURATION, "30s" ).constraint( min( Duration.ofSeconds( 1 ) ) ).build();

    // Needed to validate config, accessed via reflection
    @SuppressWarnings( "unused" )
    public static final HttpConnectorValidator httpValidator = new HttpConnectorValidator();

    /**
     * DEPRECATED: Use {@link org.neo4j.kernel.configuration.BoltConnector} instead. This will be removed in 4.0.
     */
    @Deprecated
    public static BoltConnector boltConnector( String key )
    {
        return new BoltConnector( key );
    }

    /**
     * DEPRECATED: Use {@link org.neo4j.kernel.configuration.Connector} instead. This will be removed in 4.0.
     */
    @Group( "dbms.connector" )
    public static class Connector
    {
        @Description( "Enable this connector" )
        public final Setting<Boolean> enabled;

        @Description( "Connector type. You should always set this to the connector type you want" )
        public final Setting<ConnectorType> type;

        // Note: Be careful about adding things here that does not apply to all connectors,
        //       consider future options like non-tcp transports, making `address` a bad choice
        //       as a setting that applies to every connector, for instance.

        public final GroupSettingSupport group;

        // Note: We no longer use the typeDefault parameter because it made for confusing behaviour;
        // connectors with unspecified would override settings of other, unrelated connectors.
        // However, we cannot remove the parameter at this
        public Connector( String key, @SuppressWarnings( "UnusedParameters" ) String typeDefault )
        {
            group = new GroupSettingSupport( Connector.class, key );
            enabled = group.scope( setting( "enabled", BOOLEAN, FALSE ) );
            type = group.scope( setting( "type", options( ConnectorType.class ), NO_DEFAULT ) );
        }

        public enum ConnectorType
        {
            BOLT, HTTP
        }

        public String key()
        {
            return group.groupKey;
        }
    }

    /**
     * DEPRECATED: Use {@link org.neo4j.kernel.configuration.BoltConnector} instead. This will be removed in 4.0.
     */
    @Deprecated
    @Description( "Configuration options for Bolt connectors. " +
            "\"(bolt-connector-key)\" is a placeholder for a unique name for the connector, for instance " +
            "\"bolt-public\" or some other name that describes what the connector is for." )
    public static class BoltConnector extends Connector
    {
        @Description( "Encryption level to require this connector to use" )
        public final Setting<EncryptionLevel> encryption_level;

        @Description( "Address the connector should bind to. " +
                "This setting is deprecated and will be replaced by `+listen_address+`" )
        public final Setting<ListenSocketAddress> address;

        @Description( "Address the connector should bind to" )
        public final Setting<ListenSocketAddress> listen_address;

        @Description( "Advertised address for this connector" )
        public final Setting<AdvertisedSocketAddress> advertised_address;

        // Used by config doc generator
        public BoltConnector()
        {
            this( "(bolt-connector-key)" );
        }

        public BoltConnector( String key )
        {
            super( key, null );
            encryption_level = group.scope(
                    setting( "tls_level", options( EncryptionLevel.class ), EncryptionLevel.OPTIONAL.name() ) );
            Setting<ListenSocketAddress> legacyAddressSetting = listenAddress( "address", 7687 );
            Setting<ListenSocketAddress> listenAddressSetting = legacyFallback( legacyAddressSetting,
                    listenAddress( "listen_address", 7687 ) );

            this.address = group.scope( legacyAddressSetting );
            this.listen_address = group.scope( listenAddressSetting );
            this.advertised_address = group.scope( advertisedAddress( "advertised_address", listenAddressSetting ) );
        }

        public enum EncryptionLevel
        {
            REQUIRED,
            OPTIONAL,
            DISABLED
        }
    }
}
