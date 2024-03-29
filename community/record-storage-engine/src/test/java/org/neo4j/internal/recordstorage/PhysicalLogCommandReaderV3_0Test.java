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
package org.neo4j.internal.recordstorage;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import org.neo4j.kernel.impl.store.PropertyType;
import org.neo4j.kernel.impl.store.record.AbstractBaseRecord;
import org.neo4j.kernel.impl.store.record.NeoStoreRecord;
import org.neo4j.kernel.impl.store.record.NodeRecord;
import org.neo4j.kernel.impl.store.record.PropertyRecord;
import org.neo4j.kernel.impl.store.record.RelationshipGroupRecord;
import org.neo4j.kernel.impl.store.record.RelationshipRecord;
import org.neo4j.kernel.impl.transaction.log.InMemoryClosableChannel;
import org.neo4j.storageengine.api.CommandReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.neo4j.kernel.impl.store.record.Record.NULL_REFERENCE;

class PhysicalLogCommandReaderV3_0Test
{
    private static final long NULL_REF = NULL_REFERENCE.longValue();

    @Test
    void shouldReadRelationshipCommand() throws Throwable
    {
        // Given
        InMemoryClosableChannel channel = new InMemoryClosableChannel();
        RelationshipRecord before = new RelationshipRecord( 42 );
        before.setLinks( -1, -1, -1 );
        RelationshipRecord after = new RelationshipRecord( 42 );
        after.initialize( true, 0, 1, 2, 3, 4, 5, 6, 7, true, true );
        new Command.RelationshipCommand( before, after ).serialize( channel );

        // When
        PhysicalLogCommandReaderV3_0_10 reader = new PhysicalLogCommandReaderV3_0_10();
        Command command = reader.read( channel );
        assertTrue( command instanceof Command.RelationshipCommand );

        Command.RelationshipCommand relationshipCommand = (Command.RelationshipCommand) command;

        // Then
        assertEquals( before, relationshipCommand.getBefore() );
        assertEquals( after, relationshipCommand.getAfter() );
    }

    @Test
    void readRelationshipCommandWithSecondaryUnit() throws IOException
    {
        InMemoryClosableChannel channel = new InMemoryClosableChannel();
        RelationshipRecord before = new RelationshipRecord( 42 );
        before.initialize( true, 0, 1, 2, 3, 4, 5, 6, 7, true, true );
        before.setSecondaryUnitIdOnLoad( 47 );
        RelationshipRecord after = new RelationshipRecord( 42 );
        after.initialize( true, 0, 1, 8, 3, 4, 5, 6, 7, true, true );
        new Command.RelationshipCommand( before, after ).serialize( channel );

        PhysicalLogCommandReaderV3_0_10 reader = new PhysicalLogCommandReaderV3_0_10();
        Command command = reader.read( channel );
        assertTrue( command instanceof Command.RelationshipCommand );

        Command.RelationshipCommand relationshipCommand = (Command.RelationshipCommand) command;
        assertEquals( before, relationshipCommand.getBefore() );
        verifySecondaryUnit( before, relationshipCommand.getBefore() );
        assertEquals( after, relationshipCommand.getAfter() );
    }

    @Test
    void readRelationshipCommandWithNonRequiredSecondaryUnit() throws IOException
    {
        InMemoryClosableChannel channel = new InMemoryClosableChannel();
        RelationshipRecord before = new RelationshipRecord( 42 );
        before.initialize( true, 0, 1, 2, 3, 4, 5, 6, 7, true, true );
        before.setSecondaryUnitIdOnLoad( 52 );
        RelationshipRecord after = new RelationshipRecord( 42 );
        after.initialize( true, 0, 1, 8, 3, 4, 5, 6, 7, true, true );
        new Command.RelationshipCommand( before, after ).serialize( channel );

        PhysicalLogCommandReaderV3_0_10 reader = new PhysicalLogCommandReaderV3_0_10();
        Command command = reader.read( channel );
        assertTrue( command instanceof Command.RelationshipCommand );

        Command.RelationshipCommand relationshipCommand = (Command.RelationshipCommand) command;
        assertEquals( before, relationshipCommand.getBefore() );
        verifySecondaryUnit( before, relationshipCommand.getBefore() );
        assertEquals( after, relationshipCommand.getAfter() );
    }

