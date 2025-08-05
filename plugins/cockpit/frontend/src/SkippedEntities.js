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

// Function to inject LiveReload script for development
function injectLiveReload() {
  console.log("foo", process.env.NODE_ENV);
  if (process.env.NODE_ENV !== 'production' && !document.querySelector('script[src*="livereload.js"]')) {
    const script = document.createElement('script');
    script.src = 'http://localhost:35729/livereload.js';
    script.async = true;
    document.head.appendChild(script);
  }
}

function SkippedEntities({camundaAPI}) {
  const [skippedEntities, setSkippedEntities] = useState();
  const [selectedType, setSelectedType] = useState("RUNTIME_PROCESS_INSTANCE");

  const cockpitApi = camundaAPI.cockpitApi;
  const engine = camundaAPI.engine;

  const TYPE = {
    HISTORY_PROCESS_DEFINITION: "HISTORY_PROCESS_DEFINITION",
    HISTORY_PROCESS_INSTANCE: "HISTORY_PROCESS_INSTANCE",
    HISTORY_INCIDENT: "HISTORY_INCIDENT",
    HISTORY_VARIABLE: "HISTORY_VARIABLE",
    HISTORY_USER_TASK: "HISTORY_USER_TASK",
    HISTORY_FLOW_NODE: "HISTORY_FLOW_NODE",
    HISTORY_DECISION_INSTANCE: "HISTORY_DECISION_INSTANCE",
    HISTORY_DECISION_DEFINITION: "HISTORY_DECISION_DEFINITION",
    RUNTIME_PROCESS_INSTANCE: "RUNTIME_PROCESS_INSTANCE"
  }

  useEffect(() => {
    // Inject LiveReload for development
    injectLiveReload();

    fetch(
      `${cockpitApi}/plugin/migrator-plugin/${engine}/migrator/skipped?type=${selectedType}&offset=0&limit=10`,
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
  }, [selectedType]);

  if (!skippedEntities) {
    return <div>Loading...</div>;
  }

  return (
    <>
      <section>
        <div className="inner">
          <header>
            <h1 className="section-title">Camunda 7 to 8 Data Migrator</h1>
          </header>

          <div style={{ marginBottom: '20px' }}>
            <label htmlFor="type-selector" style={{ marginRight: '10px' }}>
              Entity Type:
            </label>
            <select
              id="type-selector"
              value={selectedType}
              onChange={(e) => setSelectedType(e.target.value)}
              style={{ padding: '5px', minWidth: '200px' }}
            >
              {Object.entries(TYPE).map(([key, value]) => (
                <option key={key} value={value}>
                  {key.replace(/_/g, ' ')}
                </option>
              ))}
            </select>
          </div>

          <Table
            head={
              <>
                <Table.Head key="processInstanceId">C7 ID</Table.Head>
                <Table.Head key="type">C8 Key</Table.Head>
                <Table.Head key="type">Type</Table.Head>
                <Table.Head key="type">Skip reason</Table.Head>
              </>
            }
          >
            {skippedEntities.map(entity => {
              return (
                <Table.Row key={entity.id}>
                  <Table.Cell key="processInstanceId">
                    <a href={`#/process-instance/${entity.id}/runtime`}>{entity.id}</a>
                  </Table.Cell>
                  <Table.Cell key="type">
                    {entity.instanceKey == null ? "❌" : entity.instanceKey + "✅"}
                  </Table.Cell>
                  <Table.Cell key="type">
                    <code>{entity.type}</code>
                  </Table.Cell>
                  <Table.Cell key="skipReason">
                    Found multi-instance loop characteristics for flow node with id [MessageTask_2] in C7 process instance.
                  </Table.Cell>
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
