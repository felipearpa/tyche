FROM mcr.microsoft.com/dotnet/aspnet:6.0 AS base
WORKDIR /app
EXPOSE 80
EXPOSE 443

FROM mcr.microsoft.com/dotnet/sdk:6.0 AS build
WORKDIR /src
COPY ["Pippel.Tyche.PoolLayout.Api.fsproj", "./"]
RUN dotnet restore "Pippel.Tyche.PoolLayout.Api.fsproj"
COPY . .
WORKDIR "/src/"
RUN dotnet build "Pippel.Tyche.PoolLayout.Api.fsproj" -c Release -o /app/build

FROM build AS publish
RUN dotnet publish "Pippel.Tyche.PoolLayout.Api.fsproj" -c Release -o /app/publish

FROM base AS final
WORKDIR /app
COPY --from=publish /app/publish .
ENTRYPOINT ["dotnet", "Pippel.Tyche.PoolLayout.Api.dll"]
