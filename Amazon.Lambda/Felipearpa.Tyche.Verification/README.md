# Verification harness

Runs the production stream and queue handlers, unchanged, against **isolated temporary**
tables, streams, and queues to check the settlement → positions → projection → ranked page
pipeline end to end (`integration`), and to measure it at scale (`load`, `latency`). It is not
a test project and never runs as part of `dotnet test`. It refuses to start when the
variables below point at the production names.

| Variable | Example |
|---|---|
| `POOL_TABLE_NAME`, `POOL_LAYOUT_TABLE_NAME` | `Pool-verify-<date>`, `PoolLayout-verify-<date>` (create with `operations/pool-table.sh create-copy`) |
| `POOL_COMMAND_QUEUE_URL`, `POOL_COMMAND_DLQ_URL` | A FIFO queue and its dead-letter queue |
| `POOL_MAINTENANCE_QUEUE_URL` | A standard queue with a dead-letter queue |
| `AWS_REGION` | `us-east-2` |

```bash
dotnet run -- integration results.json
dotnet run -- load results.json 5000 100 1000     # members, matches per member, viewers
dotnet run -- latency results.json 5000           # after load, on the same tables
```

Long runs need a host that does not sleep; the recorded runs used a temporary in-region
instance (see the change's `verification.md`). Delete every temporary resource afterwards.
