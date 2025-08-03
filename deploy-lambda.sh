#!/bin/bash

# Navigate to the folder containing the .yaml file
cd Amazon.Lambda/Felipearpa.Tyche.AmazonLambda/src || {
  echo "❌ Could not find target directory."
  exit 1
}

# Run the deployment
echo "🚀 Running: dotnet lambda deploy-serverless"
dotnet lambda deploy-serverless
