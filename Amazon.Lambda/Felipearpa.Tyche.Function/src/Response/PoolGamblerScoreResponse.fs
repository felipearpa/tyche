namespace Felipearpa.Tyche.Function.Response


type PoolGamblerScoreResponse =
    { PoolId: string
      PoolName: string
      GamblerId: string
      GamblerUsername: string
      Position: int option
      BeforePosition: int option
      Score: int option
      BeforeScore: int option }