    @Test
    void readRelationshipCommandWithFixedReferenceFormat300() throws IOException
    {
        InMemoryClosableChannel channel = new InMemoryClosableChannel();
        RelationshipRecord before = new RelationshipRecord( 42 );
        before.initialize( true, 0, 1, 2, 3, 4, 5, 6, 7, true, true );
        before.setUseFixedReferences( true );
        RelationshipRecord after = new RelationshipRecord( 42 );
        after.initialize( true, 0, 1, 8, 3, 4, 5, 6, 7, true, true );
        after.setUseFixedReferences( true );
        new Command.RelationshipCommand( before, after ).serialize( channel );

        PhysicalLogCommandReaderV3_0_10 reader = new PhysicalLogCommandReaderV3_0_10();
        Command command = reader.read( channel );
        assertTrue( command instanceof Command.RelationshipCommand );

        Command.RelationshipCommand relationshipCommand = (Command.RelationshipCommand) command;
        assertEquals( before, relationshipCommand.getBefore() );
        assertTrue( relationshipCommand.getBefore().isUseFixedReferences() );
        assertEquals( after, relationshipCommand.getAfter() );
        assertTrue( relationshipCommand.getAfter().isUseFixedReferences() );
    }

    @Test
    void readRelationshipCommandWithFixedReferenceFormat302() throws IOException
    {
        InMemoryClosableChannel channel = new InMemoryClosableChannel();
        RelationshipRecord before = new RelationshipRecord( 42 );
        before.initialize( true, 0, 1, 2, 3, 4, 5, 6, 7, true, true );
        before.setUseFixedReferences( true );
        RelationshipRecord after = new RelationshipRecord( 42 );
        after.initialize( true, 0, 1, 8, 3, 4, 5, 6, 7, true, true );
        after.setUseFixedReferences( true );
        new Command.RelationshipCommand( before, after ).serialize( channel );

        PhysicalLogCommandReaderV3_0_10 reader = new PhysicalLogCommandReaderV3_0_10();
        Command command = reader.read( channel );
        assertTrue( command instanceof Command.RelationshipCommand );

        Command.RelationshipCommand relationshipCommand = (Command.RelationshipCommand) command;
        assertEquals( before, relationshipCommand.getBefore() );
        assertTrue( relationshipCommand.getBefore().isUseFixedReferences() );
        assertEquals( after, relationshipCommand.getAfter() );
        assertTrue( relationshipCommand.getAfter().isUseFixedReferences() );
    }

    @Test
    void shouldReadRelationshipGroupCommand() throws Throwable
    {
        // Given
        InMemoryClosableChannel channel = new InMemoryClosableChannel();
        RelationshipGroupRecord before = new RelationshipGroupRecord( 42 ).initialize( false, 3, NULL_REF, NULL_REF, NULL_REF, NULL_REF, NULL_REF );
        RelationshipGroupRecord after = new RelationshipGroupRecord( 42 ).initialize( true, 3, 4, 5, 6, 7, 8 );
        after.setCreated();

        new Command.RelationshipGroupCommand( before, after ).serialize( channel );

        // When
        PhysicalLogCommandReaderV3_0_10 reader = new PhysicalLogCommandReaderV3_0_10();
        Command command = reader.read( channel );
        assertTrue( command instanceof Command.RelationshipGroupCommand);

        Command.RelationshipGroupCommand relationshipGroupCommand = (Command.RelationshipGroupCommand) command;

        // Then
        assertEquals( before, relationshipGroupCommand.getBefore() );
        assertEquals( after, relationshipGroupCommand.getAfter() );
    }

