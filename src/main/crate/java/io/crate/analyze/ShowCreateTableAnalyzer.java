/*
 * Licensed to CRATE Technology GmbH ("Crate") under one or more contributor
 * license agreements.  See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.  Crate licenses
 * this file to you under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.  You may
 * obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * However, if you have executed another commercial license agreement
 * with Crate these terms will supersede the license and you may use the
 * software solely pursuant to the terms of the relevant commercial agreement.
 */

package io.crate.analyze;

import io.crate.exceptions.NoPermissionException;
import io.crate.metadata.Schemas;
import io.crate.metadata.TableIdent;
import io.crate.metadata.doc.DocTableInfo;
import io.crate.metadata.table.TableInfo;
import io.crate.sql.tree.Table;
import org.elasticsearch.authentication.AuthResult;
import org.elasticsearch.authentication.AuthService;
import org.elasticsearch.cluster.metadata.PrivilegeType;
import org.elasticsearch.common.inject.Inject;
import org.elasticsearch.common.inject.Singleton;
import org.elasticsearch.rest.RestStatus;

import javax.annotation.Nullable;

@Singleton
public class ShowCreateTableAnalyzer {

    private Schemas schemas;

    @Inject
    public ShowCreateTableAnalyzer(Schemas schemas) {
        this.schemas = schemas;
    }

    public ShowCreateTableAnalyzedStatement analyze(Table table, Analysis analysis) {
        TableInfo tableInfo = schemas.getTableInfo(TableIdent.of(table, analysis.parameterContext().defaultSchema()));
        if (!(tableInfo instanceof DocTableInfo)) {
            throw new UnsupportedOperationException("Table must be a doc table");
        }

        // Add SQL Authentication
        // GaoPan 2016/06/16
        AuthResult authResult = AuthService.sqlAuthenticate(analysis.parameterContext().getLoginUserContext(),
                tableInfo.ident().schema(), tableInfo.ident().name(), PrivilegeType.READ_ONLY);
        if (authResult.getStatus() != RestStatus.OK) {
            throw new NoPermissionException(authResult.getStatus().getStatus(), authResult.getMessage());
        }

        return new ShowCreateTableAnalyzedStatement((DocTableInfo) tableInfo);
    }
}
