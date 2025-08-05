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
  const [skippedEntities, setSkippedEntities] = useState([]);
  const [selectedType, setSelectedType] = useState("RUNTIME_PROCESS_INSTANCE");
  const [loading, setLoading] = useState(false);
  const [totalCount, setTotalCount] = useState(0);

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

  const getEntityLink = (entity) => {
    if (entity.type === TYPE.RUNTIME_PROCESS_INSTANCE) {
      return <a href={`#/process-instance/${entity.id}/runtime`}>{entity.id}</a>;
    } else if (entity.type === TYPE.HISTORY_PROCESS_INSTANCE) {
      return <a href={`#/process-instance/${entity.id}/history`}>{entity.id}</a>;
    } else {
      return entity.id;
    }
  }

  const columnHelper = createColumnHelper();

  const columns = useMemo(
    () => [
      columnHelper.accessor('id', {
        header: 'C7 ID',
        cell: info => getEntityLink(info.row.original),
      }),
      columnHelper.accessor('instanceKey', {
        header: 'C8 Key',
        cell: info => info.getValue() == null ? "❌" : info.getValue() + "✅",
      }),
      columnHelper.accessor('type', {
        header: 'Type',
        cell: info => <code>{info.getValue()}</code>,
      }),
      columnHelper.accessor('skipReason', {
        header: 'Skip reason',
        cell: () => 'Found multi-instance loop characteristics for flow node with id [MessageTask_2] in C7 process instance.',
      }),
    ],
    []
  );

  const fetchTotalCount = async () => {
    try {
      const response = await fetch(
        `${cockpitApi}/plugin/migrator-plugin/${engine}/migrator/skipped/count?type=${selectedType}`,
        {
          headers: {
            'Accept': 'application/json'
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
      const response = await fetch(
        `${cockpitApi}/plugin/migrator-plugin/${engine}/migrator/skipped?type=${selectedType}&offset=${offset}&limit=${pageSize}`,
        {
          headers: {
            'Accept': 'application/json'
          }
        }
      );
      const data = await response.json();
      setSkippedEntities(Array.isArray(data) ? data : data.items || []);
    } catch (err) {
      console.error(err);
      setSkippedEntities([]);
    } finally {
      setLoading(false);
    }
  };

  // Initial data load and when entity type changes
  useEffect(() => {
    // Inject LiveReload for development
    injectLiveReload();

    // Fetch total count first, then fetch data
    fetchTotalCount().then(() => {
      fetchData(0, 10);
    });
  }, [selectedType]);

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
