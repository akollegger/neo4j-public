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
package org.neo4j.cypher.internal.compiler

import org.neo4j.cypher.internal.logical.plans.ProcedureSignature
import org.neo4j.cypher.internal.logical.plans.QualifiedName
import org.neo4j.cypher.internal.logical.plans.UserFunctionSignature
import org.neo4j.cypher.internal.planner.spi.IndexDescriptor
import org.neo4j.cypher.internal.planner.spi.InstrumentedGraphStatistics
import org.neo4j.cypher.internal.planner.spi.PlanContext
import org.neo4j.cypher.internal.util.InternalNotificationLogger

class NotImplementedPlanContext extends PlanContext {
  override def indexesGetForLabel(labelId: Int): Iterator[IndexDescriptor] = ???

  override def indexExistsForLabel(labelId: Int): Boolean = ???

  override def indexGetForLabelAndProperties(labelName: String, propertyKeys: Seq[String]): Option[IndexDescriptor] = ???

  override def indexExistsForLabelAndProperties(labelName: String, propertyKey: Seq[String]): Boolean = ???

  override def uniqueIndexesGetForLabel(labelId: Int): Iterator[IndexDescriptor] = ???

  override def hasPropertyExistenceConstraint(labelName: String, propertyKey: String): Boolean = ???

  override def getPropertiesWithExistenceConstraint(labelName: String): Set[String] = ???

  override def txIdProvider: () => Long = ???

  override def statistics: InstrumentedGraphStatistics = ???

  override def notificationLogger(): InternalNotificationLogger = ???

  override def txStateHasChanges(): Boolean = ???

  override def procedureSignature(name: QualifiedName): ProcedureSignature = ???

  override def functionSignature(name: QualifiedName): Option[UserFunctionSignature] = ???

  override def getLabelName(id: Int): String = ???

  override def getOptLabelId(labelName: String): Option[Int] = ???

  override def getLabelId(labelName: String): Int = ???

  override def getPropertyKeyName(id: Int): String = ???

  override def getOptPropertyKeyId(propertyKeyName: String): Option[Int] = ???

  override def getPropertyKeyId(propertyKeyName: String): Int = ???

  override def getRelTypeName(id: Int): String = ???

  override def getOptRelTypeId(relType: String): Option[Int] = ???

  override def getRelTypeId(relType: String): Int = ???
}
