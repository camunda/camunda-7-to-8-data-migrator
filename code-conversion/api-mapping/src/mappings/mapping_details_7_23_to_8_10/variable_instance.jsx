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
						<>
							<pre>
								(string) filter.name
								<br />
								(string) filter.name.$like
							</pre>
							<p>
								Translate each Camunda 7 <code>%</code> wildcard
								to the Camunda 8 <code>*</code> wildcard. Before
								sending <code>$like</code>, escape literal
								backslashes, <code>*</code>, and <code>?</code>{" "}
								with the Camunda 8 backslash escape so C7 literal
								characters do not become C8 wildcards. Camunda 8
								treats <code>%</code> literally.
							</p>
						</>
					),
				},
				{
					leftEntry: (
						<pre>(string[]) processInstanceIdIn</pre>
					),
					rightEntry: (
						<>
							<pre>(string[]) filter.processInstanceKey.$in</pre>
							<p>
								Resolve Camunda 7 process-instance IDs to the
								corresponding Camunda 8 process instance keys
								before applying this filter. Keep this predicate
								separate from scope selectors; when multiple source
								filter fields are supplied, intersect their complete
								result sets by <code>variableKey</code>.
							</p>
						</>
					),
				},
				{
					leftEntry: (
						<pre>
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
							<pre>(string[]) filter.scopeKey.$in</pre>
							<p>
								Resolve Camunda 7 execution, task, activity, and
								variable-scope IDs to the corresponding Camunda 8
								element instance keys. These are separate C7
								predicates: do not combine IDs from different source
								fields into one <code>$in</code> list. Issue one
								request per populated field and intersect the complete
								result sets by <code>variableKey</code>.
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
								The Camunda 8 filter accepts one tenant ID, while
								C7 treats <code>tenantIdIn</code> as an OR list.
								Issue one request per C7 tenant ID, retrieve all
								pages, then union and deduplicate the results by{" "}
								<code>variableKey</code> before applying pagination
								or deriving a count. Do not sum per-tenant totals.
							</p>
						</>
					),
				},
				{
					leftEntry: <pre>(string) variableValues</pre>,
					rightEntry: (
						<>
							<pre>
								(string) filter.name
								<br />
								(string) filter.value.$eq
								<br />
								(string) filter.value.$neq
								<br />
								(string) filter.value.$like
							</pre>
							<p>
								Parse each C7 expression as{" "}
								<code>name_operator_value</code>. Map{" "}
								<code>eq</code>, <code>neq</code>, and{" "}
								<code>like</code> to the corresponding C8
							operator. For <code>eq</code> and{" "}
							<code>neq</code>, JSON-encode the C7 string
							value, including its quotes, before setting{" "}
							<code>filter.value</code>. For{" "}
							<code>like</code>, escape literal backslashes,{" "}
							<code>*</code>, and <code>?</code>, translate{" "}
							<code>%</code> wildcards to <code>*</code>, and
							JSON-encode the resulting string pattern. C8
							has no numeric comparison operator for variable
							values, so{" "}
							<code>gt</code>, <code>gteq</code>,{" "}
							<code>lt</code>, and <code>lteq</code> are
							unsupported.
							</p>
						</>
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
						<>
							<pre>
								(string) sort[].field
								<br />
								(enum) sort[].order
							</pre>
							<p>
								Map Camunda 7 <code>asc</code> and{" "}
								<code>desc</code> to Camunda 8{" "}
								<code>ASC</code> and <code>DESC</code>.
							</p>
						</>
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
						representation. For GET requests, values are always C7
						strings, so JSON-encode them with their quotes (for
						example, <code>"5"</code>, not <code>5</code>). For
						POST requests, preserve number, boolean, and string
						types with <code>JSON.stringify</code>; string values
						also include quotes. For <code>like</code>, escape
						literal backslashes, <code>*</code>, and{" "}
						<code>?</code> before translating C7{" "}
						<code>%</code> wildcards to C8 <code>*</code>, then
						JSON-encode the complete pattern.
						Parse each{" "}
						<code>variableValues</code> expression as{" "}
						<code>name_operator_value</code> and map the supported{" "}
						<code>eq</code>, <code>neq</code>, and{" "}
						<code>like</code> operators. The C8 filter does not
						support the numeric comparison operators{" "}
						<code>gt</code>, <code>gteq</code>,{" "}
						<code>lt</code>, or <code>lteq</code>.
					</p>
					<p>
						For multiple <code>variableValues</code> entries, issue
						one request per entry and follow each response's{" "}
						<code>page.endCursor</code> with <code>page.after</code>{" "}
						until all pages are retrieved. Do not send C7{" "}
						<code>firstResult</code> as <code>page.from</code> in
						these cursor-paginated intermediate requests, and do not
						stop traversal at C7 <code>maxResults</code>. Then
						intersect the complete result sets by{" "}
						<code>variableKey</code> before applying the C7 offset
						and limit or deriving counts. Do not intersect only the
						first page or union the responses.
					</p>
					<p>
						Map <code>variableName</code> to{" "}
						<code>sort[].field=name</code>,{" "}
						<code>tenantId</code> to{" "}
						<code>sort[].field=tenantId</code>. C8{" "}
						<code>scopeKey</code> values do not preserve the C7{" "}
						<code>activityInstanceId</code> string ordering, so do
						not map that sort directly. Resolve activity-instance
						IDs and sort client-side after retrieving the complete
						result set, or mark the sort unsupported. There is no C8
						sort field for{" "}
						<code>variableType</code>.
					</p>
					<p>
						When multiple process-instance or scope selector fields
						are supplied, issue separate searches for each populated
						field and intersect their complete result sets by{" "}
						<code>variableKey</code>. Apply the C7 conjunction before
						pagination or counting; do not combine different source
						fields into one C8 <code>$in</code> filter.
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
						<>
							<pre>
								(string) filter.name
								<br />
								(string) filter.value.$eq
								<br />
								(string) filter.value.$neq
								<br />
								(string) filter.value.$like
							</pre>
							<p>
								Map each object's <code>operator</code> value{" "}
								<code>eq</code>, <code>neq</code>, or{" "}
								<code>like</code> to the corresponding C8
								operator. Preserve each POST value's JSON type
								with <code>JSON.stringify</code> before setting{" "}
								<code>filter.value</code>; string values include
								their JSON quotes. For <code>like</code>, escape
								literal backslashes, <code>*</code>, and{" "}
								<code>?</code>, translate C7{" "}
								<code>%</code> wildcards to C8{" "}
								<code>*</code>, and JSON-encode the complete
								string pattern. The numeric{" "}
								<code>gt</code>, <code>gteq</code>,{" "}
								<code>lt</code>, and <code>lteq</code>{" "}
								operators are unsupported for C8 variable
								values.
							</p>
						</>
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
					<code>variableValues</code> entry and follow{" "}
					<code>page.endCursor</code> with <code>page.after</code> for
					each request until all pages are retrieved. Do not send C7{" "}
					<code>firstResult</code> as <code>page.from</code> in these
					cursor-paginated intermediate requests or stop traversal at
					C7 <code>maxResults</code>. Then intersect the complete
					responses by <code>variableKey</code> before applying the C7
					offset and limit or deriving counts. Apply the same complete-
					page intersection to multiple process-instance or scope
					selector fields, and the complete-page union to multiple{" "}
					<code>tenantIdIn</code> values. Do not combine different source
					fields into one <code>$in</code> filter or union conjunctive
					<code>variableValues</code> responses.
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
				<code>page.totalItems</code> and{" "}
				<code>page.hasMoreTotalItems</code> from the Camunda 8 search
				response. When <code>hasMoreTotalItems</code> is{" "}
				<code>true</code>, <code>totalItems</code> is only a lower
				bound, not the exact C7 count. For multiple{" "}
				<code>tenantIdIn</code> values,{" "}
				<code>variableValues</code> entries, or process-instance and
				scope selector fields, follow <code>page.endCursor</code> for
				every request, apply the required complete-page union or
				intersection by <code>variableKey</code>, and count the
				deduplicated result. Do not sum capped totals or read one
				search total as the combined C7 count.
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
				<code>page.totalItems</code> and{" "}
				<code>page.hasMoreTotalItems</code> from the Camunda 8 search
				response. When <code>hasMoreTotalItems</code> is{" "}
				<code>true</code>, <code>totalItems</code> is only a lower
				bound, not the exact C7 count. For multiple{" "}
				<code>tenantIdIn</code> values,{" "}
				<code>variableValues</code> entries, or process-instance and
				scope selector fields, follow <code>page.endCursor</code> for
				every request, apply the required complete-page union or
				intersection by <code>variableKey</code>, and count the
				deduplicated result. Do not sum capped totals or read one
				search total as the combined C7 count. Sorting is not needed
				for a count.
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
								Camunda 8 variable keys are generated system keys
								and cannot be derived from a Camunda 7 variable
								instance ID. Use a migration-specific ID-to-key
								correlation before calling this endpoint; without
								that correlation, there is no direct mapping.
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