    @Test
    void readRelationshipGroupCommandWithSecondaryUnit() throws IOException
    {
        // Given
        InMemoryClosableChannel channel = new InMemoryClosableChannel();
        RelationshipGroupRecord before = new RelationshipGroupRecord( 42 ).initialize( false, 3, NULL_REF, NULL_REF, NULL_REF, NULL_REF, NULL_REF );
        RelationshipGroupRecord after = new RelationshipGroupRecord( 42 ).initialize( true, 3, 4, 5, 6, 7, 8 );
        after.setSecondaryUnitIdOnCreate( 17 );
        after.setCreated();

        new Command.RelationshipGroupCommand( before, after ).serialize( channel );

        // When
        PhysicalLogCommandReaderV3_0_10 reader = new PhysicalLogCommandReaderV3_0_10();
        Command command = reader.read( channel );
        assertTrue( command instanceof Command.RelationshipGroupCommand);

        Command.RelationshipGroupCommand relationshipGroupCommand = (Command.RelationshipGroupCommand) command;

        // Then
        assertEquals( before, relationshipGroupCommand.getBefore() );
        assertEquals( after, relationshipGroupCommand.getAfter() );
        verifySecondaryUnit( after, relationshipGroupCommand.getAfter() );
    }

    @Test
    void readRelationshipGroupCommandWithNonRequiredSecondaryUnit() throws IOException
    {
        // Given
        InMemoryClosableChannel channel = new InMemoryClosableChannel();
        RelationshipGroupRecord before = new RelationshipGroupRecord( 42 ).initialize( false, 3, NULL_REF, NULL_REF, NULL_REF, NULL_REF, NULL_REF );
        RelationshipGroupRecord after = new RelationshipGroupRecord( 42 ).initialize( true, 3, 4, 5, 6, 7, 8 );
        after.setSecondaryUnitIdOnCreate( 17 );
        after.setCreated();

        new Command.RelationshipGroupCommand( before, after ).serialize( channel );

        // When
        PhysicalLogCommandReaderV3_0_10 reader = new PhysicalLogCommandReaderV3_0_10();
        Command command = reader.read( channel );
        assertTrue( command instanceof Command.RelationshipGroupCommand);

        Command.RelationshipGroupCommand relationshipGroupCommand = (Command.RelationshipGroupCommand) command;

        // Then
        assertEquals( before, relationshipGroupCommand.getBefore() );
        assertEquals( after, relationshipGroupCommand.getAfter() );
        verifySecondaryUnit( after, relationshipGroupCommand.getAfter() );
    }

    @Test
    void readRelationshipGroupCommandWithFixedReferenceFormat300() throws IOException
    {
        // Given
        InMemoryClosableChannel channel = new InMemoryClosableChannel();
        RelationshipGroupRecord before = new RelationshipGroupRecord( 42 ).initialize( false, 3, NULL_REF, NULL_REF, NULL_REF, NULL_REF, NULL_REF );
        before.setUseFixedReferences( true );
        RelationshipGroupRecord after = new RelationshipGroupRecord( 42 ).initialize( true, 3, 4, 5, 6, 7, 8 );
        after.setUseFixedReferences( true );

        new Command.RelationshipGroupCommand( before, after ).serialize( channel );

        // When
        PhysicalLogCommandReaderV3_0_10 reader = new PhysicalLogCommandReaderV3_0_10();
        Command command = reader.read( channel );
        assertTrue( command instanceof Command.RelationshipGroupCommand);

        Command.RelationshipGroupCommand relationshipGroupCommand = (Command.RelationshipGroupCommand) command;

        // Then
        assertEquals( before, relationshipGroupCommand.getBefore() );
        assertEquals( after, relationshipGroupCommand.getAfter() );
        assertTrue( relationshipGroupCommand.getBefore().isUseFixedReferences() );
        assertTrue( relationshipGroupCommand.getAfter().isUseFixedReferences() );
    }

