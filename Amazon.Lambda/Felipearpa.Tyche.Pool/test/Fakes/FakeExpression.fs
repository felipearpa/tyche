namespace Felipearpa.Tyche.Pool.Test.Fakes

open System
open System.Collections.Generic
open System.Globalization
open Amazon.DynamoDBv2
open Amazon.DynamoDBv2.Model

/// A small DynamoDB expression interpreter covering the condition, key-condition, filter,
/// projection, and update grammar the Pool repositories use. It exists so tests can exercise
/// real request builders against stateful conditional semantics instead of asserting on
/// request text.
module FakeExpression =

    type PathElement =
        | Attr of string
        | Index of int

    type Operand =
        | Path of PathElement list
        | Value of string
        | IfNotExists of PathElement list * Operand
        | Plus of Operand * Operand
        | Minus of Operand * Operand
        | Size of PathElement list

    type Condition =
        | And of Condition * Condition
        | Or of Condition * Condition
        | Not of Condition
        | Compare of string * Operand * Operand
        | Between of Operand * Operand * Operand
        | In of Operand * Operand list
        | Exists of PathElement list
        | NotExists of PathElement list
        | BeginsWith of Operand * Operand
        | Contains of Operand * Operand

    type UpdateAction =
        | SetAction of PathElement list * Operand
        | RemoveAction of PathElement list
        | AddAction of PathElement list * Operand
        | DeleteAction of PathElement list * Operand

    type private Token =
        | Ident of string
        | NameRef of string
        | ValueRef of string
        | Number of int
        | Sym of string

    let private tokenize (text: string) =
        let isIdentChar (c: char) = Char.IsLetterOrDigit c || c = '_'

        let rec loop index (acc: Token list) =
            if index >= text.Length then
                List.rev acc
            else
                let c = text[index]

                if Char.IsWhiteSpace c then
                    loop (index + 1) acc
                elif c = '#' || c = ':' then
                    let finish =
                        Seq.initInfinite (fun offset -> index + 1 + offset)
                        |> Seq.find (fun i -> i >= text.Length || not (isIdentChar text[i]))

                    let token =
                        if c = '#' then
                            NameRef(text.Substring(index, finish - index))
                        else
                            ValueRef(text.Substring(index, finish - index))

                    loop finish (token :: acc)
                elif Char.IsDigit c then
                    let finish =
                        Seq.initInfinite (fun offset -> index + offset)
                        |> Seq.find (fun i -> i >= text.Length || not (Char.IsDigit text[i]))

                    loop finish (Number(int (text.Substring(index, finish - index))) :: acc)
                elif isIdentChar c then
                    let finish =
                        Seq.initInfinite (fun offset -> index + offset)
                        |> Seq.find (fun i -> i >= text.Length || not (isIdentChar text[i]))

                    loop finish (Ident(text.Substring(index, finish - index)) :: acc)
                else
                    let two = if index + 1 < text.Length then text.Substring(index, 2) else ""

                    if two = "<=" || two = ">=" || two = "<>" then
                        loop (index + 2) (Sym two :: acc)
                    else
                        loop (index + 1) (Sym(string c) :: acc)

        loop 0 []

    let private isKeyword (word: string) (token: Token) =
        match token with
        | Ident value -> String.Equals(value, word, StringComparison.OrdinalIgnoreCase)
        | _ -> false

    let private resolveName (names: IDictionary<string, string>) (token: Token) =
        match token with
        | NameRef reference ->
            match names |> Option.ofObj |> Option.bind (fun n -> match n.TryGetValue reference with | true, v -> Some v | _ -> None) with
            | Some name -> name
            | None -> failwith $"Missing expression attribute name {reference}"
        | Ident name -> name
        | other -> failwith $"Unexpected token {other} in path"

    let rec private parsePath names (tokens: Token list) =
        let first = resolveName names tokens.Head

        let rec rest (acc: PathElement list) (remaining: Token list) =
            match remaining with
            | Sym "." :: name :: tail -> rest (Attr(resolveName names name) :: acc) tail
            | Sym "[" :: Number index :: Sym "]" :: tail -> rest (Index index :: acc) tail
            | _ -> List.rev acc, remaining

        rest [ Attr first ] tokens.Tail

    let rec private parseOperand names (tokens: Token list) : Operand * Token list =
        let single, remaining = parseSingleOperand names tokens

        match remaining with
        | Sym "+" :: tail ->
            let right, rest = parseSingleOperand names tail
            Plus(single, right), rest
        | Sym "-" :: tail ->
            let right, rest = parseSingleOperand names tail
            Minus(single, right), rest
        | _ -> single, remaining

    and private parseSingleOperand names (tokens: Token list) : Operand * Token list =
        match tokens with
        | ValueRef reference :: tail -> Value reference, tail
        | token :: Sym "(" :: tail when isKeyword "if_not_exists" token ->
            let path, afterPath = parsePath names tail

            match afterPath with
            | Sym "," :: afterComma ->
                let fallback, afterFallback = parseOperand names afterComma

                match afterFallback with
                | Sym ")" :: rest -> IfNotExists(path, fallback), rest
                | _ -> failwith "Malformed if_not_exists"
            | _ -> failwith "Malformed if_not_exists"
        | token :: Sym "(" :: tail when isKeyword "size" token ->
            let path, afterPath = parsePath names tail

            match afterPath with
            | Sym ")" :: rest -> Size path, rest
            | _ -> failwith "Malformed size"
        | _ ->
            let path, rest = parsePath names tokens
            Path path, rest

    let rec private parseOr names tokens =
        let left, remaining = parseAnd names tokens

        match remaining with
        | token :: tail when isKeyword "OR" token ->
            let right, rest = parseOr names tail
            Or(left, right), rest
        | _ -> left, remaining

    and private parseAnd names tokens =
        let left, remaining = parseNot names tokens

        match remaining with
        | token :: tail when isKeyword "AND" token ->
            let right, rest = parseAnd names tail
            And(left, right), rest
        | _ -> left, remaining

    and private parseNot names tokens =
        match tokens with
        | token :: tail when isKeyword "NOT" token ->
            let inner, rest = parseNot names tail
            Not inner, rest
        | _ -> parsePrimary names tokens

    and private parsePrimary names tokens =
        let functionCall name tail =
            match name with
            | "attribute_exists"
            | "attribute_not_exists" ->
                let path, afterPath = parsePath names tail

                match afterPath with
                | Sym ")" :: rest -> (if name = "attribute_exists" then Exists path else NotExists path), rest
                | _ -> failwith $"Malformed {name}"
            | "begins_with"
            | "contains" ->
                let first, afterFirst = parseOperand names tail

                match afterFirst with
                | Sym "," :: afterComma ->
                    let second, afterSecond = parseOperand names afterComma

                    match afterSecond with
                    | Sym ")" :: rest ->
                        (if name = "begins_with" then BeginsWith(first, second) else Contains(first, second)), rest
                    | _ -> failwith $"Malformed {name}"
                | _ -> failwith $"Malformed {name}"
            | other -> failwith $"Unsupported function {other}"

        match tokens with
        | Sym "(" :: tail ->
            let inner, afterInner = parseOr names tail

            match afterInner with
            | Sym ")" :: rest -> inner, rest
            | _ -> failwith "Unbalanced parenthesis"
        | Ident name :: Sym "(" :: tail when
            [ "attribute_exists"; "attribute_not_exists"; "begins_with"; "contains" ]
            |> List.contains (name.ToLowerInvariant())
            ->
            functionCall (name.ToLowerInvariant()) tail
        | _ ->
            let left, remaining = parseOperand names tokens

            match remaining with
            | Sym op :: tail when [ "="; "<>"; "<"; "<="; ">"; ">=" ] |> List.contains op ->
                let right, rest = parseOperand names tail
                Compare(op, left, right), rest
            | token :: tail when isKeyword "BETWEEN" token ->
                let low, afterLow = parseOperand names tail

                match afterLow with
                | andToken :: afterAnd when isKeyword "AND" andToken ->
                    let high, rest = parseOperand names afterAnd
                    Between(left, low, high), rest
                | _ -> failwith "Malformed BETWEEN"
            | token :: Sym "(" :: tail when isKeyword "IN" token ->
                let rec values acc remaining =
                    let value, afterValue = parseOperand names remaining

                    match afterValue with
                    | Sym "," :: next -> values (value :: acc) next
                    | Sym ")" :: rest -> List.rev (value :: acc), rest
                    | _ -> failwith "Malformed IN"

                let list, rest = values [] tail
                In(left, list), rest
            | _ -> failwith $"Unsupported condition near {remaining}"

    let parseCondition (names: IDictionary<string, string>) (text: string) : Condition =
        match parseOr names (tokenize text) with
        | condition, [] -> condition
        | _, leftover -> failwith $"Unparsed condition tokens {leftover}"

    let parseUpdate (names: IDictionary<string, string>) (text: string) : UpdateAction list =
        let isClause token =
            [ "SET"; "REMOVE"; "ADD"; "DELETE" ] |> List.exists (fun word -> isKeyword word token)

        let rec clauses (acc: UpdateAction list) (tokens: Token list) =
            match tokens with
            | [] -> List.rev acc
            | clause :: tail when isKeyword "SET" clause ->
                let rec assignments acc remaining =
                    let path, afterPath = parsePath names remaining

                    match afterPath with
                    | Sym "=" :: afterEquals ->
                        let operand, afterOperand = parseOperand names afterEquals
                        let acc = SetAction(path, operand) :: acc

                        match afterOperand with
                        | Sym "," :: next -> assignments acc next
                        | _ -> acc, afterOperand
                    | _ -> failwith "Malformed SET"

                let acc, rest = assignments acc tail
                clauses acc rest
            | clause :: tail when isKeyword "REMOVE" clause ->
                let rec removals acc remaining =
                    let path, afterPath = parsePath names remaining
                    let acc = RemoveAction path :: acc

                    match afterPath with
                    | Sym "," :: next -> removals acc next
                    | _ -> acc, afterPath

                let acc, rest = removals acc tail
                clauses acc rest
            | clause :: tail when isKeyword "ADD" clause || isKeyword "DELETE" clause ->
                let isAdd = isKeyword "ADD" clause

                let rec pairs acc remaining =
                    let path, afterPath = parsePath names remaining
                    let operand, afterOperand = parseSingleOperand names afterPath
                    let action = if isAdd then AddAction(path, operand) else DeleteAction(path, operand)

                    match afterOperand with
                    | Sym "," :: next when not (next.IsEmpty || isClause next.Head) -> pairs (action :: acc) next
                    | _ -> action :: acc, afterOperand

                let acc, rest = pairs acc tail
                clauses acc rest
            | other -> failwith $"Unsupported update clause near {other}"

        clauses [] (tokenize text)

    let parseProjection (names: IDictionary<string, string>) (text: string) : string list =
        tokenize text
        |> List.choose (fun token ->
            match token with
            | Sym "," -> None
            | NameRef _
            | Ident _ -> Some(resolveName names token)
            | other -> failwith $"Unsupported projection token {other}")

    let rec cloneValue (value: AttributeValue) : AttributeValue =
        let copy = AttributeValue()

        if not (isNull value.S) then copy.S <- value.S
        if not (isNull value.N) then copy.N <- value.N
        if value.BOOL.HasValue then copy.BOOL <- value.BOOL
        if value.NULL.HasValue then copy.NULL <- value.NULL

        if not (isNull value.M) then
            copy.M <- Dictionary(value.M |> Seq.map (fun pair -> KeyValuePair(pair.Key, cloneValue pair.Value)))

        if not (isNull value.L) then
            copy.L <- ResizeArray(value.L |> Seq.map cloneValue)

        if not (isNull value.SS) then copy.SS <- ResizeArray(value.SS)
        if not (isNull value.NS) then copy.NS <- ResizeArray(value.NS)
        copy

    let rec valueEquals (left: AttributeValue) (right: AttributeValue) =
        left.S = right.S
        && left.N = right.N
        && left.BOOL = right.BOOL
        && left.NULL = right.NULL
        && (match left.M, right.M with
            | null, null -> true
            | null, _
            | _, null -> false
            | l, r ->
                l.Count = r.Count
                && l |> Seq.forall (fun pair -> r.ContainsKey pair.Key && valueEquals pair.Value r[pair.Key]))
        && (match left.L, right.L with
            | null, null -> true
            | null, _
            | _, null -> false
            | l, r -> l.Count = r.Count && Seq.forall2 valueEquals l r)

    let cloneItem (item: IDictionary<string, AttributeValue>) : Dictionary<string, AttributeValue> =
        Dictionary(item |> Seq.map (fun pair -> KeyValuePair(pair.Key, cloneValue pair.Value)))

    let private parseNumber (text: string) =
        Decimal.Parse(text, NumberStyles.Float, CultureInfo.InvariantCulture)

    let private formatNumber (value: decimal) =
        value.ToString(CultureInfo.InvariantCulture)

    let rec private tryGetPath (item: IDictionary<string, AttributeValue>) (path: PathElement list) =
        match path with
        | [ Attr name ] ->
            match item.TryGetValue name with
            | true, value -> Some value
            | _ -> None
        | Attr name :: rest ->
            match item.TryGetValue name with
            | true, value -> tryGetNested value rest
            | _ -> None
        | _ -> None

    and private tryGetNested (value: AttributeValue) (path: PathElement list) =
        match path with
        | [] -> Some value
        | Attr name :: rest when not (isNull value.M) ->
            match value.M.TryGetValue name with
            | true, inner -> tryGetNested inner rest
            | _ -> None
        | Index index :: rest when not (isNull value.L) && index < value.L.Count -> tryGetNested value.L[index] rest
        | _ -> None

    let rec private setPath (item: IDictionary<string, AttributeValue>) (path: PathElement list) (value: AttributeValue) =
        match path with
        | [ Attr name ] -> item[name] <- value
        | Attr name :: rest ->
            match item.TryGetValue name with
            | true, parent when not (isNull parent.M) -> setPath parent.M rest value
            | _ -> raise (AmazonDynamoDBException "The document path provided in the update expression is invalid for update")
        | _ -> failwith "Unsupported path"

    let rec private removePath (item: IDictionary<string, AttributeValue>) (path: PathElement list) =
        match path with
        | [ Attr name ] -> item.Remove name |> ignore
        | Attr name :: rest ->
            match item.TryGetValue name with
            | true, parent when not (isNull parent.M) -> removePath parent.M rest
            | _ -> ()
        | _ -> failwith "Unsupported path"

    let private lookupValue (values: IDictionary<string, AttributeValue>) (reference: string) =
        match values |> Option.ofObj |> Option.bind (fun v -> match v.TryGetValue reference with | true, x -> Some x | _ -> None) with
        | Some value -> value
        | None -> failwith $"Missing expression attribute value {reference}"

    let rec private evaluateOperand values (item: IDictionary<string, AttributeValue>) (operand: Operand) : AttributeValue option =
        match operand with
        | Value reference -> Some(lookupValue values reference)
        | Path path -> tryGetPath item path
        | IfNotExists(path, fallback) ->
            match tryGetPath item path with
            | Some existing -> Some existing
            | None -> evaluateOperand values item fallback
        | Plus(left, right)
        | Minus(left, right) ->
            match evaluateOperand values item left, evaluateOperand values item right with
            | Some l, Some r when not (isNull l.N) && not (isNull r.N) ->
                let result =
                    match operand with
                    | Plus _ -> parseNumber l.N + parseNumber r.N
                    | _ -> parseNumber l.N - parseNumber r.N

                Some(AttributeValue(N = formatNumber result))
            | _ ->
                raise (
                    AmazonDynamoDBException
                        "An operand in the update expression has an incorrect data type or does not exist"
                )
        | Size path ->
            tryGetPath item path
            |> Option.map (fun value ->
                let size =
                    if not (isNull value.S) then value.S.Length
                    elif not (isNull value.L) then value.L.Count
                    elif not (isNull value.M) then value.M.Count
                    else 0

                AttributeValue(N = string size))

    let private compareValues (left: AttributeValue) (right: AttributeValue) : int option =
        if not (isNull left.N) && not (isNull right.N) then
            Some(compare (parseNumber left.N) (parseNumber right.N))
        elif not (isNull left.S) && not (isNull right.S) then
            Some(String.CompareOrdinal(left.S, right.S) |> sign)
        elif left.BOOL.HasValue && right.BOOL.HasValue then
            Some(compare left.BOOL.Value right.BOOL.Value)
        else
            None

    let rec evaluateCondition values (item: IDictionary<string, AttributeValue>) (condition: Condition) : bool =
        match condition with
        | And(left, right) -> evaluateCondition values item left && evaluateCondition values item right
        | Or(left, right) -> evaluateCondition values item left || evaluateCondition values item right
        | Not inner -> not (evaluateCondition values item inner)
        | Exists path -> tryGetPath item path |> Option.isSome
        | NotExists path -> tryGetPath item path |> Option.isNone
        | Compare(op, left, right) ->
            match evaluateOperand values item left, evaluateOperand values item right with
            | Some l, Some r ->
                match compareValues l r with
                | Some result ->
                    match op with
                    | "=" -> result = 0
                    | "<>" -> result <> 0
                    | "<" -> result < 0
                    | "<=" -> result <= 0
                    | ">" -> result > 0
                    | ">=" -> result >= 0
                    | _ -> false
                | None -> op = "<>"
            | None, Some _
            | Some _, None -> op = "<>"
            | None, None -> false
        | Between(target, low, high) ->
            evaluateCondition values item (Compare(">=", target, low))
            && evaluateCondition values item (Compare("<=", target, high))
        | In(target, candidates) ->
            candidates
            |> List.exists (fun candidate -> evaluateCondition values item (Compare("=", target, candidate)))
        | BeginsWith(target, prefix) ->
            match evaluateOperand values item target, evaluateOperand values item prefix with
            | Some t, Some p when not (isNull t.S) && not (isNull p.S) -> t.S.StartsWith(p.S, StringComparison.Ordinal)
            | _ -> false
        | Contains(target, operand) ->
            match evaluateOperand values item target, evaluateOperand values item operand with
            | Some t, Some o when not (isNull t.S) && not (isNull o.S) -> t.S.Contains(o.S, StringComparison.Ordinal)
            | Some t, Some o when not (isNull t.SS) && not (isNull o.S) -> t.SS.Contains o.S
            | _ -> false

    let applyUpdate values (item: Dictionary<string, AttributeValue>) (actions: UpdateAction list) =
        // DynamoDB evaluates every right-hand side against the pre-update item.
        let original = cloneItem item

        let resolved =
            actions
            |> List.map (fun action ->
                match action with
                | SetAction(path, operand) ->
                    match evaluateOperand values original operand with
                    | Some value -> SetAction(path, Value "__resolved"), Some(cloneValue value)
                    | None ->
                        raise (
                            AmazonDynamoDBException
                                "The provided expression refers to an attribute that does not exist in the item"
                        )
                | AddAction(path, operand) ->
                    let increment = evaluateOperand values original operand |> Option.get

                    let next =
                        match tryGetPath original path with
                        | Some existing when not (isNull existing.N) ->
                            AttributeValue(N = formatNumber (parseNumber existing.N + parseNumber increment.N))
                        | Some existing when not (isNull existing.SS) ->
                            AttributeValue(SS = ResizeArray(Seq.append existing.SS increment.SS |> Seq.distinct))
                        | _ -> cloneValue increment

                    SetAction(path, Value "__resolved"), Some next
                | other -> other, None)

        for action, resolvedValue in resolved do
            match action, resolvedValue with
            | SetAction(path, _), Some value -> setPath item path value
            | RemoveAction path, _ -> removePath item path
            | DeleteAction _, _ -> failwith "DELETE clause is not supported by the fake"
            | _ -> ()

    let project (attributes: string list) (item: IDictionary<string, AttributeValue>) =
        Dictionary(
            item
            |> Seq.filter (fun pair -> attributes |> List.contains pair.Key)
            |> Seq.map (fun pair -> KeyValuePair(pair.Key, cloneValue pair.Value))
        )
