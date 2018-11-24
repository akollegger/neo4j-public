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
package org.neo4j.kernel.impl.store.counts.keys;

public enum CountsKeyType
{
    EMPTY( 0 ),
    ENTITY_NODE( 2 ),
    ENTITY_RELATIONSHIP( 3 );

    public final byte code;

    CountsKeyType( int code )
    {
        this.code = (byte) code;
    }

    public static CountsKeyType value( byte val )
    {
        switch ( val )
        {
        case 2:
            return CountsKeyType.ENTITY_NODE;
        case 3:
            return CountsKeyType.ENTITY_RELATIONSHIP;
        case 4:
        case 5:
            // These are legacy entries, index statistics and index sample. They now live in IndexStatisticsStore
            return CountsKeyType.EMPTY;
        default:
            throw new IllegalArgumentException( "Parsed key type from count store deserialization of unknown type." );
        }
    }
}
