#!/bin/bash

# Go to the script's directory
cd "$(dirname "$0")" || {
  echo "❌ Could not find script directory."
  exit 1
}

# Run the tests
echo "🧪 Running tests..."
dotnet test --nologo --verbosity minimal
if [ $? -ne 0 ]; then
  echo "❌ Tests failed. Deployment aborted."
  exit 1
fi

# Go to the deployment project folder
cd Felipearpa.Tyche.AmazonLambda/src || {
  echo "❌ Could not navigate to deployment project directory."
  exit 1
}

# Deploy the Lambda
echo "🚀 Running: dotnet lambda deploy-serverless"
dotnet lambda deploy-serverless