    @Test
    void readRelationshipGroupCommandWithFixedReferenceFormat302() throws IOException
    {
        // Given
        InMemoryClosableChannel channel = new InMemoryClosableChannel();
        RelationshipGroupRecord before = new RelationshipGroupRecord( 42 ).initialize( false, 3, NULL_REF, NULL_REF, NULL_REF, NULL_REF, NULL_REF );
        before.setUseFixedReferences( true );
        RelationshipGroupRecord after = new RelationshipGroupRecord( 42 ).initialize( true, 3, 4, 5, 6, 7, 8 );
        after.setUseFixedReferences( true );

        new Command.RelationshipGroupCommand( before, after ).serialize( channel );

        // When
        PhysicalLogCommandReaderV3_0_10 reader = new PhysicalLogCommandReaderV3_0_10();
        Command command = reader.read( channel );
        assertTrue( command instanceof Command.RelationshipGroupCommand);

        Command.RelationshipGroupCommand relationshipGroupCommand = (Command.RelationshipGroupCommand) command;

        // Then
        assertEquals( before, relationshipGroupCommand.getBefore() );
        assertEquals( after, relationshipGroupCommand.getAfter() );
        assertTrue( relationshipGroupCommand.getBefore().isUseFixedReferences() );
        assertTrue( relationshipGroupCommand.getAfter().isUseFixedReferences() );
    }

    @Test
    public void readRelationshipGroupWithBiggerThanShortRelationshipType() throws IOException
    {
        // Given
        InMemoryClosableChannel channel = new InMemoryClosableChannel();
        RelationshipGroupRecord before = new RelationshipGroupRecord( 42 ).initialize( false, 3, NULL_REF, NULL_REF, NULL_REF, NULL_REF, NULL_REF );
        RelationshipGroupRecord after = new RelationshipGroupRecord( 42 ).initialize( true, (1 << Short.SIZE) + 10, 4, 5, 6, 7, 8 );

        new Command.RelationshipGroupCommand( before, after ).serialize( channel );

        // When
        PhysicalLogCommandReaderV3_0_10 reader = new PhysicalLogCommandReaderV3_0_10();
        Command command = reader.read( channel );
        assertTrue( command instanceof Command.RelationshipGroupCommand);

        Command.RelationshipGroupCommand relationshipGroupCommand = (Command.RelationshipGroupCommand) command;

        // Then
        assertEquals( before, relationshipGroupCommand.getBefore() );
        assertEquals( after, relationshipGroupCommand.getAfter() );
    }

    @Test
    void shouldReadNeoStoreCommand() throws Throwable
    {
        // Given
        InMemoryClosableChannel channel = new InMemoryClosableChannel();
        NeoStoreRecord before = new NeoStoreRecord();
        NeoStoreRecord after = new NeoStoreRecord();
        after.setNextProp( 42 );

        new Command.NeoStoreCommand( before, after ).serialize( channel );

        // When
        PhysicalLogCommandReaderV3_0_10 reader = new PhysicalLogCommandReaderV3_0_10();
        Command command = reader.read( channel );
        assertTrue( command instanceof Command.NeoStoreCommand);

        Command.NeoStoreCommand neoStoreCommand = (Command.NeoStoreCommand) command;

        // Then
        assertEquals( before.getNextProp(), neoStoreCommand.getBefore().getNextProp() );
        assertEquals( after.getNextProp(), neoStoreCommand.getAfter().getNextProp() );
    }

