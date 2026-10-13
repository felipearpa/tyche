# Verification harness

Runs the production stream and queue handlers, unchanged, against **isolated temporary**
tables, streams, and queues to check the settlement → positions → projection → ranked page
pipeline end to end (`integration`), and to measure it at scale (`load`, `latency`). The
`pagination` mode checks continuation tokens of every shared-codec query against DynamoDB
itself, and whole kickoff groups across storage pages and Timeline phases in the grouped History
and Timeline feeds, on newly created tables (see
[backend pagination](../../docs/backend-pagination.md#service-fixture)). It is not
a test project and never runs as part of `dotnet test`. It refuses to start when the
variables below point at the production names.

| Variable | Example |
|---|---|
| `POOL_TABLE_NAME`, `POOL_LAYOUT_TABLE_NAME` | `Pool-verify-<date>`, `PoolLayout-verify-<date>` (create with `operations/pool-table.sh create-copy`); the only variables `pagination` needs |
| `POOL_COMMAND_QUEUE_URL`, `POOL_COMMAND_DLQ_URL` | A FIFO queue and its dead-letter queue |
| `POOL_MAINTENANCE_QUEUE_URL` | A standard queue with a dead-letter queue |
| `AWS_REGION` | `us-east-2` |

```bash
dotnet run -- integration results.json
dotnet run -- kickoff-groups results.json         # kickoff-group settlement sequence (task 5.1)
dotnet run -- load results.json 5000 100 1000     # members, matches per member, viewers
dotnet run -- latency results.json 5000           # after load, on the same tables
dotnet run -- pagination results.json             # tables only; a few minutes at most
```

Long runs need a host that does not sleep; the recorded runs used a temporary in-region
instance (see the change's `verification.md`). Delete every temporary resource afterwards.
