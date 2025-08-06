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

import React, {useState, useEffect, useMemo} from "react";
import {createColumnHelper} from '@tanstack/react-table';
import PaginatedTable from "./PaginatedTable";
import {ENTITY_TYPES} from "./utils/types";
import {injectLiveReload} from "./utils/utils";

function SkippedEntities({camundaAPI}) {
  const [skippedEntities, setSkippedEntities] = useState([]);
  const [selectedType, setSelectedType] = useState(ENTITY_TYPES.RUNTIME_PROCESS_INSTANCE);
  const [loading, setLoading] = useState(false);
  const [totalCount, setTotalCount] = useState(0);
  const [processInstanceIds, setProcessInstanceIds] = useState({});
  const [showSkipped, setShowSkipped] = useState(true);
  const [viewMode, setViewMode] = useState('runtime'); // New state for runtime/history mode

  const cockpitApi = camundaAPI.cockpitApi;
  const engine = camundaAPI.engine;
  const restApi = '/engine-rest';

  const getEntityLink = (entity) => {
    if (entity.type === ENTITY_TYPES.RUNTIME_PROCESS_INSTANCE) {
      return <a href={`#/process-instance/${entity.id}/runtime`}>{entity.id}</a>;
    } else if (entity.type === ENTITY_TYPES.HISTORY_PROCESS_INSTANCE) {
      return <a href={`#/process-instance/${entity.id}/history`}>{entity.id}</a>;
    } else {
      return entity.id;
    }
  }

  const columnHelper = createColumnHelper();

  const columns = useMemo(
    () => {
      let baseColumns = [
        columnHelper.accessor('id', {
          header: 'C7 Legacy ID',
          cell: info => getEntityLink(info.row.original),
          size: 370
        })];

      if (!showSkipped) {
        baseColumns.push(columnHelper.accessor('instanceKey', {
          header: 'C8 Key',
          cell: info => info.getValue(),
        }));
      }

      if (viewMode === 'history') {
        baseColumns.push(
          columnHelper.accessor('type', {
            header: 'Type',
            cell: info => <code>{info.getValue()}</code>,
          }));
      }

      baseColumns = [...baseColumns,
        columnHelper.accessor('skipReason', {
          header: 'Skip reason',
          cell: info => info.getValue(),
        }),
      ];

      // Add process instance column for HISTORY_VARIABLE type
      if (selectedType === ENTITY_TYPES.HISTORY_VARIABLE) {
        baseColumns.splice(1, 0,
          columnHelper.accessor('id', {
            id: 'processInstanceId',
            header: 'Process Instance ID',
            cell: info => {
              const variableId = info.getValue();
              const processInstanceId = processInstanceIds[variableId];

              return processInstanceId ?
                <a href={`#/process-instance/${processInstanceId}/history`}>{processInstanceId}</a> :
                <span>Loading...</span>;
            },
          })
        );
      }

      return baseColumns;
    },
    [selectedType, processInstanceIds, showSkipped, viewMode]
  );

  const getEntityTypeLabel = (entityType) => {
    const labels = {
      HISTORY_PROCESS_DEFINITION: "Process Definition",
      HISTORY_PROCESS_INSTANCE: "Process Instance",
      HISTORY_INCIDENT: "Incident",
      HISTORY_VARIABLE: "Variable",
      HISTORY_USER_TASK: "User Task",
      HISTORY_FLOW_NODE: "Flow Node",
      HISTORY_DECISION_INSTANCE: "Decision Instance",
      HISTORY_DECISION_DEFINITION: "Decision Definition",
      RUNTIME_PROCESS_INSTANCE: "Process Instance"
    };
    return labels[entityType] || entityType;
  };

  // Filter entity types based on view mode
  const getAvailableEntityTypes = () => {
    if (viewMode === 'runtime') {
      return {RUNTIME_PROCESS_INSTANCE: ENTITY_TYPES.RUNTIME_PROCESS_INSTANCE};
    } else {
      // Return all history types
      const historyTypes = {};
      Object.entries(ENTITY_TYPES).forEach(([key, value]) => {
        if (key.startsWith('HISTORY_')) {
          historyTypes[key] = value;
        }
      });
      return historyTypes;
    }
  };

  const fetchTotalCount = async () => {
    try {
      const endpoint = showSkipped ? 'skipped' : 'migrated';
      const response = await fetch(
        `${cockpitApi}/plugin/migrator-plugin/${engine}/migrator/${endpoint}/count?type=${selectedType}`,
        {
          headers: {
            'Accept': 'text/plain'
          }
        }
      );
      const count = await response.json();
      setTotalCount(typeof count === 'number' ? count : count.total || count.count || 0);
    } catch (err) {
      console.error('Failed to fetch total count:', err);
      setTotalCount(0);
    }
  };

  const fetchData = async (pageIndex = 0, pageSize = 10) => {
    setLoading(true);
    try {
      // If totalCount is 0, short-circuit and set empty array
      if (totalCount === 0) {
        setSkippedEntities([]);
        return;
      }

      const offset = pageIndex * pageSize;
      const endpoint = showSkipped ? 'skipped' : 'migrated';
      const response = await fetch(
        `${cockpitApi}/plugin/migrator-plugin/${engine}/migrator/${endpoint}?type=${selectedType}&offset=${offset}&limit=${pageSize}`,
        {
          headers: {
            'Accept': 'application/json'
          }
        }
      );
      const data = await response.json();
      const entities = Array.isArray(data) ? data : data.items || [];
      setSkippedEntities(entities);

      // Fetch process instance IDs for HISTORY_VARIABLE entities
      if (selectedType === ENTITY_TYPES.HISTORY_VARIABLE && entities.length > 0) {
        fetchProcessInstanceIds(entities);
      }
    } catch (err) {
      console.error(err);
      setSkippedEntities([]);
    } finally {
      setLoading(false);
    }
  };

  // Fetch process instance IDs for HISTORY_VARIABLE entities
  const fetchProcessInstanceIds = async (entities) => {
    const newProcessInstanceIds = {...processInstanceIds};
    let hasChanges = false;

    const fetchPromises = entities.map(async (entity) => {
      // Skip if we already have the process instance ID
      if (newProcessInstanceIds[entity.id]) return;

      try {
        const response = await fetch(
          `${restApi}/history/variable-instance/${entity.id}`,
          {
            headers: {
              'Accept': 'application/json'
            }
          }
        );
        const data = await response.json();

        if (data && data.processInstanceId) {
          newProcessInstanceIds[entity.id] = data.processInstanceId;
          hasChanges = true;
        }
      } catch (err) {
        console.error(`Failed to fetch process instance ID for variable ${entity.id}:`, err);
      }
    });

    await Promise.all(fetchPromises);

    if (hasChanges) {
      // Force update with a new object reference to ensure React detects the change
      setProcessInstanceIds({...newProcessInstanceIds});
    }
  };

  // Initial data load and when entity type changes
  useEffect(() => {
    // Inject LiveReload for development
    injectLiveReload();

    // Reset process instance IDs when changing entity type
    if (selectedType !== ENTITY_TYPES.HISTORY_VARIABLE) {
      setProcessInstanceIds({});
    }

    // When switching to runtime mode, set entity type to runtime process instance
    if (viewMode === 'runtime') {
      setSelectedType(ENTITY_TYPES.RUNTIME_PROCESS_INSTANCE);
    } else if (viewMode === 'history' && selectedType === ENTITY_TYPES.RUNTIME_PROCESS_INSTANCE) {
      // When switching to history mode from runtime, default to history process instance
      setSelectedType(ENTITY_TYPES.HISTORY_PROCESS_INSTANCE);
    }

    // Fetch total count first, then fetch data
    fetchTotalCount().then(() => {
      fetchData(0, 10);
    });
  }, [selectedType, showSkipped, viewMode]); // Add viewMode to dependency array

  // Handle pagination changes
  const handlePageChange = (pageIndex, pageSize) => {
    fetchData(pageIndex, pageSize);
  };

  if (loading && skippedEntities.length === 0) {
    return <div>Loading...</div>;
  }

  return (
    <>
      <section>
        <div className="inner">
          <header>
            <h1 style={{float: 'left'}} className="section-title">Camunda 7 to 8 Data Migrator</h1>

            <div style={{marginTop: '5px', float: 'right'}}>
              <label style={{marginRight: '20px'}}>
                <input
                  type="radio"
                  value="skipped"
                  checked={showSkipped}
                  onChange={() => setShowSkipped(true)}
                  style={{marginRight: '5px'}}
                />
                Skipped
              </label>
              <label>
                <input
                  type="radio"
                  value="migrated"
                  checked={!showSkipped}
                  onChange={() => setShowSkipped(false)}
                  style={{marginRight: '5px'}}
                />
                Migrated
              </label>
              <label style={{marginRight: '40px'}}>
              </label>
              <label style={{marginRight: '20px'}}>
                <input
                  type="radio"
                  value="runtime"
                  checked={viewMode === 'runtime'}
                  onChange={() => setViewMode('runtime')}
                  style={{marginRight: '5px'}}
                />
                Running
              </label>
              <label>
                <input
                  type="radio"
                  value="history"
                  checked={viewMode === 'history'}
                  onChange={() => setViewMode('history')}
                  style={{marginRight: '5px'}}
                />
                History (completed or running)
              </label>
            </div>
          </header>

          <div style={{marginBottom: '20px'}}>
            {viewMode === 'history' && (
              <div>
                <label htmlFor="type-selector" style={{marginRight: '10px'}}>
                  Type:
                </label>
                <select
                  id="type-selector"
                  value={selectedType}
                  onChange={(e) => setSelectedType(e.target.value)}
                  style={{padding: '5px', minWidth: '200px'}}
                >
                  {Object.entries(getAvailableEntityTypes()).map(([key, value]) => (
                    <option key={key} value={value}>
                      {getEntityTypeLabel(value)}
                    </option>
                  ))}
                </select>
              </div>
            )}
          </div>

          <PaginatedTable
            columns={columns}
            data={skippedEntities}
            totalCount={totalCount}
            loading={loading}
            onPageChange={handlePageChange}
            initialPageSize={10}
            pageSizeOptions={[5, 10, 20, 30, 40, 50]}
          />
        </div>
      </section>
    </>
  );
}

export default SkippedEntities;