    @Test
    void nodeCommandWithFixedReferenceFormat300() throws Exception
    {
        // Given
        InMemoryClosableChannel channel = new InMemoryClosableChannel();
        NodeRecord before = new NodeRecord( 42 ).initialize( true, 99, false, 33, 66 );
        NodeRecord after = new NodeRecord( 42 ).initialize( true, 99, false, 33, 66 );
        before.setUseFixedReferences( true );
        after.setUseFixedReferences( true );

        new Command.NodeCommand( before, after ).serialize( channel );

        // When
        PhysicalLogCommandReaderV3_0_10 reader = new PhysicalLogCommandReaderV3_0_10();
        Command command = reader.read( channel );
        assertTrue( command instanceof Command.NodeCommand);

        Command.NodeCommand nodeCommand = (Command.NodeCommand) command;

        // Then
        assertEquals( before, nodeCommand.getBefore() );
        assertEquals( after, nodeCommand.getAfter() );
        assertTrue( nodeCommand.getBefore().isUseFixedReferences() );
        assertTrue( nodeCommand.getAfter().isUseFixedReferences() );
    }

    @Test
    void nodeCommandWithFixedReferenceFormat302() throws Exception
    {
        // Given
        InMemoryClosableChannel channel = new InMemoryClosableChannel();
        NodeRecord before = new NodeRecord( 42 ).initialize( true, 99, false, 33, 66 );
        NodeRecord after = new NodeRecord( 42 ).initialize( true, 99, false, 33, 66 );
        before.setUseFixedReferences( true );
        after.setUseFixedReferences( true );

        new Command.NodeCommand( before, after ).serialize( channel );

        // When
        PhysicalLogCommandReaderV3_0_10 reader = new PhysicalLogCommandReaderV3_0_10();
        Command command = reader.read( channel );
        assertTrue( command instanceof Command.NodeCommand);

        Command.NodeCommand nodeCommand = (Command.NodeCommand) command;

        // Then
        assertEquals( before, nodeCommand.getBefore() );
        assertEquals( after, nodeCommand.getAfter() );
        assertTrue( nodeCommand.getBefore().isUseFixedReferences() );
        assertTrue( nodeCommand.getAfter().isUseFixedReferences() );
    }

    @Test
    void readPropertyCommandWithSecondaryUnit() throws IOException
    {
        InMemoryClosableChannel channel = new InMemoryClosableChannel();
        PropertyRecord before = new PropertyRecord( 1 );
        PropertyRecord after = new PropertyRecord( 2 );
        after.setSecondaryUnitIdOnCreate( 78 );

        new Command.PropertyCommand( before, after ).serialize( channel );

        PhysicalLogCommandReaderV3_0_10 reader = new PhysicalLogCommandReaderV3_0_10();
        Command command = reader.read( channel );
        assertTrue( command instanceof Command.PropertyCommand);

        Command.PropertyCommand neoStoreCommand = (Command.PropertyCommand) command;

        // Then
        assertEquals( before.getNextProp(), neoStoreCommand.getBefore().getNextProp() );
        assertEquals( after.getNextProp(), neoStoreCommand.getAfter().getNextProp() );
        verifySecondaryUnit( after, neoStoreCommand.getAfter() );
    }

    @Test
    void readPropertyCommandWithNonRequiredSecondaryUnit() throws IOException
    {
        InMemoryClosableChannel channel = new InMemoryClosableChannel();
        PropertyRecord before = new PropertyRecord( 1 );
        PropertyRecord after = new PropertyRecord( 2 );
        after.setSecondaryUnitIdOnCreate( 78 );

        new Command.PropertyCommand( before, after ).serialize( channel );

        PhysicalLogCommandReaderV3_0_10 reader = new PhysicalLogCommandReaderV3_0_10();
        Command command = reader.read( channel );
        assertTrue( command instanceof Command.PropertyCommand);

        Command.PropertyCommand neoStoreCommand = (Command.PropertyCommand) command;

        // Then
        assertEquals( before.getNextProp(), neoStoreCommand.getBefore().getNextProp() );
        assertEquals( after.getNextProp(), neoStoreCommand.getAfter().getNextProp() );
        verifySecondaryUnit( after, neoStoreCommand.getAfter() );
    }

