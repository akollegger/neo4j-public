/*
 * Copyright (c) "Neo4j"
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
package org.neo4j.shell.log;

import org.neo4j.driver.Logger;

public class NullLogger implements Logger
{
    public static final Logger NULL_LOGGER = new NullLogger();

    public NullLogger()
    {
    }

    @Override
    public void error( String message, Throwable cause )
    {
    }

    @Override
    public void info( String message, Object... params )
    {
    }

    @Override
    public void warn( String message, Object... params )
    {
    }

    @Override
    public void warn( String message, Throwable cause )
    {
    }

    @Override
    public void debug( String message, Object... params )
    {
    }

    @Override
    public void debug( String s, Throwable throwable )
    {

    }

    @Override
    public void trace( String message, Object... params )
    {
    }

    @Override
    public boolean isTraceEnabled()
    {
        return false;
    }

    @Override
    public boolean isDebugEnabled()
    {
        return false;
    }
}
