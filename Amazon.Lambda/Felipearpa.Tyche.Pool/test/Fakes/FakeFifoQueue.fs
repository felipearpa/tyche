namespace Felipearpa.Tyche.Pool.Test.Fakes

type FakeQueueMessage =
    { Body: string
      GroupId: string
      DeduplicationId: string
      ReceiveCount: int }

/// An in-memory FIFO queue boundary with per-group ordering, deduplication, receive counts,
/// a redrive limit into a dead-letter list, and injectable send failures.
type FakeFifoQueue(maxReceiveCount: int) =
    let gate = obj ()
    let mutable messages: FakeQueueMessage list = []
    let mutable deadLetters: FakeQueueMessage list = []
    let mutable seenDeduplicationIds: Set<string> = Set.empty
    let mutable failSend: FakeQueueMessage -> exn option = fun _ -> None

    member _.Send(body: string, groupId: string, deduplicationId: string) =
        lock gate (fun () ->
            let message =
                { Body = body
                  GroupId = groupId
                  DeduplicationId = deduplicationId
                  ReceiveCount = 0 }

            match failSend message with
            | Some error -> raise error
            | None ->
                if not (seenDeduplicationIds.Contains deduplicationId) then
                    seenDeduplicationIds <- seenDeduplicationIds.Add deduplicationId
                    messages <- messages @ [ message ])

    /// Fails sends matching the predicate until cleared.
    member _.FailSendWhen(predicate: FakeQueueMessage -> exn option) = lock gate (fun () -> failSend <- predicate)

    member _.ClearSendFailures() = lock gate (fun () -> failSend <- fun _ -> None)

    /// Forgets deduplication history, as after the five-minute SQS deduplication window.
    member _.ExpireDeduplication() = lock gate (fun () -> seenDeduplicationIds <- Set.empty)

    member _.Pending = lock gate (fun () -> messages)

    member _.DeadLetters = lock gate (fun () -> deadLetters)

    /// Delivers the head message of each group once. A handler failure keeps the message at
    /// the head of its group and increments its receive count; exhausting the redrive limit
    /// moves it to the dead-letter list.
    member this.DeliverRound(handler: FakeQueueMessage -> Async<unit>) =
        async {
            let heads =
                lock gate (fun () -> messages |> List.distinctBy _.GroupId)

            for head in heads do
                let delivered = { head with ReceiveCount = head.ReceiveCount + 1 }
                let! outcome = handler delivered |> Async.Catch

                lock gate (fun () ->
                    let others = messages |> List.filter (fun message -> not (obj.ReferenceEquals(message, head)))

                    match outcome with
                    | Choice1Of2() -> messages <- others
                    | Choice2Of2 _ when delivered.ReceiveCount >= maxReceiveCount ->
                        messages <- others
                        deadLetters <- deadLetters @ [ delivered ]
                    | Choice2Of2 _ ->
                        let index = messages |> List.findIndex (fun message -> obj.ReferenceEquals(message, head))
                        messages <- messages |> List.mapi (fun i message -> if i = index then delivered else message))
        }

    /// Delivers rounds until the queue is empty or the round limit is reached.
    member this.Drain(handler: FakeQueueMessage -> Async<unit>, ?maxRounds: int) =
        let limit = defaultArg maxRounds 50

        let rec loop round =
            async {
                if round < limit && not (lock gate (fun () -> messages.IsEmpty)) then
                    do! this.DeliverRound handler
                    return! loop (round + 1)
            }

        loop 0

    /// Moves dead letters back to the queue, as an operator redrive would.
    member _.Redrive() =
        lock gate (fun () ->
            messages <- messages @ (deadLetters |> List.map (fun message -> { message with ReceiveCount = 0 }))
            deadLetters <- [])