    @Test
    void readPropertyCommandWithFixedReferenceFormat300() throws IOException
    {
        InMemoryClosableChannel channel = new InMemoryClosableChannel();
        PropertyRecord before = new PropertyRecord( 1 );
        PropertyRecord after = new PropertyRecord( 2 );
        before.setUseFixedReferences( true );
        after.setUseFixedReferences( true );

        new Command.PropertyCommand( before, after ).serialize( channel );

        PhysicalLogCommandReaderV3_0_10 reader = new PhysicalLogCommandReaderV3_0_10();
        Command command = reader.read( channel );
        assertTrue( command instanceof Command.PropertyCommand);

        Command.PropertyCommand neoStoreCommand = (Command.PropertyCommand) command;

        // Then
        assertEquals( before.getNextProp(), neoStoreCommand.getBefore().getNextProp() );
        assertEquals( after.getNextProp(), neoStoreCommand.getAfter().getNextProp() );
        assertTrue( neoStoreCommand.getBefore().isUseFixedReferences() );
        assertTrue( neoStoreCommand.getAfter().isUseFixedReferences() );
    }

    @Test
    void readPropertyCommandWithFixedReferenceFormat302() throws IOException
    {
        InMemoryClosableChannel channel = new InMemoryClosableChannel();
        PropertyRecord before = new PropertyRecord( 1 );
        PropertyRecord after = new PropertyRecord( 2 );
        before.setUseFixedReferences( true );
        after.setUseFixedReferences( true );

        new Command.PropertyCommand( before, after ).serialize( channel );

        PhysicalLogCommandReaderV3_0_10 reader = new PhysicalLogCommandReaderV3_0_10();
        Command command = reader.read( channel );
        assertTrue( command instanceof Command.PropertyCommand);

        Command.PropertyCommand neoStoreCommand = (Command.PropertyCommand) command;

        // Then
        assertEquals( before.getNextProp(), neoStoreCommand.getBefore().getNextProp() );
        assertEquals( after.getNextProp(), neoStoreCommand.getAfter().getNextProp() );
        assertTrue( neoStoreCommand.getBefore().isUseFixedReferences() );
        assertTrue( neoStoreCommand.getAfter().isUseFixedReferences() );
    }

    @Test
    void shouldReadSomeCommands() throws Exception
    {
        // GIVEN
        InMemoryClosableChannel channel = new InMemoryClosableChannel();
        Commands.createNode( 0 ).serialize( channel );
        Commands.createNode( 1 ).serialize( channel );
        Commands.createRelationshipTypeToken( 0, 0 ).serialize( channel );
        Commands.createRelationship( 0, 0, 1, 0 ).serialize( channel );
        Commands.createPropertyKeyToken( 0, 0 ).serialize( channel );
        Commands.createProperty( 0, PropertyType.SHORT_STRING, 0 ).serialize( channel );
        CommandReader reader = new PhysicalLogCommandReaderV3_0_10();

        // THEN
        assertTrue( reader.read( channel ) instanceof Command.NodeCommand );
        assertTrue( reader.read( channel ) instanceof Command.NodeCommand );
        assertTrue( reader.read( channel ) instanceof Command.RelationshipTypeTokenCommand );
        assertTrue( reader.read( channel ) instanceof Command.RelationshipCommand );
        assertTrue( reader.read( channel ) instanceof Command.PropertyKeyTokenCommand );
        assertTrue( reader.read( channel ) instanceof Command.PropertyCommand );
    }

    private <T extends AbstractBaseRecord> void verifySecondaryUnit( T record, T commandRecord )
    {
        assertEquals( record.requiresSecondaryUnit(),
                commandRecord.requiresSecondaryUnit(), "Secondary unit requirements should be the same" );
        assertEquals( record.getSecondaryUnitId(),
                commandRecord.getSecondaryUnitId(), "Secondary unit ids should be the same" );
    }
}
