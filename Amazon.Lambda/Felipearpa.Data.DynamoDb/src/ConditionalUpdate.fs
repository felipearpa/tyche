namespace Felipearpa.Data.DynamoDb

open Amazon.DynamoDBv2.Model

module ConditionalUpdate =

    let ignoreTransactionConflict (action: Async<unit>) : Async<unit> =
        async {
            try
                do! action
            with :? System.AggregateException as error when
                (error.InnerException :? ConditionalCheckFailedException
                 || error.InnerException :? TransactionCanceledException) ->
                ()
        }
