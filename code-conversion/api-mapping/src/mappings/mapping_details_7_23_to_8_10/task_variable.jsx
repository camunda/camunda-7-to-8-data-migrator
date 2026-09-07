/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
export const task_variable = [
	{
		origin: {
			path: "/task/{id}/variables",
			operation: "get",
		},
		target: {
			path: "/user-tasks/{userTaskKey}/variables/search",
			operation: "post",
		},
		direct: {
			rowInfo: [
				{
					leftEntry: <pre>(string) id</pre>,
					rightEntry: (
						<>
							<pre>(string) userTaskKey</pre>
							<p>
								See{" "}
								<a href="#key-to-id">
									Camunda 7 key → Camunda 8 id
								</a>
							</p>
						</>
					),
				},
			],
			additionalInfo: (
				<p>
					This endpoint returns all variables visible from the user
					task. The scope of each variable is provided by the field{" "}
					<code>scopeKey</code>.
				</p>
			),
		},
	},
	{
		origin: {
			path: "/task/{id}/variables",
			operation: "post",
		},
		target: {
			path: "/element-instances/{elementInstanceKey}/variables",
			operation: "put",
		},
		direct: {
			rowInfo: [
				{
					leftEntry: <pre>(object) modifications</pre>,
					rightEntry: <pre>(object) variables</pre>,
				},
			],
			additionalInfo: (
				<p>
					Set the boolean <code>local</code> to <code>false</code> to
					merge the variables into all parent scopes or until the
					variable is defined as a local variable.
				</p>
			),
		},
		discontinued: {
			rowInfo: [
				{
					leftEntry: <pre>(string) id</pre>,
					rightEntry: (
						<>
							<pre>(string) elementInstanceKey</pre>
							<p>
								The <code>elementInstanceKey</code> is not the{" "}
								<code>userTaskKey</code>. You can get the{" "}
								<code>elementInstanceKey</code> of the user task
								by retrieving the user task via the{" "}
								<code>GET Get user task</code> endpoint.
							</p>
						</>
					),
				},
				{
					leftEntry: <pre>(string[]) deletions</pre>,
					rightEntry: (
						<p>
							It is not possible to delete process variables in
							Camunda 8.10.
						</p>
					),
				},
			],
		},
	},
	{
		origin: {
			path: "/task/{id}/variables/{varName}",
			operation: "delete",
		},
		target: {},
		discontinuedExplanation:
			"There is no endpoint yet to delete a process variable in Camunda 8.10. You can update it to null or an empty string.",
	},
	{
		origin: {
			path: "/task/{id}/variables/{varName}",
			operation: "get",
		},
		target: {
			path: "/user-tasks/{userTaskKey}/variables/search",
			operation: "post",
		},
		direct: {
			rowInfo: [
				{
					leftEntry: <pre>(string) id</pre>,
					rightEntry: (
						<>
							<pre>(string) userTaskKey</pre>
							<p>
								See{" "}
								<a href="#key-to-id">
									Camunda 7 key → Camunda 8 id
								</a>
							</p>
						</>
					),
				},
				{
					leftEntry: <pre>(string) varName</pre>,
					rightEntry: <pre>(string) name</pre>,
				},
			],
			additionalInfo: (
				<p>
					This endpoint returns all variables visible from the user
					task. The scope of each variable is provided by the field{" "}
					<code>scopeKey</code>.
				</p>
			),
		},
	},
	{
		origin: {
			path: "/task/{id}/variables/{varName}",
			operation: "put",
		},
		target: {
			path: "/element-instances/{elementInstanceKey}/variables",
			operation: "put",
		},
		direct: {
			rowInfo: [
				{
					leftEntry: <pre>(string) id</pre>,
					rightEntry: (
						<>
							<pre>(string) elementInstanceKey</pre>
							<p>
								The <code>elementInstanceKey</code> is not the{" "}
								<code>userTaskKey</code>. You can get the{" "}
								<code>elementInstanceKey</code> of the user task
								by retrieving the user task via the{" "}
								<code>GET Get user task</code> endpoint.
							</p>
						</>
					),
				},
				{
					leftEntry: <pre>(string) varName</pre>,
					rightEntry: <pre>(string) variables[varName]</pre>,
				},
				{
					leftEntry: <pre>(any) value</pre>,
					rightEntry: <pre>(any) variables[varName]</pre>,
				},
			],
			additionalInfo: (
				<p>
					Send the value as{" "}
					<code>
						{"{ variables: { [varName]: value } }"}
					</code>
					. The default <code>local: false</code> behavior matches
					Camunda 7's visible-variable update, which writes to the
					outermost visible scope. Camunda 8 stores variables as JSON,
					so convert Camunda 7 serialized values before sending them;
					the Camunda 7 <code>type</code> and{" "}
					<code>valueInfo</code> metadata have no direct equivalent.
				</p>
			),
		},
		discontinued: {
			rowInfo: [
				{
					leftEntry: <pre>(string) type</pre>,
					rightEntry: (
						<p>
							Camunda 8 infers the variable type from the JSON
							value.
						</p>
					),
				},
				{
					leftEntry: <pre>(object) valueInfo</pre>,
					rightEntry: (
						<p>
							Camunda 8 does not accept Camunda 7 serialization
							metadata in this request.
						</p>
					),
				},
			],
		},
	},
	{
		origin: {
			path: "/task/{id}/variables/{varName}/data",
			operation: "get",
		},
		target: {
			path: "/documents/{documentId}",
			operation: "get",
		},
		mappedExplanation:
			"In Camunda 8.10, documents can be uploaded and downloaded via file picker components in forms. The documents are not directly related to user tasks. Instead, the documentIds can be saved in a process variable to access the documents later in the process instance.",
	},
	{
		origin: {
			path: "/task/{id}/variables/{varName}/data",
			operation: "post",
		},
		target: {
			path: "/documents",
			operation: "post",
		},
		mappedExplanation:
			"In Camunda 8.10, documents can be uploaded and downloaded via file picker components in forms. The documents are not directly related to user tasks. Instead, the documentIds can be saved in a process variable to access the documents later in the process instance.",
	},
];
