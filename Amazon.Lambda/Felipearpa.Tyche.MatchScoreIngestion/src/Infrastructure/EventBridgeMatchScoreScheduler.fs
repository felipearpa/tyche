namespace Felipearpa.Tyche.MatchScoreIngestion.Infrastructure

#nowarn "3536"

open System
open Amazon.Scheduler
open Amazon.Scheduler.Model
open Felipearpa.Tyche.MatchScoreIngestion.Domain
open Felipearpa.Type

/// Creates and deletes one EventBridge Scheduler schedule per match. The schedule starts
/// 105 minutes after kickoff, fires every 5 minutes, and is capped by an end date as a safety net.
/// Termination on completion is driven by the control table stream (this just deletes on request).
type EventBridgeMatchScoreScheduler
    (client: IAmazonScheduler, targetFunctionArn: string, schedulerRoleArn: string) =

    [<Literal>]
    let warmupMinutes = 105.0

    [<Literal>]
    let pollMinutes = 5

    [<Literal>]
    let endCapHours = 4.0

    let scheduleName (matchId: Ulid) = $"match-score-{matchId.Value}"

    let buildInput (schedule: MatchScoreSchedule) =
        let escape (value: string) = value.Replace("\"", "\\\"")
        let matchDateTime = schedule.MatchDateTime.ToUniversalTime().ToString("O")

        $"{{\"matchId\":\"{schedule.MatchId.Value}\",\"poolLayoutId\":\"{schedule.PoolLayoutId.Value}\",\"externalMatchId\":\"{escape schedule.ExternalMatchId}\",\"matchDateTime\":\"{matchDateTime}\"}}"

    interface IMatchScoreScheduler with
        member _.CreateScheduleAsync(schedule: MatchScoreSchedule) =
            async {
                let now = DateTime.UtcNow
                let kickoff = schedule.MatchDateTime.ToUniversalTime()
                let warmupStart = kickoff.AddMinutes(warmupMinutes)
                let startDate = if warmupStart > now then warmupStart else now.AddMinutes(1.0)
                let endDate = kickoff.AddHours(endCapHours)

                let target = Target(Arn = targetFunctionArn, RoleArn = schedulerRoleArn, Input = buildInput schedule)

                let request =
                    CreateScheduleRequest(
                        Name = scheduleName schedule.MatchId,
                        ScheduleExpression = $"rate({pollMinutes} minutes)",
                        ScheduleExpressionTimezone = "UTC",
                        StartDate = startDate,
                        EndDate = endDate,
                        FlexibleTimeWindow = FlexibleTimeWindow(Mode = FlexibleTimeWindowMode.OFF),
                        ActionAfterCompletion = ActionAfterCompletion.DELETE,
                        State = ScheduleState.ENABLED,
                        Target = target
                    )

                try
                    let! _ = client.CreateScheduleAsync(request) |> Async.AwaitTask
                    return ()
                with
                | :? ConflictException -> return ()
                | :? AggregateException as error when (error.InnerException :? ConflictException) -> return ()
            }

        member _.DeleteScheduleAsync(matchId: Ulid) =
            async {
                let request = DeleteScheduleRequest(Name = scheduleName matchId)

                try
                    let! _ = client.DeleteScheduleAsync(request) |> Async.AwaitTask
                    return ()
                with
                | :? ResourceNotFoundException -> return ()
                | :? AggregateException as error when (error.InnerException :? ResourceNotFoundException) -> return ()
            }
