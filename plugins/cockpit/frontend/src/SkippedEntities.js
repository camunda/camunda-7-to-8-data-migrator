/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React, {useState, useEffect} from "react";

import {Table} from "./Table";

function SkippedEntities({camundaAPI}) {
  const [skippedEntities, setSkippedEntities] = useState();

  const cockpitApi = camundaAPI.cockpitApi;
  const engine = camundaAPI.engine;

  useEffect(() => {
    fetch(
      `${cockpitApi}/plugin/sample-plugin/${engine}/migrator/skipped?type=RUNTIME_PROCESS_INSTANCE&offset=0&limit=10`,
      {
        headers: {
          'Accept': 'application/json'
        }
      }
    )
      .then(async res => {
        setSkippedEntities(await res.json());
      })
      .catch(err => {
        console.error(err);
      });
  }, []);

  if (!skippedEntities) {
    return <div>Loading...</div>;
  }

  return (
    <>
      <section>
        <div className="inner">
          <h1 className="section-title">Camunda 7 to 8 Data Migrator</h1>

          <Table
            head={
              <>
                <Table.Head key="processInstanceId">Process Instance ID</Table.Head>
                <Table.Head key="type">Type</Table.Head>
              </>
            }
          >
            {skippedEntities.map(entity => {
              return (
                <Table.Row key={entity.id}>
                  <Table.Cell key="processInstanceId">{entity.id}</Table.Cell>
                  <Table.Cell key="type">{entity.type}</Table.Cell>
                </Table.Row>
              );
            })}
          </Table>
        </div>
      </section>
    </>
  );
}

export default SkippedEntities;
