/*
 * Copyright (c) 2002-2020 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
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
package org.neo4j.kernel.impl.transaction.log.files.checkpoint;

import java.io.IOException;
import java.time.Instant;

import org.neo4j.kernel.impl.transaction.log.LogPosition;
import org.neo4j.kernel.impl.transaction.log.TransactionLogWriter;
import org.neo4j.kernel.impl.transaction.log.checkpoint.CheckpointAppender;
import org.neo4j.kernel.impl.transaction.log.files.LogFile;
import org.neo4j.kernel.impl.transaction.log.files.TransactionLogFilesContext;
import org.neo4j.kernel.impl.transaction.tracing.LogCheckPointEvent;
import org.neo4j.kernel.lifecycle.LifecycleAdapter;
import org.neo4j.monitoring.DatabaseHealth;

public class LegacyCheckpointAppender extends LifecycleAdapter implements CheckpointAppender
{
    private final LogFile logFile;
    private final DatabaseHealth databaseHealth;
    private TransactionLogWriter transactionLogWriter;

    public LegacyCheckpointAppender( LogFile logFile, TransactionLogFilesContext context )
    {
        this.logFile = logFile;
        this.databaseHealth = context.getDatabaseHealth();
    }

    @Override
    public void start() throws Exception
    {
        this.transactionLogWriter = logFile.getTransactionLogWriter();
    }

    @Override
    public void checkPoint( LogCheckPointEvent logCheckPointEvent, LogPosition logPosition, Instant checkpointTime, String reason )
            throws IOException
    {
        // Synchronized with logFile to get absolute control over concurrent rotations happening
        synchronized ( logFile )
        {
            try
            {
                var logPositionBeforeCheckpoint = transactionLogWriter.getCurrentPosition();
                transactionLogWriter.legacyCheckPoint( logPosition );
                var logPositionAfterCheckpoint = transactionLogWriter.getCurrentPosition();
                logCheckPointEvent.appendToLogFile( logPositionBeforeCheckpoint, logPositionAfterCheckpoint );
            }
            catch ( Throwable cause )
            {
                databaseHealth.panic( cause );
                throw cause;
            }
        }
        logFile.forceAfterAppend( logCheckPointEvent );
    }
}
