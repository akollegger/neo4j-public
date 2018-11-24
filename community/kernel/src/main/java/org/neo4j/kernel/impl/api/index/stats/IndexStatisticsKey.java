/*
 * Copyright (c) 2002-2018 "Neo4j,"
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
package org.neo4j.kernel.impl.api.index.stats;

import java.util.Objects;

class IndexStatisticsKey
{
    static final int SIZE = Byte.SIZE + Long.SIZE * 2;

    byte type;
    long first;
    long second;

    IndexStatisticsKey()
    {
    }

    IndexStatisticsKey( byte type, long first, long second )
    {
        this.type = type;
        this.first = first;
        this.second = second;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash( type, first, second );
    }

    @Override
    public boolean equals( Object obj )
    {
        if ( obj instanceof IndexStatisticsKey )
        {
            IndexStatisticsKey other = (IndexStatisticsKey) obj;
            return type == other.type && first == other.first && second == other.second;
        }
        return false;
    }

    @Override
    public String toString()
    {
        return "[type:" + type + ",first:" + first + ",second:" + second + "]";
    }
}
