import argparse
import csv
from datetime import datetime

import boto3
import ulid

TABLE_NAME = "PoolLayout"
REGION = "us-east-2"


def generate_ulid():
    return ulid.new().str


parser = argparse.ArgumentParser(description="Upload matches to DynamoDB")
parser.add_argument("--file", required=True, help="Path to the CSV file")
parser.add_argument("--poolLayoutId", required=True, help="Pool layout ID")

args = parser.parse_args()

csv_path = args.file
pool_layout_id = args.poolLayoutId


def get_dynamodb_client():
    return boto3.client("dynamodb", region_name=REGION)


dynamodb = get_dynamodb_client()

# Keep track of team names and their IDs
team_id_map = {}


def get_team_id(team_name: str) -> str:
    if team_name not in team_id_map:
        team_id_map[team_name] = generate_ulid()
    return team_id_map[team_name]


def upload_match_to_dynamodb(match_item: dict, local_team: str, away_team: str, match_date: str) -> None:
    response = dynamodb.put_item(TableName=TABLE_NAME, Item=match_item)
    print(f"Uploaded match {local_team} vs {away_team} on {match_date}")


with open(csv_path, newline='', encoding='utf-8') as csvfile:
    reader = csv.DictReader(csvfile)
    for row in reader:
        local_team = row["Local Team"]
        away_team = row["Away Team"]
        match_date = row["Date"]

        local_team_id = get_team_id(local_team)
        away_team_id = get_team_id(away_team)

        match_id = generate_ulid()

        match_datetime = datetime.strptime(match_date, "%Y-%m-%d").strftime("%Y-%m-%dT00:00:00.000Z")

        match_item = {
            "pk": {"S": f"POOLLAYOUT#{pool_layout_id}"},
            "sk": {"S": f"MATCH#{match_id}"},
            "id": {"S": pool_layout_id},
            "matchId": {"S": match_id},
            "matchDateTime": {"S": match_datetime},
            "localTeamName": {"S": local_team},
            "awayTeamName": {"S": away_team},
            "localTeamId": {"S": local_team_id},
            "awayTeamId": {"S": away_team_id}
        }

        upload_match_to_dynamodb(match_item, local_team, away_team, match_date)
