/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
export const job = [
	{
		origin: {
			path: "/job",
			operation: "get",
		},
		target: {
			path: "/jobs/search",
			operation: "post",
		},
		mappedExplanation:
			"In Camunda 8.10, the POST Search jobs endpoint can be used to search for jobs without activating them.",
	},
	{
		origin: {
			path: "/job",
			operation: "post",
		},
		target: {
			path: "/jobs/search",
			operation: "post",
		},
		mappedExplanation:
			"In Camunda 8.10, the POST Search jobs endpoint can be used to search for jobs without activating them.",
	},
	{
		origin: {
			path: "/job/count",
			operation: "get",
		},
		target: {
			path: "/jobs/search",
			operation: "post",
		},
		mappedExplanation:
			"In Camunda 8.10, the POST Search jobs endpoint can be used to search for jobs. The response includes a page.totalItems field that provides the total count of matching jobs.",
	},
	{
		origin: {
			path: "/job/count",
			operation: "post",
		},
		target: {
			path: "/jobs/search",
			operation: "post",
		},
		mappedExplanation:
			"In Camunda 8.10, the POST Search jobs endpoint can be used to search for jobs. The response includes a page.totalItems field that provides the total count of matching jobs.",
	},
	{
		origin: {
			path: "/job/retries",
			operation: "post",
		},
		target: {
			path: "/jobs/batch-update",
			operation: "post",
		},
		direct: {
			rowInfo: [
				{
					leftEntry: <pre>(string[]) jobIds</pre>,
					rightEntry: (
						<>
							<pre>(string[]) filter.jobKey.$in</pre>
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
					leftEntry: <pre>(object) jobQuery</pre>,
					rightEntry: <pre>(object) filter</pre>,
				},
				{
					leftEntry: <pre>(integer) retries</pre>,
					rightEntry: <pre>(int32) changeset.retries</pre>,
				},
			],
			additionalInfo: (
				<p>
					The Camunda 8.10 Update jobs (batch) endpoint is
					asynchronous; its batch operation key can be used to track
					progress.
				</p>
			),
		},
		discontinued: {
			rowInfo: [
				{
					leftEntry: <pre>(dateTime) dueDate</pre>,
					rightEntry: <p>Not applicable in Camunda 8.</p>,
				},
			],
			additionalInfo: "",
		},
	},
	{
		origin: {
			path: "/job/{id}/priority",
			operation: "put",
		},
		target: {
			path: "/jobs/{jobKey}",
			operation: "patch",
		},
		direct: {
			rowInfo: [
				{
					leftEntry: <pre>(string) id</pre>,
					rightEntry: <pre>(string) jobKey</pre>,
				},
				{
					leftEntry: <pre>(int32) priority</pre>,
					rightEntry: <pre>(int32) changeset.priority</pre>,
				},
			],
			additionalInfo: (
				<p>
					Job priority is supported by the Camunda 8.10 Update job
					endpoint.
				</p>
			),
		},
	},
	{
		origin: {
			path: "/job/{id}/retries",
			operation: "put",
		},
		target: {
			path: "/jobs/{jobKey}",
			operation: "patch",
		},
		direct: {
			rowInfo: [
				{
					leftEntry: <pre>(string) id</pre>,
					rightEntry: <pre>(string) jobKey</pre>,
				},
				{
					leftEntry: <pre>(int32) retries</pre>,
					rightEntry: <pre>(int32) changeset.retries</pre>,
				},
			],
			additionalInfo: (
				<p>
					The Camunda 8.10 Update job endpoint updates the retry count
					synchronously.
				</p>
			),
		},
		discontinued: {
			rowInfo: [
				{
					leftEntry: <pre>(dateTime) dueDate</pre>,
					rightEntry: <p>Not applicable in Camunda 8.</p>,
				},
			],
		},
	},
	{
		origin: {
			path: "/job/suspended",
			operation: "put",
		},
		target: {},
		discontinuedExplanation:
			"Not yet possible in Camunda 8.10. Activating/suspending a job is not supported.",
	},
	{
		origin: {
			path: "/job/{id}",
			operation: "delete",
		},
		target: {},
		discontinuedExplanation: "It is not possible to delete a job in Camunda 8.10.",
	},
	{
		origin: {
			path: "/job/{id}",
			operation: "get",
		},
		target: {
			path: "/jobs/search",
			operation: "post",
		},
		mappedExplanation:
			"In Camunda 8.10, the POST Search jobs endpoint can be used to retrieve a specific job by filtering on jobKey.",
	},
	{
		origin: {
			path: "/job/{id}/duedate",
			operation: "put",
		},
		target: {},
		discontinuedExplanation: "DueDate is not applicable in Camunda 8.",
	},
	{
		origin: {
			path: "/job/{id}/duedate/recalculate",
			operation: "post",
		},
		target: {},
		discontinuedExplanation: "DueDate is not applicable in Camunda 8.",
	},
];
