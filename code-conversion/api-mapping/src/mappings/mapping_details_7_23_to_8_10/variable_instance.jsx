/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
export const variable_instance = [
	{
		origin: {
			path: "/variable-instance",
			operation: "get",
		},
		target: {
			path: "/variables/search",
			operation: "post",
		},
		direct: {
			rowInfo: [
				{
					leftEntry: (
						<pre>
							(string) variableName
							<br />
							(string) variableNameLike
						</pre>
					),
					rightEntry: (
						<pre>
							(string) filter.name
							<br />
							(string) filter.name.$like
						</pre>
					),
				},
				{
					leftEntry: (
						<pre>
							(string[]) processInstanceIdIn
							<br />
							(string[]) executionIdIn
							<br />
							(string[]) taskIdIn
							<br />
							(string[]) activityInstanceIdIn
							<br />
							(string[]) variableScopeIdIn
						</pre>
					),
					rightEntry: (
						<>
							<pre>
								(string[]) filter.processInstanceKey.$in
								<br />
								(string[]) filter.scopeKey.$in
							</pre>
							<p>
								Resolve Camunda 7 execution, task, activity, and
								scope IDs to the corresponding Camunda 8 element
								instance keys before applying{" "}
								<code>filter.scopeKey</code>.
							</p>
						</>
					),
				},
				{
					leftEntry: <pre>(string[]) tenantIdIn</pre>,
					rightEntry: (
						<>
							<pre>(string) filter.tenantId</pre>
							<p>
								The Camunda 8 filter accepts one tenant ID. Issue
								one request per C7 tenant ID when multiple values
								are supplied.
							</p>
						</>
					),
				},
				{
					leftEntry: <pre>(string) variableValues</pre>,
					rightEntry: (
						<pre>
							(string) filter.name
							<br />
							(string) filter.value
						</pre>
					),
				},
				{
					leftEntry: (
						<pre>
							(string) sortBy
							<br />
							(string) sortOrder
						</pre>
					),
					rightEntry: (
						<pre>
							(string) sort[].field
							<br />
							(enum) sort[].order
						</pre>
					),
				},
				{
					leftEntry: (
						<pre>
							(integer) firstResult
							<br />
							(integer) maxResults
						</pre>
					),
					rightEntry: (
						<pre>
							(integer) page.from
							<br />
							(integer) page.limit
						</pre>
					),
				},
			],
			additionalInfo: (
				<>
					<p>
						The Camunda 8.10 Search variables endpoint returns
						variables directly defined at the requested scopes. It
						does not include variables inherited from parent scopes.
					</p>
					<p>
						Use the advanced <code>$in</code> and{" "}
						<code>$like</code> operators for array and pattern
						criteria where the target filter supports them. Variable
						values in{" "}
						<code>filter.value</code> must use their serialized JSON
						representation. For multiple{" "}
						<code>variableValues</code> entries, issue separate
						requests because the Camunda 8 filter contains one name
						and value pair.
					</p>
					<p>
						Map <code>variableName</code> to{" "}
						<code>sort[].field=name</code>,{" "}
						<code>activityInstanceId</code> to{" "}
						<code>sort[].field=scopeKey</code>, and{" "}
						<code>tenantId</code> to{" "}
						<code>sort[].field=tenantId</code>. There is no Camunda
						8 sort field for <code>variableType</code>.
					</p>
					<p>
						Include <code>?truncateValues=false</code> to return
						complete variable values.
					</p>
				</>
			),
		},
		discontinued: {
			rowInfo: [
				{
					leftEntry: (
						<pre>
							(string[]) caseInstanceIdIn
							<br />
							(string[]) caseExecutionIdIn
							<br />
							(string[]) batchIdIn
						</pre>
					),
					rightEntry: <p>Camunda 8.10 has no equivalent filter.</p>,
				},
				{
					leftEntry: (
						<pre>
							(boolean) variableNamesIgnoreCase
							<br />
							(boolean) variableValuesIgnoreCase
						</pre>
					),
					rightEntry: <p>Camunda 8.10 has no equivalent option.</p>,
				},
				{
					leftEntry: <pre>(boolean) deserializeValues</pre>,
					rightEntry: (
						<p>
							Camunda 8 stores variable values as JSON. Use{" "}
							<code>truncateValues=false</code> when the complete
							value is required.
						</p>
					),
				},
				{
					leftEntry: <pre>(string) sortBy=variableType</pre>,
					rightEntry: <p>Camunda 8.10 has no equivalent sort field.</p>,
				},
			],
		},
	},
	{
		origin: {
			path: "/variable-instance",
			operation: "post",
		},
		target: {
			path: "/variables/search",
			operation: "post",
		},
		mappedExplanation: (
			<p>
				Use the same filter, sorting, and pagination mappings as the GET
				<code>/variable-instance</code> endpoint. The Camunda 8 request
				body uses <code>filter</code>, <code>sort</code>, and{" "}
				<code>page</code> at the root level.
			</p>
		),
		direct: {
			rowInfo: [
				{
					leftEntry: <pre>(object[]) variableValues</pre>,
					rightEntry: (
						<pre>
							(string) filter.name
							<br />
							(string) filter.value
						</pre>
					),
				},
				{
					leftEntry: <pre>(object[]) sorting</pre>,
					rightEntry: (
						<pre>
							(string) sort[].field
							<br />
							(enum) sort[].order
						</pre>
					),
				},
				{
					leftEntry: (
						<pre>
							(integer) firstResult
							<br />
							(integer) maxResults
						</pre>
					),
					rightEntry: (
						<pre>
							(integer) page.from
							<br />
							(integer) page.limit
						</pre>
					),
				},
			],
			additionalInfo: (
				<p>
					See the GET <code>/variable-instance</code> mapping for
					filter conversions, scope-key resolution, and unsupported
					parameters. Use one Camunda 8 request per{" "}
					<code>variableValues</code> name/value pair.
				</p>
			),
		},
		discontinued: {
			rowInfo: [
				{
					leftEntry: (
						<pre>
							(boolean) variableNamesIgnoreCase
							<br />
							(boolean) variableValuesIgnoreCase
							<br />
							(boolean) deserializeValues
						</pre>
					),
					rightEntry: <p>Camunda 8.10 has no equivalent option.</p>,
				},
				{
					leftEntry: <pre>(string) sorting[].sortBy=variableType</pre>,
					rightEntry: <p>Camunda 8.10 has no equivalent sort field.</p>,
				},
			],
		},
	},
	{
		origin: {
			path: "/variable-instance/count",
			operation: "get",
		},
		target: {
			path: "/variables/search",
			operation: "post",
		},
		mappedExplanation: (
			<p>
				Use the same filters as the GET{" "}
				<code>/variable-instance</code> mapping and read{" "}
				<code>page.totalItems</code> from the Camunda 8 search response.
			</p>
		),
	},
	{
		origin: {
			path: "/variable-instance/count",
			operation: "post",
		},
		target: {
			path: "/variables/search",
			operation: "post",
		},
		mappedExplanation: (
			<p>
				Use the same filters as the POST{" "}
				<code>/variable-instance</code> mapping and read{" "}
				<code>page.totalItems</code> from the Camunda 8 search response.
				Sorting is not needed for a count.
			</p>
		),
	},
	{
		origin: {
			path: "/variable-instance/{id}",
			operation: "get",
		},
		target: {
			path: "/variables/{variableKey}",
			operation: "get",
		},
		direct: {
			rowInfo: [
				{
					leftEntry: <pre>(string) id</pre>,
					rightEntry: (
						<>
							<pre>(string) variableKey</pre>
							<p>
								See{" "}
								<a href="#key-to-id">
									Camunda 7 key to Camunda 8 id
								</a>
							</p>
						</>
					),
				},
			],
			additionalInfo: (
				<p>
					The Camunda 8 response contains the complete JSON value. The
					Camunda 7 <code>deserializeValue</code> option has no direct
					equivalent.
				</p>
			),
		},
		discontinued: {
			rowInfo: [
				{
					leftEntry: <pre>(boolean) deserializeValue</pre>,
					rightEntry: <p>Camunda 8 stores variable values as JSON.</p>,
				},
			],
		},
	},
	{
		origin: {
			path: "/variable-instance/{id}/data",
			operation: "get",
		},
		target: {},
		discontinuedExplanation: (
			<p>
				Camunda 8.10 does not provide a binary variable download endpoint.
				Use <code>GET /variables/{"{variableKey}"}</code> for the full
				JSON value when the variable is JSON-compatible.
			</p>
		),
	},
];
